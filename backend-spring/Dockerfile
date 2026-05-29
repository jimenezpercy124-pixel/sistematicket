# Stage 1: Build
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/backend-spring-1.0.0-SNAPSHOT.jar app.jar

# Puerto configurado en application.properties
EXPOSE 8080

# Variables de entorno por defecto (pueden ser sobrescritas en AWS)
ENV SPRING_DATASOURCE_URL=jdbc:oracle:thin:@//your-oracle-host:1521/orclpdb
ENV SPRING_DATASOURCE_USERNAME=SIIT_USER
ENV SPRING_DATASOURCE_PASSWORD=SIIT_PASSWORD

ENTRYPOINT ["java", "-jar", "app.jar"]
