FROM open-liberty:microProfile3
ADD target/myservice.war /config/dropins
COPY src/main/liberty/config /config/

# Wildfly
#FROM jboss/wildfly
#ADD build/libs/myservice.war /opt/jboss/wildfly/standalone/deployments/

# Payara
#FROM payara/micro
#ADD build/libs/myservice.war /opt/payara/deployments

# TomEE
#FROM tomee:8-jre-8.0.0-M2-microprofile
#COPY build/libs/myservice.war /usr/local/tomee/webapps/