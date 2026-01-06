# Draw Mode UI - Logical Specification

## Document Information

| Attribute | Value |
|-----------|-------|
| Document Type | Logical UI Specification |
| Component | Parcel Capture Field - Draw Mode |
| Version | 1.0 |
| Status | Draft |
| Parent System | Farmers Registry System (FRS) |
| Platform | Joget DX8 Enterprise Edition |

---

## 1. OVERVIEW

### 1.1 Purpose

This specification defines the user interface for capturing land parcel boundaries by drawing polygons on an interactive map. The Draw Mode is designed for office-based digitization where users trace parcel boundaries on satellite imagery or base maps.

### 1.2 Scope

This specification covers:
- All screens and screen states for Draw Mode
- User interactions and system responses
- Validation rules and error handling
- Information display requirements
- Navigation flows

This specification does NOT cover:
- Walk Mode (GPS boundary capture)
- Backend API implementation
- Database schema
- Code implementation details

### 1.3 Design Principles

| Principle | Description |
|-----------|-------------|
| **Progressive Disclosure** | Show only relevant controls for current state |
| **Immediate Feedback** | Visual response within 100ms of user action |
| **Error Prevention** | Prevent invalid states rather than just reporting errors |
| **Recoverability** | Allow undo/redo for all drawing actions |
| **Clarity** | Unambiguous visual indication of current mode and state |

### 1.4 User Context

**Primary User:** Agricultural Extension Officer at District Office

**Typical Scenario:** Officer has returned from field visit with paper sketches or mental notes about parcel locations. They need to digitize these parcels by drawing on satellite imagery while the farmer's registration form is open.

**Environment:**
- Desktop computer or laptop
- Mouse and keyboard input
- Stable internet connection
- Screen resolution: minimum 1366×768

---

## 2. COMPONENT STATES

### 2.1 State Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                     DRAW MODE STATE DIAGRAM                          │
└─────────────────────────────────────────────────────────────────────┘

                              ┌─────────────┐
                              │   INITIAL   │
                              │   (Empty)   │
                              └──────┬──────┘
                                     │
                          User clicks "Start Drawing"
                                     │
                                     ▼
                              ┌─────────────┐
                              │   DRAWING   │◄─────────────────────┐
                              │   (Active)  │                      │
                              └──────┬──────┘                      │
                                     │                             │
                    ┌────────────────┼────────────────┐            │
                    │                │                │            │
              User adds        User double-     User cancels       │
              vertex           clicks or             │             │
                    │          clicks first          │             │
                    │          vertex                │             │
                    ▼                │               ▼             │
             ┌─────────────┐        │        ┌─────────────┐      │
             │   DRAWING   │        │        │   INITIAL   │      │
             │ (n vertices)│        │        │   (Empty)   │      │
             └─────────────┘        │        └─────────────┘      │
                                    │                              │
                                    ▼                              │
                              ┌─────────────┐                      │
                              │  PREVIEW    │                      │
                              │  (Complete) │                      │
                              └──────┬──────┘                      │
                                     │                             │
                    ┌────────────────┼────────────────┐            │
                    │                │                │            │
              User confirms    User edits       User clears        │
                    │                │                │            │
                    ▼                ▼                └────────────┘
             ┌─────────────┐  ┌─────────────┐
             │   SAVED     │  │   EDITING   │
             │  (Final)    │  │  (Modify)   │
             └─────────────┘  └──────┬──────┘
                                     │
                              User finishes edit
                                     │
                                     ▼
                              ┌─────────────┐
                              │   PREVIEW   │
                              │  (Updated)  │
                              └─────────────┘
```

### 2.2 State Definitions

| State | Description | Entry Condition | Available Actions |
|-------|-------------|-----------------|-------------------|
| **INITIAL** | Empty map, no polygon defined | Component load / Clear action | Start Drawing, Navigate Map, Switch Tiles, Locate |
| **DRAWING** | Actively placing vertices | Click "Start Drawing" | Add Vertex, Undo Vertex, Cancel, Close Polygon |
| **PREVIEW** | Polygon complete, awaiting confirmation | Close polygon (double-click or click first vertex) | Confirm, Edit, Clear, View Metrics |
| **EDITING** | Modifying existing polygon | Click "Edit" from Preview | Move Vertex, Add Vertex, Delete Vertex, Cancel Edit, Finish Edit |
| **SAVED** | Polygon confirmed and stored | Click "Confirm" from Preview | Edit, Clear, View Only |

---

## 3. SCREEN SPECIFICATIONS

### 3.1 Component Layout

```
┌─────────────────────────────────────────────────────────────────────┐
│                      PARCEL CAPTURE FIELD                            │
│                         (Form Element)                               │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │ HEADER BAR                                                      │ │
│  │ [Mode Indicator] [Status Message]              [Tile Selector]  │ │
│  └────────────────────────────────────────────────────────────────┘ │
│                                                                      │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │                                                                 │ │
│  │                                                                 │ │
│  │                                                                 │ │
│  │                         MAP AREA                                │ │
│  │                                                                 │ │
│  │                    (Primary Interaction Zone)                   │ │
│  │                                                                 │ │
│  │                                                                 │ │
│  │                                                                 │ │
│  │  ┌──────────────┐                           ┌───────────────┐  │ │
│  │  │ ZOOM         │                           │ DRAWING       │  │ │
│  │  │ CONTROLS     │                           │ TOOLS         │  │ │
│  │  │ [+] [-]      │                           │ (contextual)  │  │ │
│  │  └──────────────┘                           └───────────────┘  │ │
│  │                                                                 │ │
│  │  ┌─────────────────────────────────────────────────────────┐   │ │
│  │  │ COORDINATE DISPLAY (on hover)                           │   │ │
│  │  └─────────────────────────────────────────────────────────┘   │ │
│  └────────────────────────────────────────────────────────────────┘ │
│                                                                      │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │ METRICS BAR                                                     │ │
│  │ [Area: -- ha] [Perimeter: -- m] [Vertices: --]                 │ │
│  └────────────────────────────────────────────────────────────────┘ │
│                                                                      │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │ ACTION BAR                                                      │ │
│  │ [Primary Action Button]    [Secondary Actions]                  │ │
│  └────────────────────────────────────────────────────────────────┘ │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 3.2 Component Dimensions

