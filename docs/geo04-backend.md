# Parcel Geometry Services - Backend API Specification

## Document Information

| Attribute | Value |
|-----------|-------|
| Document Type | Backend API Specification |
| Component | joget-parcel-services Plugin |
| Version | 1.0 |
| Status | Draft |
| Related Document | Draw Mode UI Logical Specification v1.0 |
| Platform | Joget DX8 Enterprise Edition |

---

## 1. OVERVIEW

### 1.1 Purpose

This specification defines the backend REST API services required to support the Parcel Capture Field Draw Mode UI. The API provides geometry calculations, validation, and spatial query capabilities.

### 1.2 Scope

This specification covers:
- All API endpoints required for Draw Mode
- Request/response formats
- Validation rules and error handling
- Data models
- Security considerations

### 1.3 Design Principles

| Principle | Description |
|-----------|-------------|
| **Stateless** | Each request contains all information needed for processing |
| **Idempotent** | Repeated identical requests produce same result |
| **Defensive** | Validate all inputs, assume malicious intent |
| **Informative** | Error responses include actionable guidance |
| **Efficient** | Minimize payload size, support batch operations |

### 1.4 Technology Stack

| Component | Technology |
|-----------|------------|
| **Platform** | Joget DX8 Web Service Plugin |
| **Geometry Engine** | JTS Topology Suite 1.19.0 |
| **JSON Processing** | Gson 2.10.1 |
| **Coordinate System** | WGS84 (EPSG:4326) |

---

## 2. API OVERVIEW

### 2.1 Base URL

```
/jw/web/json/plugin/org.joget.marketplace.parcel.service.ParcelGeometryWebService/service
```

### 2.2 Endpoint Summary

| Endpoint | Method | Purpose | UI State Usage |
|----------|--------|---------|----------------|
| `?action=calculate` | POST | Calculate area, perimeter, centroid | DRAWING (real-time), PREVIEW |
| `?action=validate` | POST | Validate geometry against rules | PREVIEW |
| `?action=simplify` | POST | Reduce vertex count | EDITING (optional) |
| `?action=checkOverlap` | POST | Find overlapping parcels | PREVIEW (optional) |
| `?action=geocode` | GET | Search for location by name | Location Search Panel |
| `?action=reverseGeocode` | GET | Get place name from coordinates | Map Display (optional) |
| `?action=health` | GET | Service health check | Initialization |

### 2.3 Common HTTP Status Codes

| Status | Meaning | When Used |
|--------|---------|-----------|
| **200** | Success | Request processed successfully |
| **400** | Bad Request | Invalid input parameters |
| **401** | Unauthorized | Missing or invalid authentication |
| **422** | Unprocessable Entity | Valid JSON but semantic errors |
| **500** | Internal Server Error | Unexpected server error |
| **503** | Service Unavailable | Dependent service unavailable |

### 2.4 Common Response Envelope

All responses follow this structure:

```json
{
  "success": true|false,
  "data": { },
  "error": {
    "code": "ERROR_CODE",
    "message": "Human-readable message",
    "details": { }
  },
  "meta": {
    "requestId": "uuid",
    "processingTimeMs": 45
  }
}
```

---

## 3. DATA MODELS

### 3.1 GeoJSON Geometry Input

All geometry inputs accept standard GeoJSON format:

```json
{
  "type": "Polygon",
  "coordinates": [
    [
      [longitude1, latitude1],
      [longitude2, latitude2],
      [longitude3, latitude3],
      [longitude1, latitude1]
    ]
  ]
}
```

**Or as a Feature:**

```json
{
  "type": "Feature",
  "geometry": {
    "type": "Polygon",
    "coordinates": [[[lon, lat], ...]]
  },
  "properties": { }
}
```

### 3.2 Validation Rules Model

```json
{
  "minAreaHectares": 0.01,
  "maxAreaHectares": 1000,
  "minVertices": 3,
  "maxVertices": 100,
  "allowSelfIntersection": false,
  "checkOverlap": false,
  "overlapExcludeParcelId": null
}
```

### 3.3 Calculation Result Model

```json
{
  "areaHectares": 0.82,
  "areaSquareMeters": 8200.00,
  "perimeterMeters": 362.50,
  "centroid": {
    "latitude": -29.3145,
    "longitude": 28.1234
  },
  "boundingBox": {
    "minLatitude": -29.3200,
    "maxLatitude": -29.3100,
    "minLongitude": 28.1200,
    "maxLongitude": 28.1300
  },
  "vertexCount": 4
}
```

### 3.4 Validation Result Model

