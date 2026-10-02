# Configuration Files

## MqttConfig.java

This class configures MQTT messaging for IoT device ingestion and integration. It defines the client factory, input channel, adapter, and message handler.

- **mqttClientFactory**: Creates `MqttPahoClientFactory` with connection options from properties.
- **mqttInputChannel**: Direct channel for incoming messages.
- **mqttAdapter**: Binds to topics and forwards messages to the input channel.
- **messageHandler**: Logs and delegates payloads to `MqttService`.

```java
@Bean
public MqttPahoClientFactory mqttClientFactory() { /* ... */ }

@Bean
public MessageChannel mqttInputChannel() {
    return new DirectChannel();
}

@Bean
public MqttPahoMessageDrivenChannelAdapter mqttAdapter() { /* ... */ }

@Bean
@ServiceActivator(inputChannel = "mqttInputChannel")
public MessageHandler messageHandler() { /* ... */ }
```

---

## SecurityConfig.java

This class sets up application security using Spring Security and JWT.

- **authProvider**: DAO-based provider with BCrypt password encoder.
- **securityFilterChain**: 
  - Disables CSRF.
  - Configures CORS origins and methods.
  - Permits unauthenticated access to auth endpoints, Swagger, and root.
  - Secures all other requests.
  - Adds `JwtFilter` before authentication.
- **authenticationManager**: Exposes `AuthenticationManager` bean.

```java
@Bean
SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(customizer -> customizer.disable())
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .authorizeHttpRequests(request -> request
            .requestMatchers("/api/auth/**", "/hello", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/**").permitAll()
            .anyRequest().authenticated())
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
}
```

---

## WebSocketConfig.java

Configures Spring STOMP messaging over WebSocket with JWT authentication in the inbound channel interceptor.

- **configureMessageBroker**: Configures `/topic` prefix for simple broker and `/app` for application destinations.
- **registerStompEndpoints**: Maps endpoint `/ws` with SockJS fallback.
- **configureClientInboundChannel**: Extracts Bearer token from STOMP headers on `CONNECT` and registers authentication context.

```java
@Override
public void configureMessageBroker(MessageBrokerRegistry config) {
    config.enableSimpleBroker("/topic");
    config.setApplicationDestinationPrefixes("/app");
}
```

---

# Controllers

## AccessController.java

Manages granting, listing, and revoking user access to interfaces.

```java
@RestController
@Tag(name = "Access", description = "API endpoint for Manage Access")
public class AccessController {
```

### Give Access
- **Endpoint**: `POST /api/interface/{interfaceId}/add-access`
- **Request Body**: `AddAccessRequestDTO`
- **Response**: `ApiResponse<?>`

### Show Access
- **Endpoint**: `GET /api/interface/{interfaceId}/access`
- **Response**: `ApiResponse<List<InterfaceAccessDTO>>`

### Revoke Access
- **Endpoint**: `DELETE /api/interface/{interfaceId}/access/{username}`
- **Response**: `ApiResponse<?>`

---

## AuthController.java

Handles user registration, login, OTP verification, password reset, and Google OAuth.

```java
@RequestMapping("api/auth")
@RestController
@Tag(name = "Auth", description = "API endpoints for auth")
public class AuthController {
```

### Register User
- **Endpoint**: `POST /api/auth/register`
- **Request Body**: `RegisterRequest`
- **Response**: `ApiResponse<Map<String, Object>>`

### Verify OTP
- **Endpoint**: `POST /api/auth/register/verify-otp`
- **Request Body**: `Map<String, String>`
- **Response**: `ApiResponse<Map<String, Object>>`

### Resend OTP
- **Endpoint**: `POST /api/auth/resend-otp`
- **Request Body**: `Map<String, String>`
- **Response**: `ApiResponse<Map<String, Object>>`

### Login
- **Endpoint**: `POST /api/auth/login`
- **Request Body**: `LoginRequest`
- **Response**: `ApiResponse<Map<String, Object>>`

### Google Login
- **Endpoint**: `POST /api/auth/google-login`
- **Header**: `Authorization: Bearer <google-id-token>`
- **Response**: `ApiResponse<?>`

