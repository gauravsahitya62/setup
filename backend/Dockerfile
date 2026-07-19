FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN apk add --no-cache maven && mvn -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S flowset && adduser -S flowset -G flowset
COPY --from=build /workspace/target/flowset-backend-*.jar /app/app.jar
USER flowset
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
