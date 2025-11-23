FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean install -DskipTests

# This stage just builds the common module and installs it to Maven local repo
# Other services will use this as a base

