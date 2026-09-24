FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

COPY target/paymentservice-*.jar app.jar

EXPOSE 8085

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