| Element | Specification |
|---------|---------------|
| **Total Component Width** | 100% of parent container |
| **Minimum Width** | 400px |
| **Map Area Height** | Configurable (default: 400px, range: 300-800px) |
| **Header Bar Height** | 40px |
| **Metrics Bar Height** | 32px |
| **Action Bar Height** | 48px |

---

## 4. SCREEN: INITIAL STATE

### 4.1 Visual Specification

```
┌─────────────────────────────────────────────────────────────────────┐
│ 🖱️ Draw Mode                Ready                    [Satellite ▼] │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │                                                                 │ │
│  │                                                                 │ │
│  │                    [Satellite/Map imagery]                      │ │
│  │                                                                 │ │
│  │                         Lesotho                                 │ │
│  │                     (Default center)                            │ │
│  │                                                                 │ │
│  │  [+]                                                            │ │
│  │  [-]                                                            │ │
│  │  [📍]                                              ┌─────────┐  │ │
│  │                                                    │ 🔍 Search│  │ │
│  │                                                    └─────────┘  │ │
│  │                                                                 │ │
│  │                               Lat: -29.6000, Lon: 28.2000       │ │
│  └────────────────────────────────────────────────────────────────┘ │
│                                                                      │
│  Area: -- ha        Perimeter: -- m        Vertices: --             │
│                                                                      │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │              🔲  START DRAWING                                │   │
│  └──────────────────────────────────────────────────────────────┘   │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 4.2 Element Specifications

#### 4.2.1 Header Bar

| Element | Specification |
|---------|---------------|
| **Mode Indicator** | Icon (🖱️) + Text "Draw Mode" |
| **Status Message** | Text "Ready" in neutral color |
| **Tile Selector** | Dropdown with options: Satellite, OpenStreetMap, Hybrid, Terrain |

#### 4.2.2 Map Area

| Element | Specification |
|---------|---------------|
| **Default Center** | Configurable via plugin properties (default: Lesotho center -29.6, 28.2) |
| **Default Zoom** | Configurable via plugin properties (default: 8) |
| **Tile Layer** | As selected in Tile Selector |
| **Cursor** | Default pointer when not drawing |

#### 4.2.3 Map Controls

| Control | Position | Function |
|---------|----------|----------|
| **Zoom In (+)** | Top-left | Increase zoom level by 1 |
| **Zoom Out (-)** | Top-left, below Zoom In | Decrease zoom level by 1 |
| **Locate Me (📍)** | Top-left, below Zoom Out | Center map on user's current location (if available) |
| **Search (🔍)** | Top-right | Open location search input |

#### 4.2.4 Coordinate Display

| Element | Specification |
|---------|---------------|
| **Position** | Bottom of map area, centered |
| **Content** | "Lat: {latitude}, Lon: {longitude}" |
| **Behavior** | Updates on mouse move over map |
| **Format** | 4 decimal places for coordinates |

#### 4.2.5 Metrics Bar

| Metric | Initial Display | Format When Available |
|--------|-----------------|----------------------|
| **Area** | "Area: -- ha" | "Area: {value} ha" (2 decimal places) |
| **Perimeter** | "Perimeter: -- m" | "Perimeter: {value} m" (0 decimal places) |
| **Vertices** | "Vertices: --" | "Vertices: {count}" |

#### 4.2.6 Action Bar

| Element | Specification |
|---------|---------------|
| **Primary Button** | "START DRAWING" with icon (🔲) |
| **Button Style** | Full width, prominent (primary color) |
| **Button State** | Enabled |

### 4.3 Interactions

| User Action | System Response |
|-------------|-----------------|
| Click "START DRAWING" | Transition to DRAWING state |
| Click Tile Selector | Show dropdown with tile options |
| Select tile option | Change map tile layer immediately |
| Click Zoom In | Increase zoom by 1 level |
| Click Zoom Out | Decrease zoom by 1 level |
| Click Locate Me | Request browser geolocation, center map on result |
| Click Search | Show search input field |
| Enter search term + submit | Geocode location, center map on result |
| Mouse move over map | Update coordinate display |
| Mouse scroll over map | Zoom in/out at cursor position |
| Click and drag map | Pan map |

### 4.4 Validation Rules

| Rule | Condition |
|------|-----------|
| **VR-INIT-01** | Locate Me button disabled if geolocation not supported by browser |
| **VR-INIT-02** | Search requires minimum 3 characters |

---

## 5. SCREEN: DRAWING STATE

### 5.1 Visual Specification

```
┌─────────────────────────────────────────────────────────────────────┐
│ 🖱️ Draw Mode             Drawing (3 vertices)        [Satellite ▼] │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │                                                                 │ │
│  │            1⃣━━━━━━━━━━━━━━━━━━━━━━━━━━━━2⃣                     │ │
│  │            ┃                              ┃                     │ │
│  │            ┃    [Satellite imagery]       ┃                     │ │
│  │            ┃                              ┃                     │ │
│  │            ┃         ~0.65 ha             ┃                     │ │
│  │            ┃                              ┃                     │ │
│  │  [+]       ┃                              ┃                     │ │
│  │  [-]       3⃣─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ╳  ← cursor           │ │
│  │  [📍]                                                           │ │
│  │                                                                 │ │
│  │  ┌─────────────────────────────────────────────────────────┐   │ │
│  │  │ Click to add vertex • Double-click to finish            │   │ │
│  │  │ Click first vertex (1) to close polygon                 │   │ │
│  │  └─────────────────────────────────────────────────────────┘   │ │
│  │                               Lat: -29.3145, Lon: 28.1234       │ │
│  └────────────────────────────────────────────────────────────────┘ │
│                                                                      │
│  Area: ~0.65 ha     Perimeter: ~325 m      Vertices: 3              │
│                                                                      │
│  ┌───────────────┐  ┌───────────────┐  ┌───────────────────────┐   │
│  │  ↩️ Undo Last  │  │  ❌ Cancel     │  │  ✅ Close Polygon     │   │
│  └───────────────┘  └───────────────┘  └───────────────────────┘   │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 5.2 Element Specifications