```json
{
  "valid": true|false,
  "errors": [
    {
      "code": "SELF_INTERSECTION",
      "message": "Polygon is self-intersecting",
      "severity": "ERROR",
      "location": {
        "latitude": -29.3150,
        "longitude": 28.1240
      }
    }
  ],
  "warnings": [
    {
      "code": "AREA_BELOW_MINIMUM",
      "message": "Area (0.005 ha) is below recommended minimum (0.01 ha)",
      "severity": "WARNING"
    }
  ],
  "metrics": {
    "areaHectares": 0.005,
    "vertexCount": 4
  }
}
```

### 3.5 Overlap Result Model

```json
{
  "hasOverlaps": true,
  "overlaps": [
    {
      "parcelId": "P-BER-001",
      "parcelCode": "BER-2024-00123",
      "farmerName": "John Mokoena",
      "overlapAreaHectares": 0.15,
      "overlapPercentage": 18.3,
      "overlapGeometry": {
        "type": "Polygon",
        "coordinates": [[[...]]]
      }
    }
  ],
  "totalOverlapAreaHectares": 0.15
}
```

### 3.6 Error Codes Enumeration

| Code | Severity | Description |
|------|----------|-------------|
| **INVALID_GEOJSON** | ERROR | GeoJSON parsing failed |
| **INVALID_GEOMETRY_TYPE** | ERROR | Expected Polygon, got other type |
| **EMPTY_GEOMETRY** | ERROR | No coordinates provided |
| **TOO_FEW_VERTICES** | ERROR | Less than minimum required vertices |
| **TOO_MANY_VERTICES** | ERROR | Exceeds maximum allowed vertices |
| **SELF_INTERSECTION** | ERROR | Polygon edges cross each other |
| **INVALID_RING** | ERROR | Polygon ring not properly closed |
| **AREA_ZERO** | ERROR | Calculated area is zero |
| **AREA_NEGATIVE** | ERROR | Calculated area is negative (ring orientation) |
| **AREA_EXCEEDS_MAXIMUM** | ERROR | Area larger than allowed maximum |
| **AREA_BELOW_MINIMUM** | WARNING | Area smaller than recommended minimum |
| **VERTICES_NEAR_LIMIT** | WARNING | Approaching maximum vertex limit |
| **DUPLICATE_VERTICES** | WARNING | Consecutive identical vertices found |
| **SPIKE_DETECTED** | WARNING | Very acute angle detected (potential error) |

---

## 4. ENDPOINT SPECIFICATIONS

### 4.1 Calculate Geometry Metrics

Calculates area, perimeter, centroid, and bounding box for a given geometry.

#### Request

```
POST /jw/web/json/plugin/.../service?action=calculate
Content-Type: application/json
```

**Request Body:**

```json
{
  "geometry": {
    "type": "Polygon",
    "coordinates": [
      [
        [28.1234, -29.3145],
        [28.1256, -29.3145],
        [28.1256, -29.3167],
        [28.1234, -29.3167],
        [28.1234, -29.3145]
      ]
    ]
  },
  "options": {
    "includeAreaInSquareMeters": true,
    "includeBoundingBox": true,
    "decimalPrecision": {
      "area": 4,
      "perimeter": 2,
      "coordinates": 7
    }
  }
}
```

**Request Parameters:**

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `geometry` | GeoJSON | Yes | - | Polygon geometry to calculate |
| `options.includeAreaInSquareMeters` | Boolean | No | false | Include area in m² |
| `options.includeBoundingBox` | Boolean | No | true | Include bounding box |
| `options.decimalPrecision.area` | Integer | No | 4 | Decimal places for area |
| `options.decimalPrecision.perimeter` | Integer | No | 2 | Decimal places for perimeter |
| `options.decimalPrecision.coordinates` | Integer | No | 7 | Decimal places for coordinates |

#### Response

**Success (200):**

```json
{
  "success": true,
  "data": {
    "areaHectares": 0.8234,
    "areaSquareMeters": 8234.00,
    "perimeterMeters": 362.45,
    "centroid": {
      "latitude": -29.3156,
      "longitude": 28.1245
    },
    "boundingBox": {
      "minLatitude": -29.3167,
      "maxLatitude": -29.3145,
      "minLongitude": 28.1234,
      "maxLongitude": 28.1256
    },
    "vertexCount": 4
  },
  "meta": {
    "requestId": "550e8400-e29b-41d4-a716-446655440000",
    "processingTimeMs": 12
  }
}
```

**Error (400) - Invalid GeoJSON:**

```json
{
  "success": false,
  "error": {
    "code": "INVALID_GEOJSON",
    "message": "Failed to parse GeoJSON geometry",
    "details": {
      "parseError": "Unexpected token at position 45",
      "hint": "Ensure coordinates array is properly formatted as [longitude, latitude] pairs"
    }
  },
  "meta": {
    "requestId": "550e8400-e29b-41d4-a716-446655440001",
    "processingTimeMs": 3
  }
}
```

