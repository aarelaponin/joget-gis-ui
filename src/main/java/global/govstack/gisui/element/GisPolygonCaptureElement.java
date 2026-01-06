package global.govstack.gisui.element;

import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.Form;
import org.joget.apps.form.model.FormBuilderPaletteElement;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.LogUtil;
import org.json.JSONObject;

import java.util.Map;

/**
 * GIS Polygon Capture Form Element
 *
 * A Joget form element that provides a Leaflet.js-based map for capturing
 * geographic polygon boundaries.
 *
 * Features:
 * - Walk Mode: GPS-based capture for field use (mobile)
 * - Draw Mode: Click-to-draw for office use (desktop)
 * - Real-time area and perimeter calculation
 * - GeoJSON output format
 * - Configurable validation rules
 * - Optional overlap checking
 */
public class GisPolygonCaptureElement extends Element implements FormBuilderPaletteElement {

    private static final String CLASS_NAME = GisPolygonCaptureElement.class.getName();

    @Override
    public String getName() {
        return "GIS Polygon Capture";
    }

    @Override
    public String getVersion() {
        return "8.1-SNAPSHOT";
    }

    @Override
    public String getDescription() {
        return "Leaflet.js-based map component for capturing polygon boundaries using Walk Mode (GPS) or Draw Mode (desktop)";
    }

    @Override
    public String getLabel() {
        return "GIS Polygon Capture";
    }

    @Override
    public String getClassName() {
        return CLASS_NAME;
    }

    @Override
    public String getFormBuilderCategory() {
        return "GovStack";
    }

    @Override
    public int getFormBuilderPosition() {
        return 200;
    }

    @Override
    public String getFormBuilderIcon() {
        return "<i class=\"fa fa-map-marker\"></i>";
    }

    @Override
    public String getFormBuilderTemplate() {
        return "<div class='form-cell'><label class='label'>GIS Polygon Capture</label><div class='form-cell-value'>[Map placeholder]</div></div>";
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(
            getClass().getName(),
            "/properties/GisPolygonCaptureElement.json",
            null,
            true,
            null
        );
    }