### Forget Password
- **Endpoint**: `POST /api/auth/forget-password`
- **Request Body**: `Map<String, String>`
- **Response**: `ApiResponse<Map<String, Object>>`

### Reset Password
- **Endpoint**: `POST /api/auth/reset-password`
- **Request Body**: `ResetPasswordRequest`
- **Response**: `ApiResponse<ResetPasswordRequest>`

---

## CollectionController.java

Handles execution details of collection routes, worker actions, RFID scanning, stop validation, and audits.

```java
@RestController
@RequestMapping("/api/routes")
@Tag(name = "Collection", description = "API endpoints for waste collection routes operations and logs")
public class CollectionController {
```

### Assign Worker
- **Endpoint**: `PUT /api/routes/{routeId}/assign`
- **Request Body**: `AssignWorkerRequest`
- **Response**: `ApiResponse<RouteResponseDto>`

### Start Collection
- **Endpoint**: `PUT /api/routes/{routeId}/start`
- **Request Body**: `StartCollectionRequest`
- **Response**: `ApiResponse<RouteResponseDto>`

### Collect Stop (RFID scanned)
- **Endpoint**: `PUT /api/routes/{routeId}/stops/{stopId}/collect`
- **Request Body**: `CollectStopRequest`
- **Response**: `ApiResponse<RouteStopDto>`

### Skip Stop
- **Endpoint**: `PUT /api/routes/{routeId}/stops/{stopId}/skip`
- **Request Body**: `SkipStopRequest`
- **Response**: `ApiResponse<RouteStopDto>`

### Complete Route
- **Endpoint**: `PUT /api/routes/{routeId}/complete`
- **Request Body**: `CompleteRouteRequest`
- **Response**: `ApiResponse<RouteResponseDto>`

### Get Audit Logs
- **Endpoint**: `GET /api/routes/{routeId}/audit-logs`
- **Response**: `ApiResponse<List<CollectionLog>>`

---

## DeviceController.java

CRUD and streaming for devices under a given interface.

```java
@RequestMapping("api/device")
@RestController
@Tag(name = "Device", description = "API endpoints for Device")
public class DeviceController {
```

### Create Device
- **Endpoint**: `POST /api/device/{interfaceId}`
- **Request Body**: `DeviceRequestDTO`
- **Response**: `ApiResponse<DeviceDTO>`

### Link Hardware Device
- **Endpoint**: `POST /api/device/{deviceId}/link`
- **Request Body**: `LinkRequestDto`
- **Response**: `ApiResponse<String>`

### Get All Devices
- **Endpoint**: `GET /api/device`
- **Response**: `ApiResponse<List<DeviceDTO>>`

### Get Device by ID
- **Endpoint**: `GET /api/device/{deviceId}`
- **Response**: `ApiResponse<DeviceDTO>`

### Get Device Readings
- **Endpoint**: `GET /api/device/{deviceId}/readings`
- **Response**: `ApiResponse<List<SensorReadingDTO>>`

### Stream Device Data (SSE)
- **Endpoint**: `GET /api/device/{deviceId}/stream`
- **Response**: `SseEmitter` stream

### Delete Device
- **Endpoint**: `DELETE /api/device/{deviceId}`
- **Response**: `ApiResponse<String>`

---

## InterfaceController.java

CRUD operations on interfaces with password check on delete.

```java
@RequestMapping("/api/interface")
@RestController
@Tag(name = "Interface", description = "API endpoints for Interface")
public class InterfaceController {
```

### Create Interface
- **Endpoint**: `POST /api/interface`
- **Request Body**: `InterfaceCreaterequestDto`
- **Response**: `ApiResponse<InterfaceDTO>`

### Get Interfaces
- **Endpoint**: `GET /api/interface`
- **Response**: `ApiResponse<List<InterfaceDTO>>`

### Get Interface By ID
- **Endpoint**: `GET /api/interface/{interface_id}`
- **Response**: `ApiResponse<?>`

### Delete Interface
- **Endpoint**: `DELETE /api/interface/{interface_id}`
- **Request Body**: `DeleteInterfaceRequest`
- **Response**: `ApiResponse<Void>`

---

## RouteController.java

Handles route creation, generation, and multi-route optimization with CVRP logic.

