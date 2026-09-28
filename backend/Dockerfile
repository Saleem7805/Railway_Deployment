# Build and run in one file. The dependency layer is cached separately from the
# source, so a code change does not re-download the world.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /src/target/pib-lms-1.0.0.jar app.jar
RUN mkdir -p /data/files
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
