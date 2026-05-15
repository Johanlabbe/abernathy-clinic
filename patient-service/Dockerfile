FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY gateway-service/pom.xml gateway-service/
COPY patient-service/pom.xml patient-service/
COPY note-service/pom.xml note-service/
COPY assessment-service/pom.xml assessment-service/
COPY front-end/pom.xml front-end/
RUN mvn -B -pl patient-service -am dependency:go-offline -DskipTests
COPY patient-service/src patient-service/src
RUN mvn -B -pl patient-service -am package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/patient-service/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java","-jar","/app/app.jar"]
