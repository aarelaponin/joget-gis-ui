# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Joget GIS UI Plugin - A Joget DX form element plugin that provides a Leaflet.js-based map for capturing geographic polygon boundaries. Supports two capture modes:
- **Walk Mode**: GPS-based capture for field use (mobile devices)
- **Draw Mode**: Click-to-draw for office use (desktop)

The plugin outputs GeoJSON polygons with calculated metrics (area in hectares, perimeter in meters, centroid, vertex count).

## Build Commands

```bash
# Build the OSGi bundle
mvn clean package

# Install to local Maven repository
mvn clean install
```

The built bundle JAR will be in `target/joget-gis-ui-8.1-SNAPSHOT.jar`.

## Architecture

### OSGi Bundle Structure

This is a Joget plugin packaged as an OSGi bundle using maven-bundle-plugin:
- **Bundle Activator**: `global.govstack.gisui.Activator` - Registers plugin services
- **Packaging**: `bundle` (Felix maven-bundle-plugin)

### Key Components

1. **GisPolygonCaptureElement** (`element/GisPolygonCaptureElement.java`)
   - Joget form element implementing `Element` and `FormBuilderPaletteElement`
   - Renders to FreeMarker template, passing config JSON to JavaScript
   - Validates GeoJSON on form submission
   - Appears in "GovStack" category in Form Builder

2. **GisResourcesPlugin** (`element/GisResourcesPlugin.java`)
   - Implements `PluginWebSupport` for serving static files
   - Endpoint: `/jw/web/json/plugin/global.govstack.gisui.element.GisResourcesPlugin/service?file=<filename>`
   - Serves JS/CSS from `/static/` classpath resources

3. **Frontend** (`resources/static/`)
   - `gis-capture.js`: Main JavaScript component (Leaflet.js + Turf.js integration)
   - `gis-capture.css`: Component styles
   - Dependencies loaded from CDN: Leaflet 1.9.4, Turf.js 6.x

### Resource Structure

```
resources/
├── properties/
│   └── GisPolygonCaptureElement.json  # Plugin property definitions for Form Builder
├── static/
│   ├── gis-capture.js                  # JavaScript map component
│   └── gis-capture.css                 # Component styles
└── templates/
    └── GisPolygonCaptureElement.ftl    # FreeMarker template
```

### Data Flow

1. Form element renders FreeMarker template with config JSON
2. Template loads Leaflet, Turf.js, then gis-capture.js
3. `GISCapture.init()` creates map instance with configuration
4. User captures polygon via GPS or click-to-draw
5. GeoJSON stored in hidden textarea field for form submission
6. Output fields (area, perimeter, centroid) populated automatically

## Joget Integration Notes

- Requires `wflow-core` 8.1-SNAPSHOT (provided scope)
- Plugin properties defined in JSON format (not XML)
- Form element template uses `FormUtil.generateElementHtml()` for rendering
- Backend GIS API services are in a separate plugin (`joget-gis-api`)
