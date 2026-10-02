# EcoMonitor — Backend Specification

> **Target**: Spring Boot backend (Java 21, PostgreSQL, Spring Security + JWT)
> **Purpose**: System specifications for EcoMonitor backend features (route collection, tracking, RFID, etc.), all of which are fully implemented.

---

## Existing Context

The backend has the following fully implemented and operational modules:

- **Auth**: Register, Login, OTP, Google OAuth, Password Reset (JWT-based)
- **Users**: Profile, Search
- **Interfaces**: CRUD (a grouping of devices, e.g. "Ward 12")
- **Devices**: CRUD, SSE streaming, sensor readings (types: `SINGLE_BIN`, `DUAL_BIN`)
- **Access Control**: Grant/revoke user roles (`OWNER`, `ADMIN`, `USER`) per interface
- **MQTT**: Ingests IoT sensor data from bins
- **Route Optimization & Management**: Generates CVRP optimized collection routes via local GraphHopper engine (backed by OR-Tools) or Haversine routing.
- **Collection execution & RFID verification**: Assign workers, start routes, collect/skip stops, verify bins using RFID, and complete routes.
- **Tracking & WebSockets**: Ingest and stream vehicle location updates in real-time.

### Existing Route Endpoints (already implemented)

```
POST   /api/routes/optimize                    → RouteResponseDto
GET    /api/routes/{routeId}                   → RouteResponseDto
GET    /api/routes/interface/{interfaceId}     → List<RouteResponseDto>
```

### Existing DTOs (already implemented)

```java
// Request
RouteRequestDto { UUID interfaceId, String vehicleId, String startLocation?, String endLocation? }

// Response
RouteResponseDto { UUID routeId, String vehicleId, double totalDistance, double totalDuration, String polyline, List<RouteStopDto> stops }
RouteStopDto { int sequence, UUID deviceId, String name, String location, double fillLevel, String type }
```

---

## Database Changes

### MODIFY: `routes` table — Add columns

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `assigned_worker_id` | UUID FK→users.id | YES | null | Worker assigned to execute |
| `started_at` | TIMESTAMP | YES | null | When worker started collection |
| `completed_at` | TIMESTAMP | YES | null | When collection finished |
| `total_collected` | INT | YES | 0 | Bins actually collected |
| `total_skipped` | INT | YES | 0 | Bins skipped |

`status` enum values: `PLANNED`, `ASSIGNED`, `ACTIVE`, `COMPLETED`, `CANCELLED`

### MODIFY: `route_stops` table — Add columns

| Column | Type | Nullable | Description |
|--------|------|----------|-------------|
| `collected_at` | TIMESTAMP | YES | When marked collected |
| `rfid_tag` | VARCHAR(255) | YES | Scanned RFID tag value |
| `rfid_verified` | BOOLEAN | YES | Whether RFID matched device.hardware_id |
| `skip_reason` | VARCHAR(500) | YES | Reason if skipped |
| `worker_lat` | DECIMAL(10,7) | YES | Worker GPS latitude at collection |
| `worker_lng` | DECIMAL(10,7) | YES | Worker GPS longitude at collection |

`status` enum values: `PENDING`, `COLLECTED`, `SKIPPED`

### NEW: `collection_logs` table

> Government audit trail — every action is logged immutably.

| Column | Type | Nullable | Description |
|--------|------|----------|-------------|
| `id` | BIGSERIAL PK | NO | |
| `route_id` | UUID FK→routes.id | NO | |
| `stop_id` | BIGINT FK→route_stops.id | YES | null for route-level events |
| `worker_id` | UUID FK→users.id | NO | |
| `action` | VARCHAR(50) | NO | Enum: `ROUTE_STARTED`, `ARRIVED`, `RFID_SCANNED`, `COLLECTED`, `SKIPPED`, `ROUTE_COMPLETED` |
| `rfid_tag` | VARCHAR(255) | YES | Scanned value (if applicable) |
| `latitude` | DECIMAL(10,7) | YES | |
| `longitude` | DECIMAL(10,7) | YES | |
| `timestamp` | TIMESTAMP | NO | DEFAULT NOW() |
| `notes` | TEXT | YES | Optional worker notes |

### EXISTING: `vehicle_logs` table (no changes needed)