    @Override
    public String renderTemplate(FormData formData, Map dataModel) {
        String template = "GisPolygonCaptureElement.ftl";

        // Get properties - Basic Settings
        String fieldId = getPropertyString("id");
        String label = getPropertyString("label");
        
        // Output Field Mapping
        String areaFieldId = getPropertyString("areaFieldId");
        String perimeterFieldId = getPropertyString("perimeterFieldId");
        String centroidFieldId = getPropertyString("centroidFieldId");
        String vertexCountFieldId = getPropertyString("vertexCountFieldId");

        // Capture Mode Settings
        String captureMode = getPropertyString("captureMode");
        String defaultMode = getPropertyString("defaultMode");
        
        // Map Settings
        String defaultLatitude = getPropertyString("defaultLatitude");
        String defaultLongitude = getPropertyString("defaultLongitude");
        String defaultZoom = getPropertyString("defaultZoom");
        String tileProvider = getPropertyString("tileProvider");
        String showSatelliteOption = getPropertyString("showSatelliteOption");
        String mapHeight = getPropertyString("mapHeight");

        // Validation Rules
        String minAreaHectares = getPropertyString("minAreaHectares");
        String maxAreaHectares = getPropertyString("maxAreaHectares");
        String minVertices = getPropertyString("minVertices");
        String maxVertices = getPropertyString("maxVertices");
        String allowSelfIntersection = getPropertyString("allowSelfIntersection");

        // Overlap Checking
        String enableOverlapCheck = getPropertyString("enableOverlapCheck");
        String overlapFormId = getPropertyString("overlapFormId");
        String overlapGeometryField = getPropertyString("overlapGeometryField");
        String overlapDisplayFields = getPropertyString("overlapDisplayFields");
        String overlapFilterCondition = getPropertyString("overlapFilterCondition");

        // Nearby Parcels Display
        String showNearbyParcels = getPropertyString("showNearbyParcels");
        String nearbyParcelsFormId = getPropertyString("nearbyParcelsFormId");
        String nearbyParcelsGeometryField = getPropertyString("nearbyParcelsGeometryField");
        String nearbyParcelsDisplayFields = getPropertyString("nearbyParcelsDisplayFields");
        String nearbyParcelsFilterCondition = getPropertyString("nearbyParcelsFilterCondition");
        String nearbyParcelsFillColor = getPropertyString("nearbyParcelsFillColor");
        String nearbyParcelsFillOpacity = getPropertyString("nearbyParcelsFillOpacity");
        String nearbyParcelsStrokeColor = getPropertyString("nearbyParcelsStrokeColor");
        String nearbyParcelsMaxResults = getPropertyString("nearbyParcelsMaxResults");

        // GPS Settings (Walk Mode)
        String gpsHighAccuracy = getPropertyString("gpsHighAccuracy");
        String gpsMinAccuracy = getPropertyString("gpsMinAccuracy");
        String autoCloseDistance = getPropertyString("autoCloseDistance");

        // Appearance
        String fillColor = getPropertyString("fillColor");
        String fillOpacity = getPropertyString("fillOpacity");
        String strokeColor = getPropertyString("strokeColor");
        String strokeWidth = getPropertyString("strokeWidth");

        // API Settings
        String apiEndpoint = getPropertyString("apiEndpoint");
        String apiId = getPropertyString("apiId");
        String apiKey = getPropertyString("apiKey");

        // Get record ID for edit mode (to exclude from overlap checking)
        String recordId = "";
        if (formData != null) {
            // Get primary key value - this is set when editing an existing record
            String primaryKey = formData.getPrimaryKeyValue();
            if (primaryKey != null && !primaryKey.isEmpty()) {
                recordId = primaryKey;
            }
        }

        // Apply defaults
        if (captureMode == null || captureMode.isEmpty()) captureMode = "BOTH";
        if (defaultMode == null || defaultMode.isEmpty()) defaultMode = "AUTO";
        if (defaultLatitude == null || defaultLatitude.isEmpty()) defaultLatitude = "-29.5";
        if (defaultLongitude == null || defaultLongitude.isEmpty()) defaultLongitude = "28.5";
        if (defaultZoom == null || defaultZoom.isEmpty()) defaultZoom = "10";
        if (tileProvider == null || tileProvider.isEmpty()) tileProvider = "OSM";
        if (showSatelliteOption == null || showSatelliteOption.isEmpty()) showSatelliteOption = "true";
        if (mapHeight == null || mapHeight.isEmpty()) mapHeight = "400";
        if (minAreaHectares == null || minAreaHectares.isEmpty()) minAreaHectares = "0.01";
        if (maxAreaHectares == null || maxAreaHectares.isEmpty()) maxAreaHectares = "1000";
        if (minVertices == null || minVertices.isEmpty()) minVertices = "3";
        if (maxVertices == null || maxVertices.isEmpty()) maxVertices = "100";
        if (gpsHighAccuracy == null || gpsHighAccuracy.isEmpty()) gpsHighAccuracy = "true";
        if (gpsMinAccuracy == null || gpsMinAccuracy.isEmpty()) gpsMinAccuracy = "10";
        if (autoCloseDistance == null || autoCloseDistance.isEmpty()) autoCloseDistance = "15";
        if (fillColor == null || fillColor.isEmpty()) fillColor = "#3388ff";
        if (fillOpacity == null || fillOpacity.isEmpty()) fillOpacity = "0.2";
        if (strokeColor == null || strokeColor.isEmpty()) strokeColor = "#3388ff";
        if (strokeWidth == null || strokeWidth.isEmpty()) strokeWidth = "3";
        if (apiEndpoint == null || apiEndpoint.isEmpty()) apiEndpoint = "/jw/api/gis/gis";

        // Get current value (GeoJSON geometry)
        String value = FormUtil.getElementPropertyValue(this, formData);
        if (value == null) {
            value = "";
        }

        // Build configuration JSON for JavaScript
        JSONObject config = new JSONObject();
        try {
            config.put("captureMode", captureMode);
            config.put("defaultMode", defaultMode);
            config.put("defaultLatitude", Double.parseDouble(defaultLatitude));
            config.put("defaultLongitude", Double.parseDouble(defaultLongitude));
            config.put("defaultZoom", Integer.parseInt(defaultZoom));
            config.put("tileProvider", tileProvider);
            config.put("showSatelliteOption", "true".equals(showSatelliteOption));
            config.put("mapHeight", Integer.parseInt(mapHeight));
            
            // Validation config
            JSONObject validation = new JSONObject();
            validation.put("minAreaHectares", Double.parseDouble(minAreaHectares));
            validation.put("maxAreaHectares", Double.parseDouble(maxAreaHectares));
            validation.put("minVertices", Integer.parseInt(minVertices));
            validation.put("maxVertices", Integer.parseInt(maxVertices));
            validation.put("allowSelfIntersection", "true".equals(allowSelfIntersection));
            config.put("validation", validation);
            
            // GPS config
            JSONObject gps = new JSONObject();
            gps.put("highAccuracy", "true".equals(gpsHighAccuracy));
            gps.put("minAccuracy", Double.parseDouble(gpsMinAccuracy));
            gps.put("autoCloseDistance", Double.parseDouble(autoCloseDistance));
            config.put("gps", gps);
            
            // Style config
            JSONObject style = new JSONObject();
            style.put("fillColor", fillColor);
            style.put("fillOpacity", Double.parseDouble(fillOpacity));
            style.put("strokeColor", strokeColor);
            style.put("strokeWidth", Integer.parseInt(strokeWidth));
            config.put("style", style);

            // Overlap config
            if ("true".equals(enableOverlapCheck)) {
                JSONObject overlap = new JSONObject();
                overlap.put("enabled", true);
                overlap.put("formId", overlapFormId != null ? overlapFormId : "");
                overlap.put("geometryField", overlapGeometryField != null ? overlapGeometryField : "");
                overlap.put("displayFields", overlapDisplayFields != null ? overlapDisplayFields : "");
                overlap.put("filterCondition", overlapFilterCondition != null ? overlapFilterCondition : "");
                config.put("overlap", overlap);
            }

            // Nearby Parcels Display config
            if (showNearbyParcels != null && !"DISABLED".equals(showNearbyParcels) && !showNearbyParcels.isEmpty()) {
                JSONObject nearbyParcels = new JSONObject();
                nearbyParcels.put("enabled", showNearbyParcels);
                nearbyParcels.put("formId", nearbyParcelsFormId != null ? nearbyParcelsFormId : "");
                nearbyParcels.put("geometryFieldId", nearbyParcelsGeometryField != null ? nearbyParcelsGeometryField : "c_geometry");
                nearbyParcels.put("displayFields", nearbyParcelsDisplayFields != null ? nearbyParcelsDisplayFields : "");
                nearbyParcels.put("filterCondition", nearbyParcelsFilterCondition != null ? nearbyParcelsFilterCondition : "");
                nearbyParcels.put("maxResults", nearbyParcelsMaxResults != null && !nearbyParcelsMaxResults.isEmpty()
                    ? Integer.parseInt(nearbyParcelsMaxResults) : 100);

                // Style configuration
                JSONObject nearbyStyle = new JSONObject();
                nearbyStyle.put("fillColor", nearbyParcelsFillColor != null && !nearbyParcelsFillColor.isEmpty()
                    ? nearbyParcelsFillColor : "#808080");
                nearbyStyle.put("fillOpacity", nearbyParcelsFillOpacity != null && !nearbyParcelsFillOpacity.isEmpty()
                    ? Double.parseDouble(nearbyParcelsFillOpacity) : 0.15);
                nearbyStyle.put("strokeColor", nearbyParcelsStrokeColor != null && !nearbyParcelsStrokeColor.isEmpty()
                    ? nearbyParcelsStrokeColor : "#666666");
                nearbyStyle.put("strokeWidth", 1);
                nearbyStyle.put("strokeDashArray", "3, 3");
                nearbyParcels.put("style", nearbyStyle);

                config.put("nearbyParcels", nearbyParcels);
            }

            // Output field mapping
            JSONObject outputFields = new JSONObject();
            outputFields.put("areaFieldId", areaFieldId != null ? areaFieldId : "");
            outputFields.put("perimeterFieldId", perimeterFieldId != null ? perimeterFieldId : "");
            outputFields.put("centroidFieldId", centroidFieldId != null ? centroidFieldId : "");
            outputFields.put("vertexCountFieldId", vertexCountFieldId != null ? vertexCountFieldId : "");
            config.put("outputFields", outputFields);

        } catch (Exception e) {
            LogUtil.error(CLASS_NAME, e, "Error building config JSON");
        }

        // Base URL for static resources (PluginWebSupport)
        String resourceBase = "/jw/web/json/plugin/" + GisResourcesPlugin.class.getName() + "/service?file=";

        // Add to data model
        dataModel.put("fieldId", fieldId);
        dataModel.put("value", value);
        dataModel.put("resourceBase", resourceBase);
        dataModel.put("apiBase", apiEndpoint);
        dataModel.put("apiId", apiId != null ? apiId : "");
        dataModel.put("apiKey", apiKey != null ? apiKey : "");
        dataModel.put("elementId", "gis_" + fieldId + "_" + System.currentTimeMillis());
        dataModel.put("config", config.toString());
        dataModel.put("mapHeight", mapHeight);

        // Output field IDs for JavaScript
        dataModel.put("areaFieldId", areaFieldId != null ? areaFieldId : "");
        dataModel.put("perimeterFieldId", perimeterFieldId != null ? perimeterFieldId : "");
        dataModel.put("centroidFieldId", centroidFieldId != null ? centroidFieldId : "");
        dataModel.put("vertexCountFieldId", vertexCountFieldId != null ? vertexCountFieldId : "");

        // Record ID for edit mode (to exclude from overlap checking)
        dataModel.put("recordId", recordId);

        // Render
        String html = FormUtil.generateElementHtml(this, formData, template, dataModel);
        return html;
    }

    @Override
    public FormData formatDataForValidation(FormData formData) {
        return formData;
    }

    @Override
    public Boolean selfValidate(FormData formData) {
        String fieldId = FormUtil.getElementParameterName(this);
        String value = formData.getRequestParameter(fieldId);
        
        // Check if required
        String required = getPropertyString("required");
        if ("true".equals(required) && (value == null || value.isEmpty())) {
            String errorMsg = getPropertyString("requiredMessage");
            if (errorMsg == null || errorMsg.isEmpty()) {
                errorMsg = "Please capture a polygon boundary";
            }
            formData.addFormError(fieldId, errorMsg);
            return false;
        }

        // Basic GeoJSON validation if value provided
        if (value != null && !value.isEmpty()) {
            try {
                JSONObject geojson = new JSONObject(value);
                String type = geojson.optString("type", "");
                if (!"Polygon".equals(type) && !"Feature".equals(type)) {
                    formData.addFormError(fieldId, "Invalid geometry type. Expected Polygon.");
                    return false;
                }
            } catch (Exception e) {
                formData.addFormError(fieldId, "Invalid GeoJSON format");
                return false;
            }
        }

        return true;
    }
}
