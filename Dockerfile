# Use Java 17
FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

# Copy jar
COPY target/erp-0.0.1-SNAPSHOT.jar app.jar

# Run app
ENTRYPOINT ["java","-jar","/app/app.jar"]
