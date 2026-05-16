# ----- STAGE 1: Build the JAR -----
FROM maven:3.9.4-eclipse-temurin-17 AS builder
WORKDIR /app

# Copy all your project files into the Docker container
COPY . .

# Set WORKDIR to the specific service folder because there is no root pom.xml
WORKDIR /app/flight-service
RUN mvn clean package -DskipTests

# ----- STAGE 2: Run the JAR -----
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app

# Copy the built jar from Stage 1 into this final image
COPY --from=builder /app/flight-service/target/*.jar app.jar

# Start the application
ENTRYPOINT ["java", "-jar", "app.jar"]
