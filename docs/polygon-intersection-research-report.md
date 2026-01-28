# Polygon Self-Intersection Detection: Research Report

## Executive Summary

**Root Cause Identified**: The `sweepline-intersections` library fallback is likely not being invoked correctly due to a **global variable name mismatch**. Additionally, `turf.kinks()` has documented false negatives that affect certain polygon configurations.

**Status**: Your dual-algorithm approach is correct in principle, but implementation details need adjustment.

---

## Investigation Findings

### 1. Global Variable Name Mismatch (CRITICAL)

**Current Code** (lines 1954, 2058 in gis-capture.js):
```javascript
if (typeof sweeplineIntersections !== 'undefined') {
    var sweeplineResult = sweeplineIntersections(polygon.geometry);
```

**Problem**: The `sweepline-intersections` library exports as `findIntersections` in CommonJS/ES modules:
```javascript
const findIntersections = require('sweepline-intersections')
```

The UMD build may expose a different global name. The jsdelivr CDN serves `sweeplineIntersections.min.js`, but the **actual global variable name** may be `findIntersections` or `sweeplineIntersections` depending on the rollup configuration.

**Verification Needed**: Check browser console after page load:
```javascript
console.log('sweeplineIntersections:', typeof sweeplineIntersections);
console.log('findIntersections:', typeof findIntersections);
```

### 2. turf.kinks() Known False Negatives

**GitHub Issue #934** (September 2017): During refactoring of the kinks package to support LineStrings, the logic to prevent intersections of the start/end point was not properly updated. This causes false negatives for certain intersection patterns.

**GitHub Issue #1094** (November 2017): `turf.kinks()` may detect intersections that `turf.unkinkPolygon()` cannot split, indicating inconsistencies in the detection algorithm.

**Impact**: The bowtie test cases should work (simple diagonal crossings), but more complex self-intersections may be missed.

### 3. Your Self-Test Results

Your `_testSelfIntersection()` method tests:
- Bowtie: `[[[0, 0], [1, 1], [1, 0], [0, 1], [0, 0]]]` - Should find 1 intersection
- Square: `[[[0, 0], [1, 0], [1, 1], [0, 1], [0, 0]]]` - Should find 0 intersections
- Figure-8: `[[[0, 0], [2, 2], [2, 0], [0, 2], [0, 0]]]` - Should find 1 intersection
- Triangle: `[[[0, 0], [1, 0], [0.5, 1], [0, 0]]]` - Should find 0 intersections

**If turf.kinks() is failing these tests**, the issue is with the library itself or coordinate precision.

**If tests pass but real polygons fail**, the issue is:
- Geographic coordinate precision (6-8 decimal places for lat/lng)
- The sweepline fallback not being invoked

### 4. Coordinate System Considerations

Your code correctly converts Leaflet's `{lat, lng}` to GeoJSON's `[lng, lat]`:
```javascript
var coords = this.state.vertices.map(function(v) {
    return [v.lng, v.lat];  // Correct!
});
```

However, geographic coordinates have very small values (e.g., Lesotho is around `-29.5, 28.5`). The edge crossing calculations involve:
- Subtractions of nearly-equal numbers
- Division by very small denominators
- Potential floating-point precision issues

---

## Recommended Fixes

### Fix 1: Verify and Correct Global Variable Name

Add diagnostic logging to the FreeMarker template after script load:

```javascript
// In GisPolygonCaptureElement.ftl, after sweepline loads:
loadScript(
    'https://cdn.jsdelivr.net/npm/sweepline-intersections@2.0.1/dist/sweeplineIntersections.min.js',
    function() {
        // Debug: Check actual global name
        console.log('[GIS] Sweepline globals check:');
        console.log('  typeof sweeplineIntersections:', typeof sweeplineIntersections);
        console.log('  typeof findIntersections:', typeof findIntersections);
        console.log('  typeof window.sweeplineIntersections:', typeof window.sweeplineIntersections);
        console.log('  typeof window.findIntersections:', typeof window.findIntersections);
        
        // ... rest of loading code
    }
);
```

### Fix 2: Update Code to Handle Both Global Names

In `gis-capture.js`, update the fallback check:

```javascript
// Method 2: Fallback to sweepline-intersections
// Check for both possible global names
var sweeplineFn = (typeof sweeplineIntersections !== 'undefined') 
    ? sweeplineIntersections 
    : (typeof findIntersections !== 'undefined') 
        ? findIntersections 
        : null;

if (intersections.length === 0 && sweeplineFn) {
    try {
        var sweeplineResult = sweeplineFn(polygon.geometry);
        if (sweeplineResult && sweeplineResult.length > 0) {
            console.log('[GIS] Self-intersection detected via sweepline: ' + sweeplineResult.length + ' intersection(s)');
            intersections = sweeplineResult.map(function(pt) {
                return {
                    lng: pt[0],
                    lat: pt[1]
                };
            });
        }
    } catch (sweepErr) {
        console.warn('[GIS] Sweepline fallback failed:', sweepErr);
    }
}
```

