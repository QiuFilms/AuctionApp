FROM tomcat:9.0-jdk17

RUN rm -rf /usr/local/tomcat/webapps/*

# Upewnij się, że nazwa pliku .war zgadza się z tym co masz w folderze target
COPY target/mmoauctions-0.0.1-SNAPSHOT.war /usr/local/tomcat/webapps/ROOT.war
COPY server.xml /usr/local/tomcat/conf/server.xml
COPY keystore.p12 /usr/local/tomcat/conf/keystore.p12

# Eksponujemy oba porty: 8080 dla HTTP i 8443 dla HTTPS
EXPOSE 8443

CMD ["catalina.sh", "run"]