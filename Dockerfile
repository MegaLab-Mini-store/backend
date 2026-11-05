FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /app

COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline

COPY src ./src
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:25-jre-alpine
LABEL org.opencontainers.image.source="https://github.com/MegaLab-Mini-store/backend"
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=builder --chown=spring:spring /app/target/*.jar app.jar

USER spring:spring

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=60 -XX:+UseSerialGC -Xss512k"

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]