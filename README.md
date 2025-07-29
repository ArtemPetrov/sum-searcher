# Sum Searcher

A simple REST API service that finds all combinations of integers from a provided list that sum up to a target value.

## Prerequisites

**Docker** must be installed and running.

## Building

```bash
docker build -t sum-searcher .
```

## Running

Running application on port `8080`
```bash
docker run --name sum-searcher -d --rm -p 8080:8080 sum-searcher
```

## API

Application exposes REST endpoint:
- method: `POST`
- url: `/find`
- Content-Type header: `application/json`
- Request body:
  * `.data`: array of ints (addends candidates)
  * `.targetOpt`: int (sum for which addends should be found)
- Response body is an array of objects with indexes and values of found addends:
  * `.[].indexes` - indexes of found addends from the request `.data` array
  * `.[].values` - values of found addends from the request `.data` array

#### Example

**Request**:
```bash
curl 'http://127.0.0.1:8080/find' \
  -H "Content-Type: application/json" \
  -d $'{
         "data": [1, 2, 3],
         "targetOpt": 5
       }'
```
**Response**:
```json
[
  {
    "indexes": [1, 2],
    "values": [2, 3]
  }
]
```
