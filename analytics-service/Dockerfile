FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy JAR built by Maven
COPY target/*.jar app.jar

# Expose service port
EXPOSE 8088

ENTRYPOINT ["java", "-jar", "app.jar"]