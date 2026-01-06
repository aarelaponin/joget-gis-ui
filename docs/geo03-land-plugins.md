## Plugin Architecture Design Document

### 1. Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                    PARCEL CAPTURE SYSTEM                             │
│                    Architecture Overview                             │
└─────────────────────────────────────────────────────────────────────┘

  ┌──────────────────────────────────────────────────────────────────┐
  │                         JOGET PLATFORM                            │
  │  ┌────────────────────────────────────────────────────────────┐  │
  │  │                      FORM BUILDER                           │  │
  │  │                                                             │  │
  │  │   ┌─────────────────────────────────────────────────────┐  │  │
  │  │   │           PLUGIN 1: joget-parcel-capture            │  │  │
  │  │   │              (Form Element Plugin)                   │  │  │
  │  │   │                                                      │  │  │
  │  │   │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  │  │  │
  │  │   │  │ Walk Mode   │  │ Draw Mode   │  │ View Mode   │  │  │  │
  │  │   │  │ Component   │  │ Component   │  │ Component   │  │  │  │
  │  │   │  └─────────────┘  └─────────────┘  └─────────────┘  │  │  │
  │  │   │                        │                             │  │  │
  │  │   │              ┌─────────┴─────────┐                   │  │  │
  │  │   │              │   Map Core Engine │                   │  │  │
  │  │   │              │   (Leaflet.js)    │                   │  │  │
  │  │   │              └───────────────────┘                   │  │  │
  │  │   └─────────────────────────────────────────────────────┘  │  │
  │  └────────────────────────────────────────────────────────────┘  │
  │                                    │                              │
  │                                    │ REST API / JSON API          │
  │                                    ▼                              │
  │  ┌────────────────────────────────────────────────────────────┐  │
  │  │           PLUGIN 2: joget-parcel-services                  │  │
  │  │              (Web Service + Process Tool)                   │  │
  │  │                                                             │  │
  │  │  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐        │  │
  │  │  │  Geometry    │ │  Validation  │ │   Spatial    │        │  │
  │  │  │  Calculator  │ │  Service     │ │   Query      │        │  │
  │  │  └──────────────┘ └──────────────┘ └──────────────┘        │  │
  │  │                          │                                  │  │
  │  │              ┌───────────┴───────────┐                      │  │
  │  │              │  JTS Topology Suite   │                      │  │
  │  │              │  (Geometry Engine)    │                      │  │
  │  │              └───────────────────────┘                      │  │
  │  └────────────────────────────────────────────────────────────┘  │
  │                                    │                              │
  └────────────────────────────────────┼──────────────────────────────┘
                                       │
                                       ▼
                              ┌─────────────────┐
                              │    DATABASE     │
                              │   (MySQL)       │
                              │                 │
                              │  app_fd_parcel  │
                              └─────────────────┘
```

---

### 2. Plugin 1: joget-parcel-capture (Form Element)

#### 2.1 Project Structure

```
joget-parcel-capture/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── org/joget/marketplace/parcel/
│   │   │       ├── ParcelCaptureField.java           ← Main Form Element
│   │   │       ├── ParcelCaptureFieldValidator.java  ← Client-side prep
│   │   │       └── model/
│   │   │           ├── ParcelGeometry.java           ← GeoJSON model
│   │   │           ├── CaptureMode.java              ← Enum
│   │   │           └── TileProvider.java             ← Enum
│   │   │
│   │   └── resources/
│   │       ├── templates/
│   │       │   └── parcelCaptureField.ftl            ← Freemarker template
│   │       │
│   │       ├── js/
│   │       │   ├── parcel-capture-core.js            ← Core map engine
│   │       │   ├── parcel-capture-walk.js            ← Walk mode logic
│   │       │   ├── parcel-capture-draw.js            ← Draw mode logic
│   │       │   └── parcel-capture-view.js            ← Read-only view
│   │       │
│   │       ├── css/
│   │       │   └── parcel-capture.css                ← Component styles
│   │       │
│   │       ├── lib/
│   │       │   ├── leaflet.js                        ← Leaflet 1.9.4
│   │       │   ├── leaflet.css
│   │       │   ├── leaflet.draw.js                   ← Leaflet.Draw
│   │       │   ├── leaflet.draw.css
│   │       │   └── turf.min.js                       ← Turf.js for calculations
│   │       │
│   │       ├── messages/
│   │       │   ├── ParcelCaptureField_en.properties
│   │       │   └── ParcelCaptureField_st.properties  ← Sesotho
│   │       │
│   │       └── properties/
│   │           └── ParcelCaptureField.json           ← Plugin properties
│   │
│   └── test/
│       └── java/
│           └── org/joget/marketplace/parcel/
│               └── ParcelCaptureFieldTest.java
│
└── README.md
```

#### 2.2 Class Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                     PLUGIN 1: CLASS DIAGRAM                          │
└─────────────────────────────────────────────────────────────────────┘

  ┌───────────────────────────────────────────────────────────────────┐
  │                    org.joget.apps.form.model                       │
  │  ┌─────────────────────────────────────────────────────────────┐  │
  │  │                        Element                               │  │
  │  │                       (abstract)                             │  │
  │  └─────────────────────────────────────────────────────────────┘  │
  │                              △                                     │
  │                              │ extends                             │
  │  ┌─────────────────────────────────────────────────────────────┐  │
  │  │                    FormBinder                                │  │
  │  │                    (interface)                               │  │
  │  └─────────────────────────────────────────────────────────────┘  │
  │                              △                                     │
  │                              │ implements                          │
  └──────────────────────────────┼────────────────────────────────────┘
                                 │
  ┌──────────────────────────────┼────────────────────────────────────┐
  │        org.joget.marketplace.parcel                               │
  │                              │                                     │
  │  ┌───────────────────────────┴─────────────────────────────────┐  │
  │  │                  ParcelCaptureField                          │  │
  │  ├─────────────────────────────────────────────────────────────┤  │
  │  │ - PROPERTY_CAPTURE_MODE: String = "captureMode"             │  │
  │  │ - PROPERTY_TILE_PROVIDER: String = "tileProvider"           │  │
  │  │ - PROPERTY_DEFAULT_CENTER: String = "defaultCenter"         │  │
  │  │ - PROPERTY_DEFAULT_ZOOM: String = "defaultZoom"             │  │
  │  │ - PROPERTY_MIN_AREA: String = "minArea"                     │  │
  │  │ - PROPERTY_MAX_AREA: String = "maxArea"                     │  │
  │  │ - PROPERTY_GEOMETRY_FIELD: String = "geometryField"         │  │
  │  │ - PROPERTY_AREA_FIELD: String = "areaField"                 │  │
  │  │ - PROPERTY_CENTROID_LAT_FIELD: String = "centroidLatField"  │  │
  │  │ - PROPERTY_CENTROID_LON_FIELD: String = "centroidLonField"  │  │
  │  │ - PROPERTY_SERVICES_URL: String = "servicesUrl"             │  │
  │  ├─────────────────────────────────────────────────────────────┤  │
  │  │ + getName(): String                                          │  │
  │  │ + getVersion(): String                                       │  │
  │  │ + getDescription(): String                                   │  │
  │  │ + getLabel(): String                                         │  │
  │  │ + getClassName(): String                                     │  │
  │  │ + getPropertyOptions(): String                               │  │
  │  │ + renderTemplate(formData, dataModel): String                │  │
  │  │ + formatData(element, formData, value): String               │  │
  │  │ + getFormBuilderCategory(): String                           │  │
  │  │ + getFormBuilderIcon(): String                               │  │
  │  │ + getFormBuilderTemplate(): String                           │  │
  │  │ # buildJsConfig(): Map<String, Object>                       │  │
  │  │ # getIncludedScripts(): String[]                             │  │
  │  │ # getIncludedStyles(): String[]                              │  │
  │  └─────────────────────────────────────────────────────────────┘  │
  │                              │                                     │
  │                              │ uses                                │
  │                              ▼                                     │
  │  ┌─────────────────────────────────────────────────────────────┐  │
  │  │                    ParcelGeometry                            │  │
  │  ├─────────────────────────────────────────────────────────────┤  │
  │  │ - type: String                                               │  │
  │  │ - coordinates: double[][][]                                  │  │
  │  │ - properties: Map<String, Object>                            │  │
  │  ├─────────────────────────────────────────────────────────────┤  │
  │  │ + fromGeoJson(json: String): ParcelGeometry                  │  │
  │  │ + toGeoJson(): String                                        │  │
  │  │ + isValid(): boolean                                         │  │
  │  │ + getCoordinateCount(): int                                  │  │
  │  └─────────────────────────────────────────────────────────────┘  │
  │                                                                    │
  │  ┌─────────────────────┐      ┌─────────────────────┐             │
  │  │    CaptureMode      │      │    TileProvider     │             │
  │  │    <<enum>>         │      │    <<enum>>         │             │
  │  ├─────────────────────┤      ├─────────────────────┤             │
  │  │ WALK                │      │ OSM                 │             │
  │  │ DRAW                │      │ SATELLITE_ESRI      │             │
  │  │ BOTH                │      │ SATELLITE_GOOGLE    │             │
  │  │ VIEW_ONLY           │      │ HYBRID              │             │
  │  └─────────────────────┘      │ TERRAIN             │             │
  │                               └─────────────────────┘             │
  └───────────────────────────────────────────────────────────────────┘
```

