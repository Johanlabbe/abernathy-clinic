FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY gateway-service/pom.xml gateway-service/
COPY patient-service/pom.xml patient-service/
COPY note-service/pom.xml note-service/
COPY assessment-service/pom.xml assessment-service/
COPY front-end/pom.xml front-end/
RUN mvn -B -pl assessment-service -am dependency:go-offline -DskipTests
COPY assessment-service/src assessment-service/src
RUN mvn -B -pl assessment-service -am package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/assessment-service/target/*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java","-jar","/app/app.jar"]
