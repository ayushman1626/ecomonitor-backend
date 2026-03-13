# EcoMonitor — Mobile App Specification (Worker App)

> **Target**: Android mobile app (React Native or Flutter)
> **User**: Collection Worker
> **Purpose**: Standalone spec for an agent to build the worker-facing mobile application.

---

## App Overview

A **waste collection worker app** that lets workers view assigned routes, navigate turn-by-turn to each bin, scan RFID tags via Bluetooth, and mark bins as collected or skipped — all while broadcasting live GPS to the admin dashboard.

---

## Authentication

Uses the **same auth system** as the web dashboard.

### Login

```
POST {BASE_URL}/api/auth/login
```

**Request**:
```json
{ "email": "worker@example.com", "password": "pa$$w0rd" }
```

**Response**:
```json
{
  "success": true,
  "message": "Login successful",
  "data": { "token": "<jwt>" }
}
```

- Store JWT in secure storage (e.g. `SecureStore` / `EncryptedSharedPreferences`)
- Attach to all requests: `Authorization: Bearer <jwt>`

---

## API Endpoints to Consume

> Base URL: `http://<server>:8080/api`
> All responses follow: `{ success: boolean, message: string, data: T }`

### 1. Get Assigned Routes

```
GET /api/routes/interface/{interfaceId}
```

Filter client-side: show only routes where `assignedWorkerId == currentUser.id` and `status` is `ASSIGNED` or `ACTIVE`.

**Response** — each route:
```json
{
  "routeId": "uuid",
  "vehicleId": "TN-01-AB-1234",
  "status": "ASSIGNED",
  "totalDistance": 12.5,
  "totalDuration": 45.2,
  "polyline": "encoded_polyline_string",
  "assignedWorkerId": "uuid",
  "assignedWorkerName": "Raju K.",
  "stops": [
    {
      "id": 5,
      "sequence": 1,
      "deviceId": "uuid",
      "name": "Bin-101",
      "location": "12.9716,77.5946",
      "fillLevel": 85.0,
      "type": "SINGLE_BIN",
      "status": "PENDING"
    }
  ]
}
```

### 2. Start Collection

```
PUT /api/routes/{routeId}/start
```

**Request**:
```json
{ "latitude": 12.9716, "longitude": 77.5946 }
```

**Response**: Route data with `status: "ACTIVE"` and `startedAt` timestamp.

### 3. Collect Stop (after RFID scan)

```
PUT /api/routes/{routeId}/stops/{stopId}/collect
```

**Request**:
```json
{
  "rfidTag": "RFID-BIN-00142",
  "latitude": 12.9720,
  "longitude": 77.5950,
  "notes": ""
}
```

**Response**:
```json
{
  "success": true,
  "data": {
    "stopId": 5,
    "status": "COLLECTED",
    "rfidVerified": true,
    "collectedAt": "2026-02-19T14:45:00Z"
  }
}
```

> If `rfidVerified: false`, show a ⚠️ warning but still proceed.

### 4. Skip Stop

```
PUT /api/routes/{routeId}/stops/{stopId}/skip
```

**Request**:
```json
{
  "reason": "Access blocked by parked vehicle",
  "latitude": 12.9718,
  "longitude": 77.5948
}
```

### 5. Complete Route

```
PUT /api/routes/{routeId}/complete
```

**Request**:
```json
{ "latitude": 12.9716, "longitude": 77.5946 }
```

**Response**: Final stats — `totalCollected`, `totalSkipped`, `completedAt`.

### 6. Send GPS Location (background service)

```
POST /api/tracking/location
```

**Request** (sent every 5 seconds while route is ACTIVE):
```json
{
  "routeId": "uuid",
  "latitude": 12.9720,
  "longitude": 77.5950,
  "speed": 25.5
}
```

---

## Screens

### Screen 1: Login

- Email + Password fields
- Login button → store JWT → navigate to Home

### Screen 2: Home (Route List)

- Shows assigned routes as cards
- Each card shows: Vehicle ID, distance, duration, stop count, status badge
- **ASSIGNED** routes → "Start Collection" button
- **ACTIVE** routes → "Continue" button (resume if app was closed)
- **COMPLETED** routes → view-only summary

