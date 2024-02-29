FROM openjdk:17-jdk-alpine
MAINTAINER "Jens d'Hondt"

# Copy jar
COPY target/stelar-cd-api-0.0.1.jar stelar-cd-server-0.0.1.jar

# Copy example datasets
COPY data/ data/

ENTRYPOINT ["java","-jar","/stelar-cd-server-0.0.1.jar"]