**Error (422) - Invalid Geometry Type:**

```json
{
  "success": false,
  "error": {
    "code": "INVALID_GEOMETRY_TYPE",
    "message": "Expected Polygon geometry, received Point",
    "details": {
      "receivedType": "Point",
      "supportedTypes": ["Polygon", "MultiPolygon"]
    }
  },
  "meta": {
    "requestId": "550e8400-e29b-41d4-a716-446655440002",
    "processingTimeMs": 2
  }
}
```

#### UI Integration Points

| UI State | Usage |
|----------|-------|
| **DRAWING** | Called on each vertex add for real-time area/perimeter display (debounced) |
| **PREVIEW** | Called once when entering preview to get final metrics |
| **SAVED** | Metrics stored in form fields, no API call needed |

---

### 4.2 Validate Geometry

Validates geometry against configurable rules and returns detailed results.

#### Request

```
POST /jw/web/json/plugin/.../service?action=validate
Content-Type: application/json
```

**Request Body:**

```json
{
  "geometry": {
    "type": "Polygon",
    "coordinates": [
      [
        [28.1234, -29.3145],
        [28.1256, -29.3145],
        [28.1256, -29.3167],
        [28.1234, -29.3167],
        [28.1234, -29.3145]
      ]
    ]
  },
  "rules": {
    "minAreaHectares": 0.01,
    "maxAreaHectares": 1000,
    "minVertices": 3,
    "maxVertices": 100,
    "allowSelfIntersection": false
  },
  "options": {
    "includeMetrics": true,
    "detectSpikes": true,
    "spikeAngleThreshold": 10
  }
}
```

**Request Parameters:**

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `geometry` | GeoJSON | Yes | - | Polygon geometry to validate |
| `rules.minAreaHectares` | Decimal | No | 0.01 | Minimum area (warning) |
| `rules.maxAreaHectares` | Decimal | No | 1000 | Maximum area (error) |
| `rules.minVertices` | Integer | No | 3 | Minimum vertices (error) |
| `rules.maxVertices` | Integer | No | 100 | Maximum vertices (error) |
| `rules.allowSelfIntersection` | Boolean | No | false | Allow self-intersecting polygons |
| `options.includeMetrics` | Boolean | No | true | Include calculated metrics |
| `options.detectSpikes` | Boolean | No | true | Detect very acute angles |
| `options.spikeAngleThreshold` | Decimal | No | 10 | Angle (degrees) below which to warn |

#### Response

**Success - Valid (200):**

```json
{
  "success": true,
  "data": {
    "valid": true,
    "errors": [],
    "warnings": [],
    "metrics": {
      "areaHectares": 0.8234,
      "perimeterMeters": 362.45,
      "vertexCount": 4
    }
  },
  "meta": {
    "requestId": "550e8400-e29b-41d4-a716-446655440010",
    "processingTimeMs": 18
  }
}
```

**Success - With Warnings (200):**

```json
{
  "success": true,
  "data": {
    "valid": true,
    "errors": [],
    "warnings": [
      {
        "code": "AREA_BELOW_MINIMUM",
        "message": "Area (0.005 ha) is below recommended minimum (0.01 ha)",
        "severity": "WARNING",
        "context": {
          "actualValue": 0.005,
          "threshold": 0.01,
          "unit": "hectares"
        }
      }
    ],
    "metrics": {
      "areaHectares": 0.005,
      "perimeterMeters": 28.30,
      "vertexCount": 4
    }
  },
  "meta": {
    "requestId": "550e8400-e29b-41d4-a716-446655440011",
    "processingTimeMs": 15
  }
}
```

**Success - Invalid (200):**

```json
{
  "success": true,
  "data": {
    "valid": false,
    "errors": [
      {
        "code": "SELF_INTERSECTION",
        "message": "Polygon is self-intersecting",
        "severity": "ERROR",
        "location": {
          "latitude": -29.3150,
          "longitude": 28.1240
        },
        "context": {
          "intersectionType": "RING_SELF_INTERSECTION",
          "edgeIndices": [1, 3]
        }
      }
    ],
    "warnings": [],
    "metrics": {
      "vertexCount": 5
    }
  },
  "meta": {
    "requestId": "550e8400-e29b-41d4-a716-446655440012",
    "processingTimeMs": 22
  }
}
```

**Multiple Errors (200):**

