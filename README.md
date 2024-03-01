# STELAR-CD-API
API for running Correlation Detective jobs, as part of the STELAR KLMS project

# How to run docker
## Step 1: Pull the image
```bash
docker pull correlationdetective/stelar-api:latest
```

## Step 2: Run the image
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
or in Python:
```python
import requests
import json

url = "http://localhost:8079/cd/run"

payload = json.dumps({
  "inputPath": "stocks",
  "simMetric": "PEARSON_CORRELATION",
  "maxPLeft": 2,
  "maxPRight": 1,
  "nVectors": 100
})
headers = {
  'Content-Type': 'application/json'
}

response = requests.request("POST", url, headers=headers, data=payload)

print(response.text)
```
