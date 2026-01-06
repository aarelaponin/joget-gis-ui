# GIS UI Plugin - Complete Implementation Plan

## Overview
Implement all missing features from `07-gis-ui-ux-spec.md` in 6 testable phases.

**Current State:** Core draw/walk modes work, metrics calculation complete, basic validation exists.
**Target:** Full specification compliance including overlap checking, accessibility, and UX polish.

---

## Phase 1: Draw Mode UX Enhancements
**Goal:** Improve drawing experience with visual feedback and guidance

### Features to Implement
1. **Empty State UI** - "No boundary captured yet" + "Capture Boundary" button
2. **Ghost Line** - Dashed line from last vertex to cursor while drawing
3. **Numbered Vertex Markers** - Labels 1, 2, 3... on each vertex
4. **Close Polygon Hint** - Enlarge first vertex on hover, show "Click to close" tooltip
5. **Double-click to Finish** - Alternative to clicking near start
6. **Step Progress Indicator** - "STEP 1 of 2: Draw" / "STEP 2 of 2: Preview"

### Files to Modify
- `src/main/resources/static/gis-capture.js` (lines 127-350, 600-680)
- `src/main/resources/static/gis-capture.css` (new styles)

### Testing Checklist
- [ ] Empty state shows when no geometry exists
- [ ] Ghost line follows cursor during drawing
- [ ] Vertices show numbers 1, 2, 3...
- [ ] First vertex enlarges on hover with tooltip
- [ ] Double-click closes polygon
- [ ] Progress indicator shows correct step

---

## Phase 2: Edit Mode Enhancements
**Goal:** Enable full polygon editing after initial draw

### Features to Implement
1. **Midpoint Handles** - Diamond markers on edges, drag to add vertex
2. **Vertex Selection** - Click to select, visual highlight
3. **Delete Vertex** - Delete key or button removes selected vertex
4. **Keyboard Shortcuts** - Ctrl+Z undo, Delete remove vertex
5. **"Done Editing" Button** - Exit edit mode explicitly

### Files to Modify
- `src/main/resources/static/gis-capture.js` (lines 870-1000, new edit functions)
- `src/main/resources/static/gis-capture.css` (midpoint, selection styles)

### Testing Checklist
- [ ] Midpoint handles appear on all edges
- [ ] Dragging midpoint creates new vertex
- [ ] Click vertex to select (highlighted)
- [ ] Delete key removes selected vertex (min 3 vertices)
- [ ] Ctrl+Z undoes last action
- [ ] "Done Editing" returns to preview state

---

## Phase 3: Walk Mode UX Enhancements
**Goal:** Improve mobile GPS capture experience

### Features to Implement
1. **Full GPS Accuracy Bar** - Visual bar with Excellent/Good/Fair/Poor labels
2. **Contextual Instructions** - "Go to first corner" -> "MARK FIRST CORNER" -> walking progress
3. **Close & Finish Flow** - When near start: "CLOSE & FINISH" + "Undo" + "Add More" buttons
4. **GPS Average in Summary** - Show average GPS accuracy at completion
5. **Redo Boundary Option** - Clear and restart at completion screen

### Files to Modify
- `src/main/resources/static/gis-capture.js` (lines 410-600)
- `src/main/resources/static/gis-capture.css` (GPS bar, mobile buttons)

### Testing Checklist
- [ ] GPS bar shows visual accuracy level
- [ ] Instructions change based on state
- [ ] Near-start detection shows close options
- [ ] Completion shows GPS average accuracy
- [ ] Redo clears and restarts walk mode

---

## Phase 4: Real-time Validation & Visual Feedback
**Goal:** Show validation issues during drawing, not just at completion

### Features to Implement
1. **Real-time Self-Intersection Detection** - Check on each vertex add
2. **Visual Intersection Highlighting** - Red edges at crossing points, warning icon
3. **Disable Save on Invalid** - "Complete" button disabled with tooltip
4. **Vertex Limit Warning** - "95/100 vertices - approaching limit"
5. **Area Warnings During Draw** - Show if area too small/large

### Files to Modify
- `src/main/resources/static/gis-capture.js` (validation logic ~lines 890-960)
- `src/main/resources/static/gis-capture.css` (error highlighting styles)

### Testing Checklist
- [ ] Self-intersection detected immediately when edges cross
- [ ] Crossing edges highlighted in red
- [ ] Complete button disabled when invalid
- [ ] Vertex count warning at 90% of max
- [ ] Area warnings show during drawing

---

## Phase 5: Overlap Checking Integration
**Goal:** Check new polygon against existing parcels via API

### Features to Implement
1. **API Integration** - Call `POST /jw/api/gis/gis/checkOverlap` on completion
2. **Visual Overlap Display** - Show overlapping area in red on map
3. **Overlap Details Panel** - List overlapping parcels with area/percentage
4. **Action Buttons** - "Adjust Boundary" / "Save Anyway" / "View Details"
5. **Loading State** - Show spinner while checking

### API Format (already implemented in joget-gis-server)
```json
POST /jw/api/gis/gis/checkOverlap
{
  "geometry": { GeoJSON },
  "options": {
    "formId": "parcel",
    "geometryField": "c_geometry",
    "filterCondition": "status = 'ACTIVE'"
  }
}
```

### Files to Modify
- `src/main/resources/static/gis-capture.js` (new overlap checking ~100 lines)
- `src/main/resources/static/gis-capture.css` (overlap panel styles)

### Testing Checklist
- [ ] API called when polygon completed (if overlap check enabled)
- [ ] Loading spinner shows during API call
- [ ] Overlapping areas displayed in red
- [ ] Panel shows overlapping parcel details
- [ ] "Save Anyway" allows submission despite overlaps
- [ ] "Adjust Boundary" returns to edit mode

---

## Phase 6: Accessibility & Location Search
**Goal:** Keyboard navigation, screen readers, and location search

### Features to Implement
1. **Keyboard Navigation** - Tab through controls, Enter activate, Escape cancel
2. **Arrow Keys** - Pan map with arrow keys when focused
3. **+/- Keys** - Zoom in/out
4. **ARIA Labels** - All interactive elements labeled
5. **Screen Reader Announcements** - "Corner 3 marked. Area 0.8 hectares"
6. **Location Search** - Search field with OSM Nominatim geocoding

### Files to Modify
- `src/main/resources/static/gis-capture.js` (keyboard handlers, ARIA, search)
- `src/main/resources/static/gis-capture.css` (focus styles, search UI)

### Testing Checklist
- [ ] Tab navigates through all controls
- [ ] Enter/Space activates buttons
- [ ] Escape cancels current action
- [ ] Arrow keys pan map
- [ ] Screen reader announces vertex additions
- [ ] Location search navigates map to result

---

## Build & Deploy Commands
```bash
cd /Users/aarelaponin/IdeaProjects/gs-plugins/joget-gis-ui
mvn clean package
# Deploy target/joget-gis-ui-8.1-SNAPSHOT.jar to Joget
```

## Success Criteria
After all phases:
- All 16 missing features from gap analysis implemented
- Full compliance with 07-gis-ui-ux-spec.md
- Works on desktop (Draw Mode) and mobile (Walk Mode)
- Accessible via keyboard and screen reader
- Overlap checking integrated with backend API