#### 5.2.1 Header Bar

| Element | Specification |
|---------|---------------|
| **Status Message** | "Drawing ({n} vertices)" - updates with each vertex added |
| **Status Color** | Active/in-progress color (e.g., blue) |

#### 5.2.2 Map Area - Drawing Elements

| Element | Specification |
|---------|---------------|
| **Placed Vertices** | Numbered markers (1⃣, 2⃣, 3⃣...) at each clicked point |
| **Vertex Marker Style** | Circle with number, 24px diameter, primary color fill, white text |
| **Completed Edges** | Solid line connecting consecutive vertices |
| **Edge Style** | 3px width, primary color, 100% opacity |
| **Pending Edge** | Dashed line from last vertex to cursor position |
| **Pending Edge Style** | 2px width, primary color, 50% opacity, dashed pattern |
| **Polygon Fill** | Semi-transparent fill when 3+ vertices |
| **Fill Style** | Primary color, 20% opacity |
| **Area Label** | Estimated area displayed at polygon centroid |
| **Area Label Style** | White background, primary color text, rounded corners |

#### 5.2.3 Cursor Behavior

| Condition | Cursor Style |
|-----------|--------------|
| Over map (drawing mode) | Crosshair (+) |
| Over first vertex (3+ vertices placed) | Pointer with close indicator |
| Over existing vertex | Default pointer |

#### 5.2.4 Instruction Panel

| Element | Specification |
|---------|---------------|
| **Position** | Bottom of map area, above coordinate display |
| **Background** | Semi-transparent dark overlay |
| **Content Line 1** | "Click to add vertex • Double-click to finish" |
| **Content Line 2** | "Click first vertex (1) to close polygon" (shown when 3+ vertices) |
| **Visibility** | Always visible during DRAWING state |

#### 5.2.5 Metrics Bar

| Metric | Display | Note |
|--------|---------|------|
| **Area** | "Area: ~{value} ha" | Prefixed with ~ to indicate estimate |
| **Perimeter** | "Perimeter: ~{value} m" | Prefixed with ~ to indicate estimate |
| **Vertices** | "Vertices: {count}" | Exact count |

#### 5.2.6 Action Bar

| Button | Position | State | Behavior |
|--------|----------|-------|----------|
| **Undo Last** | Left | Enabled when vertices > 0 | Remove last placed vertex |
| **Cancel** | Center | Always enabled | Discard drawing, return to INITIAL |
| **Close Polygon** | Right | Enabled when vertices ≥ 3 | Complete polygon, go to PREVIEW |

### 5.3 Interactions

| User Action | System Response |
|-------------|-----------------|
| Click on map | Add vertex at click location |
| Add 1st vertex | Show numbered marker (1), update vertex count |
| Add 2nd vertex | Show marker (2), draw solid edge 1→2 |
| Add 3rd vertex | Show marker (3), draw edges, show polygon fill with estimated area |
| Add nth vertex | Continue pattern, update all metrics |
| Double-click on map | Add final vertex, close polygon, transition to PREVIEW |
| Click on first vertex (when ≥3 vertices) | Close polygon, transition to PREVIEW |
| Click "Undo Last" | Remove last vertex and associated edges |
| Click "Undo Last" (1 vertex) | Remove vertex, return to drawing state with 0 vertices |
| Click "Cancel" | Show confirmation dialog |
| Confirm cancel | Discard all vertices, return to INITIAL |
| Decline cancel | Remain in DRAWING state |
| Click "Close Polygon" | Close polygon, transition to PREVIEW |
| Mouse move | Update pending edge, update coordinate display |

### 5.4 Validation Rules

| Rule ID | Rule | User Feedback |
|---------|------|---------------|
| **VR-DRAW-01** | Minimum 3 vertices required to close polygon | "Close Polygon" button disabled until 3 vertices placed |
| **VR-DRAW-02** | Cannot place vertex on exact same location as existing vertex | Ignore click, no feedback needed |
| **VR-DRAW-03** | Maximum vertices limit (configurable, default 100) | Show warning toast when approaching limit (90%), prevent additional vertices at limit |