Already has: `id`, `device_id` (FK→devices/trucks), `latitude`, `longitude`, `speed`, `timestamp`

---

## New API Endpoints

### 1. Assign Worker to Route

```
PUT /api/routes/{routeId}/assign
```

**Auth**: Bearer token (must be ADMIN or OWNER of the interface)

**Request Body**:
```json
{
  "workerId": "uuid-of-worker"
}
```

**Response** (200):
```json
{
  "success": true,
  "message": "Worker assigned successfully",
  "data": {
    "routeId": "...",
    "status": "ASSIGNED",
    "assignedWorkerId": "...",
    "assignedWorkerName": "Raju K."
  }
}
```

**Errors**: 403 (not admin), 404 (route/worker not found), 409 (route not in PLANNED status)

**Logic**:
1. Verify caller has ADMIN/OWNER role on route's interface
2. Verify route status is `PLANNED`
3. Set `assigned_worker_id`, update status to `ASSIGNED`

---

### 2. Start Collection (Worker)

```
PUT /api/routes/{routeId}/start
```

**Auth**: Bearer token (must be the assigned worker)

**Request Body**:
```json
{
  "latitude": 12.9716,
  "longitude": 77.5946
}
```

**Response** (200):
```json
{
  "success": true,
  "message": "Collection started",
  "data": {
    "routeId": "...",
    "status": "ACTIVE",
    "startedAt": "2026-02-19T14:30:00Z",
    "stops": [ /* full RouteStopDto list */ ]
  }
}
```

**Logic**:
1. Verify caller is the assigned worker
2. Verify route status is `ASSIGNED`
3. Set `started_at = NOW()`, status = `ACTIVE`
4. Write `COLLECTION_LOG` with action `ROUTE_STARTED`

---

### 3. Collect Stop

```
PUT /api/routes/{routeId}/stops/{stopId}/collect
```

**Auth**: Bearer token (assigned worker)

**Request Body**:
```json
{
  "rfidTag": "RFID-BIN-00142",
  "latitude": 12.9720,
  "longitude": 77.5950,
  "notes": ""
}
```

**Response** (200):
```json
{
  "success": true,
  "message": "Stop collected and verified",
  "data": {
    "stopId": 5,
    "status": "COLLECTED",
    "rfidVerified": true,
    "collectedAt": "2026-02-19T14:45:00Z"
  }
}
```

**Logic**:
1. Verify caller is assigned worker, route is `ACTIVE`
2. Look up the stop's `device_id` → get `device.hardware_id`
3. Compare `rfidTag` with `device.hardware_id` → set `rfid_verified`
4. Update stop: `status=COLLECTED`, `collected_at=NOW()`, `rfid_tag`, `rfid_verified`, `worker_lat`, `worker_lng`
5. Write `COLLECTION_LOG` with action `COLLECTED` (or `RFID_SCANNED` + `COLLECTED`)
6. If `rfid_verified == false`, still mark collected but flag it (response includes `rfidVerified: false`)

---

### 4. Skip Stop

```
PUT /api/routes/{routeId}/stops/{stopId}/skip
```

**Auth**: Bearer token (assigned worker)

**Request Body**:
```json
{
  "reason": "Access blocked by parked vehicle",
  "latitude": 12.9718,
  "longitude": 77.5948
}
```

**Response** (200):
```json
{
  "success": true,
  "message": "Stop skipped",
  "data": {
    "stopId": 5,
    "status": "SKIPPED",
    "skipReason": "Access blocked by parked vehicle"
  }
}
```

**Logic**:
1. Update stop: `status=SKIPPED`, `skip_reason`, `worker_lat`, `worker_lng`
2. Write `COLLECTION_LOG` with action `SKIPPED`

---

### 5. Complete Route

```
PUT /api/routes/{routeId}/complete
```

**Auth**: Bearer token (assigned worker)

**Request Body**:
```json
{
  "latitude": 12.9716,
  "longitude": 77.5946
}
```

**Response** (200):
```json
{
  "success": true,
  "message": "Route completed",
  "data": {
    "routeId": "...",
    "status": "COMPLETED",
    "completedAt": "2026-02-19T15:30:00Z",
    "totalCollected": 6,
    "totalSkipped": 2
  }
}
```