#### 2.3 Plugin Properties Definition

```json
[
  {
    "title": "Configure Parcel Capture Field",
    "properties": [
      {
        "name": "id",
        "label": "ID",
        "type": "textfield",
        "required": "true"
      },
      {
        "name": "label",
        "label": "Label",
        "type": "textfield",
        "value": "Land Parcel"
      }
    ]
  },
  {
    "title": "Capture Settings",
    "properties": [
      {
        "name": "captureMode",
        "label": "Capture Mode",
        "type": "selectbox",
        "required": "true",
        "options": [
          {"value": "BOTH", "label": "User Chooses (Walk or Draw)"},
          {"value": "WALK", "label": "Walk Boundaries Only (GPS Field Mode)"},
          {"value": "DRAW", "label": "Draw on Map Only (Office Mode)"},
          {"value": "VIEW_ONLY", "label": "View Only (Read-only Display)"}
        ],
        "value": "BOTH"
      },
      {
        "name": "tileProvider",
        "label": "Default Map Tiles",
        "type": "selectbox",
        "options": [
          {"value": "OSM", "label": "OpenStreetMap (Free)"},
          {"value": "SATELLITE_ESRI", "label": "Satellite - ESRI (Free)"},
          {"value": "SATELLITE_GOOGLE", "label": "Satellite - Google (API Key Required)"},
          {"value": "HYBRID", "label": "Hybrid (Labels on Satellite)"},
          {"value": "TERRAIN", "label": "Terrain"}
        ],
        "value": "SATELLITE_ESRI"
      },
      {
        "name": "allowTileSwitch",
        "label": "Allow User to Switch Tiles",
        "type": "checkbox",
        "options": [
          {"value": "true", "label": ""}
        ],
        "value": "true"
      }
    ]
  },
  {
    "title": "Map Defaults",
    "properties": [
      {
        "name": "defaultCenterLat",
        "label": "Default Center Latitude",
        "type": "textfield",
        "value": "-29.6"
      },
      {
        "name": "defaultCenterLon",
        "label": "Default Center Longitude",
        "type": "textfield",
        "value": "28.2"
      },
      {
        "name": "defaultZoom",
        "label": "Default Zoom Level (1-20)",
        "type": "textfield",
        "value": "8"
      },
      {
        "name": "maxZoom",
        "label": "Maximum Zoom Level",
        "type": "textfield",
        "value": "19"
      }
    ]
  },
  {
    "title": "Validation Rules",
    "properties": [
      {
        "name": "minArea",
        "label": "Minimum Area (hectares)",
        "type": "textfield",
        "value": "0.01"
      },
      {
        "name": "maxArea",
        "label": "Maximum Area (hectares)",
        "type": "textfield",
        "value": "1000"
      },
      {
        "name": "minVertices",
        "label": "Minimum Vertices (corners)",
        "type": "textfield",
        "value": "3"
      },
      {
        "name": "maxVertices",
        "label": "Maximum Vertices (corners)",
        "type": "textfield",
        "value": "100"
      },
      {
        "name": "allowSelfIntersection",
        "label": "Allow Self-Intersecting Polygons",
        "type": "checkbox",
        "options": [
          {"value": "true", "label": ""}
        ],
        "value": ""
      }
    ]
  },
  {
    "title": "Field Bindings",
    "properties": [
      {
        "name": "geometryField",
        "label": "Geometry Field ID (stores GeoJSON)",
        "type": "textfield",
        "required": "true",
        "value": "parcel_geometry"
      },
      {
        "name": "areaField",
        "label": "Area Field ID (auto-calculated hectares)",
        "type": "textfield",
        "value": "area_hectares"
      },
      {
        "name": "perimeterField",
        "label": "Perimeter Field ID (auto-calculated meters)",
        "type": "textfield",
        "value": "perimeter_meters"
      },
      {
        "name": "centroidLatField",
        "label": "Centroid Latitude Field ID",
        "type": "textfield",
        "value": "centroid_lat"
      },
      {
        "name": "centroidLonField",
        "label": "Centroid Longitude Field ID",
        "type": "textfield",
        "value": "centroid_lon"
      }
    ]
  },
  {
    "title": "GPS Settings (Walk Mode)",
    "properties": [
      {
        "name": "gpsHighAccuracy",
        "label": "Use High Accuracy GPS",
        "type": "checkbox",
        "options": [
          {"value": "true", "label": ""}
        ],
        "value": "true"
      },
      {
        "name": "gpsMinAccuracy",
        "label": "Minimum GPS Accuracy (meters)",
        "description": "Will warn user if accuracy is worse than this",
        "type": "textfield",
        "value": "10"
      },
      {
        "name": "autoCloseDistance",
        "label": "Auto-Close Distance (meters)",
        "description": "Distance from start point to offer closing polygon",
        "type": "textfield",
        "value": "15"
      }
    ]
  },
  {
    "title": "Backend Services",
    "properties": [
      {
        "name": "servicesEnabled",
        "label": "Enable Backend Validation",
        "type": "checkbox",
        "options": [
          {"value": "true", "label": ""}
        ],
        "value": "true"
      },
      {
        "name": "servicesAppId",
        "label": "Services App ID",
        "description": "App ID where parcel-services plugin is installed",
        "type": "textfield",
        "value": ""
      },
      {
        "name": "checkOverlap",
        "label": "Check for Overlapping Parcels",
        "type": "checkbox",
        "options": [
          {"value": "true", "label": ""}
        ],
        "value": ""
      }
    ]
  },
  {
    "title": "UI Customization",
    "properties": [
      {
        "name": "mapHeight",
        "label": "Map Height (pixels)",
        "type": "textfield",
        "value": "400"
      },
      {
        "name": "showAreaRealtime",
        "label": "Show Area in Real-time",
        "type": "checkbox",
        "options": [
          {"value": "true", "label": ""}
        ],
        "value": "true"
      },
      {
        "name": "showCoordinates",
        "label": "Show Coordinates on Hover",
        "type": "checkbox",
        "options": [
          {"value": "true", "label": ""}
        ],
        "value": "true"
      },
      {
        "name": "polygonFillColor",
        "label": "Polygon Fill Color",
        "type": "textfield",
        "value": "#3388ff"
      },
      {
        "name": "polygonFillOpacity",
        "label": "Polygon Fill Opacity (0-1)",
        "type": "textfield",
        "value": "0.3"
      }
    ]
  }
]
```

