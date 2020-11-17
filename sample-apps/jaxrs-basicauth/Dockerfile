# OpenLiberty
FROM openliberty/open-liberty:full-java8-openj9-ubi
COPY src/main/liberty/config /config/
ADD build/libs/myservice.war /config/apps/

# Wildfly
#FROM jboss/wildfly
#ADD build/libs/myservice.war /opt/jboss/wildfly/standalone/deployments/

# Payara
#FROM payara/micro:5.193
#CMD ["--deploymentDir", "/opt/payara/deployments", "--noCluster"]
#ADD build/libs/myservice.war /opt/payara/deployments

# TomEE
#FROM tomee:8-jre-8.0.0-M2-microprofile
#COPY build/libs/myservice.war /usr/local/tomee/webapps/
