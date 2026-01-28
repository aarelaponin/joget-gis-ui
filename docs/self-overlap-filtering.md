# Self-Overlap Filtering in Edit Mode

## Overview

When editing an existing parcel, the overlap detection API correctly returns the original parcel as an "overlap" because the modified geometry intersects with the stored geometry in the database. This creates **false positive warnings** that confuse users.

This document describes the client-side filtering logic that removes these self-overlaps while preserving legitimate overlap warnings for other parcels.

> **IMPORTANT**: This logic is critical for a good user experience. Any changes to the overlap filtering code in `gis-capture.js` should be tested against all three scenarios described below.

---

## The Problem

### Scenario: User edits their own parcel

1. User opens form to edit parcel `ABC-123` (stored area: 74 ha)
2. User drags a vertex to adjust the boundary
3. System calls `POST /gis/checkOverlap` with the new geometry
4. Server finds overlap with parcel `ABC-123` (the same parcel being edited!)
5. **Without filtering**: User sees "Overlap Detected" warning for their own parcel

This happens in three sub-scenarios:
- **Shrinking**: User drags vertex inward → new polygon fits inside original
- **Same size**: User moves vertices but area stays similar
- **Expanding**: User drags vertex outward → original fits inside new polygon

---

## The Solution: Three-Strategy Filter

Located in `gis-capture.js` around line 2960, inside the overlap response handler.

### Strategy 1: Shrunk Polygon Detection

**Condition**: `overlapPercentage >= 95% AND initialArea > currentArea`

**Scenario**: User shrinks the parcel by dragging vertices inward.

```
Original (74 ha):  ┌─────────────┐
                   │             │
                   │             │
                   └─────────────┘

New (60 ha):       ┌─────────┐
                   │         │
                   └─────────┘

Overlap: 100% of new polygon (it fits entirely inside original)
```

**Logic**: When the polygon is shrunk, the entire new polygon overlaps with the original stored geometry. The overlap percentage will be ~100%. We detect this by checking if `initialArea > currentArea`.

```javascript
if (overlap.overlapPercentage >= 95.0 && initialArea && initialArea > currentArea) {
    // Polygon was shrunk - the new smaller polygon is inside the original
    return false; // Filter out this overlap
}
```

### Strategy 2: Same-Size Polygon Detection

**Condition**: `overlapPercentage >= 99% AND |overlapArea - currentArea| < 2%`

**Scenario**: User adjusts the polygon shape but overall area stays similar.

```
Original (74 ha):  ┌─────────────┐
                   │             │
                   └─────────────┘

New (74 ha):       ┌───────────────┐
                   │               │
                   └───────────────┘
                   (shifted slightly)

Overlap: ~99% with overlap area ≈ current area
```

**Logic**: When the overlap area is essentially equal to the current polygon area, it's the same parcel with minor adjustments.

```javascript
if (overlap.overlapPercentage >= 99.0) {
    var areaDiff = Math.abs(overlap.overlapArea - currentArea);
    var areaThreshold = currentArea * 0.02; // 2% tolerance
    if (areaDiff <= areaThreshold) {
        return false; // Filter out this overlap
    }
}
```

### Strategy 3: Expanded Polygon Detection (Spatial Containment)

**Condition**: `currentArea > initialArea AND turf.booleanContains(current, initial) AND |overlapArea - initialArea| < 10%`

**Scenario**: User expands the parcel by dragging vertices outward.

```
Original (74 ha):  ┌─────────────┐
                   │             │
                   └─────────────┘

New (100 ha):      ┌───────────────────┐
                   │   ┌─────────┐     │
                   │   │ original│     │
                   │   └─────────┘     │
                   └───────────────────┘

Overlap: ~74% (the original area is inside the expanded polygon)
         overlapArea ≈ initialArea (74 ha)
```

**Logic**: When the polygon is expanded:
1. The overlap percentage will be < 100% (e.g., 74%)
2. The overlap area will approximately equal the **initial** area (not current)
3. The expanded polygon should **contain** the original geometry

We use `turf.booleanContains()` for a robust spatial check:

```javascript
if (isEditMode && self.state.initialGeometry && currentArea > initialArea) {
    try {
        var currentGeojson = self._toGeoJSON();
        var initialGeojson = self.state.initialGeometry;

        // Check if current polygon fully contains the original
        if (turf.booleanContains(currentGeojson, initialGeojson)) {
            var areaDiffFromInitial = Math.abs(overlap.overlapArea - initialArea);
            var initialThreshold = initialArea * 0.10; // 10% tolerance
            if (areaDiffFromInitial <= initialThreshold) {
                return false; // Filter out this overlap
            }
        }
    } catch (e) {
        // Fallback to simple area comparison with tighter tolerance
        var areaDiffFromInitial = Math.abs(overlap.overlapArea - initialArea);
        var initialThreshold = initialArea * 0.05; // 5% tolerance
        if (areaDiffFromInitial <= initialThreshold) {
            return false; // Filter out this overlap
        }
    }
}
```