```java
@RestController
@RequestMapping("/api/routes")
@Tag(name = "Route", description = "API endpoints for route generation, optimization, and management")
public class RouteController {
```

### Optimize Route
- **Endpoint**: `POST /api/routes/optimize`
- **Request Body**: `RouteRequestDto`
- **Response**: `ApiResponse<List<RouteResponseDto>>`

### Get Route By ID
- **Endpoint**: `GET /api/routes/{routeId}`
- **Response**: `ApiResponse<RouteResponseDto>`

### Get Routes By Interface ID
- **Endpoint**: `GET /api/routes/interface/{interfaceId}`
- **Response**: `ApiResponse<List<RouteResponseDto>>`

### Get Completed Routes By Interface ID
- **Endpoint**: `GET /api/routes/interface/{interfaceId}/completed`
- **Response**: `ApiResponse<List<RouteResponseDto>>`

### Get My Assigned Routes
- **Endpoint**: `GET /api/routes/assigned`
- **Response**: `ApiResponse<List<RouteResponseDto>>`

### Delete Route
- **Endpoint**: `DELETE /api/routes/{routeId}`
- **Response**: `ApiResponse<Void>`

---

## TrackingController.java

Ingests and retrieves GPS trail logs for active vehicles and routes.

```java
@RestController
@RequestMapping("/api/tracking")
@Tag(name = "Tracking", description = "API endpoints for vehicle/device tracking and logs")
public class TrackingController {
```

### Update Location
- **Endpoint**: `POST /api/tracking/location`
- **Request Body**: `LocationUpdateRequest`
- **Response**: `ApiResponse<String>` (triggers WebSocket broadcast to `/topic/tracking/{routeId}`)

### Get Vehicle Logs
- **Endpoint**: `GET /api/tracking/route/{routeId}/logs`
- **Response**: `ApiResponse<List<VehicleLog>>`

---

## UserController.java

User profile and user lookup operations.

```java
@RestController
@Tag(name = "User", description = "API endpoints for user profile management and search")
public class UserController {
```

### Get Current User Profile
- **Endpoint**: `GET /api/users/me`
- **Response**: `UserDTO`

### Search Users
- **Endpoint**: `GET /api/users/search`
- **Query Parameters**: `q` or `query`
- **Response**: `ApiResponse<List<UserDTO>>`

### Hello Check
- **Endpoint**: `GET /hello`
- **Response**: `String`

---

## VehicleController.java

Manages garbage collection vehicles/trucks and driver assignments.

```java
@RestController
@RequestMapping("/api/vehicles")
@Tag(name = "Vehicle", description = "API endpoints for vehicle management and driver assignment")
public class VehicleController {
```

### Create Vehicle
- **Endpoint**: `POST /api/vehicles`
- **Request Body**: `VehicleRequestDto`
- **Response**: `ApiResponse<VehicleResponseDto>`

### Get Vehicles By Interface
- **Endpoint**: `GET /api/vehicles/interface/{interfaceId}`
- **Response**: `ApiResponse<List<VehicleResponseDto>>`

### Assign Driver
- **Endpoint**: `PUT /api/vehicles/{vehicleId}/driver/{driverId}`
- **Response**: `ApiResponse<VehicleResponseDto>`

### Toggle Active Status
- **Endpoint**: `PUT /api/vehicles/{vehicleId}/status`
- **Query Parameter**: `isActive`
- **Response**: `ApiResponse<VehicleResponseDto>`

### Delete Vehicle
- **Endpoint**: `DELETE /api/vehicles/{vehicleId}`
- **Response**: `ApiResponse<Void>`

---

# Exception Handling

## GlobalExceptionHandler.java

Translates common runtime exceptions into structured `ApiResponse` JSON bodies with appropriate HTTP statuses.

- `EntityNotFoundException` -> `404 NOT FOUND`
- `AccessDeniedException` -> `403 FORBIDDEN`
- `VerificationTokenExpiredException` -> `400 BAD REQUEST`
- `UserAlreadyExistsException` -> `409 CONFLICT`

---

# Persistence Models

