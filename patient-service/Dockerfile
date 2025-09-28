# ---- Build stage ----
FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy only pom.xml first to leverage Docker layer caching
COPY pom.xml .

# Download dependencies (note: "dependency" was misspelled in your file)
RUN mvn dependency:go-offline -B

# Copy the actual source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# ---- Run stage ----
FROM openjdk:21-jdk AS runner

WORKDIR /app

# Copy the fat jar from the builder stage
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 4000

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]

