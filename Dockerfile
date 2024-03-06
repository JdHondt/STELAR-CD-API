FROM alexdarancio7/pyvis-custom:latest
MAINTAINER "Jens d'Hondt"

# Copy jar
COPY target/stelar-cd-api-0.0.2.jar stelar-cd-server-0.0.2.jar

ENTRYPOINT ["java","-jar","/stelar-cd-server-0.0.2.jar"]