- **User**: User records with credentials, role mapping, and verification flags.
- **Interface**: Collection of IoT devices.
- **Device**: IoT hardware device reporting metrics.
- **SensorReading**: Logs recorded from devices.
- **UserInterface**: Joins User and Interface with a specific role status.
- **Route**: Representation of a planned or completed waste collection trip.
- **RouteStop**: Individual stop points (Device bins) assigned on a route.
- **Vehicle**: Truck assigned to collection routes.
- **CollectionLog**: Immutable audit logs of worker actions on a route (Route Started, Collected, Skipped, etc.).
- **VehicleLog**: Periodic GPS trail of vehicles during active routing.

---

# Spring Data Repositories

- **UserRepo**: CRUD operations on User.
- **InterfaceRepo**: CRUD operations on Interface.
- **DeviceRepo**: Operations on Device.
- **SensorReadingRepo**: Operations on SensorReading.
- **UserInterfaceRepo**: Coordinates interface access controls.
- **RouteRepository**: Core CRUD and status queries for routes.
- **RouteStopRepository**: Sequence queries for stops on a route.
- **VehicleRepository**: Vehicle lookups by interface and driver.
- **CollectionLogRepository**: Read audit trail records.
- **VehicleLogRepository**: Retrieves GPS points per vehicle.

---

# Services

- **AuthService**: Handles registration, verification, password resetting, and login.
- **UserService**: Performs profile and user lookup operations.
- **OtpService**: Manages user email OTP codes.
- **EmailService**: Sends system mail notifications.
- **GoogleVerifierService**: Decodes Google ID login payloads.
- **MyUserDetailsService**: Custom authentication provider context adapter.
- **AccessService**: Implements access controls on interfaces.
- **InterfaceService**: Performs interface creations and cascades.
- **DeviceService**: Operations on devices, including hardware linking.
- **MqttService**: Resolves telemetry updates from devices.
- **CollectionService**: Manages route assignment and lifecycle modifications (start, collect, skip, complete).
- **RouteService**: Main orchestrator of routing calculations.
- **VehicleService**: Handles vehicle registry and driver linking.
- **TrackingService**: Logs locations and pushes real-time WebSocket telemetry updates.
- **RoutingService**: Connects to the GraphHopper library or falls back to Haversine calculations.
- **CvrpSolverService**: Runs Google OR-Tools CVRP solver on available vehicles and bin fill levels.

---

# Utilities

- **DeviceStreamManager**: Handles active SSE connections.
- **JwtUtil**: Generates and parses JWT payloads.
- **ScheduledTaskManager**: Regularly handles offline device marking and temporary unverified user sweeps.

---

```mermaid
flowchart TD
  subgraph API Layer
    AuthController --> AuthService
    UserController --> UserService
    InterfaceController --> InterfaceService
    DeviceController --> DeviceService
    AccessController --> AccessService
    RouteController --> RouteService
    CollectionController --> CollectionService
    TrackingController --> TrackingService
    VehicleController --> VehicleService
  end
  
  subgraph Service Layer
    AuthService --> OtpService --> EmailService
    AuthService --> UserRepo
    UserService --> UserRepo
    InterfaceService --> InterfaceRepo & UserInterfaceRepo
    DeviceService --> DeviceRepo & SensorReadingRepo
    AccessService --> UserInterfaceRepo & UserRepo
    CollectionService --> RouteRepository & RouteStopRepository & CollectionLogRepository
    RouteService --> VehicleRepository & RoutingService & CvrpSolverService & GoogleMapsService
    VehicleService --> VehicleRepository
    TrackingService --> VehicleLogRepository
    MqttService --> DeviceRepo & SensorReadingRepo & DeviceStreamManager
  end
  
  subgraph Persistence Layer
    UserRepo
    InterfaceRepo
    DeviceRepo
    SensorReadingRepo
    UserInterfaceRepo
    RouteRepository
    RouteStopRepository
    VehicleRepository
    CollectionLogRepository
    VehicleLogRepository
  end
  
  MqttConfig --> MqttService
  WebSocketConfig --> TrackingService
  SecurityConfig --> JwtFilter --> MyUserDetailsService
  ScheduledTaskManager --> DeviceRepo & UserService
  DeviceStreamManager --> SSE Clients
```