**Why spatial containment?**
- More robust than pure area comparison
- Handles edge cases where another parcel coincidentally has similar area
- Uses Turf.js which is already loaded (no new dependencies)
- Semantically correct: "does the expanded polygon contain the original?"

---

## State Requirements

For the filter to work, the following state must be captured when entering edit mode:

| State Variable | Description | Set In |
|---------------|-------------|--------|
| `state.initialGeometry` | Original GeoJSON from database | `_loadExistingValue()` |
| `state.initialAreaHectares` | Original area in hectares | `_loadExistingValue()` |
| `options.recordId` | The record ID being edited | Passed from template |

These are set when loading an existing value:

```javascript
// In _loadExistingValue():
self.state.initialGeometry = geojson;
self.state.initialAreaHectares = metrics.areaHectares;
```

---

## Console Logging

The filter logs its decisions for debugging:

```
[GIS] Self-overlap filter check:
[GIS]   isEditMode=true (recordId=eea39e76-016c-4a4a-8c54-3688f0be5029)
[GIS]   initialArea=74.0778 ha
[GIS]   currentArea=88.8800 ha
[GIS]   Polygon state: EXPANDED
[GIS] Checking overlap: id=078122ec-..., percentage=83.3%, overlapArea=73.9121 ha
[GIS] Self-overlap filter: expanded polygon contains original geometry, overlap area (73.9121 ha) ≈ initial area (74.0778 ha), filtering out
[GIS] Self-overlap filter removed 1 overlap(s)
```

---

## Testing Checklist

When modifying overlap-related code, verify all scenarios:

### Shrink Scenario
- [ ] Open existing parcel in edit mode
- [ ] Drag vertex **inward** to reduce area
- [ ] Verify NO "Overlap Detected" warning for same parcel
- [ ] Console shows: "Polygon was shrunk... filtering as self-overlap"

### Same-Size Scenario
- [ ] Open existing parcel in edit mode
- [ ] Slightly adjust vertices without significant area change
- [ ] Verify NO "Overlap Detected" warning for same parcel
- [ ] Console shows: "Overlap area ≈ current area - filtering as self-overlap"

### Expand Scenario
- [ ] Open existing parcel in edit mode
- [ ] Drag vertex **outward** to increase area
- [ ] Verify NO "Overlap Detected" warning for same parcel
- [ ] Console shows: "expanded polygon contains original geometry... filtering out"

### Legitimate Overlap (Control Test)
- [ ] Create new parcel that overlaps an existing different parcel
- [ ] Verify "Overlap Detected" warning IS shown
- [ ] Verify the overlapping parcel details are correct

---

## Tolerance Values

| Strategy | Tolerance | Rationale |
|----------|-----------|-----------|
| Strategy 1 (Shrunk) | 95% overlap | Allows for minor GPS/editing variance |
| Strategy 2 (Same-size) | 99% overlap, 2% area diff | Very tight - only exact matches |
| Strategy 3 (Expanded, spatial) | 10% area diff from initial | Wider tolerance since spatial check is robust |
| Strategy 3 (Expanded, fallback) | 5% area diff from initial | Tighter without spatial verification |

---

## Edge Cases

### Multiple Overlaps
If the modified polygon overlaps with BOTH the original parcel AND a different parcel:
- Self-overlap with original parcel → filtered out
- Overlap with different parcel → kept, warning shown

### Invalid Geometry
If `turf.booleanContains()` fails (e.g., invalid geometry):
- Falls back to simple area comparison with tighter tolerance (5%)
- Logged as warning: "Spatial containment check failed"

### Missing Initial State
If `initialGeometry` or `initialAreaHectares` not set:
- Filter is skipped (defensive)
- All overlaps are shown (safe default)

---

## Related Files

| File | Relevant Lines | Description |
|------|---------------|-------------|
| `gis-capture.js` | ~2940-3020 | Self-overlap filter implementation |
| `gis-capture.js` | ~1150-1200 | `_loadExistingValue()` - sets initial state |
| `GisPolygonCaptureElement.java` | ~180 | Passes `recordId` to template |

---

## History

| Date | Change | Reason |
|------|--------|--------|
| 2026-01-28 | Added Strategy 3 (spatial containment) | Fix false positive when expanding polygons |
| 2026-01-XX | Added Strategy 1 & 2 | Initial self-overlap filtering for shrink/same-size |
