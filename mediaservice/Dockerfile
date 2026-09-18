FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

COPY target/mediaservice-*.jar app.jar

EXPOSE 8087

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