### Fix 3: Add Coordinate Scaling for Precision

For very small coordinate differences, scale coordinates to improve floating-point precision:

```javascript
_checkSelfIntersection: function() {
    this._clearIntersectionHighlights();

    if (this.state.vertices.length < 3) {
        this._hideWarning('intersection');
        return false;
    }

    try {
        // Calculate centroid for coordinate normalization
        var sumLat = 0, sumLng = 0;
        var n = this.state.vertices.length;
        for (var i = 0; i < n; i++) {
            sumLat += this.state.vertices[i].lat;
            sumLng += this.state.vertices[i].lng;
        }
        var centroidLat = sumLat / n;
        var centroidLng = sumLng / n;

        // Scale coordinates relative to centroid (improves precision)
        var SCALE = 1000000; // Scale factor for better precision
        var coords = this.state.vertices.map(function(v) {
            return [
                (v.lng - centroidLng) * SCALE,
                (v.lat - centroidLat) * SCALE
            ];
        });
        coords.push(coords[0]); // Close the ring

        var polygon = turf.polygon([coords]);
        var intersections = [];

        // ... rest of detection code
        
        // Remember to scale intersection points back to original coordinates
        if (intersections.length > 0) {
            intersections = intersections.map(function(pt) {
                return {
                    lng: (pt.lng / SCALE) + centroidLng,
                    lat: (pt.lat / SCALE) + centroidLat
                };
            });
        }
```

### Fix 4: Alternative - Use geojson-polygon-self-intersections (gpsi)

If sweepline continues to fail, consider `geojson-polygon-self-intersections`:

```html
<script src="https://unpkg.com/geojson-polygon-self-intersections@1.2.1/dist/index.js"></script>
```

```javascript
// Usage
if (typeof gpsi !== 'undefined') {
    var isects = gpsi(feature);
    // isects is a GeoJSON MultiPoint of intersection locations
}
```

---

## Diagnostic Checklist

Run these checks in the browser console:

### 1. Check Library Loading
```javascript
console.log('Leaflet:', typeof L);
console.log('Turf:', typeof turf);
console.log('sweeplineIntersections:', typeof sweeplineIntersections);
console.log('findIntersections:', typeof findIntersections);
```

### 2. Test turf.kinks() Directly
```javascript
// Create a bowtie (guaranteed self-intersecting)
var bowtie = turf.polygon([[[0, 0], [1, 1], [1, 0], [0, 1], [0, 0]]]);
var kinks = turf.kinks(bowtie);
console.log('Bowtie kinks:', kinks.features.length); // Should be 1
```

### 3. Test sweepline Directly
```javascript
// Use the correct global name
var fn = sweeplineIntersections || findIntersections;
if (fn) {
    var bowtie = {type: 'Polygon', coordinates: [[[0, 0], [1, 1], [1, 0], [0, 1], [0, 0]]]};
    var result = fn(bowtie);
    console.log('Sweepline result:', result); // Should be [[0.5, 0.5]]
}
```

### 4. Test with Real Coordinates
```javascript
// Use GIS_DEBUG.createBowtie() then check console output
GIS_DEBUG.createBowtie();
// Check if warnings appear
```

---

## Summary of Issues

| Issue | Severity | Status |
|-------|----------|--------|
| Global variable name mismatch | HIGH | Likely cause of fallback failure |
| turf.kinks() false negatives | MEDIUM | Known issue, fallback should handle |
| Floating-point precision | LOW | May affect edge cases |
| Coordinate conversion | NONE | Currently correct (lng, lat order) |

---

## Next Steps

1. **Immediate**: Add diagnostic logging to verify sweepline global name
2. **Quick Fix**: Update code to check both `sweeplineIntersections` and `findIntersections`
3. **Testing**: Run `GIS_DEBUG.createBowtie()` and check console output
4. **If still failing**: Implement coordinate scaling or switch to gpsi library

---

## References

- [turf.kinks() GitHub Issue #934](https://github.com/Turfjs/turf/issues/934) - False negatives documentation
- [turf.kinks() GitHub Issue #1094](https://github.com/Turfjs/turf/issues/1094) - Inconsistency with unkinkPolygon
- [sweepline-intersections GitHub](https://github.com/rowanwins/sweepline-intersections) - Library documentation
- [geojson-polygon-self-intersections](https://github.com/mclaeysb/geojson-polygon-self-intersections) - Alternative library
