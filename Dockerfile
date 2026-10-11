# ──────────────────────────────────────────────────────────────
# Stage 1 — Build
# ──────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /workspace

# Copy Maven wrapper and POM first (layer-cache friendly)
COPY mvnw mvnw.cmd ./
COPY .mvn .mvn
COPY pom.xml ./

RUN chmod +x ./mvnw
# Download dependencies before copying source (cached unless pom.xml changes)
RUN ./mvnw dependency:go-offline

# Copy source
COPY src ./src

# Package, skip tests during image build (tests run in CI)
RUN ./mvnw package -DskipTests

# ──────────────────────────────────────────────────────────────
# Stage 2 — Runtime
# ──────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

# Non-root user for security
RUN addgroup -S m3 && adduser -S m3 -G m3

WORKDIR /app

# Copy the fat JAR from the builder stage
COPY --from=builder /workspace/target/*.jar app.jar

# Give ownership to the non-root user
RUN chown m3:m3 app.jar

USER m3

EXPOSE 3003

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]
