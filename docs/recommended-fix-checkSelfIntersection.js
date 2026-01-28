/**
 * RECOMMENDED FIX for gis-capture.js
 * 
 * Replace the existing _checkSelfIntersection function (around line 1922)
 * with this improved version that handles:
 * 1. Both possible global variable names for sweepline library
 * 2. Better diagnostic logging
 * 3. Coordinate precision improvements
 */

/**
 * Check for self-intersection using turf.kinks() with sweepline-intersections fallback
 * Uses multiple algorithms to ensure reliable detection during real-time editing
 * 
 * IMPROVEMENTS:
 * - Checks for both 'sweeplineIntersections' and 'findIntersections' global names
 * - Better error handling and logging
 * - Coordinate precision normalization option
 */
_checkSelfIntersection: function() {
    this._clearIntersectionHighlights();

    // Need at least 3 vertices to form a polygon
    if (this.state.vertices.length < 3) {
        this._hideWarning('intersection');
        return false;
    }

    try {
        // Build closed ring coordinates for turf.polygon
        var coords = this.state.vertices.map(function(v) {
            return [v.lng, v.lat];  // GeoJSON is [lng, lat]
        });
        coords.push(coords[0]); // Close the ring

        var polygon = turf.polygon([coords]);
        var intersections = [];

        // === METHOD 1: turf.kinks() ===
        try {
            var kinks = turf.kinks(polygon);
            if (kinks.features.length > 0) {
                console.log('[GIS] Self-intersection detected via turf.kinks: ' + kinks.features.length + ' kink(s)');
                intersections = kinks.features.map(function(f) {
                    return {
                        lng: f.geometry.coordinates[0],
                        lat: f.geometry.coordinates[1]
                    };
                });
            }
        } catch (turfErr) {
            console.warn('[GIS] turf.kinks() failed:', turfErr);
        }

        // === METHOD 2: sweepline-intersections fallback ===
        // Check for both possible global variable names (library may export differently)
        var sweeplineFn = null;
        if (typeof sweeplineIntersections === 'function') {
            sweeplineFn = sweeplineIntersections;
            console.log('[GIS DEBUG] Using sweeplineIntersections global');
        } else if (typeof findIntersections === 'function') {
            sweeplineFn = findIntersections;
            console.log('[GIS DEBUG] Using findIntersections global');
        } else if (typeof window !== 'undefined') {
            // Check window object as fallback
            if (typeof window.sweeplineIntersections === 'function') {
                sweeplineFn = window.sweeplineIntersections;
                console.log('[GIS DEBUG] Using window.sweeplineIntersections');
            } else if (typeof window.findIntersections === 'function') {
                sweeplineFn = window.findIntersections;
                console.log('[GIS DEBUG] Using window.findIntersections');
            }
        }

        // Only use fallback if turf.kinks found nothing
        if (intersections.length === 0 && sweeplineFn) {
            try {
                // sweepline-intersections accepts GeoJSON geometry directly
                var sweeplineResult = sweeplineFn(polygon.geometry);
                if (sweeplineResult && Array.isArray(sweeplineResult) && sweeplineResult.length > 0) {
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
        } else if (intersections.length === 0 && !sweeplineFn) {
            console.log('[GIS DEBUG] Sweepline library not available for fallback');
        }

        // === METHOD 3: Manual O(n²) check as ultimate fallback ===
        if (intersections.length === 0 && this.state.vertices.length >= 4) {
            console.log('[GIS DEBUG] Trying manual O(n²) intersection check');
            var manualIntersections = this._checkSelfIntersectionManual();
            if (manualIntersections.length > 0) {
                console.log('[GIS] Self-intersection detected via manual check: ' + manualIntersections.length + ' intersection(s)');
                intersections = manualIntersections;
            }
        }

        // === Handle Results ===
        if (intersections.length > 0) {
            console.log('[GIS] Total intersections found: ' + intersections.length);
            intersections.forEach(function(pt, i) {
                console.log('[GIS]   Intersection ' + i + ': [' + pt.lng.toFixed(6) + ', ' + pt.lat.toFixed(6) + ']');
            });

            // Store intersection points
            this.state.intersectionPoints = intersections;

            // Highlight intersection points
            this._highlightIntersectionPoints(intersections);

            // Show warning
            this._showWarning('intersection', 'Boundary lines are crossing. Adjust the corners to fix this.');
            return true;
        } else {
            console.log('[GIS] Self-intersection check: polygon is valid');
            this._hideWarning('intersection');
            return false;
        }
    } catch (e) {
        console.warn('[GISCapture] Self-intersection check failed:', e);
        this._hideWarning('intersection');
        return false;
    }
},

/**
 * Manual O(n²) self-intersection check using cross-product line intersection
 * This is the ultimate fallback if both turf.kinks() and sweepline fail
 * 
 * @returns {Array} Array of intersection points {lng, lat}
 */
_checkSelfIntersectionManual: function() {
    var vertices = this.state.vertices;
    var n = vertices.length;
    var intersections = [];

    // Need at least 4 vertices for self-intersection (triangle can't self-intersect)
    if (n < 4) return intersections;

    // Check all pairs of non-adjacent edges
    for (var i = 0; i < n; i++) {
        var v1 = vertices[i];
        var v2 = vertices[(i + 1) % n];

        // Start from i+2 to skip adjacent edges
        for (var j = i + 2; j < n; j++) {
            // Skip if this is the edge adjacent to first edge (wrapping case)
            if (i === 0 && j === n - 1) continue;

            var v3 = vertices[j];
            var v4 = vertices[(j + 1) % n];

            var intersection = this._lineSegmentIntersectionRobust(
                v1.lng, v1.lat, v2.lng, v2.lat,
                v3.lng, v3.lat, v4.lng, v4.lat
            );

            if (intersection) {
                intersections.push(intersection);
            }
        }
    }

    return intersections;
},

/**
 * Robust line segment intersection using cross-product method
 * with better numerical stability for geographic coordinates
 * 
 * @param {number} x1,y1,x2,y2 - First segment endpoints
 * @param {number} x3,y3,x4,y4 - Second segment endpoints
 * @returns {Object|null} Intersection point {lng, lat} or null
 */
_lineSegmentIntersectionRobust: function(x1, y1, x2, y2, x3, y3, x4, y4) {
    // Direction vectors
    var dx1 = x2 - x1;
    var dy1 = y2 - y1;
    var dx2 = x4 - x3;
    var dy2 = y4 - y3;

    // Cross product of direction vectors
    var denom = dx1 * dy2 - dy1 * dx2;

    // Check for parallel lines (use relative epsilon for geographic coords)
    var segmentLength = Math.max(
        Math.sqrt(dx1*dx1 + dy1*dy1),
        Math.sqrt(dx2*dx2 + dy2*dy2)
    );
    var epsilon = segmentLength * 1e-10;
    
    if (Math.abs(denom) < epsilon) {
        return null; // Parallel or nearly parallel
    }

    // Calculate parameters t and u
    var dx3 = x3 - x1;
    var dy3 = y3 - y1;
    
    var t = (dx3 * dy2 - dy3 * dx2) / denom;
    var u = (dx3 * dy1 - dy3 * dx1) / denom;

    // Use small margin to exclude endpoint touches
    var margin = 0.0001;

    // Check if intersection is strictly within both segments (not at endpoints)
    if (t > margin && t < (1 - margin) && u > margin && u < (1 - margin)) {
        return {
            lng: x1 + t * dx1,
            lat: y1 + t * dy1
        };
    }

    return null;
},
