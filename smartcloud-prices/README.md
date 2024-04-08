# Smartcloud instance prices API

# Setup

Follow the instruction at [smartcloud](https://hub.docker.com/r/smartpayco/smartcloud) to run the Docker container on your machine.

Clone or download this project onto your machine and run

```
$ sbt run
```

The API should be running on your port 8080.

## Running the Project

To run locally you can just go with `sbt run` as described above. This will start the server on port 8080.

To run tests you can use `sbt test`.

### Running with Docker

To make life easier you can just use docker compose from the root directory. See the readme this in the root directory for more information.

### Endpoints

```
GET /prices?kind=sc2-micro
{
  "kind": "sc2-small",
  "amount": 0.463
}
```

```
GET /localhost:8080/instance-kinds
[
  {
    "kind": "sc2-micro"
  },
  {
    "kind": "sc2-small"
  },
  ...
]
```