```json
{
  "success": true,
  "data": {
    "valid": false,
    "errors": [
      {
        "code": "SELF_INTERSECTION",
        "message": "Polygon is self-intersecting",
        "severity": "ERROR",
        "location": {
          "latitude": -29.3150,
          "longitude": 28.1240
        }
      },
      {
        "code": "AREA_EXCEEDS_MAXIMUM",
        "message": "Area (1500.00 ha) exceeds maximum allowed (1000.00 ha)",
        "severity": "ERROR",
        "context": {
          "actualValue": 1500.00,
          "threshold": 1000.00,
          "unit": "hectares"
        }
      }
    ],
    "warnings": [
      {
        "code": "SPIKE_DETECTED",
        "message": "Very acute angle (5.2°) detected at vertex 3",
        "severity": "WARNING",
        "location": {
          "latitude": -29.3160,
          "longitude": 28.1250
        },
        "context": {
          "angle": 5.2,
          "vertexIndex": 3
        }
      }
    ],
    "metrics": {
      "areaHectares": 1500.00,
      "vertexCount": 6
    }
  },
  "meta": {
    "requestId": "550e8400-e29b-41d4-a716-446655440013",
    "processingTimeMs": 28
  }
}
```

#### Validation Rules Detail

| Rule | Type | Condition | Result |
|------|------|-----------|--------|
| **Topology Valid** | ERROR | JTS IsValidOp fails | INVALID_RING, SELF_INTERSECTION, etc. |
| **Self-Intersection** | ERROR | Edges cross (if not allowed) | SELF_INTERSECTION with location |
| **Minimum Vertices** | ERROR | vertexCount < minVertices | TOO_FEW_VERTICES |
| **Maximum Vertices** | ERROR | vertexCount > maxVertices | TOO_MANY_VERTICES |
| **Maximum Area** | ERROR | area > maxAreaHectares | AREA_EXCEEDS_MAXIMUM |
| **Zero/Negative Area** | ERROR | area <= 0 | AREA_ZERO or AREA_NEGATIVE |
| **Minimum Area** | WARNING | area < minAreaHectares | AREA_BELOW_MINIMUM |
| **Vertices Near Limit** | WARNING | vertexCount > maxVertices * 0.9 | VERTICES_NEAR_LIMIT |
| **Duplicate Vertices** | WARNING | Consecutive identical coordinates | DUPLICATE_VERTICES |
| **Spike Detection** | WARNING | Internal angle < threshold | SPIKE_DETECTED |

#### UI Integration Points

| UI State | Usage |
|----------|-------|
| **PREVIEW** | Called when entering preview state to show validation result panel |
| **EDITING** | Optionally called during editing for real-time feedback |

---

### 4.3 Simplify Geometry

Reduces vertex count while maintaining shape accuracy using Douglas-Peucker algorithm.

#### Request

```
POST /jw/web/json/plugin/.../service?action=simplify
Content-Type: application/json
```

**Request Body:**

```json
{
  "geometry": {
    "type": "Polygon",
    "coordinates": [
      [
        [28.1234, -29.3145],
        [28.1238, -29.3145],
        [28.1242, -29.3146],
        [28.1246, -29.3145],
        [28.1250, -29.3145],
        [28.1256, -29.3145],
        [28.1256, -29.3167],
        [28.1234, -29.3167],
        [28.1234, -29.3145]
      ]
    ]
  },
  "options": {
    "toleranceMeters": 5,
    "preserveTopology": true,
    "targetVertexCount": null,
    "maxAreaChangePercent": 1.0
  }
}
```

**Request Parameters:**

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `geometry` | GeoJSON | Yes | - | Polygon geometry to simplify |
| `options.toleranceMeters` | Decimal | No | 1.0 | Simplification tolerance in meters |
| `options.preserveTopology` | Boolean | No | true | Prevent creating invalid geometry |
| `options.targetVertexCount` | Integer | No | null | Try to reduce to this vertex count |
| `options.maxAreaChangePercent` | Decimal | No | 1.0 | Maximum allowed area change |

#### Response

**Success (200):**

```json
{
  "success": true,
  "data": {
    "simplifiedGeometry": {
      "type": "Polygon",
      "coordinates": [
        [
          [28.1234, -29.3145],
          [28.1256, -29.3145],
          [28.1256, -29.3167],
          [28.1234, -29.3167],
          [28.1234, -29.3145]
        ]
      ]
    },
    "originalVertexCount": 8,
    "simplifiedVertexCount": 4,
    "verticesRemoved": 4,
    "areaChange": {
      "originalHectares": 0.8234,
      "simplifiedHectares": 0.8230,
      "changePercent": -0.05
    },
    "toleranceUsed": 5.0
  },
  "meta": {
    "requestId": "550e8400-e29b-41d4-a716-446655440020",
    "processingTimeMs": 8
  }
}
```

