#!/bin/bash

# Prepare Postgres
echo "Preparing Postgres..."
docker pull postgres:latest
POSTGRES_SHA=$(docker ps | grep postgres | cut -d ' ' -f 1) > /dev/null 2>&1

if [ -z "$POSTGRES_SHA" ]
then
  POSTGRES_SHA=$(
    docker run \
      --env POSTGRES_USER=postgres \
      --env POSTGRES_PASSWORD=postgres \
      --detach \
      --restart unless-stopped \
      -p 5432:5432 \
      -d postgres:latest
  )
  sleep 5
fi

echo -e "Postgres is running in container $POSTGRES_SHA\n\n"

# Configure the DB
POSTGRES_BOOTSTRAPPING_SQL="
CREATE DATABASE exposed_template1;
CREATE USER exposed_template1 WITH ENCRYPTED PASSWORD 'exposed_template1';
ALTER DATABASE exposed_template1 OWNER TO exposed_template1;
ALTER USER exposed_template1 CREATEDB;
"
echo "Configuring the postgres testing database..."
echo $POSTGRES_BOOTSTRAPPING_SQL | docker exec -i $POSTGRES_SHA psql -U postgres
echo -e "\nSuccessfully configured the postgres testing database!"



# Prepare MySQL
echo -e "\n\n\nPreparing MySQL..."
docker pull mysql:latest
MYSQL_SHA=$(docker ps | grep mysql | cut -d ' ' -f 1) > /dev/null 2>&1

if [ -z "$MYSQL_SHA" ]
then
  MYSQL_SHA=$(
    docker run \
      --env MYSQL_USER=mysql \
      --env MYSQL_PASSWORD=mysql \
      --env MYSQL_ALLOW_EMPTY_PASSWORD=true \
      --detach \
      --restart unless-stopped \
      -p 3306:3306 \
      -d mysql:latest
  )
  sleep 5
fi

echo -e "MySQL is running in container $MYSQL_SHA\n\n"

# Configure the DB
MYSQL_BOOTSTRAPPING_SQL="
CREATE DATABASE exposed_template1;
CREATE USER exposed_template1 IDENTIFIED BY 'exposed_template1';
GRANT ALL PRIVILEGES ON exposed_template1.* TO exposed_template1;
"
echo "Configuring  the MySQL testing database..."
echo $MYSQL_BOOTSTRAPPING_SQL | docker exec -i $MYSQL_SHA mysql -uroot
echo -e "\nSuccessfully configured the MySQL testing database!"
