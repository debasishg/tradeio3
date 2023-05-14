# tradeio3
Sample trading domain model using Scala 3

## Run docker compose
>docker compose up

## Connect to docker
>docker ps

use the container id returned fom docker ps
>docker exec -it 2a70a427bec5 bash

invoke psql
>psql -U postgres

connect to database
>\c trading;

use database
>select * from orders;


