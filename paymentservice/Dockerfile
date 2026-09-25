FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

COPY target/paymentservice-*.jar app.jar

EXPOSE 8085

ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