**Error - Cannot Simplify (422):**

```json
{
  "success": false,
  "error": {
    "code": "SIMPLIFICATION_FAILED",
    "message": "Cannot simplify geometry without exceeding area change limit",
    "details": {
      "requestedTolerance": 50.0,
      "maxAllowedAreaChange": 1.0,
      "actualAreaChange": 15.3,
      "suggestion": "Reduce tolerance or increase maxAreaChangePercent"
    }
  },
  "meta": {
    "requestId": "550e8400-e29b-41d4-a716-446655440021",
    "processingTimeMs": 12
  }
}
```

#### UI Integration Points

| UI State | Usage |
|----------|-------|
| **EDITING** | Optional tool to simplify complex boundaries |
| **PREVIEW** | Could be offered if vertex count is high |

---

### 4.4 Check Overlap

Checks if the given geometry overlaps with existing parcels in the database.

#### Request

```
POST /jw/web/json/plugin/.../service?action=checkOverlap
Content-Type: application/json
```

**Request Body:**

```json
{
  "geometry": {
    "type": "Polygon",
    "coordinates": [[[28.1234, -29.3145], ...]]
  },
  "options": {
    "excludeParcelId": "P-BER-001",
    "tableName": "app_fd_parcel",
    "geometryColumn": "parcel_geometry",
    "includeOverlapGeometry": true,
    "minOverlapPercent": 5.0,
    "maxResults": 10
  }
}
```

**Request Parameters:**

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `geometry` | GeoJSON | Yes | - | Polygon geometry to check |
| `options.excludeParcelId` | String | No | null | Exclude this parcel (for editing existing) |
| `options.tableName` | String | No | app_fd_parcel | Database table name |
| `options.geometryColumn` | String | No | parcel_geometry | Column containing GeoJSON |
| `options.includeOverlapGeometry` | Boolean | No | false | Return overlap polygon geometry |
| `options.minOverlapPercent` | Decimal | No | 1.0 | Minimum overlap % to report |
| `options.maxResults` | Integer | No | 10 | Maximum overlaps to return |

#### Response

**Success - No Overlaps (200):**

```json
{
  "success": true,
  "data": {
    "hasOverlaps": false,
    "overlaps": [],
    "totalOverlapAreaHectares": 0,
    "checkedParcelCount": 47
  },
  "meta": {
    "requestId": "550e8400-e29b-41d4-a716-446655440030",
    "processingTimeMs": 125
  }
}
```

**Success - Overlaps Found (200):**

```json
{
  "success": true,
  "data": {
    "hasOverlaps": true,
    "overlaps": [
      {
        "parcelId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
        "parcelCode": "BER-2024-00123",
        "parcelName": "Northern Field",
        "farmerId": "f9e8d7c6-b5a4-3210-fedc-ba0987654321",
        "farmerName": "John Mokoena",
        "overlapAreaHectares": 0.15,
        "overlapPercentOfNew": 18.3,
        "overlapPercentOfExisting": 12.5,
        "overlapGeometry": {
          "type": "Polygon",
          "coordinates": [[[28.1240, -29.3150], ...]]
        }
      },
      {
        "parcelId": "b2c3d4e5-f6a7-8901-bcde-f23456789012",
        "parcelCode": "BER-2024-00456",
        "parcelName": "River Plot",
        "farmerId": "e8d7c6b5-a433-2109-edcb-a98765432100",
        "farmerName": "Mary Lebona",
        "overlapAreaHectares": 0.08,
        "overlapPercentOfNew": 9.7,
        "overlapPercentOfExisting": 6.2,
        "overlapGeometry": null
      }
    ],
    "totalOverlapAreaHectares": 0.23,
    "checkedParcelCount": 47
  },
  "meta": {
    "requestId": "550e8400-e29b-41d4-a716-446655440031",
    "processingTimeMs": 180
  }
}
```

#### UI Integration Points

| UI State | Usage |
|----------|-------|
| **PREVIEW** | Called if overlap checking is enabled in configuration |

---

### 4.5 Geocode Location

Searches for a location by name and returns coordinates.

#### Request

```
GET /jw/web/json/plugin/.../service?action=geocode&query={query}&limit={limit}
```

**Query Parameters:**

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `query` | String | Yes | - | Search term (min 3 characters) |
| `limit` | Integer | No | 5 | Maximum results to return |
| `countryCode` | String | No | LS | ISO country code to bias results |
| `boundingBox` | String | No | null | Bounding box to limit search (minLon,minLat,maxLon,maxLat) |

#### Response

**Success (200):**

