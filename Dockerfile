FROM maven:3.9-eclipse-temurin-8 AS build

WORKDIR /workspace
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:8-jre

WORKDIR /app
COPY --from=build /workspace/target/tenx-ai-gateway-0.0.1-SNAPSHOT.jar /app/tenx-ai-gateway.jar

EXPOSE 8088

ENTRYPOINT ["java", "-jar", "/app/tenx-ai-gateway.jar"]