### 5.5 Keyboard Shortcuts

| Key | Action |
|-----|--------|
| **Escape** | Cancel drawing (with confirmation) |
| **Ctrl+Z** | Undo last vertex |
| **Enter** | Close polygon (if ≥3 vertices) |

---

## 6. SCREEN: PREVIEW STATE

### 6.1 Visual Specification

```
┌─────────────────────────────────────────────────────────────────────┐
│ 🖱️ Draw Mode            Preview - Verify Boundary    [Satellite ▼] │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │                                                                 │ │
│  │            ┌─────────────────────────────────┐                  │ │
│  │            │▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓│                  │ │
│  │            │▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓│                  │ │
│  │            │▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓│                  │ │
│  │            │▓▓▓▓▓▓▓▓▓ 0.82 ha ▓▓▓▓▓▓▓▓▓▓▓▓▓▓│                  │ │
│  │            │▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓│                  │ │
│  │  [+]       │▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓│                  │ │
│  │  [-]       └─────────────────────────────────┘                  │ │
│  │                                                                 │ │
│  │                                                                 │ │
│  │  ┌─────────────────────────────────────────────────────────┐   │ │
│  │  │ ✅ Validation Passed                                     │   │ │
│  │  │    Area within limits • Valid polygon shape              │   │ │
│  │  └─────────────────────────────────────────────────────────┘   │ │
│  │                               Lat: -29.3145, Lon: 28.1234       │ │
│  └────────────────────────────────────────────────────────────────┘ │
│                                                                      │
│  Area: 0.82 ha      Perimeter: 362 m       Vertices: 4              │
│                                                                      │
│  ┌───────────────┐  ┌───────────────┐  ┌───────────────────────┐   │
│  │  ✏️ Edit       │  │  🗑️ Clear      │  │  ✅ Confirm & Save    │   │
│  └───────────────┘  └───────────────┘  └───────────────────────┘   │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 6.2 Alternative: Preview with Validation Warnings

```
┌─────────────────────────────────────────────────────────────────────┐
│ 🖱️ Draw Mode            Preview - Verify Boundary    [Satellite ▼] │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │                                                                 │ │
│  │            ┌─────────────────────────────────┐                  │ │
│  │            │░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░│                  │ │
│  │            │░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░│ ← orange fill    │ │
│  │            │░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░│   indicates      │ │
│  │            │░░░░░░░░░ 0.005 ha ░░░░░░░░░░░░░░│   warning        │ │
│  │            │░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░│                  │ │
│  │  [+]       └─────────────────────────────────┘                  │ │
│  │  [-]                                                            │ │
│  │                                                                 │ │
│  │  ┌─────────────────────────────────────────────────────────┐   │ │
│  │  │ ⚠️ Validation Warning                                    │   │ │
│  │  │    Area (0.005 ha) is below recommended minimum (0.01)   │   │ │
│  │  │    You may still save, but please verify this is correct │   │ │
│  │  └─────────────────────────────────────────────────────────┘   │ │
│  │                               Lat: -29.3145, Lon: 28.1234       │ │
│  └────────────────────────────────────────────────────────────────┘ │
│                                                                      │
│  Area: 0.005 ha     Perimeter: 28 m        Vertices: 4              │
│                                                                      │
│  ┌───────────────┐  ┌───────────────┐  ┌───────────────────────┐   │
│  │  ✏️ Edit       │  │  🗑️ Clear      │  │  ⚠️ Save Anyway      │   │
│  └───────────────┘  └───────────────┘  └───────────────────────┘   │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 6.3 Alternative: Preview with Validation Errors