#### 2.4 JavaScript Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                  JAVASCRIPT MODULE ARCHITECTURE                      │
└─────────────────────────────────────────────────────────────────────┘

  ┌───────────────────────────────────────────────────────────────────┐
  │                     GLOBAL NAMESPACE                               │
  │                     JogetParcelCapture                             │
  └───────────────────────────────────────────────────────────────────┘
                                 │
           ┌─────────────────────┼─────────────────────┐
           │                     │                     │
           ▼                     ▼                     ▼
  ┌─────────────────┐   ┌─────────────────┐   ┌─────────────────┐
  │   Core Module   │   │   Walk Module   │   │   Draw Module   │
  │                 │   │                 │   │                 │
  │ parcel-capture- │   │ parcel-capture- │   │ parcel-capture- │
  │ core.js         │   │ walk.js         │   │ draw.js         │
  └─────────────────┘   └─────────────────┘   └─────────────────┘
           │                     │                     │
           └─────────────────────┼─────────────────────┘
                                 │
                                 ▼
                    ┌─────────────────────┐
                    │   View Module       │
                    │                     │
                    │ parcel-capture-     │
                    │ view.js             │
                    └─────────────────────┘
```

**Core Module (parcel-capture-core.js):**

```javascript
/**
 * JogetParcelCapture - Core Module
 * 
 * Provides shared functionality for all capture modes:
 * - Map initialization
 * - Tile layer management
 * - GeoJSON serialization/deserialization
 * - Area/perimeter calculations
 * - Form field binding
 * - Event handling
 */
var JogetParcelCapture = (function() {
    'use strict';
    
    // Private state
    var instances = {};
    
    /**
     * ParcelMap class - core map functionality
     */
    function ParcelMap(elementId, config) {
        this.elementId = elementId;
        this.config = $.extend({}, ParcelMap.DEFAULTS, config);
        this.map = null;
        this.drawnLayer = null;
        this.currentPolygon = null;
        this.mode = null;
        
        this._init();
    }
    
    ParcelMap.DEFAULTS = {
        center: [-29.6, 28.2],
        zoom: 8,
        maxZoom: 19,
        tileProvider: 'SATELLITE_ESRI',
        minArea: 0.01,
        maxArea: 1000,
        minVertices: 3,
        maxVertices: 100,
        polygonStyle: {
            color: '#3388ff',
            weight: 2,
            opacity: 1,
            fillColor: '#3388ff',
            fillOpacity: 0.3
        },
        mapHeight: 400
    };
    
    ParcelMap.TILE_PROVIDERS = {
        OSM: {
            url: 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',
            attribution: '© OpenStreetMap contributors',
            maxZoom: 19
        },
        SATELLITE_ESRI: {
            url: 'https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}',
            attribution: '© Esri',
            maxZoom: 19
        },
        HYBRID: {
            url: 'https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}',
            attribution: '© Esri',
            maxZoom: 19,
            overlay: {
                url: 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',
                opacity: 0.4
            }
        },
        TERRAIN: {
            url: 'https://{s}.tile.opentopomap.org/{z}/{x}/{y}.png',
            attribution: '© OpenTopoMap',
            maxZoom: 17
        }
    };
    
    ParcelMap.prototype = {
        /**
         * Initialize the map
         */
        _init: function() {
            var container = document.getElementById(this.elementId);
            if (!container) {
                console.error('ParcelMap: Container not found: ' + this.elementId);
                return;
            }
            
            // Set container height
            container.style.height = this.config.mapHeight + 'px';
            
            // Create map
            this.map = L.map(this.elementId, {
                center: this.config.center,
                zoom: this.config.zoom,
                maxZoom: this.config.maxZoom
            });
            
            // Add tile layer
            this._addTileLayer(this.config.tileProvider);
            
            // Create feature group for drawn items
            this.drawnLayer = new L.FeatureGroup();
            this.map.addLayer(this.drawnLayer);
            
            // Load existing geometry if present
            if (this.config.existingGeometry) {
                this.loadGeometry(this.config.existingGeometry);
            }
        },
        
        /**
         * Add tile layer by provider key
         */
        _addTileLayer: function(providerKey) {
            var provider = ParcelMap.TILE_PROVIDERS[providerKey];
            if (!provider) {
                provider = ParcelMap.TILE_PROVIDERS.OSM;
            }
            
            if (this.tileLayer) {
                this.map.removeLayer(this.tileLayer);
            }
            
            this.tileLayer = L.tileLayer(provider.url, {
                attribution: provider.attribution,
                maxZoom: provider.maxZoom || 19
            }).addTo(this.map);
            
            // Add overlay for hybrid mode
            if (provider.overlay) {
                this.overlayLayer = L.tileLayer(provider.overlay.url, {
                    opacity: provider.overlay.opacity || 0.4
                }).addTo(this.map);
            }
        },
        
        /**
         * Switch tile provider
         */
        switchTiles: function(providerKey) {
            if (this.overlayLayer) {
                this.map.removeLayer(this.overlayLayer);
                this.overlayLayer = null;
            }
            this._addTileLayer(providerKey);
        },
        
        /**
         * Load GeoJSON geometry onto map
         */
        loadGeometry: function(geoJson) {
            if (!geoJson) return;
            
            try {
                var geom = typeof geoJson === 'string' ? JSON.parse(geoJson) : geoJson;
                
                this.drawnLayer.clearLayers();
                
                var layer = L.geoJSON(geom, {
                    style: this.config.polygonStyle
                });
                
                layer.eachLayer(function(l) {
                    this.drawnLayer.addLayer(l);
                    this.currentPolygon = l;
                }.bind(this));
                
                // Fit map to polygon bounds
                if (this.drawnLayer.getLayers().length > 0) {
                    this.map.fitBounds(this.drawnLayer.getBounds(), {
                        padding: [20, 20]
                    });
                }
                
            } catch (e) {
                console.error('ParcelMap: Failed to load geometry', e);
            }
        },
        
        /**
         * Get current polygon as GeoJSON
         */
        getGeometry: function() {
            if (!this.currentPolygon) return null;
            return this.currentPolygon.toGeoJSON();
        },
        
        /**
         * Get geometry as GeoJSON string
         */
        getGeometryString: function() {
            var geom = this.getGeometry();
            return geom ? JSON.stringify(geom) : '';
        },
        
        /**
         * Calculate area in hectares
         */
        calculateArea: function() {
            if (!this.currentPolygon) return 0;
            
            var latlngs = this.currentPolygon.getLatLngs()[0];
            var areaM2 = L.GeometryUtil.geodesicArea(latlngs);
            return (areaM2 / 10000).toFixed(4); // Convert to hectares
        },
        
        /**
         * Calculate perimeter in meters
         */
        calculatePerimeter: function() {
            if (!this.currentPolygon) return 0;
            
            var latlngs = this.currentPolygon.getLatLngs()[0];
            var perimeter = 0;
            
            for (var i = 0; i < latlngs.length - 1; i++) {
                perimeter += latlngs[i].distanceTo(latlngs[i + 1]);
            }
            // Add distance from last to first point
            perimeter += latlngs[latlngs.length - 1].distanceTo(latlngs[0]);
            
            return perimeter.toFixed(2);
        },
        
        /**
         * Calculate centroid
         */
        calculateCentroid: function() {
            if (!this.currentPolygon) return null;
            
            var bounds = this.currentPolygon.getBounds();
            var center = bounds.getCenter();
            
            return {
                lat: center.lat.toFixed(7),
                lon: center.lng.toFixed(7)
            };
        },
        
        /**
         * Validate polygon against rules
         */
        validate: function() {
            var errors = [];
            
            if (!this.currentPolygon) {
                errors.push('No polygon defined');
                return { valid: false, errors: errors };
            }
            
            var latlngs = this.currentPolygon.getLatLngs()[0];
            var vertexCount = latlngs.length;
            var area = parseFloat(this.calculateArea());
            
            // Check vertex count
            if (vertexCount < this.config.minVertices) {
                errors.push('Polygon must have at least ' + this.config.minVertices + ' corners');
            }
            if (vertexCount > this.config.maxVertices) {
                errors.push('Polygon cannot have more than ' + this.config.maxVertices + ' corners');
            }
            
            // Check area
            if (area < this.config.minArea) {
                errors.push('Area is too small (minimum ' + this.config.minArea + ' ha)');
            }
            if (area > this.config.maxArea) {
                errors.push('Area is too large (maximum ' + this.config.maxArea + ' ha)');
            }
            
            return {
                valid: errors.length === 0,
                errors: errors,
                vertexCount: vertexCount,
                area: area
            };
        },
        
        /**
         * Clear current polygon
         */
        clear: function() {
            this.drawnLayer.clearLayers();
            this.currentPolygon = null;
        },
        
        /**
         * Update bound form fields
         */
        updateFormFields: function() {
            var geomField = document.getElementById(this.config.geometryField);
            var areaField = document.getElementById(this.config.areaField);
            var perimeterField = document.getElementById(this.config.perimeterField);
            var centroidLatField = document.getElementById(this.config.centroidLatField);
            var centroidLonField = document.getElementById(this.config.centroidLonField);
            
            if (geomField) {
                geomField.value = this.getGeometryString();
            }
            
            if (areaField) {
                areaField.value = this.calculateArea();
            }
            
            if (perimeterField) {
                perimeterField.value = this.calculatePerimeter();
            }
            
            var centroid = this.calculateCentroid();
            if (centroid) {
                if (centroidLatField) centroidLatField.value = centroid.lat;
                if (centroidLonField) centroidLonField.value = centroid.lon;
            }
        },
        
        /**
         * Locate user's current position
         */
        locateUser: function(callback) {
            var self = this;
            
            if (!navigator.geolocation) {
                callback({ error: 'Geolocation not supported' });
                return;
            }
            
            navigator.geolocation.getCurrentPosition(
                function(position) {
                    var latlng = [position.coords.latitude, position.coords.longitude];
                    self.map.setView(latlng, 17);
                    callback({
                        success: true,
                        latlng: latlng,
                        accuracy: position.coords.accuracy
                    });
                },
                function(error) {
                    callback({ error: error.message });
                },
                {
                    enableHighAccuracy: self.config.gpsHighAccuracy,
                    timeout: 10000,
                    maximumAge: 0
                }
            );
        },
        
        /**
         * Destroy the map instance
         */
        destroy: function() {
            if (this.map) {
                this.map.remove();
                this.map = null;
            }
        }
    };
    
    // Public API
    return {
        /**
         * Create a new ParcelMap instance
         */
        create: function(elementId, config) {
            if (instances[elementId]) {
                instances[elementId].destroy();
            }
            instances[elementId] = new ParcelMap(elementId, config);
            return instances[elementId];
        },
        
        /**
         * Get existing instance
         */
        get: function(elementId) {
            return instances[elementId];
        },
        
        /**
         * Destroy instance
         */
        destroy: function(elementId) {
            if (instances[elementId]) {
                instances[elementId].destroy();
                delete instances[elementId];
            }
        },
        
        /**
         * Expose ParcelMap class for extension
         */
        ParcelMap: ParcelMap
    };
    
})();
```

**Walk Module (parcel-capture-walk.js):**

```javascript
/**
 * JogetParcelCapture - Walk Module
 * 
 * Extends core with GPS boundary walking functionality
 */