**Logic**:
1. Verify all stops are either `COLLECTED` or `SKIPPED` (or allow partial completion)
2. Count collected/skipped, set `completed_at = NOW()`, status = `COMPLETED`
3. Write `COLLECTION_LOG` with action `ROUTE_COMPLETED`

---

### 6. Send GPS Location (Worker)

```
POST /api/tracking/location
```

**Auth**: Bearer token

**Request Body**:
```json
{
  "routeId": "uuid",
  "latitude": 12.9720,
  "longitude": 77.5950,
  "speed": 25.5
}
```

**Response** (200):
```json
{ "success": true, "message": "Location recorded" }
```

**Logic**:
1. Save to `vehicle_logs` table (device_id = route's vehicle_id)
2. **Broadcast** via WebSocket to topic `/topic/tracking/{routeId}`

---

### 7. Get GPS Trail

```
GET /api/tracking/route/{routeId}/logs
```

**Auth**: Bearer token (ADMIN/OWNER of interface)

**Response** (200):
```json
{
  "success": true,
  "message": "Vehicle logs fetched",
  "data": [
    { "latitude": 12.9716, "longitude": 77.5946, "speed": 0, "timestamp": "..." },
    { "latitude": 12.9720, "longitude": 77.5950, "speed": 25.5, "timestamp": "..." }
  ]
}
```

---

### 8. Get Collection Logs (Audit)

```
GET /api/routes/{routeId}/audit-logs
```

**Auth**: Bearer token (ADMIN/OWNER)

**Response** (200):
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "action": "ROUTE_STARTED",
      "workerName": "Raju K.",
      "latitude": 12.9716,
      "longitude": 77.5946,
      "timestamp": "2026-02-19T14:30:00Z",
      "notes": null
    }
  ]
}
```

---

## WebSocket Configuration

### Setup (Spring STOMP + SockJS)

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
```

### Topics

| Topic | Payload | Publisher | Subscriber |
|-------|---------|-----------|------------|
| `/topic/tracking/{routeId}` | `{ lat, lng, speed, timestamp }` | TrackingController (on GPS POST) | Web Dashboard |
| `/topic/route/{routeId}/updates` | `{ stopId, status, action }` | CollectionController (on collect/skip) | Web Dashboard |

### JWT Auth for WebSocket

Add a `ChannelInterceptor` that extracts and validates the JWT from the STOMP `CONNECT` frame's `Authorization` header.

---

## Modified DTOs

### RouteResponseDto — Extended

Add these fields to the existing response:

```java
String status;              // PLANNED, ASSIGNED, ACTIVE, COMPLETED
String assignedWorkerId;    // nullable
String assignedWorkerName;  // nullable
String startedAt;           // nullable ISO timestamp
String completedAt;         // nullable ISO timestamp
int totalCollected;
int totalSkipped;
```

### RouteStopDto — Extended

Add these fields:

```java
String status;          // PENDING, COLLECTED, SKIPPED
String collectedAt;     // nullable
String rfidTag;         // nullable
boolean rfidVerified;
String skipReason;      // nullable
```

---

## Implementation Order

1. **DB migrations** (add columns to routes/route_stops, create collection_logs)
2. **Entity updates** (Route, RouteStop, new CollectionLog)
3. **Repository** (CollectionLogRepo, extend RouteRepo/RouteStopRepo)
4. **CollectionService** (assign, start, collect, skip, complete)
5. **CollectionController** (REST endpoints)
6. **TrackingService + TrackingController** (GPS ingestion)
7. **WebSocket config** + broadcast logic
8. **Audit endpoint** (GET collection logs)
9. **Extended DTOs** (update RouteResponseDto, RouteStopDto)
10. **Testing** (integration tests for full route lifecycle)

---

## Compatibility Notes

> [!IMPORTANT]
> The **mobile app** and **web frontend** both depend on these exact endpoint contracts. Do NOT change the JSON field names or response structure without updating both spec documents.

- All responses use the existing `ApiResponse<T>` wrapper: `{ success, message, data }`
- All endpoints require `Authorization: Bearer <jwt>` header (except WebSocket which uses STOMP header)
- GPS coordinates are always `decimal(10,7)` — latitude/longitude as separate fields
- Device locations remain stored as `"lat,lng"` strings in the devices table
- RFID verification: compare `scannedTag == device.hardware_id` (case-insensitive)