### Screen 3: Active Collection (Main Screen)

This is the **primary screen** while the worker is on a route.

```
┌─────────────────────────────────┐
│         🗺️ MAP VIEW             │
│  (Google Maps with route line)  │
│  📍 Current position            │
│  🔴 Next stop marker            │
│  ⬜ Remaining stops             │
│  ✅ Completed stops              │
│                                 │
├─────────────────────────────────┤
│ ⬆ Swipe-up Bottom Sheet        │
│                                 │
│  Next: Bin-101 (85% full)       │
│  Distance: 1.2 km | ETA: 3 min │
│                                 │
│  [ 🔵 Navigate ]               │
│                                 │
│  Stop List (collapsible):       │
│  ✅ ① Bin-204 — Collected      │
│  🔵 ② Bin-101 — In Progress   │
│  ⬜ ③ Bin-307 — Pending        │
│  ⬜ ④ Bin-115 — Pending        │
└─────────────────────────────────┘
```

**Map features**:
- Decoded polyline drawn on map
- Numbered markers for each stop (colored by status)
- Worker's current position (blue dot, updated from GPS)
- Camera auto-follows worker position

### Screen 4: Stop Arrival (triggered by geofence ~50m)

When worker is within **50 meters** of the next stop:

```
┌─────────────────────────────────┐
│                                 │
│   📍 Arrived at Bin-101        │
│   Type: Single Bin              │
│   Fill Level: 85%               │
│                                 │
│   ┌───────────────────────┐     │
│   │  [ 📡 Scan RFID ]    │     │
│   └───────────────────────┘     │
│                                 │
│   ┌───────────────────────┐     │
│   │  [ ⏭ Skip Stop ]     │     │
│   └───────────────────────┘     │
│                                 │
└─────────────────────────────────┘
```

- **Scan RFID** → initiates Bluetooth scan → on success sends collect request
- **Skip Stop** → shows reason picker → sends skip request

### Screen 5: RFID Scan

```
┌─────────────────────────────────┐
│                                 │
│   📡 Scanning for RFID...      │
│   ┌─────────────────────┐       │
│   │  ████████░░░░░░░░░  │       │
│   └─────────────────────┘       │
│                                 │
│   Status: Searching for device  │
│                                 │
│   [ Cancel ]                    │
│                                 │
│ ─── On Success ───              │
│                                 │
│   ✅ RFID Verified!             │
│   Tag: RFID-BIN-00142          │
│   Bin: Bin-101 ✓ Match          │
│                                 │
│   [ ✅ Confirm Collection ]    │
│                                 │
│ ─── On Mismatch ───             │
│                                 │
│   ⚠️ RFID Mismatch!            │
│   Expected: RFID-BIN-00142     │
│   Scanned:  RFID-BIN-00999     │
│                                 │
│   [ Collect Anyway ]            │
│   [ Retry Scan ]                │
│   [ Skip Stop ]                 │
│                                 │
└─────────────────────────────────┘
```

### Screen 6: Skip Reason

```
┌─────────────────────────────────┐
│   Why skip this stop?           │
│                                 │
│   ○ Access blocked              │
│   ○ Bin not found               │
│   ○ Bin damaged                 │
│   ○ Area unsafe                 │
│   ○ Other: [______________]     │
│                                 │
│   [ Confirm Skip ]              │
└─────────────────────────────────┘
```

### Screen 7: Route Complete

```
┌─────────────────────────────────┐
│                                 │
│   🎉 Route Completed!          │
│                                 │
│   ✅ Collected: 6 bins          │
│   ⏭ Skipped: 2 bins            │
│   📏 Distance: 12.5 km         │
│   ⏱ Duration: 42 min           │
│                                 │
│   [ Back to Home ]              │
│                                 │
└─────────────────────────────────┘
```

---

## Bluetooth RFID Integration

### Flow

```
1. Worker taps "Scan RFID"
2. App scans for nearby BLE devices matching a known service UUID
3. App connects to the RFID reader module attached to the bin
4. App reads the RFID tag characteristic
5. App receives tag string (e.g. "RFID-BIN-00142")
6. App sends tag to backend via collect endpoint
7. Backend verifies tag == device.hardware_id
8. App shows verified/mismatch result
```