```
┌─────────────────────────────────────────────────────────────────────┐
│ 🖱️ Draw Mode            Preview - Verify Boundary    [Satellite ▼] │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │                                                                 │ │
│  │               ╱╲                                                │ │
│  │              ╱  ╲                                               │ │
│  │             ╱ ╳  ╲   ← self-intersection point marked          │ │
│  │            ╱  ┃   ╲                                             │ │
│  │           ╱   ┃    ╲                                            │ │
│  │  [+]      ╲   ┃    ╱   ← red outline indicates error           │ │
│  │  [-]       ╲  ┃   ╱                                             │ │
│  │             ╲ ┃  ╱                                              │ │
│  │              ╲┃ ╱                                               │ │
│  │               ╳╱                                                │ │
│  │                                                                 │ │
│  │  ┌─────────────────────────────────────────────────────────┐   │ │
│  │  │ ❌ Validation Error                                      │   │ │
│  │  │    Polygon is self-intersecting                          │   │ │
│  │  │    Please edit the boundary to fix this issue            │   │ │
│  │  └─────────────────────────────────────────────────────────┘   │ │
│  │                               Lat: -29.3145, Lon: 28.1234       │ │
│  └────────────────────────────────────────────────────────────────┘ │
│                                                                      │
│  Area: -- ha        Perimeter: -- m        Vertices: 5              │
│                                                                      │
│  ┌───────────────┐  ┌───────────────┐  ┌───────────────────────┐   │
│  │  ✏️ Edit       │  │  🗑️ Clear      │  │  ✅ Confirm (disabled)│   │
│  └───────────────┘  └───────────────┘  └───────────────────────┘   │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 6.4 Element Specifications

#### 6.4.1 Header Bar

| Element | Specification |
|---------|---------------|
| **Status Message** | "Preview - Verify Boundary" |
| **Status Color** | Neutral (preview mode) |

#### 6.4.2 Map Area - Preview Elements

| Element | Specification |
|---------|---------------|
| **Polygon Display** | Closed polygon with fill |
| **Polygon Style (Valid)** | Green border, green semi-transparent fill (30% opacity) |
| **Polygon Style (Warning)** | Orange border, orange semi-transparent fill (30% opacity) |
| **Polygon Style (Error)** | Red border, red semi-transparent fill (20% opacity) |
| **Vertex Markers** | Hidden in preview mode (shown in edit mode) |
| **Area Label** | Centered in polygon, larger font than drawing mode |
| **Error Marker** | Red X icon at location of geometric error (if applicable) |

#### 6.4.3 Validation Panel

| Validation State | Panel Style | Icon | Message Format |
|------------------|-------------|------|----------------|
| **Passed** | Green background | ✅ | "Validation Passed" + details |
| **Warning** | Orange/amber background | ⚠️ | "Validation Warning" + specific issue + guidance |
| **Error** | Red background | ❌ | "Validation Error" + specific issue + required action |

#### 6.4.4 Metrics Bar

| Metric | Display | Note |
|--------|---------|------|
| **Area** | "Area: {value} ha" | No ~ prefix (exact calculation) |
| **Perimeter** | "Perimeter: {value} m" | No ~ prefix (exact calculation) |
| **Vertices** | "Vertices: {count}" | Final count |

#### 6.4.5 Action Bar

| Button | Position | Enabled Condition | Style |
|--------|----------|-------------------|-------|
| **Edit** | Left | Always | Secondary style |
| **Clear** | Center | Always | Warning/destructive style |
| **Confirm & Save** | Right | Validation passed or warning only | Primary style (green) |
| **Save Anyway** | Right (replaces Confirm) | Validation warning | Warning style (orange) |
| **Confirm (disabled)** | Right | Validation error | Disabled/grayed |

### 6.5 Interactions

| User Action | System Response |
|-------------|-----------------|
| View screen | Automatically run validation, display result |
| Click "Edit" | Transition to EDITING state |
| Click "Clear" | Show confirmation dialog |
| Confirm clear | Discard polygon, return to INITIAL |
| Decline clear | Remain in PREVIEW |
| Click "Confirm & Save" | Save geometry to form fields, transition to SAVED |
| Click "Save Anyway" (warning) | Save geometry to form fields, transition to SAVED |
| Hover over error marker | Show tooltip with error details |
| Click on polygon | No action (view only in preview) |
| Pan/zoom map | Allowed for verification purposes |

### 6.6 Validation Rules (Applied in Preview)

| Rule ID | Type | Rule | User Feedback |
|---------|------|------|---------------|
| **VR-PREV-01** | Error | Self-intersecting polygon | "Polygon is self-intersecting" |
| **VR-PREV-02** | Error | Area = 0 or negative | "Invalid polygon shape" |
| **VR-PREV-03** | Error | Area exceeds maximum (configurable) | "Area ({value} ha) exceeds maximum ({max} ha)" |
| **VR-PREV-04** | Warning | Area below minimum (configurable) | "Area ({value} ha) is below recommended minimum ({min} ha)" |
| **VR-PREV-05** | Warning | Vertices exceed recommended (configurable) | "Polygon has {n} vertices, consider simplifying" |
| **VR-PREV-06** | Info | Backend validation enabled | Call backend API, display any additional errors/warnings |

---

## 7. SCREEN: EDITING STATE

### 7.1 Visual Specification

```
┌─────────────────────────────────────────────────────────────────────┐
│ 🖱️ Draw Mode              Editing                    [Satellite ▼] │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │                                                                 │ │
│  │            ○─────────────────────────────●                      │ │
│  │            │                              │                     │ │
│  │            │                              │  ● = selected       │ │
│  │            │         0.78 ha              │      vertex         │ │
│  │            │                              │                     │ │
│  │  [+]       │                              │  ○ = editable       │ │
│  │  [-]       ○──────────────────────────────○      vertex         │ │
│  │                                                                 │ │
│  │                         ◇ = midpoint handle (add vertex)        │ │
│  │                                                                 │ │
│  │  ┌─────────────────────────────────────────────────────────┐   │ │
│  │  │ Drag vertices to move • Click midpoints to add vertex   │   │ │
│  │  │ Right-click vertex to delete (min 3 required)           │   │ │
│  │  └─────────────────────────────────────────────────────────┘   │ │
│  │                               Lat: -29.3145, Lon: 28.1234       │ │
│  └────────────────────────────────────────────────────────────────┘ │
│                                                                      │
│  Area: ~0.78 ha     Perimeter: ~354 m      Vertices: 4              │
│                                                                      │
│  ┌───────────────┐  ┌───────────────┐  ┌───────────────────────┐   │
│  │  ↩️ Undo       │  │  ❌ Cancel     │  │  ✅ Finish Editing    │   │
│  └───────────────┘  └───────────────┘  └───────────────────────┘   │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 7.2 Element Specifications

#### 7.2.1 Header Bar

| Element | Specification |
|---------|---------------|
| **Status Message** | "Editing" |
| **Status Color** | Active/editing color (e.g., amber) |

#### 7.2.2 Map Area - Editing Elements

