# syntax=docker/dockerfile:1

# Etapa 1: Build
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build

COPY pom.xml mvnw mvnw.cmd ./
COPY .mvn/ .mvn/

# Normaliza finales de línea de Windows (CRLF a LF) y otorga permisos de ejecución
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw

RUN ./mvnw dependency:go-offline -B || true

COPY src/ src/
COPY contracts/ contracts/
RUN ./mvnw clean package -DskipTests -B

# Agente fijado y verificado; sólo se activa cuando JAVA_TOOL_OPTIONS lo solicita.
ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.34.0/opentelemetry-javaagent.jar /tmp/opentelemetry-javaagent.jar
RUN echo 'da44154cea128257779c8a782b9651fd7c0c6aa177cd3810f9a867ac8509555d  /tmp/opentelemetry-javaagent.jar' | sha256sum -c - \
    && chmod 644 /tmp/opentelemetry-javaagent.jar

# Etapa 2: Runtime
FROM eclipse-temurin:21-jre-alpine AS runner
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=builder /build/target/*.jar app.jar
COPY --from=builder /tmp/opentelemetry-javaagent.jar /opt/otel/opentelemetry-javaagent.jar
RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 3003

HEALTHCHECK --interval=30s --timeout=5s --start-period=25s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:3003/health/live || exit 1

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]
