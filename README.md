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
* Invoke http://localhost:8080/docs to use Swagger UI