FROM openjdk:21-jdk-slim
VOLUME /tmp
COPY target/*.jar app.jar
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*
ENTRYPOINT ["java","-jar","/app.jar"]