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


