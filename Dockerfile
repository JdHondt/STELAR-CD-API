FROM openjdk:17-jdk-alpine
MAINTAINER "Jens d'Hondt"

# Copy jar
COPY target/stelar-cd-api-0.0.2.jar stelar-cd-server-0.0.2.jar

ENTRYPOINT ["java","-jar","/stelar-cd-server-0.0.2.jar"]