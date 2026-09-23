FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

COPY target/marketingservice-*.jar app.jar

EXPOSE 8084

ENTRYPOINT ["java", "-jar", "/app/app.jar"]