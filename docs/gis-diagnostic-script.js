/**
 * GIS Self-Intersection Diagnostic Script
 * 
 * Copy and paste this entire script into the browser console
 * when the GIS map is loaded to diagnose self-intersection detection issues.
 * 
 * Run: GIS_DIAGNOSTIC.runAll()
 */
window.GIS_DIAGNOSTIC = {
    
    /**
     * Run all diagnostic tests
     */
    runAll: function() {
        console.log('========================================');
        console.log('GIS SELF-INTERSECTION DIAGNOSTIC REPORT');
        console.log('========================================');
        console.log('');
        
        this.checkLibraries();
        console.log('');
        this.testTurfKinks();
        console.log('');
        this.testSweepline();
        console.log('');
        this.testCurrentPolygon();
        console.log('');
        
        console.log('========================================');
        console.log('DIAGNOSTIC COMPLETE');
        console.log('========================================');
    },
    
    /**
     * Check which libraries are loaded and their global variable names
     */
    checkLibraries: function() {
        console.log('--- LIBRARY CHECK ---');
        
        // Leaflet
        console.log('Leaflet (L):', typeof L !== 'undefined' ? 'LOADED ✓' : 'NOT FOUND ✗');
        if (typeof L !== 'undefined') {
            console.log('  Version:', L.version || 'unknown');
        }
        
        // Turf.js
        console.log('Turf.js (turf):', typeof turf !== 'undefined' ? 'LOADED ✓' : 'NOT FOUND ✗');
        if (typeof turf !== 'undefined') {
            console.log('  turf.kinks:', typeof turf.kinks === 'function' ? 'available ✓' : 'NOT FOUND ✗');
            console.log('  turf.polygon:', typeof turf.polygon === 'function' ? 'available ✓' : 'NOT FOUND ✗');
        }
        
        // Sweepline-intersections (check all possible global names)
        var sweeplineFound = false;
        var sweeplineGlobalName = null;
        
        if (typeof sweeplineIntersections === 'function') {
            sweeplineFound = true;
            sweeplineGlobalName = 'sweeplineIntersections';
        } else if (typeof findIntersections === 'function') {
            sweeplineFound = true;
            sweeplineGlobalName = 'findIntersections';
        } else if (typeof window.sweeplineIntersections === 'function') {
            sweeplineFound = true;
            sweeplineGlobalName = 'window.sweeplineIntersections';
        } else if (typeof window.findIntersections === 'function') {
            sweeplineFound = true;
            sweeplineGlobalName = 'window.findIntersections';
        }
        
        if (sweeplineFound) {
            console.log('Sweepline-intersections: LOADED ✓');
            console.log('  Global variable name:', sweeplineGlobalName);
        } else {
            console.log('Sweepline-intersections: NOT FOUND ✗');
            console.log('  Checked: sweeplineIntersections, findIntersections, window.sweeplineIntersections, window.findIntersections');
            console.log('  ACTION NEEDED: The sweepline fallback is not available!');
        }
        
        // GIS Instance
        if (window._gisInstance) {
            console.log('GIS Instance: FOUND ✓');
            console.log('  Vertices:', window._gisInstance.state?.vertices?.length || 0);
        } else {
            console.log('GIS Instance: NOT FOUND (normal if no form loaded)');
        }
        
        return {
            leaflet: typeof L !== 'undefined',
            turf: typeof turf !== 'undefined',
            sweepline: sweeplineFound,
            sweeplineGlobal: sweeplineGlobalName
        };
    },
    
    /**
     * Test turf.kinks() with known polygons
     */
    testTurfKinks: function() {
        console.log('--- TURF.KINKS() TESTS ---');
        
        if (typeof turf === 'undefined' || typeof turf.kinks !== 'function') {
            console.log('SKIPPED: turf.kinks not available');
            return;
        }
        
        var tests = [
            {
                name: 'Bowtie (should find 1 intersection)',
                polygon: [[[0, 0], [1, 1], [1, 0], [0, 1], [0, 0]]],
                expected: 1
            },
            {
                name: 'Square (should find 0 intersections)',
                polygon: [[[0, 0], [1, 0], [1, 1], [0, 1], [0, 0]]],
                expected: 0
            },
            {
                name: 'Figure-8 (should find 1 intersection)',
                polygon: [[[0, 0], [2, 2], [2, 0], [0, 2], [0, 0]]],
                expected: 1
            },
            {
                name: 'Triangle (should find 0 intersections)',
                polygon: [[[0, 0], [1, 0], [0.5, 1], [0, 0]]],
                expected: 0
            },
            {
                name: 'Real coords bowtie (Lesotho area)',
                polygon: [[[28.5, -29.5], [28.51, -29.49], [28.51, -29.5], [28.5, -29.49], [28.5, -29.5]]],
                expected: 1
            }
        ];
        
        var allPassed = true;
        
        tests.forEach(function(test) {
            try {
                var poly = turf.polygon(test.polygon);
                var kinks = turf.kinks(poly);
                var found = kinks.features.length;
                var passed = found === test.expected;
                
                console.log(
                    (passed ? '✓ PASS' : '✗ FAIL') + ': ' + test.name + 
                    ' (expected: ' + test.expected + ', found: ' + found + ')'
                );
                
                if (found > 0) {
                    kinks.features.forEach(function(f, i) {
                        console.log('    Intersection ' + i + ': [' + 
                            f.geometry.coordinates[0].toFixed(6) + ', ' + 
                            f.geometry.coordinates[1].toFixed(6) + ']'
                        );
                    });
                }
                
                if (!passed) allPassed = false;
            } catch (e) {
                console.log('✗ ERROR: ' + test.name + ' - ' + e.message);
                allPassed = false;
            }
        });
        
        console.log('');
        console.log('turf.kinks() overall result:', allPassed ? 'ALL PASSED ✓' : 'SOME FAILED ✗');
        
        return allPassed;
    },
    
    /**
     * Test sweepline-intersections with known polygons
     */
    testSweepline: function() {
        console.log('--- SWEEPLINE TESTS ---');
        
        // Get the sweepline function
        var sweeplineFn = null;
        if (typeof sweeplineIntersections === 'function') sweeplineFn = sweeplineIntersections;
        else if (typeof findIntersections === 'function') sweeplineFn = findIntersections;
        else if (typeof window.sweeplineIntersections === 'function') sweeplineFn = window.sweeplineIntersections;
        else if (typeof window.findIntersections === 'function') sweeplineFn = window.findIntersections;
        
        if (!sweeplineFn) {
            console.log('SKIPPED: sweepline-intersections not available');
            console.log('ACTION: Check if the CDN script loaded correctly');
            return;
        }
        
        var tests = [
            {
                name: 'Bowtie (should find 1 intersection)',
                geometry: {type: 'Polygon', coordinates: [[[0, 0], [1, 1], [1, 0], [0, 1], [0, 0]]]},
                expected: 1
            },
            {
                name: 'Square (should find 0 intersections)',
                geometry: {type: 'Polygon', coordinates: [[[0, 0], [1, 0], [1, 1], [0, 1], [0, 0]]]},
                expected: 0
            },
            {
                name: 'Figure-8 (should find 1 intersection)',
                geometry: {type: 'Polygon', coordinates: [[[0, 0], [2, 2], [2, 0], [0, 2], [0, 0]]]},
                expected: 1
            },
            {
                name: 'Real coords bowtie (Lesotho area)',
                geometry: {type: 'Polygon', coordinates: [[[28.5, -29.5], [28.51, -29.49], [28.51, -29.5], [28.5, -29.49], [28.5, -29.5]]]},
                expected: 1
            }
        ];
        
        var allPassed = true;
        
        tests.forEach(function(test) {
            try {
                var result = sweeplineFn(test.geometry);
                var found = Array.isArray(result) ? result.length : 0;
                var passed = found === test.expected;
                
                console.log(
                    (passed ? '✓ PASS' : '✗ FAIL') + ': ' + test.name + 
                    ' (expected: ' + test.expected + ', found: ' + found + ')'
                );
                
                if (found > 0 && Array.isArray(result)) {
                    result.forEach(function(pt, i) {
                        console.log('    Intersection ' + i + ': [' + 
                            pt[0].toFixed(6) + ', ' + pt[1].toFixed(6) + ']'
                        );
                    });
                }
                
                if (!passed) allPassed = false;
            } catch (e) {
                console.log('✗ ERROR: ' + test.name + ' - ' + e.message);
                allPassed = false;
            }
        });
        
        console.log('');
        console.log('sweepline overall result:', allPassed ? 'ALL PASSED ✓' : 'SOME FAILED ✗');
        
        return allPassed;
    },
    
    /**
     * Test the current polygon in the GIS component
     */
    testCurrentPolygon: function() {
        console.log('--- CURRENT POLYGON TEST ---');
        
        var instance = window._gisInstance;
        if (!instance || !instance.state || !instance.state.vertices) {
            console.log('SKIPPED: No GIS instance or no vertices');
            return;
        }
        
        var vertices = instance.state.vertices;
        console.log('Vertex count:', vertices.length);
        
        if (vertices.length < 3) {
            console.log('Not enough vertices for a polygon');
            return;
        }
        
        // Display vertices
        console.log('Vertices:');
        vertices.forEach(function(v, i) {
            console.log('  V' + i + ': [' + v.lng.toFixed(6) + ', ' + v.lat.toFixed(6) + ']');
        });
        
        // Build GeoJSON
        var coords = vertices.map(function(v) { return [v.lng, v.lat]; });
        coords.push(coords[0]);
        
        // Test with turf.kinks
        if (typeof turf !== 'undefined' && typeof turf.kinks === 'function') {
            try {
                var polygon = turf.polygon([coords]);
                var kinks = turf.kinks(polygon);
                console.log('turf.kinks() result:', kinks.features.length + ' intersection(s)');
                if (kinks.features.length > 0) {
                    kinks.features.forEach(function(f, i) {
                        console.log('  Intersection ' + i + ': [' + 
                            f.geometry.coordinates[0].toFixed(6) + ', ' + 
                            f.geometry.coordinates[1].toFixed(6) + ']'
                        );
                    });
                }
            } catch (e) {
                console.log('turf.kinks() error:', e.message);
            }
        }
        
        // Test with sweepline
        var sweeplineFn = null;
        if (typeof sweeplineIntersections === 'function') sweeplineFn = sweeplineIntersections;
        else if (typeof findIntersections === 'function') sweeplineFn = findIntersections;
        else if (typeof window.sweeplineIntersections === 'function') sweeplineFn = window.sweeplineIntersections;
        else if (typeof window.findIntersections === 'function') sweeplineFn = window.findIntersections;
        
        if (sweeplineFn) {
            try {
                var geometry = {type: 'Polygon', coordinates: [coords]};
                var result = sweeplineFn(geometry);
                var count = Array.isArray(result) ? result.length : 0;
                console.log('sweepline result:', count + ' intersection(s)');
                if (count > 0) {
                    result.forEach(function(pt, i) {
                        console.log('  Intersection ' + i + ': [' + 
                            pt[0].toFixed(6) + ', ' + pt[1].toFixed(6) + ']'
                        );
                    });
                }
            } catch (e) {
                console.log('sweepline error:', e.message);
            }
        }
        
        // Trigger the component's check
        if (typeof instance._checkSelfIntersection === 'function') {
            console.log('');
            console.log('Triggering instance._checkSelfIntersection()...');
            var result = instance._checkSelfIntersection();
            console.log('Component result:', result ? 'INTERSECTION DETECTED' : 'NO INTERSECTION');
        }
    },
    
    /**
     * Create a bowtie polygon at the current map center for testing
     */
    createTestBowtie: function() {
        console.log('--- CREATE TEST BOWTIE ---');
        
        if (typeof GIS_DEBUG !== 'undefined' && typeof GIS_DEBUG.createBowtie === 'function') {
            GIS_DEBUG.createBowtie();
            console.log('Bowtie created via GIS_DEBUG.createBowtie()');
            console.log('Check the map and console for intersection detection results');
        } else {
            console.log('GIS_DEBUG.createBowtie() not available');
            console.log('Try creating a bowtie manually by dragging vertices to cross edges');
        }
    }
};

console.log('GIS_DIAGNOSTIC loaded. Run GIS_DIAGNOSTIC.runAll() to start diagnostics.');
