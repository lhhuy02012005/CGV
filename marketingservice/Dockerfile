FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

COPY target/marketingservice-*.jar app.jar

EXPOSE 8084

ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]