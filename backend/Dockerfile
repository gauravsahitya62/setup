FROM maven:3.9.9-eclipse-temurin-21-alpine AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -q -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
ENV JAVA_TOOL_OPTIONS="-Duser.timezone=UTC -Dfile.encoding=UTF-8"
RUN apk add --no-cache wget \
    && addgroup -S flowset \
    && adduser -S flowset -G flowset
COPY --from=build /workspace/target/flowset-backend-*.jar /app/app.jar
USER flowset
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=50s --retries=5 \
  CMD wget -qO- http://127.0.0.1:8080/actuator/health/liveness >/dev/null || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
