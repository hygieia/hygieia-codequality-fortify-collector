## Due to lack of activity, this repo is currently not being supported and is archived as of 07/06/22. Please note since archival, the project is not maintained and will be available in a read-only state. Please reach out to hygieia2@capitalone.com should you have any questions.
# Hygieia Security Collectors / Fortify

Fortify finds software issues of several kinds; however, fortify-collector is only intended to gather _security issues_.

This project uses Spring Boot to package the collector as an executable JAR with dependencies.

## Building and Deploying

Run the following command to package the collector into an executable jar. Find the jar file at `target/`.
```
mvn clean install package
```

Copy this file to your server and launch it using:
```
java -jar fortify-api-collector-3.0.0-SNAPSHOT.jar --spring.config.location=[path to application.properties file]
```

## application.properties

You will need to provide an **application.properties** file that contains information about how to connect to the Dashboard MongoDB database instance, as well as properties that Fortify collector requires. See the Spring Boot [documentation](http://docs.spring.io/spring-boot/docs/current-SNAPSHOT/reference/htmlsingle/#boot-features-external-config-application-property-files) for information about sourcing this properties file.

### Sample application.properties file

```properties
# Database Name
dbname=dashboard

# Database HostName - default is localhost
dbhost=10.0.1.1

# Database Port - default is 27017
dbport=27017

# Database Username - default is blank
dbusername=db

# Database Password - default is blank
dbpassword=dbpass


# Collector schedule (required)
fortify.cron=0 0/5 * * * *
```