| Element | Specification |
|---------|---------------|
| **Vertex Handles (○)** | Draggable circles at each polygon vertex |
| **Handle Size** | 12px diameter |
| **Handle Style (Normal)** | White fill, primary color border, 2px border width |
| **Handle Style (Hover)** | Primary color fill, darker border |
| **Handle Style (Selected/Dragging)** | Solid primary color (●) |
| **Midpoint Handles (◇)** | Small diamonds at midpoint of each edge |
| **Midpoint Size** | 8px |
| **Midpoint Style** | Light gray fill, gray border |
| **Midpoint Style (Hover)** | Primary color fill |
| **Polygon Edges** | Solid lines connecting vertices |
| **Polygon Fill** | Semi-transparent, updates in real-time during edit |

#### 7.2.3 Cursor Behavior

| Condition | Cursor Style |
|-----------|--------------|
| Over map (not over handle) | Default pointer |
| Over vertex handle | Move cursor (four arrows) |
| Over midpoint handle | Crosshair (+) |
| Dragging vertex | Grabbing hand |

#### 7.2.4 Instruction Panel

| Element | Specification |
|---------|---------------|
| **Line 1** | "Drag vertices to move • Click midpoints to add vertex" |
| **Line 2** | "Right-click vertex to delete (min 3 required)" |

#### 7.2.5 Action Bar

| Button | Position | Enabled Condition |
|--------|----------|-------------------|
| **Undo** | Left | Edit history not empty |
| **Cancel** | Center | Always |
| **Finish Editing** | Right | Always |

### 7.3 Interactions

| User Action | System Response |
|-------------|-----------------|
| Hover over vertex handle | Change handle to hover style, change cursor to move |
| Click and drag vertex | Move vertex, update polygon shape and metrics in real-time |
| Release dragged vertex | Finalize vertex position, add to edit history |
| Hover over midpoint | Change midpoint to hover style, change cursor to crosshair |
| Click midpoint | Insert new vertex at midpoint, polygon now has n+1 vertices |
| Right-click on vertex (n > 3) | Show context menu with "Delete vertex" option |
| Right-click on vertex (n = 3) | Show context menu with "Delete vertex" disabled + tooltip "Minimum 3 vertices required" |
| Select "Delete vertex" | Remove vertex, update polygon shape |
| Click "Undo" | Revert last edit operation |
| Click "Cancel" | Show confirmation dialog |
| Confirm cancel | Discard all edits, return to PREVIEW with original polygon |
| Decline cancel | Remain in EDITING |
| Click "Finish Editing" | Exit editing, return to PREVIEW with updated polygon |
| Pan/zoom map | Allowed during editing |

### 7.4 Edit Operations Stack

The system maintains an undo stack for edit operations:

| Operation Type | Data Stored |
|----------------|-------------|
| **Move Vertex** | Original position, new position, vertex index |
| **Add Vertex** | New vertex position, insertion index |
| **Delete Vertex** | Deleted vertex position, original index |

### 7.5 Validation During Editing

| Rule ID | Rule | Feedback |
|---------|------|----------|
| **VR-EDIT-01** | Minimum 3 vertices | Prevent deletion that would result in < 3 vertices |
| **VR-EDIT-02** | Real-time self-intersection warning | Show warning indicator if edit creates self-intersection (do not prevent) |
| **VR-EDIT-03** | Maximum vertices | Prevent adding vertex if at maximum limit |

---

## 8. SCREEN: SAVED STATE

### 8.1 Visual Specification