(function(JogetParcelCapture) {
    'use strict';
    
    /**
     * WalkMode class
     */
    function WalkMode(parcelMap) {
        this.parcelMap = parcelMap;
        this.map = parcelMap.map;
        this.config = parcelMap.config;
        
        this.isWalking = false;
        this.corners = [];
        this.cornerMarkers = [];
        this.pathLine = null;
        this.currentPositionMarker = null;
        this.watchId = null;
        
        this._init();
    }
    
    WalkMode.prototype = {
        _init: function() {
            // Create UI controls
            this._createControls();
        },
        
        _createControls: function() {
            var self = this;
            
            // Add walk control panel
            var WalkControl = L.Control.extend({
                options: { position: 'topright' },
                onAdd: function(map) {
                    var container = L.DomUtil.create('div', 'parcel-walk-control');
                    container.innerHTML = 
                        '<div class="walk-panel">' +
                        '  <div class="walk-status">' +
                        '    <span class="gps-indicator">●</span>' +
                        '    <span class="gps-accuracy">GPS: --</span>' +
                        '  </div>' +
                        '  <div class="walk-info">' +
                        '    <span class="corner-count">Corners: 0</span>' +
                        '    <span class="est-area">Area: -- ha</span>' +
                        '  </div>' +
                        '  <div class="walk-buttons">' +
                        '    <button class="btn-start-walk" title="Start Walking">🚶 Start</button>' +
                        '    <button class="btn-mark-corner" title="Mark Corner" disabled>📍 Mark Corner</button>' +
                        '    <button class="btn-close-polygon" title="Close Polygon" disabled>✅ Close</button>' +
                        '    <button class="btn-undo" title="Undo Last" disabled>↩️ Undo</button>' +
                        '    <button class="btn-cancel" title="Cancel" disabled>❌ Cancel</button>' +
                        '  </div>' +
                        '</div>';
                    
                    // Prevent map click events on control
                    L.DomEvent.disableClickPropagation(container);
                    
                    return container;
                }
            });
            
            this.walkControl = new WalkControl();
            this.map.addControl(this.walkControl);
            
            // Bind button events
            this._bindEvents();
        },
        
        _bindEvents: function() {
            var self = this;
            var container = this.walkControl.getContainer();
            
            container.querySelector('.btn-start-walk').addEventListener('click', function() {
                self.startWalking();
            });
            
            container.querySelector('.btn-mark-corner').addEventListener('click', function() {
                self.markCorner();
            });
            
            container.querySelector('.btn-close-polygon').addEventListener('click', function() {
                self.closePolygon();
            });
            
            container.querySelector('.btn-undo').addEventListener('click', function() {
                self.undoLastCorner();
            });
            
            container.querySelector('.btn-cancel').addEventListener('click', function() {
                self.cancelWalking();
            });
        },
        
        startWalking: function() {
            var self = this;
            
            if (this.isWalking) return;
            
            // Check for geolocation support
            if (!navigator.geolocation) {
                alert('GPS is not available on this device');
                return;
            }
            
            this.isWalking = true;
            this.corners = [];
            this.cornerMarkers = [];
            this.parcelMap.clear();
            
            // Update UI
            this._setButtonStates(true);
            this._updateStatus('Acquiring GPS...', 'acquiring');
            
            // Start watching position
            this.watchId = navigator.geolocation.watchPosition(
                function(position) {
                    self._onPositionUpdate(position);
                },
                function(error) {
                    self._onPositionError(error);
                },
                {
                    enableHighAccuracy: true,
                    timeout: 10000,
                    maximumAge: 0
                }
            );
        },
        
        _onPositionUpdate: function(position) {
            var lat = position.coords.latitude;
            var lon = position.coords.longitude;
            var accuracy = position.coords.accuracy;
            
            // Update current position marker
            var latlng = L.latLng(lat, lon);
            
            if (this.currentPositionMarker) {
                this.currentPositionMarker.setLatLng(latlng);
            } else {
                this.currentPositionMarker = L.circleMarker(latlng, {
                    radius: 8,
                    fillColor: '#4285f4',
                    fillOpacity: 1,
                    color: '#fff',
                    weight: 2
                }).addTo(this.map);
                
                // Center map on first position
                this.map.setView(latlng, 17);
            }
            
            // Update accuracy indicator
            var accuracyClass = accuracy <= 5 ? 'excellent' : 
                               accuracy <= 10 ? 'good' : 
                               accuracy <= 20 ? 'fair' : 'poor';
            
            this._updateStatus(
                'GPS: ±' + accuracy.toFixed(1) + 'm', 
                accuracyClass
            );
            
            // Check if near starting point
            if (this.corners.length >= 3) {
                var distToStart = latlng.distanceTo(this.corners[0]);
                if (distToStart <= this.config.autoCloseDistance) {
                    this._showCloseHint();
                }
            }
            
            // Store current position for marking
            this.currentPosition = {
                latlng: latlng,
                accuracy: accuracy,
                timestamp: position.timestamp
            };
        },
        
        _onPositionError: function(error) {
            this._updateStatus('GPS Error: ' + error.message, 'error');
        },
        
        markCorner: function() {
            if (!this.currentPosition) {
                alert('Waiting for GPS position...');
                return;
            }
            
            if (this.currentPosition.accuracy > this.config.gpsMinAccuracy) {
                if (!confirm('GPS accuracy is ' + this.currentPosition.accuracy.toFixed(1) + 
                             'm. Recommended accuracy is under ' + this.config.gpsMinAccuracy + 
                             'm. Mark corner anyway?')) {
                    return;
                }
            }
            
            var latlng = this.currentPosition.latlng;
            var cornerNum = this.corners.length + 1;
            
            // Add corner marker
            var marker = L.marker(latlng, {
                icon: L.divIcon({
                    className: 'corner-marker',
                    html: '<div class="corner-number">' + cornerNum + '</div>',
                    iconSize: [24, 24]
                })
            }).addTo(this.map);
            
            this.corners.push(latlng);
            this.cornerMarkers.push(marker);
            
            // Update path line
            this._updatePathLine();
            
            // Update UI
            this._updateCornerCount();
            this._updateEstimatedArea();
            
            // Enable close button if enough corners
            if (this.corners.length >= 3) {
                this._enableButton('.btn-close-polygon');
            }
        },
        
        _updatePathLine: function() {
            if (this.pathLine) {
                this.map.removeLayer(this.pathLine);
            }
            
            if (this.corners.length >= 2) {
                this.pathLine = L.polyline(this.corners, {
                    color: '#3388ff',
                    weight: 3,
                    dashArray: '10, 5'
                }).addTo(this.map);
            }
        },
        
        closePolygon: function() {
            if (this.corners.length < 3) {
                alert('At least 3 corners are required');
                return;
            }
            
            // Stop GPS tracking
            this.stopWalking();
            
            // Create polygon from corners
            var polygon = L.polygon(this.corners, this.parcelMap.config.polygonStyle);
            this.parcelMap.drawnLayer.addLayer(polygon);
            this.parcelMap.currentPolygon = polygon;
            
            // Remove temporary markers and lines
            this._cleanup();
            
            // Update form fields
            this.parcelMap.updateFormFields();
            
            // Show summary
            this._showSummary();
            
            // Reset UI
            this._setButtonStates(false);
        },
        
        undoLastCorner: function() {
            if (this.corners.length === 0) return;
            
            this.corners.pop();
            
            var marker = this.cornerMarkers.pop();
            if (marker) {
                this.map.removeLayer(marker);
            }
            
            this._updatePathLine();
            this._updateCornerCount();
            this._updateEstimatedArea();
            
            if (this.corners.length < 3) {
                this._disableButton('.btn-close-polygon');
            }
        },
        
        cancelWalking: function() {
            if (confirm('Cancel boundary capture? All marked corners will be lost.')) {
                this.stopWalking();
                this._cleanup();
                this.corners = [];
                this.cornerMarkers = [];
                this._setButtonStates(false);
            }
        },
        
        stopWalking: function() {
            if (this.watchId) {
                navigator.geolocation.clearWatch(this.watchId);
                this.watchId = null;
            }
            this.isWalking = false;
        },
        
        _cleanup: function() {
            // Remove path line
            if (this.pathLine) {
                this.map.removeLayer(this.pathLine);
                this.pathLine = null;
            }
            
            // Remove corner markers
            this.cornerMarkers.forEach(function(marker) {
                this.map.removeLayer(marker);
            }.bind(this));
            this.cornerMarkers = [];
            
            // Remove position marker
            if (this.currentPositionMarker) {
                this.map.removeLayer(this.currentPositionMarker);
                this.currentPositionMarker = null;
            }
        },
        
        _setButtonStates: function(walking) {
            var container = this.walkControl.getContainer();
            
            container.querySelector('.btn-start-walk').disabled = walking;
            container.querySelector('.btn-mark-corner').disabled = !walking;
            container.querySelector('.btn-undo').disabled = !walking;
            container.querySelector('.btn-cancel').disabled = !walking;
            container.querySelector('.btn-close-polygon').disabled = true;
        },
        
        _enableButton: function(selector) {
            var btn = this.walkControl.getContainer().querySelector(selector);
            if (btn) btn.disabled = false;
        },
        
        _disableButton: function(selector) {
            var btn = this.walkControl.getContainer().querySelector(selector);
            if (btn) btn.disabled = true;
        },
        
        _updateStatus: function(text, className) {
            var container = this.walkControl.getContainer();
            var indicator = container.querySelector('.gps-indicator');
            var accuracy = container.querySelector('.gps-accuracy');
            
            indicator.className = 'gps-indicator ' + (className || '');
            accuracy.textContent = text;
        },
        
        _updateCornerCount: function() {
            var container = this.walkControl.getContainer();
            container.querySelector('.corner-count').textContent = 
                'Corners: ' + this.corners.length;
        },
        
        _updateEstimatedArea: function() {
            var container = this.walkControl.getContainer();
            var areaSpan = container.querySelector('.est-area');
            
            if (this.corners.length >= 3) {
                // Create temporary polygon to calculate area
                var tempPolygon = L.polygon(this.corners);
                var latlngs = tempPolygon.getLatLngs()[0];
                var areaM2 = L.GeometryUtil.geodesicArea(latlngs);
                var areaHa = (areaM2 / 10000).toFixed(2);
                areaSpan.textContent = 'Area: ~' + areaHa + ' ha';
            } else {
                areaSpan.textContent = 'Area: -- ha';
            }
        },
        
        _showCloseHint: function() {
            var container = this.walkControl.getContainer();
            var closeBtn = container.querySelector('.btn-close-polygon');
            closeBtn.classList.add('highlight');
        },
        
        _showSummary: function() {
            var validation = this.parcelMap.validate();
            var area = this.parcelMap.calculateArea();
            var perimeter = this.parcelMap.calculatePerimeter();
            
            alert(
                'Parcel Captured!\n\n' +
                'Area: ' + area + ' hectares\n' +
                'Perimeter: ' + perimeter + ' meters\n' +
                'Corners: ' + this.corners.length + '\n\n' +
                (validation.valid ? '✅ Validation passed' : '⚠️ ' + validation.errors.join('\n'))
            );
        }
    };
    
    // Extend JogetParcelCapture
    JogetParcelCapture.WalkMode = WalkMode;
    
    JogetParcelCapture.enableWalkMode = function(elementId) {
        var instance = JogetParcelCapture.get(elementId);
        if (instance) {
            instance.walkMode = new WalkMode(instance);
            return instance.walkMode;
        }
        return null;
    };
    
})(JogetParcelCapture);
```

---

### 3. Plugin 2: joget-parcel-services (Backend)

#### 3.1 Project Structure

```
joget-parcel-services/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── org/joget/marketplace/parcel/
│   │   │       ├── service/
│   │   │       │   ├── ParcelGeometryWebService.java    ← Web Service Plugin
│   │   │       │   ├── ParcelValidationTool.java        ← Process Tool Plugin
│   │   │       │   └── ParcelOverlapChecker.java        ← Process Tool Plugin
│   │   │       │
│   │   │       ├── geometry/
│   │   │       │   ├── GeometryEngine.java              ← JTS wrapper
│   │   │       │   ├── AreaCalculator.java              
│   │   │       │   ├── PolygonValidator.java            
│   │   │       │   └── SpatialQueryService.java         
│   │   │       │
│   │   │       ├── model/
│   │   │       │   ├── ParcelGeometryRequest.java       
│   │   │       │   ├── ParcelGeometryResponse.java      
│   │   │       │   ├── ValidationResult.java            
│   │   │       │   └── OverlapResult.java               
│   │   │       │
│   │   │       └── util/
│   │   │           ├── GeoJsonParser.java               
│   │   │           └── CoordinateTransformer.java       
│   │   │
│   │   └── resources/
│   │       ├── messages/
│   │       │   └── ParcelServices_en.properties
│   │       └── properties/
│   │           ├── ParcelGeometryWebService.json
│   │           ├── ParcelValidationTool.json
│   │           └── ParcelOverlapChecker.json
│   │
│   └── test/
│       └── java/
│           └── org/joget/marketplace/parcel/
│               ├── GeometryEngineTest.java
│               └── PolygonValidatorTest.java
│
└── README.md
```

#### 3.2 Class Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                     PLUGIN 2: CLASS DIAGRAM                          │
└─────────────────────────────────────────────────────────────────────┘

  ┌───────────────────────────────────────────────────────────────────┐
  │                    Joget Plugin Interfaces                         │
  │                                                                    │
  │  ┌──────────────────────┐         ┌──────────────────────┐        │
  │  │   WebServicePlugin   │         │  DefaultApplicationPlugin │   │
  │  │   (interface)        │         │  (abstract)               │   │
  │  └──────────────────────┘         └──────────────────────┘        │
  │            △                                 △                     │
  │            │                                 │                     │
  └────────────┼─────────────────────────────────┼─────────────────────┘
               │                                 │
  ┌────────────┼─────────────────────────────────┼─────────────────────┐
  │            │   org.joget.marketplace.parcel  │                     │
  │            │                                 │                     │
  │  ┌─────────┴──────────────┐      ┌──────────┴───────────────┐     │
  │  │ ParcelGeometryWebService│      │ ParcelValidationTool     │     │
  │  ├────────────────────────┤      ├──────────────────────────┤     │
  │  │ + processRequest()     │      │ + execute()              │     │
  │  │ + calculateArea()      │      │ + validateGeometry()     │     │
  │  │ + calculateCentroid()  │      │ + checkMinArea()         │     │
  │  │ + validateGeometry()   │      │ + checkMaxArea()         │     │
  │  │ + simplifyGeometry()   │      │ + checkSelfIntersection()│     │
  │  └────────────────────────┘      └──────────────────────────┘     │
  │            │                                 │                     │
  │            │         ┌──────────────────────┐│                     │
  │            │         │ParcelOverlapChecker  ││                     │
  │            │         ├──────────────────────┤│                     │
  │            │         │+ execute()           ││                     │
  │            │         │+ findOverlaps()      ││                     │
  │            │         │+ calculateOverlapArea││                     │
  │            │         └──────────────────────┘│                     │
  │            │                    │            │                     │
  │            └────────────┬───────┴────────────┘                     │
  │                         │                                          │
  │                         ▼                                          │
  │            ┌──────────────────────────┐                            │
  │            │     GeometryEngine       │                            │
  │            ├──────────────────────────┤                            │
  │            │ - geometryFactory        │                            │
  │            ├──────────────────────────┤                            │
  │            │ + parseGeoJson()         │                            │
  │            │ + toGeoJson()            │                            │
  │            │ + calculateArea()        │                            │
  │            │ + calculatePerimeter()   │                            │
  │            │ + calculateCentroid()    │                            │
  │            │ + isValid()              │                            │
  │            │ + isSelfIntersecting()   │                            │
  │            │ + simplify()             │                            │
  │            │ + buffer()               │                            │
  │            │ + intersects()           │                            │
  │            │ + intersection()         │                            │
  │            │ + union()                │                            │
  │            └──────────────────────────┘                            │
  │                         │                                          │
  │                         │ uses                                     │
  │                         ▼                                          │
  │            ┌──────────────────────────┐                            │
  │            │    JTS Topology Suite    │                            │
  │            │    (External Library)    │                            │
  │            │                          │                            │
  │            │  org.locationtech.jts    │                            │
  │            └──────────────────────────┘                            │
  │                                                                    │
  │  ┌──────────────────────┐      ┌──────────────────────┐           │
  │  │   Model Classes      │      │   Utility Classes    │           │
  │  ├──────────────────────┤      ├──────────────────────┤           │
  │  │ ParcelGeometryRequest│      │ GeoJsonParser        │           │
  │  │ ParcelGeometryResponse│     │ CoordinateTransformer│           │
  │  │ ValidationResult     │      └──────────────────────┘           │
  │  │ OverlapResult        │                                          │
  │  └──────────────────────┘                                          │
  │                                                                    │
  └────────────────────────────────────────────────────────────────────┘
```

