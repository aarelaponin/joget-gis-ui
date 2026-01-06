## Current State Analysis

My current form (`f01_02.json`) captures only a **single GPS point** (latitude/longitude coordinates), which is insufficient for defining land parcel boundaries. For proper land parcel capture, you need **polygon geometry** - a series of coordinates that define the boundary of each parcel.

## Recommended UI Architecture for Land Parcel Capture

### 1. Data Model Extension

First, extend your data model to support polygon geometry:

```
Table: farm_parcel
---------------------------------------------
Field                  | Type              | Description
---------------------------------------------
id                     | VARCHAR(36)       | Primary key
parent_id              | VARCHAR(36)       | FK to farmer
parcel_code            | VARCHAR(20)       | Unique parcel identifier
parcel_name            | VARCHAR(100)      | User-friendly name
parcel_geometry        | TEXT/MEDIUMTEXT   | GeoJSON polygon
area_hectares          | DECIMAL(10,4)     | Auto-calculated from polygon
centroid_lat           | DECIMAL(10,7)     | Centroid latitude
centroid_lon           | DECIMAL(10,7)     | Centroid longitude
land_use_type          | VARCHAR(20)       | FK to MDM (CROP/LIVESTOCK/MIXED)
tenure_type            | VARCHAR(20)       | FK to MDM (OWNED/RENTED/LEASED)
```

### 2. UI Organization - Three-Panel Approach

I recommend organizing the land parcel capture UI into a three-panel layout:

```
+------------------------------------------------------------------+
|                    FARMER LAND PARCELS                           |
+------------------------------------------------------------------+
|  [+ Add Parcel]   [📥 Import GeoJSON]   [📤 Export All]          |
+------------------------------------------------------------------+
|                                                                   |
|  +------------------+  +-------------------------------------+   |
|  | PARCEL LIST      |  | MAP VIEW                            |   |
|  |------------------|  |                                     |   |
|  | ● Parcel A       |  |    [Satellite] [Terrain] [Hybrid]   |   |
|  |   2.5 ha, Owned  |  |   +-----------------------------+   |   |
|  |                  |  |   |                             |   |   |
|  | ○ Parcel B       |  |   |     Interactive Map         |   |   |
|  |   1.2 ha, Rented |  |   |     with Drawing Tools      |   |   |
|  |                  |  |   |                             |   |   |
|  | ○ Parcel C       |  |   |     [🔲Draw] [✏️Edit]       |   |   |
|  |   0.8 ha, Owned  |  |   |     [🗑️Delete] [📍Locate]   |   |   |
|  |                  |  |   +-----------------------------+   |   |
|  +------------------+  +-------------------------------------+   |
|                                                                   |
+------------------------------------------------------------------+
|  PARCEL DETAILS (shown when parcel selected)                      |
|  +------------------------------------------------------------+  |
|  | Name: [____________]  Land Use: [Crop Production ▼]        |  |
|  | Tenure: [Owned ▼]     Area: 2.5 ha (auto-calculated)       |  |
|  +------------------------------------------------------------+  |
+------------------------------------------------------------------+
```

### 3. Technology Stack Recommendation

For Joget DX8 integration, I recommend **Leaflet.js with Leaflet.Draw plugin**:

**Why Leaflet over Google Maps:**
- **Free/Open Source** - No API costs for government systems
- **OpenStreetMap tiles** - Good coverage in Lesotho
- **Offline capable** - Critical for rural field registration
- **Leaflet.Draw** - Purpose-built polygon drawing tools
- **GeoJSON native** - Standard format for interoperability

### 4. Implementation Approach Using Custom HTML

Create a Custom HTML form element in Joget that integrates Leaflet:

```html
<!-- In Joget Custom HTML Element -->
<div id="parcelMapContainer">
    <div id="mapToolbar">
        <button type="button" onclick="startDrawing()">📐 Draw Parcel</button>
        <button type="button" onclick="editParcel()">✏️ Edit</button>
        <button type="button" onclick="deleteParcel()">🗑️ Delete</button>
        <button type="button" onclick="locateMe()">📍 My Location</button>
    </div>
    <div id="parcelMap" style="height: 400px;"></div>
</div>

<!-- Hidden field to store GeoJSON -->
<input type="hidden" id="parcel_geometry" name="parcel_geometry" />
<input type="hidden" id="area_hectares" name="area_hectares" />

<link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/leaflet.draw/1.0.4/leaflet.draw.css" />
<script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/leaflet.draw/1.0.4/leaflet.draw.js"></script>

<script>
// Initialize map centered on Lesotho
var map = L.map('parcelMap').setView([-29.6, 28.2], 8);

// Add OpenStreetMap tiles
L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '© OpenStreetMap contributors'
}).addTo(map);

// Feature group for editable layers
var drawnItems = new L.FeatureGroup();
map.addLayer(drawnItems);

// Drawing control
var drawControl = new L.Control.Draw({
    draw: {
        polygon: {
            allowIntersection: false,
            showArea: true,
            metric: true
        },
        polyline: false,
        rectangle: false,
        circle: false,
        marker: false,
        circlemarker: false
    },
    edit: {
        featureGroup: drawnItems
    }
});
map.addControl(drawControl);

// Handle polygon creation
map.on(L.Draw.Event.CREATED, function(e) {
    var layer = e.layer;
    drawnItems.addLayer(layer);
    
    // Convert to GeoJSON and store
    var geoJson = layer.toGeoJSON();
    document.getElementById('parcel_geometry').value = JSON.stringify(geoJson);
    
    // Calculate area in hectares
    var areaM2 = L.GeometryUtil.geodesicArea(layer.getLatLngs()[0]);
    var areaHa = (areaM2 / 10000).toFixed(4);
    document.getElementById('area_hectares').value = areaHa;
});

// Load existing parcel if editing
var existingGeometry = '#form.parcel_geometry#';
if (existingGeometry && existingGeometry !== '') {
    var geoJson = JSON.parse(existingGeometry);
    var parcelLayer = L.geoJSON(geoJson).addTo(drawnItems);
    map.fitBounds(parcelLayer.getBounds());
}
</script>
```

