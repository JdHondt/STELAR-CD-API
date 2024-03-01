# How to run docker
```bash
docker run -p8079:8080 correlationdetective/stelar-api:latest
```

# How to query
Example query:
```bash
curl --location --request POST 'http://localhost:8079/cd/run' \
--header 'Content-Type: application/json' \
--data-raw '{
    "inputPath": "stocks",
    "simMetric": "PEARSON_CORRELATION",
    "maxPLeft": 2,
    "maxPRight": 1,
    "nVectors": 100
}'
```