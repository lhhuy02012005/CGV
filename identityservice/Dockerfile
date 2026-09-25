FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

COPY target/identityservice-*.jar app.jar

EXPOSE 8081

ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
