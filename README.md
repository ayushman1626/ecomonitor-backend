## Running the EcoMonitor Backend with Docker

This project provides a Docker setup for building and running the **EcoMonitor** backend (Java Spring Boot) using Eclipse Temurin JDK 21 and Maven. The application is exposed on port **8000** by default.

### Project-Specific Docker Requirements
- **Java Version:** Eclipse Temurin 21 (JDK for build, JRE for runtime)
- **Build Tool:** Maven Wrapper (`mvnw`)
- **Exposed Port:** 8000 (Spring Boot application)
- **User:** Runs as a non-root user (`appuser`) for security
- **JVM Options:** Container-aware memory settings via `JAVA_OPTS`

### Environment Variables
- No required environment variables are specified by default in the Docker or Compose files.
- If you need to customize application properties (e.g., database credentials), set them via environment variables or by mounting a custom `application.properties` file.
- If you use a `.env` file for environment variables, uncomment the `env_file` line in `docker-compose.yml`.

### Build and Run Instructions
1. **Build and start the backend service:**
   ```sh
   docker compose up --build
   ```
   This will build the application JAR using Maven and run it in a container.

2. **Access the application:**
   - The backend will be available at [http://localhost:8000](http://localhost:8000)

### Special Configuration
- The Docker setup is ready for extension with additional services (e.g., PostgreSQL). See the commented example in `docker-compose.yml` if you need a database.
- To persist database data, uncomment and configure the `volumes` section in `docker-compose.yml`.
- If you need to expose additional ports or services, update the `ports` and `depends_on` sections accordingly.

### Ports
- **java-backend:** 8000 (host) → 8000 (container)

---

_Refer to the `docker-compose.yml` file for further customization and to add additional services as needed for your development or production environment._