#### 3.3 Maven Dependencies (pom.xml)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
                             http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <groupId>org.joget.marketplace</groupId>
    <artifactId>joget-parcel-services</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>
    
    <name>Joget Parcel Services</name>
    <description>Backend geometry services for parcel capture</description>
    
    <properties>
        <java.version>11</java.version>
        <joget.version>8.0-SNAPSHOT</joget.version>
        <jts.version>1.19.0</jts.version>
        <gson.version>2.10.1</gson.version>
    </properties>
    
    <dependencies>
        <!-- Joget DX8 -->
        <dependency>
            <groupId>org.joget</groupId>
            <artifactId>wflow-core</artifactId>
            <version>${joget.version}</version>
            <scope>provided</scope>
        </dependency>
        
        <!-- JTS Topology Suite - Geometry operations -->
        <dependency>
            <groupId>org.locationtech.jts</groupId>
            <artifactId>jts-core</artifactId>
            <version>${jts.version}</version>
        </dependency>
        
        <!-- JTS IO for GeoJSON -->
        <dependency>
            <groupId>org.locationtech.jts.io</groupId>
            <artifactId>jts-io-common</artifactId>
            <version>${jts.version}</version>
        </dependency>
        
        <!-- JSON Processing -->
        <dependency>
            <groupId>com.google.code.gson</groupId>
            <artifactId>gson</artifactId>
            <version>${gson.version}</version>
        </dependency>
        
        <!-- Testing -->
        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <version>4.13.2</version>
            <scope>test</scope>
        </dependency>
    </dependencies>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
                <configuration>
                    <source>${java.version}</source>
                    <target>${java.version}</target>
                </configuration>
            </plugin>
            
            <!-- Bundle dependencies into plugin JAR -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-shade-plugin</artifactId>
                <version>3.5.0</version>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals>
                            <goal>shade</goal>
                        </goals>
                        <configuration>
                            <artifactSet>
                                <includes>
                                    <include>org.locationtech.jts:*</include>
                                </includes>
                            </artifactSet>
                            <relocations>
                                <relocation>
                                    <pattern>org.locationtech.jts</pattern>
                                    <shadedPattern>org.joget.marketplace.jts</shadedPattern>
                                </relocation>
                            </relocations>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

