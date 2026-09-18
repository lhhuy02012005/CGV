FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

COPY target/notificationservice-*.jar app.jar

EXPOSE 8086

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