### 5. Multiple Parcels Management with Form Grid

Since a farmer can have multiple parcels, use a **List Grid** or **Form Grid** element:

```
+------------------------------------------------------------------+
| LAND PARCELS                                        [+ Add New]   |
+------------------------------------------------------------------+
| Parcel Name    | Area (ha) | Land Use    | Tenure  | Actions     |
|----------------|-----------|-------------|---------|-------------|
| Northern Field | 2.50      | Maize       | Owned   | [👁️][✏️][🗑️]|
| River Plot     | 1.20      | Vegetables  | Rented  | [👁️][✏️][🗑️]|
| Hillside       | 0.80      | Grazing     | Owned   | [👁️][✏️][🗑️]|
+------------------------------------------------------------------+
| Total Area: 4.50 ha                                               |
+------------------------------------------------------------------+
```

### 6. Visualization Modes

Provide three visualization contexts:

**A. Individual Parcel View (Form Context)**
- Full drawing/editing capabilities
- Satellite imagery toggle
- Area calculation display

**B. Farmer Overview (Dashboard)**
- All parcels displayed on single map
- Different colors by land use type
- Popup with parcel details on click

**C. Administrative View (List/Report)**
- District/village level aggregation
- Heat maps showing farm density
- Export capabilities (KML, GeoJSON, Shapefile)

### 7. Data Capture Workflow

```
┌─────────────────────────────────────────────────────────────────┐
│                    PARCEL CAPTURE WORKFLOW                       │
└─────────────────────────────────────────────────────────────────┘

  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐
  │ 1. LOCATE    │───▶│ 2. DRAW      │───▶│ 3. VERIFY    │
  │              │    │              │    │              │
  │ • GPS locate │    │ • Click map  │    │ • Review     │
  │ • Search     │    │   corners    │    │   boundaries │
  │ • Navigate   │    │ • Close      │    │ • Check area │
  └──────────────┘    │   polygon    │    │ • Confirm    │
                      └──────────────┘    └──────────────┘
                                                 │
  ┌──────────────┐    ┌──────────────┐           │
  │ 5. STORE     │◀───│ 4. ENRICH    │◀──────────┘
  │              │    │              │
  │ • GeoJSON    │    │ • Name       │
  │ • Calculate  │    │ • Land use   │
  │   centroid   │    │ • Tenure     │
  │ • Auto area  │    │ • Crops      │
  └──────────────┘    └──────────────┘
```

### 8. GeoJSON Storage Format

Store polygon data in standard GeoJSON format for interoperability:

```json
{
  "type": "Feature",
  "properties": {
    "parcel_code": "P-BER-001",
    "parcel_name": "Northern Field",
    "area_hectares": 2.5,
    "land_use": "CROP",
    "tenure": "OWNED"
  },
  "geometry": {
    "type": "Polygon",
    "coordinates": [[
      [28.234, -29.612],
      [28.238, -29.612],
      [28.238, -29.615],
      [28.234, -29.615],
      [28.234, -29.612]
    ]]
  }
}
```

### 9. MDM Integration

Add these master data tables:

```
md_land_use_type:
  code: CROP, LIVESTOCK, MIXED, FALLOW, FOREST, OTHER
  
md_tenure_type:
  code: OWNED, RENTED, LEASED, COMMUNAL, INHERITED
  
md_soil_type:
  code: (Lesotho-specific soil classifications)
```

### 10. Form Definition Extension

Here's the suggested extension to your forms:

**New Form: `f01_08_landParcels.json`** (Land Parcels sub-form)

This would be linked via a **Form Grid** or **List Grid** element in `f01_02.json` (Location section).

---

**Next Steps:**

1. **Phase 1**: Create the data model (joget entity for parcels)
2. **Phase 2**: Develop the joget dx map component with Leaflet.Draw
3. **Phase 3**: Integrate with Form Grid for multiple parcels
4. **Phase 4**: Build visualization dashboards
