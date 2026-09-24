FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

COPY target/catalogservice-*.jar app.jar

# Cổng 8083: REST API, Cổng 9090: gRPC Showtime Pricing Server
EXPOSE 8083 9090

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
