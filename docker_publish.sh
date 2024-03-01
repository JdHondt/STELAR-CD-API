#!/bin/bash

# Make sure to package through intellij maven plugin

docker login -u correlationdetective -p 'f73qpvPUrxg6^&gs!tROb'

docker build -t correlationdetective/stelar-api:latest .

docker push correlationdetective/stelar_api:latest