### Technical Details

| Item | Value |
|------|-------|
| Protocol | Bluetooth Low Energy (BLE) |
| Connection | Worker's phone ↔ RFID reader on bin |
| Service UUID | Define a custom UUID (e.g. `0000FFE0-0000-1000-8000-00805F9B34FB`) |
| Characteristic | Read characteristic containing RFID tag string |
| Libraries | **React Native**: `react-native-ble-plx` / **Flutter**: `flutter_blue_plus` |
| Timeout | 10 seconds scan timeout, then show "Device not found" |
| Fallback | "Enter RFID manually" text input if BLE fails |

---

## GPS Tracking (Background Service)

### Requirements

- Start when worker taps **"Start Collection"**
- Send GPS every **5 seconds** to `POST /api/tracking/location`
- Must run in **background** (even when screen is off or app is minimized)
- Stop when route is **completed** or worker manually stops

### Implementation

| Platform | Approach |
|----------|----------|
| **React Native** | `react-native-background-geolocation` by Transistor Software |
| **Flutter** | `geolocator` + `workmanager` or `flutter_background_service` |

### Payload

```json
{
  "routeId": "current-route-uuid",
  "latitude": 12.9720,
  "longitude": 77.5950,
  "speed": 25.5
}
```

### Offline Handling

If no network, queue GPS points locally and batch-send when connectivity resumes.

---

## Navigation

### Turn-by-Turn Navigation

Use **Google Maps SDK** (or Mapbox) for in-app navigation:

**Option A — In-app (recommended for seamless UX)**:
- Decode the `polyline` string from route response
- Draw on map, show route line + distance/ETA to next stop
- Use `Google Directions API` between current position and next stop for live navigation

**Option B — External Google Maps intent**:
- When worker taps "Navigate", open Google Maps with destination = next stop's `location`
- Less seamless but simpler to implement

### Geofencing

- Set a **50-meter geofence** around each stop's coordinates
- When worker enters geofence → auto-switch to "Arrival" screen
- Library: `react-native-background-geolocation` (has built-in geofencing)

---

## WebSocket Subscription (Optional for Mobile)

The mobile app can **optionally** subscribe to route update events:

```
WebSocket: ws://<server>:8080/ws
Topic: /topic/route/{routeId}/updates
```

This would receive real-time updates if the admin cancels or modifies the route. Not critical for v1.

---

## Offline & Error Handling

| Scenario | Behavior |
|----------|----------|
| No internet during GPS tracking | Queue locally, batch-send on reconnect |
| No internet during collect/skip | Queue actions, show "pending sync" badge, retry on reconnect |
| BLE device not found | Show timeout message + "Enter RFID manually" fallback |
| RFID mismatch | Allow "Collect Anyway" with warning logged |
| App killed mid-route | On reopen, check for ACTIVE route and resume |

---

## Tech Stack Recommendations

| Concern | Recommendation |
|---------|---------------|
| Framework | **React Native** (Expo bare workflow) or **Flutter** |
| Maps | Google Maps SDK (`react-native-maps` / `google_maps_flutter`) |
| BLE | `react-native-ble-plx` / `flutter_blue_plus` |
| Background GPS | `react-native-background-geolocation` / `geolocator` |
| Navigation | `@react-navigation/native` / `go_router` |
| HTTP | `axios` / `dio` |
| Storage | `AsyncStorage` or `SQLite` for offline queue |
| Auth token | `expo-secure-store` / `flutter_secure_storage` |

---

## Compatibility Contract

> [!IMPORTANT]
> These API contracts are shared with the backend spec. Any changes must be synchronized.

- All responses: `{ success: boolean, message: string, data: T }`
- Auth header: `Authorization: Bearer <jwt>`
- GPS: `latitude` and `longitude` as separate decimal fields
- Device locations in stop data: `"lat,lng"` string — parse on client side
- RFID tag: plain string comparison with `device.hardware_id`
- Route statuses: `PLANNED` → `ASSIGNED` → `ACTIVE` → `COMPLETED`
- Stop statuses: `PENDING` → `COLLECTED` | `SKIPPED`