```json
{
  "success": true,
  "data": {
    "results": [
      {
        "displayName": "Teyateyaneng, Berea District, Lesotho",
        "latitude": -29.1475,
        "longitude": 27.7386,
        "type": "city",
        "importance": 0.65,
        "boundingBox": {
          "minLatitude": -29.1700,
          "maxLatitude": -29.1200,
          "minLongitude": 27.7100,
          "maxLongitude": 27.7700
        }
      },
      {
        "displayName": "Teyateyaneng Airport, Berea District, Lesotho",
        "latitude": -29.1389,
        "longitude": 27.7517,
        "type": "aeroway",
        "importance": 0.35,
        "boundingBox": null
      }
    ],
    "query": "Teyateyaneng",
    "totalResults": 2
  },
  "meta": {
    "requestId": "550e8400-e29b-41d4-a716-446655440040",
    "processingTimeMs": 340
  }
}
```

**Error - Query Too Short (400):**

```json
{
  "success": false,
  "error": {
    "code": "QUERY_TOO_SHORT",
    "message": "Search query must be at least 3 characters",
    "details": {
      "provided": "Te",
      "minimum": 3
    }
  },
  "meta": {
    "requestId": "550e8400-e29b-41d4-a716-446655440041",
    "processingTimeMs": 1
  }
}
```

**Error - Geocoding Service Unavailable (503):**

```json
{
  "success": false,
  "error": {
    "code": "GEOCODING_UNAVAILABLE",
    "message": "Location search service is temporarily unavailable",
    "details": {
      "provider": "Nominatim",
      "retryAfterSeconds": 30
    }
  },
  "meta": {
    "requestId": "550e8400-e29b-41d4-a716-446655440042",
    "processingTimeMs": 5002
  }
}
```

#### UI Integration Points

| UI Element | Usage |
|------------|-------|
| **Location Search Panel** | Called when user submits search query |

---

### 4.6 Reverse Geocode

Gets place name from coordinates.

#### Request

```
GET /jw/web/json/plugin/.../service?action=reverseGeocode&lat={latitude}&lon={longitude}
```

**Query Parameters:**

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `lat` | Decimal | Yes | - | Latitude |
| `lon` | Decimal | Yes | - | Longitude |
| `zoom` | Integer | No | 14 | Detail level (higher = more specific) |

#### Response

**Success (200):**

```json
{
  "success": true,
  "data": {
    "displayName": "Ha Matala, Berea District, Lesotho",
    "address": {
      "village": "Ha Matala",
      "district": "Berea District",
      "country": "Lesotho",
      "countryCode": "LS"
    },
    "coordinates": {
      "latitude": -29.3145,
      "longitude": 28.1234
    }
  },
  "meta": {
    "requestId": "550e8400-e29b-41d4-a716-446655440050",
    "processingTimeMs": 280
  }
}
```

#### UI Integration Points

| UI Element | Usage |
|------------|-------|
| **Map Display** | Optional - show place name for centroid |

---

### 4.7 Health Check

Returns service status and capabilities.

#### Request

```
GET /jw/web/json/plugin/.../service?action=health
```

#### Response

**Success (200):**

```json
{
  "success": true,
  "data": {
    "status": "healthy",
    "version": "1.0.0",
    "capabilities": {
      "calculate": true,
      "validate": true,
      "simplify": true,
      "checkOverlap": true,
      "geocode": true,
      "reverseGeocode": true
    },
    "configuration": {
      "geometryEngine": "JTS 1.19.0",
      "geocodingProvider": "Nominatim",
      "maxGeometryVertices": 10000,
      "maxBatchSize": 100
    },
    "dependencies": {
      "database": "connected",
      "geocodingService": "available"
    }
  },
  "meta": {
    "requestId": "550e8400-e29b-41d4-a716-446655440060",
    "processingTimeMs": 45,
    "serverTime": "2026-01-04T14:30:00Z"
  }
}
```

**Degraded Service (200):**

```json
{
  "success": true,
  "data": {
    "status": "degraded",
    "version": "1.0.0",
    "capabilities": {
      "calculate": true,
      "validate": true,
      "simplify": true,
      "checkOverlap": false,
      "geocode": false,
      "reverseGeocode": false
    },
    "dependencies": {
      "database": "connected",
      "geocodingService": "unavailable"
    },
    "issues": [
      {
        "component": "geocodingService",
        "message": "Nominatim service not responding",
        "since": "2026-01-04T14:25:00Z"
      }
    ]
  },
  "meta": {
    "requestId": "550e8400-e29b-41d4-a716-446655440061",
    "processingTimeMs": 5020
  }
}
```

#### UI Integration Points

| UI State | Usage |
|----------|-------|
| **INITIAL** | Called on component initialization to check service availability |

