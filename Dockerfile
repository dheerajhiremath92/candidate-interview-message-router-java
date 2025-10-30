# STAGE 1: Build
# Use the official Maven image with Java 17, as required by your pom.xml
FROM maven:3.9-eclipse-temurin-17 AS builder

# Add author credit
LABEL author="Erik (original) + code completion by user"

# Set the working directory inside the container
WORKDIR /app

# Copy the pom.xml first to download dependencies
# This layer is cached and only re-runs if pom.xml changes
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy the rest of your source code
COPY src ./src

# Run the 'package' command. This will compile your code,
# run all the tests from MessageRouterTest.java,
# and create the final .jar file.
RUN mvn package


# STAGE 2: Final Image
# Start from a slim Java 17 Runtime image
FROM eclipse-temurin:17-jre-jammy

# Add author credit to the final image as well
LABEL author="Erik (original) + Parts of code completion by Dheeraj Hiremath"

WORKDIR /app

# Copy only the compiled .jar file from the 'builder' stage
# The jar name comes from your pom.xml
COPY --from=builder /app/target/message-router-1.0-SNAPSHOT.jar .

# This image now contains your compiled library.
# Since it's a library and not a runnable app, we don't need a CMD.