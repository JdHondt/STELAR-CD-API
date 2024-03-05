# STELAR-CD-API
API for running Correlation Detective jobs, as part of the STELAR KLMS project

# How to run docker
## Step 1: Pull the image
```bash
docker pull correlationdetective/stelar-api:latest
```

## Step 2: Run the image
```bash
docker run -p8080:8080 --network='host' correlationdetective/stelar-api:latest
```

# How to query
Example query:
```bash
curl --location --request POST 'http://localhost:8080/cd/run' \
--header 'Content-Type: application/json' \
--data-raw '{
    "inputPath": "s3://correlation-detective/example_data.csv",
    "outputPath": "s3://correlation-detective",
    "simMetric": "PEARSON_CORRELATION",
    "maxPLeft": 1,
    "maxPRight": 2,
    "nVectors": 100,
    "topK": 50,
    "MINIO_ENDPOINT_URL": "http://127.0.0.1:9000",
    "MINIO_ACCESS_KEY": "minioadmin",
    "MINIO_SECRET_KEY": "minioadmin"
}'
```
or in Python:
```python
import requests
import json

url = "http://localhost:8080/cd/run"

payload = json.dumps({
  "inputPath": "s3://correlation-detective/example_data.csv",
  "outputPath": "s3://correlation-detective",
  "simMetric": "PEARSON_CORRELATION",
  "maxPLeft": 1,
  "maxPRight": 2,
  "nVectors": 100,
  "topK": 50,
  "MINIO_ENDPOINT_URL": "http://127.0.0.1:9000",
  "MINIO_ACCESS_KEY": "minioadmin",
  "MINIO_SECRET_KEY": "minioadmin"
})
headers = {
  'Content-Type': 'application/json'
}

response = requests.request("POST", url, headers=headers, data=payload)

print(response.text)
```