#### 3.4 Core Java Classes

**GeometryEngine.java:**

```java
package org.joget.marketplace.parcel.geometry;

import org.locationtech.jts.geom.*;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.geojson.GeoJsonReader;
import org.locationtech.jts.io.geojson.GeoJsonWriter;
import org.locationtech.jts.operation.valid.IsValidOp;
import org.locationtech.jts.operation.valid.TopologyValidationError;
import org.locationtech.jts.simplify.DouglasPeuckerSimplifier;
import org.locationtech.jts.algorithm.Centroid;

import java.util.ArrayList;
import java.util.List;

/**
 * Core geometry engine wrapping JTS Topology Suite.
 * Provides all spatial operations needed for parcel management.
 */
public class GeometryEngine {
    
    private static final double EARTH_RADIUS_METERS = 6371000.0;
    private static final double SQ_METERS_PER_HECTARE = 10000.0;
    
    private final GeometryFactory geometryFactory;
    private final GeoJsonReader geoJsonReader;
    private final GeoJsonWriter geoJsonWriter;
    
    public GeometryEngine() {
        this.geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
        this.geoJsonReader = new GeoJsonReader(geometryFactory);
        this.geoJsonWriter = new GeoJsonWriter();
        this.geoJsonWriter.setEncodeCRS(false);
    }
    
    /**
     * Parse GeoJSON string to JTS Geometry
     */
    public Geometry parseGeoJson(String geoJson) throws ParseException {
        if (geoJson == null || geoJson.trim().isEmpty()) {
            throw new IllegalArgumentException("GeoJSON string cannot be null or empty");
        }
        return geoJsonReader.read(geoJson);
    }
    
    /**
     * Convert JTS Geometry to GeoJSON string
     */
    public String toGeoJson(Geometry geometry) {
        if (geometry == null) {
            return null;
        }
        return geoJsonWriter.write(geometry);
    }
    
    /**
     * Calculate geodesic area in hectares.
     * Uses spherical approximation for accuracy.
     */
    public double calculateAreaHectares(Geometry geometry) {
        if (!(geometry instanceof Polygon) && !(geometry instanceof MultiPolygon)) {
            throw new IllegalArgumentException("Geometry must be Polygon or MultiPolygon");
        }
        
        double areaM2 = calculateGeodesicAreaM2(geometry);
        return areaM2 / SQ_METERS_PER_HECTARE;
    }
    
    /**
     * Calculate geodesic area in square meters using Shoelace formula
     * with spherical Earth approximation.
     */
    private double calculateGeodesicAreaM2(Geometry geometry) {
        if (geometry instanceof MultiPolygon) {
            double totalArea = 0;
            for (int i = 0; i < geometry.getNumGeometries(); i++) {
                totalArea += calculateGeodesicAreaM2(geometry.getGeometryN(i));
            }
            return totalArea;
        }
        
        Polygon polygon = (Polygon) geometry;
        Coordinate[] coords = polygon.getExteriorRing().getCoordinates();
        
        // Calculate area using spherical excess formula
        double area = 0;
        int n = coords.length - 1; // Exclude closing coordinate
        
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            
            double lat1 = Math.toRadians(coords[i].y);
            double lon1 = Math.toRadians(coords[i].x);
            double lat2 = Math.toRadians(coords[j].y);
            double lon2 = Math.toRadians(coords[j].x);
            
            area += (lon2 - lon1) * (2 + Math.sin(lat1) + Math.sin(lat2));
        }
        
        area = Math.abs(area * EARTH_RADIUS_METERS * EARTH_RADIUS_METERS / 2.0);
        
        // Subtract holes
        for (int h = 0; h < polygon.getNumInteriorRing(); h++) {
            Coordinate[] holeCoords = polygon.getInteriorRingN(h).getCoordinates();
            Polygon holePoly = geometryFactory.createPolygon(holeCoords);
            area -= calculateGeodesicAreaM2(holePoly);
        }
        
        return area;
    }
    
    /**
     * Calculate geodesic perimeter in meters.
     */
    public double calculatePerimeterMeters(Geometry geometry) {
        if (!(geometry instanceof Polygon) && !(geometry instanceof MultiPolygon)) {
            throw new IllegalArgumentException("Geometry must be Polygon or MultiPolygon");
        }
        
        if (geometry instanceof MultiPolygon) {
            double totalPerimeter = 0;
            for (int i = 0; i < geometry.getNumGeometries(); i++) {
                totalPerimeter += calculatePerimeterMeters(geometry.getGeometryN(i));
            }
            return totalPerimeter;
        }
        
        Polygon polygon = (Polygon) geometry;
        Coordinate[] coords = polygon.getExteriorRing().getCoordinates();
        
        double perimeter = 0;
        for (int i = 0; i < coords.length - 1; i++) {
            perimeter += haversineDistance(
                coords[i].y, coords[i].x,
                coords[i+1].y, coords[i+1].x
            );
        }
        
        return perimeter;
    }
    
    /**
     * Haversine formula for distance between two coordinates in meters.
     */
    private double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return EARTH_RADIUS_METERS * c;
    }
    
    /**
     * Calculate centroid coordinates.
     */
    public Coordinate calculateCentroid(Geometry geometry) {
        Point centroid = geometry.getCentroid();
        return centroid.getCoordinate();
    }
    
    /**
     * Validate geometry topology.
     */
    public ValidationResult validate(Geometry geometry) {
        ValidationResult result = new ValidationResult();
        
        if (geometry == null) {
            result.setValid(false);
            result.addError("Geometry is null");
            return result;
        }
        
        IsValidOp validOp = new IsValidOp(geometry);
        result.setValid(validOp.isValid());
        
        if (!validOp.isValid()) {
            TopologyValidationError error = validOp.getValidationError();
            result.addError(error.getMessage() + " at " + error.getCoordinate());
        }
        
        return result;
    }
    
    /**
     * Check if polygon is self-intersecting.
     */
    public boolean isSelfIntersecting(Geometry geometry) {
        if (!(geometry instanceof Polygon)) {
            return false;
        }
        
        IsValidOp validOp = new IsValidOp(geometry);
        if (!validOp.isValid()) {
            TopologyValidationError error = validOp.getValidationError();
            return error.getErrorType() == TopologyValidationError.SELF_INTERSECTION ||
                   error.getErrorType() == TopologyValidationError.RING_SELF_INTERSECTION;
        }
        
        return false;
    }
    
    /**
     * Simplify geometry using Douglas-Peucker algorithm.
     * Tolerance is in degrees (approximately 0.00001 = 1 meter at equator).
     */
    public Geometry simplify(Geometry geometry, double toleranceDegrees) {
        return DouglasPeuckerSimplifier.simplify(geometry, toleranceDegrees);
    }
    
    /**
     * Create buffer around geometry.
     * Distance is in degrees.
     */
    public Geometry buffer(Geometry geometry, double distanceDegrees) {
        return geometry.buffer(distanceDegrees);
    }
    
    /**
     * Check if two geometries intersect.
     */
    public boolean intersects(Geometry geom1, Geometry geom2) {
        return geom1.intersects(geom2);
    }
    
    /**
     * Calculate intersection of two geometries.
     */
    public Geometry intersection(Geometry geom1, Geometry geom2) {
        return geom1.intersection(geom2);
    }
    
    /**
     * Calculate union of two geometries.
     */
    public Geometry union(Geometry geom1, Geometry geom2) {
        return geom1.union(geom2);
    }
    
    /**
     * Get number of vertices in geometry.
     */
    public int getVertexCount(Geometry geometry) {
        return geometry.getNumPoints();
    }
    
    /**
     * Get bounding box of geometry.
     */
    public Envelope getBoundingBox(Geometry geometry) {
        return geometry.getEnvelopeInternal();
    }
    
    /**
     * Create polygon from coordinate array.
     */
    public Polygon createPolygon(double[][] coordinates) {
        Coordinate[] coords = new Coordinate[coordinates.length];
        for (int i = 0; i < coordinates.length; i++) {
            coords[i] = new Coordinate(coordinates[i][0], coordinates[i][1]);
        }
        return geometryFactory.createPolygon(coords);
    }
    
    /**
     * Validation result container.
     */
    public static class ValidationResult {
        private boolean valid = true;
        private List<String> errors = new ArrayList<>();
        
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        
        public List<String> getErrors() { return errors; }
        public void addError(String error) { 
            this.errors.add(error);
            this.valid = false;
        }
    }
}
```

