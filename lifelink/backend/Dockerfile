# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# ---- Run stage ----
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/target/lifelink.jar app.jar

ENV SPRING_PROFILES_ACTIVE=prod
# Keeps the JVM inside a 512 MB free-tier container
ENV JAVA_OPTS="-Xmx384m -XX:+UseSerialGC"

# The app listens on $PORT (set by the host) and falls back to 8080
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