---

## 5. BATCH OPERATIONS

### 5.1 Batch Calculate

Calculate metrics for multiple geometries in a single request.

#### Request

```
POST /jw/web/json/plugin/.../service?action=batchCalculate
Content-Type: application/json
```

**Request Body:**

```json
{
  "geometries": [
    {
      "id": "parcel-1",
      "geometry": {
        "type": "Polygon",
        "coordinates": [[[...]]]
      }
    },
    {
      "id": "parcel-2",
      "geometry": {
        "type": "Polygon",
        "coordinates": [[[...]]]
      }
    }
  ],
  "options": {
    "continueOnError": true
  }
}
```

#### Response

```json
{
  "success": true,
  "data": {
    "results": [
      {
        "id": "parcel-1",
        "success": true,
        "data": {
          "areaHectares": 0.82,
          "perimeterMeters": 362.45,
          "centroid": { "latitude": -29.3156, "longitude": 28.1245 },
          "vertexCount": 4
        }
      },
      {
        "id": "parcel-2",
        "success": false,
        "error": {
          "code": "INVALID_GEOJSON",
          "message": "Failed to parse GeoJSON geometry"
        }
      }
    ],
    "summary": {
      "total": 2,
      "successful": 1,
      "failed": 1
    }
  },
  "meta": {
    "requestId": "550e8400-e29b-41d4-a716-446655440070",
    "processingTimeMs": 45
  }
}
```

---

## 6. ERROR HANDLING

### 6.1 Error Response Structure

All error responses follow this structure:

```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "Human-readable error message",
    "details": {
      "field": "geometry",
      "reason": "Specific technical detail",
      "suggestion": "How to fix the issue"
    }
  },
  "meta": {
    "requestId": "uuid",
    "processingTimeMs": 5
  }
}
```

### 6.2 Error Codes Reference

| Code | HTTP Status | Description | Recovery Action |
|------|-------------|-------------|-----------------|
| **INVALID_GEOJSON** | 400 | Cannot parse GeoJSON | Fix JSON syntax |
| **INVALID_GEOMETRY_TYPE** | 422 | Wrong geometry type | Use Polygon or MultiPolygon |
| **EMPTY_GEOMETRY** | 422 | No coordinates | Provide coordinates |
| **INVALID_COORDINATES** | 422 | Coordinates out of valid range | Ensure lon [-180,180], lat [-90,90] |
| **MISSING_PARAMETER** | 400 | Required parameter missing | Include required parameter |
| **INVALID_PARAMETER** | 400 | Parameter value invalid | Fix parameter value |
| **TOO_MANY_VERTICES** | 422 | Exceeds maximum vertices | Simplify geometry |
| **GEOMETRY_TOO_COMPLEX** | 422 | Processing would take too long | Simplify geometry |
| **DATABASE_ERROR** | 500 | Database query failed | Retry or contact support |
| **GEOCODING_UNAVAILABLE** | 503 | Geocoding service down | Retry later |
| **INTERNAL_ERROR** | 500 | Unexpected error | Contact support |
| **RATE_LIMITED** | 429 | Too many requests | Wait and retry |

### 6.3 Retry Strategy

| Error Type | Retry | Backoff |
|------------|-------|---------|
| **400 Bad Request** | No | - |
| **422 Unprocessable** | No | - |
| **429 Rate Limited** | Yes | Exponential (1s, 2s, 4s) |
| **500 Internal Error** | Yes | Exponential (1s, 2s, 4s), max 3 retries |
| **503 Unavailable** | Yes | Use Retry-After header or exponential |

---

## 7. SECURITY CONSIDERATIONS

### 7.1 Authentication

| Requirement | Implementation |
|-------------|----------------|
| **Session Required** | Joget session cookie must be valid |
| **App Permission** | User must have access to app where plugin is installed |
| **CSRF Protection** | Standard Joget CSRF token handling |

### 7.2 Input Validation

| Input | Validation |
|-------|------------|
| **GeoJSON** | Parse with strict mode, reject invalid JSON |
| **Coordinates** | Must be valid WGS84 range |
| **Vertex Count** | Maximum 10,000 vertices per request |
| **String Parameters** | Sanitize for SQL injection |
| **Numeric Parameters** | Validate range and type |

### 7.3 Output Sanitization

| Output | Handling |
|--------|----------|
| **Error Messages** | Never expose stack traces or internal paths |
| **Database Errors** | Generic message, log details server-side |
| **User Data** | Only return data user is authorized to see |

### 7.4 Rate Limiting

