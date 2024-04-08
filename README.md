# Smartcloud Prices

[![Build](https://github.com/RawToast/potential-potato/actions/workflows/build.yaml/badge.svg)](https://github.com/RawToast/potential-potato/actions/workflows/build.yaml)

### Development

For more information about running and testing the API, please see the [readme](./smartcloud-prices/README.md) for the service

### Running the service

This project can be ran using docker compose:

`docker compose build` - will build the images for the server and the underlying service
`docker compose up` - will start the services. The API should be avilable on port 8080

### Endpoints

```
GET localhost:8080/prices?kind=sc2-micro
{
  "kind": "sc2-small",
  "amount": 0.463
}
```

```
GET localhost:8080/instance-kinds
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
