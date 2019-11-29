# OpenLiberty
FROM openliberty/open-liberty:full-java8-openj9-ubi
COPY src/main/liberty/config /config
ADD build/libs/myservice.war /config/dropins