| Endpoint | Limit | Window |
|----------|-------|--------|
| **calculate** | 100 requests | per minute |
| **validate** | 100 requests | per minute |
| **checkOverlap** | 20 requests | per minute |
| **geocode** | 30 requests | per minute |
| **batchCalculate** | 10 requests | per minute |

---

## 8. PERFORMANCE REQUIREMENTS

### 8.1 Response Time Targets

| Endpoint | Target (p95) | Maximum |
|----------|--------------|---------|
| **calculate** | 50ms | 200ms |
| **validate** | 100ms | 500ms |
| **simplify** | 100ms | 500ms |
| **checkOverlap** | 500ms | 2000ms |
| **geocode** | 500ms | 2000ms |
| **health** | 100ms | 500ms |

### 8.2 Payload Size Limits

| Direction | Limit |
|-----------|-------|
| **Request Body** | 5 MB maximum |
| **Response Body** | 10 MB maximum |
| **Geometry Vertices** | 10,000 per geometry |
| **Batch Size** | 100 geometries |

### 8.3 Caching Strategy

| Data | Cache Duration | Cache Key |
|------|----------------|-----------|
| **Geocode Results** | 24 hours | query + countryCode |
| **Reverse Geocode** | 24 hours | lat + lon + zoom |
| **Health Check** | 30 seconds | none |
| **Calculate/Validate** | No cache | - |

---

## 9. LOGGING AND MONITORING

### 9.1 Request Logging

Each request logs:

```json
{
  "timestamp": "2026-01-04T14:30:00.123Z",
  "requestId": "550e8400-e29b-41d4-a716-446655440000",
  "action": "validate",
  "userId": "user123",
  "appId": "farmers_registry",
  "inputSize": 1245,
  "outputSize": 523,
  "processingTimeMs": 45,
  "success": true,
  "errorCode": null
}
```

### 9.2 Metrics to Collect

| Metric | Type | Purpose |
|--------|------|---------|
| **Request Count** | Counter | Volume monitoring |
| **Response Time** | Histogram | Performance tracking |
| **Error Rate** | Gauge | Reliability monitoring |
| **Validation Failures** | Counter | Data quality insights |
| **Overlap Detections** | Counter | Business metric |

### 9.3 Alerts

| Condition | Severity | Action |
|-----------|----------|--------|
| **Error Rate > 5%** | Warning | Investigate |
| **Error Rate > 20%** | Critical | Page on-call |
| **Response Time p95 > 2s** | Warning | Investigate |
| **Geocoding Unavailable** | Warning | Check provider |

---

## 10. API VERSIONING

### 10.1 Version Strategy

- Current version: **v1** (implicit, no version in URL)
- Future versions: Add version parameter `?v=2`
- Deprecation: 6 months notice before removing old version

### 10.2 Backward Compatibility

| Change Type | Compatibility |
|-------------|---------------|
| **Add new optional field** | Compatible |
| **Add new endpoint** | Compatible |
| **Add new error code** | Compatible |
| **Remove field** | Breaking |
| **Change field type** | Breaking |
| **Change error code** | Breaking |

---

## 11. TESTING REQUIREMENTS

### 11.1 Test Categories

| Category | Coverage Target |
|----------|-----------------|
| **Unit Tests** | 80% code coverage |
| **Integration Tests** | All endpoints |
| **Performance Tests** | Response time targets |
| **Security Tests** | Input validation, auth |

### 11.2 Test Data

Provide test geometries for:
- Valid simple polygon (4 vertices)
- Valid complex polygon (50+ vertices)
- Self-intersecting polygon
- Very small polygon (below minimum area)
- Very large polygon (above maximum area)
- Polygon with holes (MultiPolygon)
- Edge cases (dateline crossing, polar regions)

---

## 12. APPENDICES

### 12.1 GeoJSON Coordinate Order

**Important:** GeoJSON uses `[longitude, latitude]` order (not `[latitude, longitude]`).

```json
{
  "type": "Point",
  "coordinates": [28.1234, -29.3145]
}
```

This represents:
- Longitude: 28.1234° E
- Latitude: 29.3145° S

### 12.2 Area Calculation Method

The API uses the **spherical excess formula** for geodesic area calculation:

1. Coordinates are converted to radians
2. Shoelace formula applied on spherical Earth model
3. Earth radius: 6,371,000 meters
4. Result converted to hectares (÷ 10,000)

Accuracy: ±0.1% for parcels under 100 hectares at mid-latitudes.

### 12.3 Coordinate Reference System

| Attribute | Value |
|-----------|-------|
| **CRS** | WGS84 |
| **EPSG Code** | 4326 |
| **Longitude Range** | -180 to +180 |
| **Latitude Range** | -90 to +90 |
| **Precision** | 7 decimal places (≈1cm accuracy) |

---

**End of Specification**