```
┌─────────────────────────────────────────────────────────────────────┐
│ 🖱️ Draw Mode              ✅ Saved                   [Satellite ▼] │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │                                                                 │ │
│  │            ┌─────────────────────────────────┐                  │ │
│  │            │▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓│                  │ │
│  │            │▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓│                  │ │
│  │            │▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓│                  │ │
│  │            │▓▓▓▓▓▓▓▓▓ 0.82 ha ▓▓▓▓▓▓▓▓▓▓▓▓▓▓│                  │ │
│  │            │▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓│                  │ │
│  │  [+]       │▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓│                  │ │
│  │  [-]       └─────────────────────────────────┘                  │ │
│  │                                                                 │ │
│  │                                                                 │ │
│  │                                                                 │ │
│  │                                                                 │ │
│  │                               Lat: -29.3145, Lon: 28.1234       │ │
│  └────────────────────────────────────────────────────────────────┘ │
│                                                                      │
│  Area: 0.82 ha      Perimeter: 362 m       Vertices: 4              │
│                                                                      │
│  ┌───────────────────────────────┐  ┌───────────────────────────┐   │
│  │  ✏️ Modify Parcel              │  │  🗑️ Remove Parcel          │   │
│  └───────────────────────────────┘  └───────────────────────────┘   │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 8.2 Element Specifications

#### 8.2.1 Header Bar

| Element | Specification |
|---------|---------------|
| **Status Message** | "✅ Saved" |
| **Status Color** | Success color (green) |

#### 8.2.2 Map Area

| Element | Specification |
|---------|---------------|
| **Polygon Display** | Read-only display of saved polygon |
| **Polygon Style** | Green border, green semi-transparent fill |
| **Interaction** | Pan and zoom only, no editing |

#### 8.2.3 Action Bar

| Button | Position | Function |
|--------|----------|----------|
| **Modify Parcel** | Left | Enter EDITING state |
| **Remove Parcel** | Right | Clear polygon (with confirmation) |

### 8.3 Interactions

| User Action | System Response |
|-------------|-----------------|
| Click "Modify Parcel" | Transition to EDITING state |
| Click "Remove Parcel" | Show confirmation dialog |
| Confirm remove | Clear geometry from form fields, transition to INITIAL |
| Decline remove | Remain in SAVED state |
| Pan/zoom map | Allowed |
| Click on polygon | No action (read-only) |

---

## 9. SUPPLEMENTARY SCREENS

### 9.1 Location Search Panel

```
┌────────────────────────────────────────────────────────────────┐
│ 🔍 Search Location                                        [×]  │
├────────────────────────────────────────────────────────────────┤
│                                                                 │
│  [                                              ] [Search]      │
│                                                                 │
│  Recent Searches:                                               │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ 📍 Maseru, Lesotho                                        │  │
│  │ 📍 Berea District                                         │  │
│  │ 📍 Mohale's Hoek                                          │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
│  Search Results:                                                │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ 📍 Teyateyaneng, Berea, Lesotho                          │  │
│  │ 📍 Teyateyaneng Airport                                   │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
└────────────────────────────────────────────────────────────────┘
```

| Element | Specification |
|---------|---------------|
| **Trigger** | Click search icon on map |
| **Position** | Overlay on top-right of map |
| **Search Input** | Text field with placeholder "Enter village, district, or coordinates" |
| **Recent Searches** | Last 5 searches stored in browser local storage |
| **Search Provider** | Nominatim (OpenStreetMap) or configurable |
| **Result Click** | Center map on selected location, zoom to appropriate level |

### 9.2 Tile Layer Selector

```
┌─────────────────────────────────┐
│ Map Style                   [×] │
├─────────────────────────────────┤
│ ◉ Satellite (ESRI)              │
│ ○ OpenStreetMap                 │
│ ○ Hybrid (Satellite + Labels)   │
│ ○ Terrain                       │
└─────────────────────────────────┘
```

| Element | Specification |
|---------|---------------|
| **Trigger** | Click tile selector dropdown |
| **Options** | Radio button list of available tile providers |
| **Selection** | Immediately apply selected tile layer |
| **Persistence** | Remember selection for session |

### 9.3 Confirmation Dialogs

#### 9.3.1 Cancel Drawing Confirmation

```
┌─────────────────────────────────────────────────────────────┐
│ ⚠️ Cancel Drawing?                                          │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  You have placed {n} vertices. Canceling will discard       │
│  all progress and you will need to start over.              │
│                                                              │
│  Are you sure you want to cancel?                           │
│                                                              │
│           ┌──────────────┐    ┌──────────────┐              │
│           │ Keep Drawing │    │ Yes, Cancel  │              │
│           └──────────────┘    └──────────────┘              │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

#### 9.3.2 Clear Parcel Confirmation

```
┌─────────────────────────────────────────────────────────────┐
│ 🗑️ Remove Parcel?                                           │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  This will remove the parcel boundary (0.82 ha).            │
│  This action cannot be undone.                              │
│                                                              │
│  Are you sure you want to remove this parcel?               │
│                                                              │
│           ┌──────────────┐    ┌──────────────┐              │
│           │ Keep Parcel  │    │ Yes, Remove  │              │
│           └──────────────┘    └──────────────┘              │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

#### 9.3.3 Cancel Edit Confirmation

```
┌─────────────────────────────────────────────────────────────┐
│ ⚠️ Discard Changes?                                         │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  You have made changes to the parcel boundary.              │
│  Canceling will revert to the previous boundary.            │
│                                                              │
│  Are you sure you want to discard your changes?             │
│                                                              │
│           ┌──────────────┐    ┌──────────────┐              │
│           │ Keep Editing │    │ Yes, Discard │              │
│           └──────────────┘    └──────────────┘              │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

---

## 10. FORM FIELD INTEGRATION

### 10.1 Bound Hidden Fields

The Draw Mode component writes to the following hidden form fields:

| Field ID | Data Type | Content | Update Trigger |
|----------|-----------|---------|----------------|
| **{geometryField}** | String | GeoJSON Feature object | On Confirm/Save |
| **{areaField}** | Decimal | Area in hectares (4 decimal places) | On Confirm/Save |
| **{perimeterField}** | Decimal | Perimeter in meters (2 decimal places) | On Confirm/Save |
| **{centroidLatField}** | Decimal | Centroid latitude (7 decimal places) | On Confirm/Save |
| **{centroidLonField}** | Decimal | Centroid longitude (7 decimal places) | On Confirm/Save |

### 10.2 GeoJSON Output Format

```json
{
  "type": "Feature",
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
  "properties": {
    "captureMethod": "DRAW",
    "captureDate": "2026-01-04T14:32:15Z",
    "vertexCount": 4
  }
}
```

### 10.3 Loading Existing Data

When the form loads with existing geometry data:

| Condition | Behavior |
|-----------|----------|
| **geometryField is empty** | Start in INITIAL state |
| **geometryField has valid GeoJSON** | Load polygon, start in SAVED state |
| **geometryField has invalid GeoJSON** | Log error, start in INITIAL state, show warning toast |

---

## 11. ERROR HANDLING

### 11.1 Error Types and Responses

| Error Type | Condition | User Feedback | System Action |
|------------|-----------|---------------|---------------|
| **Geolocation Unavailable** | Browser doesn't support geolocation | Toast: "Location services not available" | Disable Locate Me button |
| **Geolocation Denied** | User denied location permission | Toast: "Location access denied. Please enable in browser settings." | Show manual navigation hint |
| **Geolocation Timeout** | Location request timed out | Toast: "Could not get location. Please try again." | Allow retry |
| **Search No Results** | Geocoding returns no results | "No results found for '{query}'" | Keep search panel open |
| **Search Error** | Geocoding service error | "Search service unavailable. Please navigate manually." | Allow manual navigation |
| **Tile Load Error** | Map tiles fail to load | Show placeholder tiles, toast: "Map images temporarily unavailable" | Retry automatically, offer alternative tiles |
| **Backend Validation Error** | API call fails | "Could not validate parcel. Saving without server validation." | Allow save with client-side validation only |
| **Save Error** | Form field update fails | "Failed to save parcel. Please try again." | Keep in PREVIEW state, allow retry |

