package global.govstack.gisui.element;

import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.Form;
import org.joget.apps.form.model.FormBuilderPaletteElement;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.LogUtil;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    // UUID pattern for extracting clean UUIDs from potentially malformed parameters
    // Allows 8-16 hex chars in last segment to handle both standard (12) and non-standard UUIDs
    private static final Pattern UUID_PATTERN = Pattern.compile(
        "([a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{8,16})"
    );

    // Allowed SQL operators for filter conditions (whitelist approach for security)
    private static final Set<String> ALLOWED_SQL_OPERATORS = new HashSet<>(Arrays.asList(
        "=", "!=", "<>", "<", ">", "<=", ">=", "LIKE", "NOT LIKE", "IN", "NOT IN",
        "IS NULL", "IS NOT NULL", "BETWEEN", "AND", "OR", "NOT"
    ));

    // Dangerous SQL patterns that should be rejected
    private static final Pattern DANGEROUS_SQL_PATTERN = Pattern.compile(
        "(?i)(;|--|\\/\\*|\\*\\/|DROP|DELETE|INSERT|UPDATE|TRUNCATE|ALTER|CREATE|EXEC|EXECUTE|UNION|INTO|OUTFILE|DUMPFILE)",
        Pattern.CASE_INSENSITIVE
    );

    /**
     * Extract a valid UUID from a potentially malformed input string.
     * Handles cases where the URL parameter may be malformed (e.g., "uuid_mode=edit" instead of "uuid&_mode=edit")
     *
     * @param input The input string that may contain a UUID
     * @return The extracted UUID, or the original input if no UUID found (might be numeric ID)
     */
    private String extractUUID(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        Matcher matcher = UUID_PATTERN.matcher(input);
        if (matcher.find()) {
            String extracted = matcher.group(1);
            LogUtil.debug(CLASS_NAME, "extractUUID: input='" + input + "' -> extracted='" + extracted + "'");
            return extracted;
        }
        LogUtil.debug(CLASS_NAME, "extractUUID: input='" + input + "' -> no UUID match, returning original");
        return input; // Return original if no UUID found (might be numeric ID)
    }

    /**
     * Check if a record ID is valid (non-null, non-empty, doesn't contain URL artifacts like "_mode")
     *
     * @param id The ID to validate
     * @return true if the ID appears to be valid
     */
    private boolean isValidRecordId(String id) {
        return id != null && !id.isEmpty() && !id.contains("_mode");
    }

    /**
     * Validate and sanitize a SQL filter condition.
     * Returns the sanitized condition or empty string if dangerous patterns detected.
     *
     * @param filterCondition The filter condition to validate
     * @return Sanitized filter condition or empty string if invalid
     */
    private String sanitizeFilterCondition(String filterCondition) {
        if (filterCondition == null || filterCondition.trim().isEmpty()) {
            return "";
        }

        String trimmed = filterCondition.trim();

        // Check for dangerous SQL patterns
        if (DANGEROUS_SQL_PATTERN.matcher(trimmed).find()) {
            LogUtil.warn(CLASS_NAME, "Blocked potentially dangerous filter condition: " + trimmed);
            return "";
        }

        return trimmed;
    }

    /**
     * Get property value with a default fallback.
     *
     * @param value The property value (may be null or empty)
     * @param defaultValue The default value to use if value is null/empty
     * @return The value or default
     */
    private String getPropertyWithDefault(String value, String defaultValue) {
        return (value == null || value.isEmpty()) ? defaultValue : value;
    }

    /**
     * Safely parse a double value with a default fallback.
     *
     * @param value The string value to parse
     * @param defaultValue The default value if parsing fails
     * @return The parsed double or default
     */
    private double parseDoubleSafe(String value, double defaultValue) {
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            LogUtil.debug(CLASS_NAME, "Invalid double value '" + value + "', using default: " + defaultValue);
            return defaultValue;
        }
    }

    /**
     * Safely parse an integer value with a default fallback.
     *
     * @param value The string value to parse
     * @param defaultValue The default value if parsing fails
     * @return The parsed integer or default
     */
    private int parseIntSafe(String value, int defaultValue) {
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            LogUtil.debug(CLASS_NAME, "Invalid integer value '" + value + "', using default: " + defaultValue);
            return defaultValue;
        }
    }

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

        // Auto-Center Settings
        String enableAutoCenter = getPropertyString("enableAutoCenter");
        String autoCenterDistrictFieldId = getPropertyString("autoCenterDistrictFieldId");
        String autoCenterVillageFieldId = getPropertyString("autoCenterVillageFieldId");
        String autoCenterLatFieldId = getPropertyString("autoCenterLatFieldId");
        String autoCenterLonFieldId = getPropertyString("autoCenterLonFieldId");
        String autoCenterCountrySuffix = getPropertyString("autoCenterCountrySuffix");
        String autoCenterZoom = getPropertyString("autoCenterZoom");
        String autoCenterRetryOnFieldChange = getPropertyString("autoCenterRetryOnFieldChange");

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
        // Try multiple sources since getPrimaryKeyValue() may be null in multi-tab forms
        // Use extractUUID() to handle malformed URL parameters (e.g., "uuid_mode=edit" instead of "uuid&_mode=edit")
        String recordId = "";
        if (formData != null) {
            // 1. Try primary key value (works for direct form load)
            String primaryKey = formData.getPrimaryKeyValue();
            LogUtil.debug(CLASS_NAME, "RecordId extraction - primaryKey raw: " + primaryKey);
            if (isValidRecordId(primaryKey)) {
                recordId = primaryKey;
                LogUtil.debug(CLASS_NAME, "RecordId from primaryKey: " + recordId);
            }

            // 2. Try loaded binder data via root form (most reliable for multi-tab/sub-forms)
            // This gets the actual record ID from the loaded FormRowSet, not from URL parameters
            if (recordId.isEmpty()) {
                Form rootForm = FormUtil.findRootForm(this);
                if (rootForm != null) {
                    // First try getLoadBinderData which returns the actual loaded row data
                    FormRowSet rowSet = formData.getLoadBinderData(rootForm);
                    LogUtil.debug(CLASS_NAME, "RecordId extraction - FormRowSet: " + (rowSet != null ? "size=" + rowSet.size() : "null"));
                    if (rowSet != null && !rowSet.isEmpty()) {
                        FormRow row = rowSet.get(0);
                        if (row != null) {
                            // FormRow.getId() returns the actual primary key value
                            String rowId = row.getId();
                            LogUtil.debug(CLASS_NAME, "RecordId extraction - FormRow.getId(): " + rowId);
                            if (isValidRecordId(rowId)) {
                                recordId = rowId;
                                LogUtil.debug(CLASS_NAME, "RecordId from FormRow.getId(): " + recordId);
                            }
                        }
                    }

                    // Fallback to getLoadBinderDataProperty if FormRowSet didn't work
                    if (recordId.isEmpty()) {
                        String binderId = formData.getLoadBinderDataProperty(rootForm, "id");
                        LogUtil.debug(CLASS_NAME, "RecordId extraction - getLoadBinderDataProperty(id): " + binderId);
                        if (isValidRecordId(binderId)) {
                            recordId = binderId;
                            LogUtil.debug(CLASS_NAME, "RecordId from getLoadBinderDataProperty: " + recordId);
                        }
                    }
                }
            }

            // 3. Try request parameter 'id' with UUID extraction (fallback for malformed URLs)
            if (recordId.isEmpty()) {
                String idParam = formData.getRequestParameter("id");
                LogUtil.debug(CLASS_NAME, "RecordId extraction - idParam raw: " + idParam);
                String extractedId = extractUUID(idParam);
                if (isValidRecordId(extractedId)) {
                    recordId = extractedId;
                    LogUtil.debug(CLASS_NAME, "RecordId from URL parameter (extracted): " + recordId);
                }
            }

            // 4. Try 'recordId' request parameter
            if (recordId.isEmpty()) {
                String recordIdParam = formData.getRequestParameter("recordId");
                LogUtil.debug(CLASS_NAME, "RecordId extraction - recordIdParam raw: " + recordIdParam);
                String extractedId = extractUUID(recordIdParam);
                if (isValidRecordId(extractedId)) {
                    recordId = extractedId;
                    LogUtil.debug(CLASS_NAME, "RecordId from recordIdParam (extracted): " + recordId);
                }
            }

            LogUtil.debug(CLASS_NAME, "RecordId final value: " + recordId);
        }

        // Apply defaults using utility method
        captureMode = getPropertyWithDefault(captureMode, "BOTH");
        defaultMode = getPropertyWithDefault(defaultMode, "AUTO");
        tileProvider = getPropertyWithDefault(tileProvider, "OSM");
        showSatelliteOption = getPropertyWithDefault(showSatelliteOption, "true");
        fillColor = getPropertyWithDefault(fillColor, "#3388ff");
        strokeColor = getPropertyWithDefault(strokeColor, "#3388ff");
        apiEndpoint = getPropertyWithDefault(apiEndpoint, "/jw/api/gis/gis");

        // Sanitize filter conditions for security
        String sanitizedOverlapFilter = sanitizeFilterCondition(overlapFilterCondition);
        String sanitizedNearbyFilter = sanitizeFilterCondition(nearbyParcelsFilterCondition);

        // Get current value (GeoJSON geometry)
        String value = FormUtil.getElementPropertyValue(this, formData);
        if (value == null) {
            value = "";
        }

        // Build configuration JSON for JavaScript using safe parsing
        JSONObject config = new JSONObject();
        try {
            config.put("captureMode", captureMode);
            config.put("defaultMode", defaultMode);
            config.put("defaultLatitude", parseDoubleSafe(defaultLatitude, -29.5));
            config.put("defaultLongitude", parseDoubleSafe(defaultLongitude, 28.5));
            config.put("defaultZoom", parseIntSafe(defaultZoom, 10));
            config.put("tileProvider", tileProvider);
            config.put("showSatelliteOption", "true".equals(showSatelliteOption));
            config.put("mapHeight", parseIntSafe(mapHeight, 400));

            // Validation config with safe parsing
            JSONObject validation = new JSONObject();
            validation.put("minAreaHectares", parseDoubleSafe(minAreaHectares, 0.01));
            validation.put("maxAreaHectares", parseDoubleSafe(maxAreaHectares, 1000.0));
            validation.put("minVertices", parseIntSafe(minVertices, 3));
            validation.put("maxVertices", parseIntSafe(maxVertices, 100));
            validation.put("allowSelfIntersection", "true".equals(allowSelfIntersection));
            config.put("validation", validation);

            // GPS config with safe parsing
            JSONObject gps = new JSONObject();
            gps.put("highAccuracy", !"false".equals(getPropertyWithDefault(gpsHighAccuracy, "true")));
            gps.put("minAccuracy", parseDoubleSafe(gpsMinAccuracy, 10.0));
            gps.put("autoCloseDistance", parseDoubleSafe(autoCloseDistance, 15.0));
            config.put("gps", gps);

            // Style config with safe parsing
            JSONObject style = new JSONObject();
            style.put("fillColor", fillColor);
            style.put("fillOpacity", parseDoubleSafe(fillOpacity, 0.2));
            style.put("strokeColor", strokeColor);
            style.put("strokeWidth", parseIntSafe(strokeWidth, 3));
            config.put("style", style);

            // Overlap config with sanitized filter
            if ("true".equals(enableOverlapCheck)) {
                JSONObject overlap = new JSONObject();
                overlap.put("enabled", true);
                overlap.put("formId", getPropertyWithDefault(overlapFormId, ""));
                overlap.put("geometryField", getPropertyWithDefault(overlapGeometryField, ""));
                overlap.put("displayFields", getPropertyWithDefault(overlapDisplayFields, ""));
                overlap.put("filterCondition", sanitizedOverlapFilter);
                config.put("overlap", overlap);
            }

            // Nearby Parcels Display config with sanitized filter
            if (showNearbyParcels != null && !"DISABLED".equals(showNearbyParcels) && !showNearbyParcels.isEmpty()) {
                JSONObject nearbyParcels = new JSONObject();
                nearbyParcels.put("enabled", showNearbyParcels);
                nearbyParcels.put("formId", getPropertyWithDefault(nearbyParcelsFormId, ""));
                nearbyParcels.put("geometryFieldId", getPropertyWithDefault(nearbyParcelsGeometryField, "c_geometry"));
                nearbyParcels.put("displayFields", getPropertyWithDefault(nearbyParcelsDisplayFields, ""));
                nearbyParcels.put("filterCondition", sanitizedNearbyFilter);
                nearbyParcels.put("maxResults", parseIntSafe(nearbyParcelsMaxResults, 100));

                // Style configuration with safe parsing
                JSONObject nearbyStyle = new JSONObject();
                nearbyStyle.put("fillColor", getPropertyWithDefault(nearbyParcelsFillColor, "#808080"));
                nearbyStyle.put("fillOpacity", parseDoubleSafe(nearbyParcelsFillOpacity, 0.15));
                nearbyStyle.put("strokeColor", getPropertyWithDefault(nearbyParcelsStrokeColor, "#666666"));
                nearbyStyle.put("strokeWidth", 1);
                nearbyStyle.put("strokeDashArray", "3, 3");
                nearbyParcels.put("style", nearbyStyle);

                config.put("nearbyParcels", nearbyParcels);
            }

            // Output field mapping
            JSONObject outputFields = new JSONObject();
            outputFields.put("areaFieldId", getPropertyWithDefault(areaFieldId, ""));
            outputFields.put("perimeterFieldId", getPropertyWithDefault(perimeterFieldId, ""));
            outputFields.put("centroidFieldId", getPropertyWithDefault(centroidFieldId, ""));
            outputFields.put("vertexCountFieldId", getPropertyWithDefault(vertexCountFieldId, ""));
            config.put("outputFields", outputFields);

            // Auto-Center config with safe parsing
            if ("true".equals(enableAutoCenter)) {
                JSONObject autoCenter = new JSONObject();
                autoCenter.put("enabled", true);
                autoCenter.put("districtFieldId", getPropertyWithDefault(autoCenterDistrictFieldId, ""));
                autoCenter.put("villageFieldId", getPropertyWithDefault(autoCenterVillageFieldId, ""));
                autoCenter.put("latFieldId", getPropertyWithDefault(autoCenterLatFieldId, ""));
                autoCenter.put("lonFieldId", getPropertyWithDefault(autoCenterLonFieldId, ""));
                autoCenter.put("countrySuffix", getPropertyWithDefault(autoCenterCountrySuffix, "Lesotho"));
                autoCenter.put("zoom", parseIntSafe(autoCenterZoom, 14));
                autoCenter.put("retryOnFieldChange", !"false".equals(autoCenterRetryOnFieldChange));
                config.put("autoCenter", autoCenter);
            }

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
        // NOTE: API credentials (apiId, apiKey) are intentionally NOT passed to client-side
        // The backend GIS API should handle authentication via session or server-side proxy
        dataModel.put("elementId", "gis_" + fieldId + "_" + System.currentTimeMillis());
        dataModel.put("config", config.toString());
        dataModel.put("mapHeight", parseIntSafe(mapHeight, 400));

        // Output field IDs for JavaScript
        dataModel.put("areaFieldId", getPropertyWithDefault(areaFieldId, ""));
        dataModel.put("perimeterFieldId", getPropertyWithDefault(perimeterFieldId, ""));
        dataModel.put("centroidFieldId", getPropertyWithDefault(centroidFieldId, ""));
        dataModel.put("vertexCountFieldId", getPropertyWithDefault(vertexCountFieldId, ""));

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

        // Comprehensive GeoJSON validation if value provided
        if (value != null && !value.isEmpty()) {
            String validationError = validateGeoJSON(value);
            if (validationError != null) {
                formData.addFormError(fieldId, validationError);
                return false;
            }
        }

        return true;
    }

    /**
     * Validate GeoJSON structure and coordinate bounds.
     *
     * @param geojsonStr The GeoJSON string to validate
     * @return Error message if invalid, null if valid
     */
    private String validateGeoJSON(String geojsonStr) {
        try {
            JSONObject geojson = new JSONObject(geojsonStr);
            String type = geojson.optString("type", "");

            // Handle Feature type - extract geometry
            JSONObject geometry;
            if ("Feature".equals(type)) {
                geometry = geojson.optJSONObject("geometry");
                if (geometry == null) {
                    return "Feature is missing geometry";
                }
            } else if ("Polygon".equals(type)) {
                geometry = geojson;
            } else {
                return "Invalid geometry type. Expected Polygon or Feature.";
            }

            // Verify geometry type is Polygon
            String geometryType = geometry.optString("type", "");
            if (!"Polygon".equals(geometryType)) {
                return "Invalid geometry type. Expected Polygon.";
            }

            // Validate coordinates exist
            JSONArray coordinates = geometry.optJSONArray("coordinates");
            if (coordinates == null || coordinates.length() == 0) {
                return "Polygon is missing coordinates";
            }

            // Get the outer ring (first coordinate array)
            JSONArray outerRing = coordinates.optJSONArray(0);
            if (outerRing == null || outerRing.length() < 4) {
                return "Polygon must have at least 4 coordinate points (3 vertices + closing point)";
            }

            // Validate each coordinate
            for (int i = 0; i < outerRing.length(); i++) {
                JSONArray coord = outerRing.optJSONArray(i);
                if (coord == null || coord.length() < 2) {
                    return "Invalid coordinate at position " + i;
                }

                double lng = coord.optDouble(0, Double.NaN);
                double lat = coord.optDouble(1, Double.NaN);

                if (Double.isNaN(lng) || Double.isNaN(lat)) {
                    return "Invalid coordinate values at position " + i;
                }

                // Validate coordinate bounds
                if (lat < -90 || lat > 90) {
                    return "Latitude out of bounds at position " + i + ": " + lat;
                }
                if (lng < -180 || lng > 180) {
                    return "Longitude out of bounds at position " + i + ": " + lng;
                }
            }

            // Verify polygon is closed (first coord == last coord)
            JSONArray firstCoord = outerRing.optJSONArray(0);
            JSONArray lastCoord = outerRing.optJSONArray(outerRing.length() - 1);
            if (firstCoord != null && lastCoord != null) {
                double firstLng = firstCoord.optDouble(0, Double.NaN);
                double firstLat = firstCoord.optDouble(1, Double.NaN);
                double lastLng = lastCoord.optDouble(0, Double.NaN);
                double lastLat = lastCoord.optDouble(1, Double.NaN);

                if (Math.abs(firstLng - lastLng) > 0.0000001 || Math.abs(firstLat - lastLat) > 0.0000001) {
                    return "Polygon is not closed (first and last coordinates must match)";
                }
            }

            return null; // Valid

        } catch (Exception e) {
            LogUtil.debug(CLASS_NAME, "GeoJSON validation error: " + e.getMessage());
            return "Invalid GeoJSON format: " + e.getMessage();
        }
    }
}
