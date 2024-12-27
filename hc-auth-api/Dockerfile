FROM openjdk:17-jdk-slim

RUN groupadd -r spring && useradd -r spring -g spring
USER spring:spring

WORKDIR /app

COPY target/hc-auth-api.jar app.jar

ENV CLASSPATH=/etc/secrets

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
