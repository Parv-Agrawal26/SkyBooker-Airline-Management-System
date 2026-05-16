# ----- STAGE 1: Build the JAR -----
FROM maven:3.9.4-eclipse-temurin-17 AS builder
WORKDIR /app

# Copy all your project files into the Docker container
COPY . .

# Tell Maven to ONLY build the Flight-Service (and skip tests for speed)
RUN mvn clean package -pl Flight-Service -am -DskipTests


# ----- STAGE 2: Run the JAR -----
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app

# Copy the built jar from Stage 1 into this final image
COPY --from=builder /app/Flight-Service/target/*.jar app.jar

# Start the application
ENTRYPOINT ["java", "-jar", "app.jar"]
