FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

COPY target/mediaservice-*.jar app.jar

EXPOSE 8087

ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
