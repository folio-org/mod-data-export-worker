#!/bin/sh
#
#DB_PROPS_FILE=/usr/verticles/1.txt
#touch ${DB_PROPS_FILE}
#echo "spring.datasource.username=${DB_USERNAME} spring.datasource.password=${DB_PASSWORD} spring.datasource.url=jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_DATABASE}" | sed 's/\s\+/\n/g' | tee ${DB_PROPS_FILE}
#
DB_URL="jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_DATABASE}"
#
DB_OPTS="-Dspring.datasource.username=${DB_USERNAME} -Dspring.datasource.password=${DB_PASSWORD} -Dspring.datasource.url=${DB_URL}"
#
export JAVA_OPTIONS="${JAVA_OPTIONS:-} ${DB_OPTS}"