**ParcelGeometryWebService.java:**

```java
package org.joget.marketplace.parcel.service;

import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.ExtDefaultPlugin;
import org.joget.plugin.base.PluginWebSupport;
import org.joget.marketplace.parcel.geometry.GeometryEngine;
import org.joget.marketplace.parcel.model.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;

/**
 * Web Service Plugin providing REST API for parcel geometry operations.
 * 
 * Endpoints:
 *   POST /calculate  - Calculate area, perimeter, centroid
 *   POST /validate   - Validate geometry against rules
 *   POST /simplify   - Simplify geometry (reduce vertices)
 *   POST /checkOverlap - Check for overlapping parcels
 */
public class ParcelGeometryWebService extends ExtDefaultPlugin implements PluginWebSupport {
    
    private static final String NAME = "Parcel Geometry Web Service";
    private static final String VERSION = "1.0.0";
    
    private final GeometryEngine geometryEngine = new GeometryEngine();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    @Override
    public String getName() {
        return NAME;
    }
    
    @Override
    public String getVersion() {
        return VERSION;
    }
    
    @Override
    public String getDescription() {
        return "REST API for parcel geometry calculations and validation";
    }
    
    @Override
    public void webService(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter writer = response.getWriter();
        
        try {
            String action = request.getParameter("action");
            
            if (action == null || action.isEmpty()) {
                writeError(writer, "Missing 'action' parameter");
                return;
            }
            
            switch (action) {
                case "calculate":
                    handleCalculate(request, writer);
                    break;
                case "validate":
                    handleValidate(request, writer);
                    break;
                case "simplify":
                    handleSimplify(request, writer);
                    break;
                case "checkOverlap":
                    handleCheckOverlap(request, writer);
                    break;
                default:
                    writeError(writer, "Unknown action: " + action);
            }
            
        } catch (Exception e) {
            LogUtil.error(getClass().getName(), e, "Error processing request");
            writeError(writer, "Server error: " + e.getMessage());
        }
    }
    
    /**
     * Calculate area, perimeter, and centroid for given geometry.
     */
    private void handleCalculate(HttpServletRequest request, PrintWriter writer) 
            throws Exception {
        
        String geoJson = request.getParameter("geometry");
        
        if (geoJson == null || geoJson.isEmpty()) {
            writeError(writer, "Missing 'geometry' parameter");
            return;
        }
        
        Geometry geometry = geometryEngine.parseGeoJson(geoJson);
        
        ParcelGeometryResponse response = new ParcelGeometryResponse();
        response.setSuccess(true);
        response.setAreaHectares(geometryEngine.calculateAreaHectares(geometry));
        response.setPerimeterMeters(geometryEngine.calculatePerimeterMeters(geometry));
        
        Coordinate centroid = geometryEngine.calculateCentroid(geometry);
        response.setCentroidLat(centroid.y);
        response.setCentroidLon(centroid.x);
        
        response.setVertexCount(geometryEngine.getVertexCount(geometry));
        
        writer.write(gson.toJson(response));
    }
    
    /**
     * Validate geometry against configurable rules.
     */
    private void handleValidate(HttpServletRequest request, PrintWriter writer) 
            throws Exception {
        
        String geoJson = request.getParameter("geometry");
        double minArea = parseDouble(request.getParameter("minArea"), 0.01);
        double maxArea = parseDouble(request.getParameter("maxArea"), 1000);
        int minVertices = parseInt(request.getParameter("minVertices"), 3);
        int maxVertices = parseInt(request.getParameter("maxVertices"), 100);
        boolean allowSelfIntersection = "true".equals(request.getParameter("allowSelfIntersection"));
        
        if (geoJson == null || geoJson.isEmpty()) {
            writeError(writer, "Missing 'geometry' parameter");
            return;
        }
        
        Geometry geometry = geometryEngine.parseGeoJson(geoJson);
        
        ValidationResult result = new ValidationResult();
        result.setValid(true);
        
        // Topology validation
        GeometryEngine.ValidationResult topoResult = geometryEngine.validate(geometry);
        if (!topoResult.isValid()) {
            result.setValid(false);
            result.getErrors().addAll(topoResult.getErrors());
        }
        
        // Self-intersection check
        if (!allowSelfIntersection && geometryEngine.isSelfIntersecting(geometry)) {
            result.setValid(false);
            result.addError("Polygon is self-intersecting");
        }
        
        // Area validation
        double area = geometryEngine.calculateAreaHectares(geometry);
        result.setAreaHectares(area);
        
        if (area < minArea) {
            result.setValid(false);
            result.addError("Area (" + area + " ha) is below minimum (" + minArea + " ha)");
        }
        
        if (area > maxArea) {
            result.setValid(false);
            result.addError("Area (" + area + " ha) exceeds maximum (" + maxArea + " ha)");
        }
        
        // Vertex count validation
        int vertexCount = geometryEngine.getVertexCount(geometry);
        result.setVertexCount(vertexCount);
        
        if (vertexCount < minVertices) {
            result.setValid(false);
            result.addError("Polygon has " + vertexCount + " vertices, minimum is " + minVertices);
        }
        
        if (vertexCount > maxVertices) {
            result.setValid(false);
            result.addError("Polygon has " + vertexCount + " vertices, maximum is " + maxVertices);
        }
        
        writer.write(gson.toJson(result));
    }
    
    /**
     * Simplify geometry to reduce vertex count.
     */
    private void handleSimplify(HttpServletRequest request, PrintWriter writer) 
            throws Exception {
        
        String geoJson = request.getParameter("geometry");
        double tolerance = parseDouble(request.getParameter("tolerance"), 0.00001);
        
        if (geoJson == null || geoJson.isEmpty()) {
            writeError(writer, "Missing 'geometry' parameter");
            return;
        }
        
        Geometry geometry = geometryEngine.parseGeoJson(geoJson);
        Geometry simplified = geometryEngine.simplify(geometry, tolerance);
        
        ParcelGeometryResponse response = new ParcelGeometryResponse();
        response.setSuccess(true);
        response.setGeometry(geometryEngine.toGeoJson(simplified));
        response.setVertexCount(geometryEngine.getVertexCount(simplified));
        response.setOriginalVertexCount(geometryEngine.getVertexCount(geometry));
        
        writer.write(gson.toJson(response));
    }
    
    /**
     * Check for overlapping parcels in database.
     */
    private void handleCheckOverlap(HttpServletRequest request, PrintWriter writer) 
            throws Exception {
        
        String geoJson = request.getParameter("geometry");
        String excludeParcelId = request.getParameter("excludeParcelId");
        String tableName = request.getParameter("tableName");
        String geometryColumn = request.getParameter("geometryColumn");
        
        if (geoJson == null || geoJson.isEmpty()) {
            writeError(writer, "Missing 'geometry' parameter");
            return;
        }
        
        // This would query the database for overlapping parcels
        // Implementation depends on your data access layer
        
        OverlapResult result = checkOverlapsInDatabase(
            geoJson, 
            excludeParcelId, 
            tableName, 
            geometryColumn
        );
        
        writer.write(gson.toJson(result));
    }
    
    /**
     * Check for overlapping parcels in the database.
     * Uses spatial query to find intersecting geometries.
     */
    private OverlapResult checkOverlapsInDatabase(
            String geoJson, 
            String excludeParcelId,
            String tableName,
            String geometryColumn) {
        
        OverlapResult result = new OverlapResult();
        result.setHasOverlaps(false);
        
        // TODO: Implement database query
        // This requires access to FormDataDao and spatial queries
        // For MySQL with JSON geometry:
        // SELECT id, geometry FROM table 
        // WHERE ST_Intersects(ST_GeomFromGeoJSON(geometry), ST_GeomFromGeoJSON(?))
        // AND id != ?
        
        return result;
    }
    
    private void writeError(PrintWriter writer, String message) {
        ParcelGeometryResponse response = new ParcelGeometryResponse();
        response.setSuccess(false);
        response.setError(message);
        writer.write(gson.toJson(response));
    }
    
    private double parseDouble(String value, double defaultValue) {
        if (value == null || value.isEmpty()) return defaultValue;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    private int parseInt(String value, int defaultValue) {
        if (value == null || value.isEmpty()) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
```

