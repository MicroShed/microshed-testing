FROM open-liberty:full-java11-openj9
COPY src/main/liberty/config /config/
ADD build/libs/myservice.war /config/dropins/

# Wildfly
#FROM quay.io/wildfly/wildfly:30.0.0.Final-jdk11
#ADD build/libs/myservice.war /opt/jboss/wildfly/standalone/deployments/

# Payara
#FROM payara/micro5.2022.5-jdk11
#CMD ["--deploymentDir", "/opt/payara/deployments", "--noCluster"]
#ADD build/libs/myservice.war /opt/payara/deployments

# TomEE (not working yet)
#FROM tomee:8-jre-8.0.0-M2-microprofile
#COPY build/libs/myservice.war /usr/local/tomee/webapps/
