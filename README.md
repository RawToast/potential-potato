# Smartcloud Prices

This project can be ran using docker compose:

`docker compose build` - will build the images for the server and the underlying service
`docker compose up` - will start the services. The API should be avilable on port 8080

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