---

### 4. API Contract

```yaml
openapi: 3.0.0
info:
  title: Parcel Geometry Services API
  version: 1.0.0
  description: Backend services for parcel geometry operations

servers:
  - url: /jw/web/json/plugin/org.joget.marketplace.parcel.service.ParcelGeometryWebService/service

paths:
  /?action=calculate:
    post:
      summary: Calculate geometry metrics
      parameters:
        - name: geometry
          in: query
          required: true
          schema:
            type: string
          description: GeoJSON geometry string
      responses:
        '200':
          description: Successful calculation
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/CalculateResponse'

  /?action=validate:
    post:
      summary: Validate geometry against rules
      parameters:
        - name: geometry
          in: query
          required: true
          schema:
            type: string
        - name: minArea
          in: query
          schema:
            type: number
            default: 0.01
        - name: maxArea
          in: query
          schema:
            type: number
            default: 1000
        - name: minVertices
          in: query
          schema:
            type: integer
            default: 3
        - name: maxVertices
          in: query
          schema:
            type: integer
            default: 100
        - name: allowSelfIntersection
          in: query
          schema:
            type: boolean
            default: false
      responses:
        '200':
          description: Validation result
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ValidationResponse'

components:
  schemas:
    CalculateResponse:
      type: object
      properties:
        success:
          type: boolean
        areaHectares:
          type: number
        perimeterMeters:
          type: number
        centroidLat:
          type: number
        centroidLon:
          type: number
        vertexCount:
          type: integer
        error:
          type: string

    ValidationResponse:
      type: object
      properties:
        valid:
          type: boolean
        errors:
          type: array
          items:
            type: string
        areaHectares:
          type: number
        vertexCount:
          type: integer
```