### 11.2 Toast Notification Specifications

| Type | Duration | Style | Position |
|------|----------|-------|----------|
| **Info** | 3 seconds | Blue/neutral | Top-right of component |
| **Success** | 3 seconds | Green | Top-right of component |
| **Warning** | 5 seconds | Orange/amber | Top-right of component |
| **Error** | Until dismissed | Red | Top-right of component |

---

## 12. ACCESSIBILITY REQUIREMENTS

### 12.1 Keyboard Navigation

| Key | Action |
|-----|--------|
| **Tab** | Move focus between controls |
| **Enter** | Activate focused button |
| **Escape** | Cancel current operation |
| **Arrow Keys** | When map focused: pan map |
| **+/-** | When map focused: zoom in/out |

### 12.2 Screen Reader Support

| Element | ARIA Label |
|---------|------------|
| **Map Container** | "Land parcel map. Use arrow keys to pan, plus and minus to zoom." |
| **Start Drawing Button** | "Start drawing parcel boundary" |
| **Vertex Marker** | "Vertex {n} of parcel boundary" |
| **Area Display** | "Parcel area: {value} hectares" |
| **Validation Status** | "Validation {passed/warning/error}: {message}" |

### 12.3 Color Contrast

| Element | Foreground | Background | Contrast Ratio |
|---------|------------|------------|----------------|
| **Button Text** | White | Primary color | ≥ 4.5:1 |
| **Status Text** | Dark gray | Light background | ≥ 4.5:1 |
| **Error Text** | Dark red | Light red background | ≥ 4.5:1 |
| **Map Labels** | Per tile provider | Per tile provider | N/A |

---

## 13. RESPONSIVE BEHAVIOR

### 13.1 Breakpoints

| Breakpoint | Width | Layout Adjustments |
|------------|-------|-------------------|
| **Desktop** | ≥ 1024px | Full layout as specified |
| **Tablet** | 768px - 1023px | Action buttons stack vertically |
| **Mobile** | < 768px | Simplified controls, larger touch targets |

### 13.2 Mobile-Specific Adjustments

| Element | Desktop | Mobile |
|---------|---------|--------|
| **Vertex Handle Size** | 12px | 20px |
| **Button Height** | 36px | 48px |
| **Instruction Panel** | Always visible | Collapsible |
| **Metrics Bar** | Single row | Two rows |
| **Map Height** | Configurable | Minimum 300px |

---

## 14. PERFORMANCE REQUIREMENTS

| Metric | Requirement |
|--------|-------------|
| **Initial Load** | Map visible within 2 seconds |
| **Tile Load** | First tiles visible within 1 second of pan/zoom |
| **Vertex Placement** | Visual feedback within 100ms |
| **Area Calculation** | Update within 200ms of vertex change |
| **Validation** | Complete within 500ms |
| **Maximum Vertices** | Support up to 100 vertices without performance degradation |

---

## 15. CONFIGURATION OPTIONS SUMMARY

These options are configurable via plugin properties:

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| **defaultCenterLat** | Decimal | -29.6 | Default map center latitude |
| **defaultCenterLon** | Decimal | 28.2 | Default map center longitude |
| **defaultZoom** | Integer | 8 | Default map zoom level |
| **maxZoom** | Integer | 19 | Maximum zoom level |
| **tileProvider** | Enum | SATELLITE_ESRI | Default tile layer |
| **allowTileSwitch** | Boolean | true | Allow user to change tiles |
| **minArea** | Decimal | 0.01 | Minimum area (hectares) - warning |
| **maxArea** | Decimal | 1000 | Maximum area (hectares) - error |
| **minVertices** | Integer | 3 | Minimum vertices - enforced |
| **maxVertices** | Integer | 100 | Maximum vertices - enforced |
| **allowSelfIntersection** | Boolean | false | Allow self-intersecting polygons |
| **mapHeight** | Integer | 400 | Map height in pixels |
| **geometryField** | String | parcel_geometry | ID of hidden field for GeoJSON |
| **areaField** | String | area_hectares | ID of hidden field for area |
| **perimeterField** | String | perimeter_meters | ID of hidden field for perimeter |
| **centroidLatField** | String | centroid_lat | ID of hidden field for centroid lat |
| **centroidLonField** | String | centroid_lon | ID of hidden field for centroid lon |
| **enableBackendValidation** | Boolean | true | Call backend validation API |

---

## 16. GLOSSARY

| Term | Definition |
|------|------------|
| **Vertex** | A corner point of the polygon boundary |
| **Edge** | A line segment connecting two consecutive vertices |
| **Polygon** | A closed shape formed by connecting all vertices |
| **GeoJSON** | A standard format for encoding geographic data structures |
| **Tile Layer** | A layer of pre-rendered map image tiles |
| **Centroid** | The geometric center point of the polygon |
| **Self-intersecting** | A polygon where edges cross each other |
| **Geodesic** | Calculations that account for Earth's curvature |

---

**End of Specification**