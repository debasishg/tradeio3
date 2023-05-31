# tradeio3
Sample trading domain model using Scala 3

## Run docker compose
Spins up PostgreSQL in docker

```
docker compose up
```

## Connect to docker and run psql

```
docker ps
```

Use the container id returned fom docker ps

```
docker exec -it 2a70a427bec5 bash
```

Invoke psql

```
psql -U postgres
```

Connect to database

```
\c trading;
```

Use database to fetch data

```
select * from accounts;
```

## Run the trading application

The trading application runs with the front office order and execution files as present in `modules/core/src/main/resources`.

```
sbt "project core; runMain tradex.domain.TradeApp"
```

## Tapir integration

Service integration with tapir is available for selective end-points. Try the instrument query service once the server is started as follows:

```
sbt "project core; runMain tradex.domain.Main"
```

* Invoke http://localhost:8080/api/instrument/US0378331005 for a sample instrument query

* Invoke `curl -X PUT http://localhost:8080/api/instrument -H "Accept: application/json" -H "Content-Type: application/json" -d @./equity.json`

with equity.json having the following:

```{
  "equityData": {
    "isin": "US30303M1027",
    "name": {"value" : "Meta"},
    "lotSize": 1,
    "issueDate": "2019-08-25T19:10:25",
    "unitPrice": 180.00
  }
}```

* Running `TradeApp` will generate trades and insert into trade repository. Run `Main` and invoke http://localhost:8080/api/trade/ibm-123?tradedate=2023-05-28 for a sample trade query

* Invoke http://localhost:8080/docs to use Swagger UI