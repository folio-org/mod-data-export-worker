#!/bin/sh
DB_URL="jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_DATABASE}"
KAFKA_URL="${KAFKA_HOST}:${KAFKA_PORT}"
OPTS="-Dspring.datasource.username=${DB_USERNAME} -Dspring.datasource.password=${DB_PASSWORD} -Dspring.datasource.url=${DB_URL} -Dspring.kafka.bootstrap-servers=${KAFKA_URL}"
export JAVA_OPTIONS="${JAVA_OPTIONS:-} ${OPTS}"
