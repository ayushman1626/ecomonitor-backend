it add# Configuration Files

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
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
      .csrf().disable()
      .cors().configurationSource(corsConfigurationSource())
      .authorizeHttpRequests(auth -> auth
        .requestMatchers("api/auth/**","hello","/swagger-ui/**").permitAll()
        .anyRequest().authenticated()
      )
      .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
}
```

---

# Controllers

## AccessController.java

Manages granting, listing, and revoking user access to interfaces.

```java
@RestController
@Tag(name = "Access", description = "Manage interface access")
public class AccessController {
  @Autowired private AccessService accessService;
```

### Give Access

```api
{
  "title": "Give Access",
  "description": "Grant a user a role on an interface",
  "method": "POST",
  "baseUrl": "https://api.example.com",
  "endpoint": "/api/interface/{interfaceId}/add-access",
  "headers": [{"key":"Authorization","value":"Bearer <token>","required":true}],
  "pathParams": [{"key":"interfaceId","value":"Interface UUID","required":true}],
  "bodyType": "json",
  "requestBody": "{\n  \"username\": \"jane\",\n  \"role\": \"ADMIN\"\n}",
  "responses": {
    "200": {"description":"Success","body":"{\"success\":true,\"message\":\"New user granted access as ADMIN\",\"data\":null}"},
    "401": {"description":"Unauthorized"}
  }
}
```

### Show Access

```api
{
  "title": "List Interface Accesses",
  "description": "Fetch all user roles for an interface",
  "method": "GET",
  "endpoint": "/api/interface/{interfaceId}/access",
  "responses": {
    "200": {"description":"Success","body":"{\"success\":true,\"message\":\"All access fetched successfully\",\"data\":[…]}"},
    "403": {"description":"Forbidden"}
  }
}
```

### Revoke Access

```api
{
  "title": "Revoke Access",
  "description": "Remove a user's role from an interface",
  "method": "DELETE",
  "endpoint": "/api/interface/{interfaceId}/access/{username}",
  "responses": {
    "200": {"description":"Success","body":"{\"success\":true,\"message\":\"Access removed successfully\",\"data\":null}"},
    "403": {"description":"Forbidden"}
  }
}
```

---

## AuthController.java

Handles user registration, login, OTP verification, password reset, and Google OAuth.

```java
@RequestMapping("api/auth")
@RestController
@Tag(name = "Auth", description = "Authentication endpoints")
public class AuthController {
  @Autowired private AuthService authService;
```

### Register User

```api
{
  "title": "Register User",
  "description": "Register new user and send verification OTP",
  "method": "POST",
  "endpoint": "/api/auth/register",
  "bodyType": "json",
  "requestBody": "{\n  \"email\":\"john@example.com\",\n  \"password\":\"pa$$w0rd\",\n  \"fullName\":\"John Doe\",\n  \"username\":\"john\"\n}",
  "responses": {
    "201": {"description":"Created","body":"{\"success\":true,\"message\":\"Registration successful. Verification email sent.\",\"data\":{\"email\":\"john@example.com\",\"otpExpiresInSeconds\":600}}"},
    "409": {"description":"Conflict","body":"{\"success\":false,\"message\":\"User already exists\",\"data\":null}"}
  }
}
```

### Verify OTP

```api
{
  "title": "Verify OTP",
  "description": "Confirm email using OTP",
  "method": "POST",
  "endpoint": "/api/auth/register/verify-otp",
  "requestBody": "{\n  \"email\":\"john@example.com\",\n  \"otp\":\"123456\"\n}",
  "responses": {
    "200": {"description":"Success","body":"{\"success\":true,\"message\":\"OTP verification successful\",\"data\":{\"email\":\"john@example.com\"}}"},
    "400": {"description":"Bad Request"}
  }
}
```

### Resend OTP

```api
{
  "title": "Resend OTP",
  "description": "Send a new verification OTP",
  "method": "POST",
  "endpoint": "/api/auth/resend-otp",
  "requestBody": "{ \"email\":\"john@example.com\" }",
  "responses": {
    "200": {"description":"Success","body":"{\"success\":true,\"message\":\"OTP sent to your email\",\"data\":{\"email\":\"john@example.com\"}}"}
  }
}
```

### Login

```api
{
  "title": "User Login",
  "description": "Authenticate user and receive JWT",
  "method": "POST",
  "endpoint": "/api/auth/login",
  "requestBody": "{\n  \"email\":\"john@example.com\",\n  \"password\":\"pa$$w0rd\"\n}",
  "responses": {
    "200": {"description":"Success","body":"{\"success\":true,\"message\":\"Login successful\",\"data\":{\"token\":\"<jwt>\",…}}"},
    "401": {"description":"Unauthorized"}
  }
}
```

### Google Login

```api
{
  "title": "Google OAuth Login",
  "description": "Login via Google ID token",
  "method": "POST",
  "endpoint": "/api/auth/google-login",
  "requestBody": "{ \"idToken\":\"<google-id-token>\" }",
  "responses": {
    "200": {"description":"Success","body":"{\"success\":true,\"message\":\"Google login successful\",\"data\":{\"token\":\"<jwt>\",…}}"},
    "401": {"description":"Invalid token"}
  }
}
```

### Forget Password

```api
{
  "title": "Forget Password",
  "description": "Send OTP for password reset",
  "method": "POST",
  "endpoint": "/api/auth/forget-password",
  "requestBody": "{ \"email\":\"john@example.com\" }",
  "responses": {
    "200": {"description":"OTP sent","body":"{\"success\":true,\"message\":\"OTP sent to your email\",\"data\":{\"email\":\"john@example.com\"}}"}
  }
}
```

### Reset Password

```api
{
  "title": "Reset Password",
  "description": "Reset password via OTP or old password",
  "method": "POST",
  "endpoint": "/api/auth/reset-password",
  "requestBody": "{\n  \"email\":\"john@example.com\",\n  \"otp\":\"123456\",\n  \"newPassword\":\"newPa$$\",\n  \"oldPassword\":null\n}",
  "responses": {
    "200": {"description":"Success"},
    "400": {"description":"Invalid OTP or old password"}
  }
}
```

---

## DeviceController.java

CRUD and streaming for devices under a given interface.

```java
@RequestMapping("api/device")
@RestController
@Tag(name = "Device", description = "Device management and data stream")
public class DeviceController {
  @Autowired private DeviceService deviceService;
```

### Create Device

```api
{
  "title": "Create Device",
  "description": "Add a new device to an interface",
  "method": "POST",
  "endpoint": "/api/device/{interfaceId}",
  "requestBody": "{\n  \"name\":\"Sensor1\",\n  \"type\":\"SMART_BIN\"\n}",
  "responses": {
    "201": {"description":"Created","body":"{\"success\":true,\"message\":\"Device created successfully\",\"data\":{…}}"},
    "403": {"description":"Forbidden"}
  }
}
```

### List Devices

```api
{
  "title": "List Devices",
  "description": "Fetch all devices for all interfaces user has access to",
  "method": "GET",
  "endpoint": "/api/device",
  "responses": {
    "200": {"description":"Success","body":"{\"success\":true,\"message\":\"Devices fetched successfully\",\"data\":[…]}"},
    "404": {"description":"No devices found"}
  }
}
```

### Get Device Readings

```api
{
  "title": "Get Device Readings",
  "description": "Retrieve sensor readings for a device",
  "method": "GET",
  "endpoint": "/api/device/{deviceId}/readings",
  "queryParams": [
    {"key":"days","value":"Number of days back","required":false},
    {"key":"hours","value":"Number of hours back","required":false}
  ],
  "responses": {
    "200": {"description":"Success","body":"{\"success\":true,\"message\":\"Device readings fetched successfully\",\"data\":[…]}"},
    "404": {"description":"No readings found"}
  }
}
```

### SSE Stream

```api
{
  "title": "Stream Device Data",
  "description": "Subscribe to live sensor updates",
  "method": "GET",
  "endpoint": "/api/device/{deviceId}/stream",
  "headers": [{"key":"Accept","value":"text/event-stream","required":true}],
  "responses": {
    "200": {"description":"Stream starts"},
    "401": {"description":"Unauthorized"}
  }
}
```

### Delete Device

```api
{
  "title": "Delete Device",
  "description": "Remove a device",
  "method": "DELETE",
  "endpoint": "/api/device/{deviceId}",
  "responses": {
    "200": {"description":"Deleted","body":"{\"success\":true,\"message\":\"Device deleted successfully\",\"data\":null}"},
    "403": {"description":"Forbidden"}
  }
}
```

---

## InterfaceController.java

CRUD operations on interfaces, including cascading delete.

```java
@RequestMapping("/api/interface")
@RestController
@Tag(name = "Interface", description = "Interface management")
public class InterfaceController {
  @Autowired private InterfaceService interfaceService;
```

### Create Interface

```api
{
  "title": "Create Interface",
  "description": "Define a new interface",
  "method": "POST",
  "endpoint": "/api/interface",
  "requestBody": "{\n  \"name\":\"GreenBin\",\n  \"description\":\"Civic center bins\"\n}",
  "responses": {
    "200": {"description":"Created","body":"{\"success\":true,\"message\":\"Interface created successfully\",\"data\":{…}}"},
    "401": {"description":"Unauthorized"}
  }
}
```

### List Interfaces

```api
{
  "title": "List Interfaces",
  "description": "Get interfaces accessible to user",
  "method": "GET",
  "endpoint": "/api/interface",
  "responses": {
    "200": {"description":"Success","body":"{\"success\":true,\"message\":\"Interfaces fetched successfully\",\"data\":[…]}"},
    "404": {"description":"No Interfaces present"}
  }
}
```

### Get Interface By ID

```api
{
  "title": "Get Interface By Id",
  "description": "Fetch interface and its devices",
  "method": "GET",
  "endpoint": "/api/interface/{interface_id}",
  "responses": {
    "200": {"description":"Success","body":"{\"success\":true,\"message\":\"Interface fetched successfully\",\"data\":{…}}"}
  }
}
```

### Delete Interface

```api
{
  "title": "Delete Interface",
  "description": "Remove interface and its devices with password confirmation",
  "method": "DELETE",
  "endpoint": "/api/interface/{interface_id}",
  "bodyType": "json",
  "requestBody": "{ \"password\":\"pa$$w0rd\" }",
  "responses": {
    "200": {"description":"Deleted","body":"{\"success\":true,\"message\":\"Interface and devices deleted successfully\",\"data\":null}"}
  }
}
```

---

## UserController.java

User profile and search endpoints.

```java
@RestController
public class UserController {
  @Autowired private UserService userService;
```

### Get My Profile

```api
{
  "title": "Get User Profile",
  "description": "Fetch authenticated user’s details",
  "method": "GET",
  "endpoint": "/api/users/me",
  "responses": {
    "200": {"description":"Success","body":"{…UserDTO…}"},
    "401": {"description":"Unauthorized"}
  }
}
```

### Search Users

```api
{
  "title": "Search Users",
  "description": "Search verified users by username prefix",
  "method": "GET",
  "endpoint": "/api/users/search",
  "queryParams": [{"key":"q","value":"prefix","required":true}],
  "responses": {
    "200": {"description":"Success","body":"{\"success\":true,\"message\":\"Users found\",\"data\":[…]}"},
    "400": {"description":"Bad Request"}
  }
}
```

---

# Exception Handling

## GlobalExceptionHandler.java

Catches common exceptions across controllers, translating them into consistent `ApiResponse` payloads.

- Handles `EntityNotFoundException` → 404
- Handles `AccessDeniedException` → 403
- Handles `VerificationTokenExpiredException` → 400
- Handles `UserAlreadyExistsException` → 409
- Catches all other exceptions → 500

---

## UserAlreadyExistsException.java

Simple `RuntimeException` indicating duplicate user registration.

---

## VerificationTokenExpiredException.java

Signals that an OTP or email verification token has expired.

---

# Security Filter

## JwtFilter.java

Intercepts requests (except Google login), extracts and validates JWT, and populates Spring Security context.

- Skips filter on `/api/auth/google-login`.
- Checks `Authorization: Bearer <token>`.
- Uses `JwtUtil` to extract username and validate token.
- Loads `UserDetails` via `MyUserDetailsService`.

---

# Data Transfer Objects (DTOs)

### Auth

- **RegisterRequest**: `{ email, password, fullName, username }`
- **RegistrationResponse**: `{ email, message, status }`
- **LoginRequest**: `{ email, password }`
- **LoginResponse**: `{ data: { token,… }, success, message }`
- **ResetPasswordRequest**: `{ email, otp?, oldPassword?, newPassword }`

### Access

- **AddAccessRequestDTO**: `{ username, role }`
- **InterfaceAccessDTO**: `{ userId, username, fullName, role }`

### Common

- **ApiResponse<T>**: `{ success, message, data:T }`

### Device

- **DeviceRequestDTO**: `{ name, type }`
- **DeviceDTO**: `{ id, name, type, status, lastValue1, lastValue2,… }`

### Interface

- **InterfaceDTO**: `{ id, name, description, ownerId, ownerUsername }`
- **InterfaceWithDevicesDTO**: extends `InterfaceDTO` with `devices: List<DeviceDTO>`

### Sensor

- **SensorReadingDTO**: `{ deviceId, value1, value2, timestamp }`

### User

- **UserDTO**: `{ id, fullName, email, username, createdAt }`

---

# Enumerations

- **Role**: `OWNER`, `ADMIN`, `USER`
- **DeviceType**: `SMART_BIN`, `SMART_WATER`, etc.
- **DeviceStatus**: enum for active/inactive.
- **SensorReadingStatus**: for reading health.

---

# Persistence Models

- **User**: Core user entity with OTP, verification flags.
- **Interface**: Grouping of devices, owned by a user.
- **Device**: IoT endpoint with last readings and status.
- **SensorReading**: Historical data points for a device.
- **UserInterface**: Join entity linking users to interfaces with roles.
- **UserInterfaceId**: Composite key for `UserInterface`.
- **UserPrinciple**: Adapter from `User` to Spring Security’s `UserDetails`.

---

# Spring Data Repositories

- **UserRepo**: CRUD on `User` + find by email/username, OTP management.
- **InterfaceRepo**: CRUD on `Interface`.
- **DeviceRepo**: Query by interface, status, lastUpdated.
- **SensorReadingRepo**: Custom query by date.
- **UserInterfaceRepo**: Manage user-interface relationships.

---

# Services

- **AuthService**: Business logic for registration, OTP, login.
- **UserService**: Profile fetching, search, cleanup of unverified users.
- **OtpService**: Generate, verify, resend OTP via `EmailService`.
- **EmailService**: Sends HTML emails via `JavaMailSender`.
- **GoogleVerifierService**: Validates Google ID tokens.
- **MyUserDetailsService**: Loads verified users for Spring Security.
- **AccessService**: Grant/revoke/list interface access.
- **InterfaceService**: CRUD and cascade delete.
- **DeviceService**: CRUD, reading retrieval, deletion.
- **MqttService**: Processes incoming MQTT payloads.
- **MqttStatusService**: Reports MQTT connection status.

---

# Utilities

## DeviceStreamManager.java

Manages SSE emitters per device, broadcasting new readings to connected clients.

## JwtUtil.java

Generates and validates JWTs, extracts claims.

## ScheduledTaskManager.java

- Marks devices offline if no update within 1 minute.
- Removes unverified users daily at midnight.

---

# Application & Infrastructure

## ProjectXBackendApplication.java

Bootstraps Spring Boot with scheduling enabled and logs environment variables on startup.

## application.properties

Defines server port, Hibernate, MQTT topics, JWT secrets, mail settings, active profile.

## application-prod.properties

Overrides DB and mail credentials via environment variables and configures production settings.

---

# Testing

## ProjectXBackendApplicationTests.java

Basic context load test to ensure Spring Boot application context starts.

---

# Project Metadata

## README.md

Overview of the **EcoMonitor** system, its features, tech stack, and IoT hardware.

## pom.xml

Maven configuration with dependencies for Spring Boot, Spring Security, MQTT, Jackson, Lombok, Google API client, OpenAPI, etc.

---

```mermaid
flowchart TD
  subgraph API Layer
    AuthController --> AuthService
    UserController --> UserService
    InterfaceController --> InterfaceService
    DeviceController --> DeviceService
    AccessController --> AccessService
  end
  subgraph Service Layer
    AuthService --> OtpService --> EmailService
    AuthService --> UserRepo
    UserService --> UserRepo
    InterfaceService --> InterfaceRepo & UserInterfaceRepo
    DeviceService --> DeviceRepo & SensorReadingRepo
    AccessService --> UserInterfaceRepo & UserRepo
    MqttService --> DeviceRepo & SensorReadingRepo & DeviceStreamManager
  end
  subgraph Persistence Layer
    UserRepo
    InterfaceRepo
    DeviceRepo
    SensorReadingRepo
    UserInterfaceRepo
  end
  MqttConfig --> MqttService
  SecurityConfig --> JwtFilter --> MyUserDetailsService
  ScheduledTaskManager --> DeviceRepo & UserService
  DeviceStreamManager --> SSE Clients
```

*This documentation captures the purpose and interplay of each file in the project.*