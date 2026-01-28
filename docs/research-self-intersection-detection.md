# Research Prompt: Real-Time Polygon Self-Intersection Detection

## Prompt for Perplexity/Research

---

**COPY BELOW FOR PERPLEXITY:**

---

I'm building a web-based polygon drawing tool using **Leaflet.js** and **Turf.js** and need help with **real-time self-intersection detection** during vertex dragging.

### The Problem

I have a polygon editor where users can:
1. Click to add vertices and draw a polygon
2. Drag existing vertices to edit the polygon shape

I need to detect and warn users **in real-time** (while dragging) when the polygon becomes self-intersecting (edges cross each other, creating a "bowtie" or figure-8 shape).

### Tech Stack
- **Leaflet.js 1.9.4** - Map rendering and interaction
- **Turf.js 6.x** - Geospatial calculations
- **Vanilla JavaScript** (ES5 compatible, no modern frameworks)
- Browser-based, must work on mobile devices

### What I've Tried

**Attempt 1: Manual line segment intersection algorithm**
- Implemented parametric line intersection using cross-products
- Check each pair of non-adjacent edges for intersection
- **Problem**: Works in isolated unit tests but fails during real vertex dragging. The t/u parameters come back outside [0,1] even when edges visually cross.

**Attempt 2: Using `turf.kinks()`**
- Build a GeoJSON polygon from current vertices
- Call `turf.kinks(polygon)` to detect self-intersections
- **Problem**: `turf.kinks()` returns 0 features even when the polygon is visually self-intersecting on the map.

### Code Example (Current Implementation)

```javascript
_checkSelfIntersection: function() {
    if (this.state.vertices.length < 3) return false;

    // Build closed ring for turf.polygon
    var coords = this.state.vertices.map(function(v) {
        return [v.lng, v.lat];  // GeoJSON is [lng, lat]
    });
    coords.push(coords[0]); // Close the ring

    var polygon = turf.polygon([coords]);
    var kinks = turf.kinks(polygon);

    console.log('Kinks found:', kinks.features.length);
    // PROBLEM: Always returns 0 even for obvious bowties

    return kinks.features.length > 0;
}
```

### When This Function Is Called
- On `dragend` event when user finishes dragging a vertex
- After each vertex is added/moved
- The vertices array contains `{lat, lng}` objects from Leaflet

### What I Need

1. **A reliable method** to detect polygon self-intersection in real-time during editing
2. Should work with Leaflet coordinate system (lat/lng)
3. Should work with Turf.js (already loaded) or suggest alternative library
4. Performance: Must handle polygons up to ~50 vertices without lag
5. Bonus: Return the intersection point(s) for visual highlighting

### Questions

1. Is there a known issue with `turf.kinks()` and certain polygon orientations or coordinate systems?
2. Are there alternative Turf.js functions for self-intersection detection?
3. What's the recommended approach for real-time polygon validation in Leaflet editors?
4. Are there other JavaScript libraries better suited for this (e.g., JSTS, martinez-polygon-clipping)?
5. Could this be a coordinate precision or winding order issue?

### Example Test Case

Create a bowtie polygon:
```javascript
var bowtie = [
    [0, 0],      // bottom-left
    [1, 1],      // top-right (diagonal)
    [1, 0],      // bottom-right
    [0, 1],      // top-left (crosses previous edge)
    [0, 0]       // close
];
// Edges 0→1 and 2→3 should cross at (0.5, 0.5)
```

When I create this programmatically using `turf.polygon([bowtie])` and call `turf.kinks()`, it should return 1 intersection point at [0.5, 0.5], but I'm getting inconsistent results.

---

**END OF PROMPT**

---

## Additional Context for Research

### Files Involved
- `src/main/resources/static/gis-capture.js` - Main JavaScript component (~4600 lines)

### Current Architecture
- Polygon vertices stored as array of `{lat, lng}` objects
- Leaflet handles map rendering and drag events
- Turf.js used for area/perimeter calculations (working correctly)
- Self-intersection check called on `dragend` and after vertex operations

### What Success Looks Like
1. User drags vertex to create crossing edges
2. Warning appears immediately: "Boundary lines are crossing"
3. Red dot marker shows at intersection point
4. Form submission is blocked until fixed

### Previous Approaches That Failed
| Approach | Result |
|----------|--------|
| Manual O(n²) edge intersection | Works in unit tests, fails with real coordinates |
| `turf.kinks()` | Returns empty results for self-intersecting polygons |
| `turf.booleanValid()` | Not available in Turf 6.x |

### Research Areas to Explore
1. JSTS (JavaScript Topology Suite) - `isSimple()` method
2. martinez-polygon-clipping library
3. Leaflet.draw built-in validation
4. Mapbox GL JS polygon validation
5. OpenLayers polygon validation approach

---

## Research Results

### Solution Implemented

We implemented a **dual-algorithm approach** using:
1. **Primary**: `turf.kinks()` - already loaded, works for most cases
2. **Fallback**: `sweepline-intersections` - 3KB library using sweep-line algorithm

### Key Libraries Evaluated

| Library | Size | Performance | Notes |
|---------|------|-------------|-------|
| turf.kinks() | (included) | Good | Known false negatives in edge cases |
| sweepline-intersections | 3KB | 1.1M ops/sec | Fast sweep-line algorithm |
| geojson-polygon-self-intersections | ~5KB | Good | Uses rbush spatial index |
| JSTS | ~200KB | Excellent | Full topology suite, overkill for this |

### References
- [turf.kinks() documentation](https://turfjs.org/docs/api/kinks)
- [sweepline-intersections GitHub](https://github.com/rowanwins/sweepline-intersections)
- [geojson-polygon-self-intersections GitHub](https://github.com/mclaeysb/geojson-polygon-self-intersections)
- [JSTS GitHub](https://github.com/bjornharrtell/jsts)
- [Turf.js kinks false negatives issue #934](https://github.com/Turfjs/turf/issues/934)
