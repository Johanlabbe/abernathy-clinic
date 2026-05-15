FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY gateway-service/pom.xml gateway-service/
COPY patient-service/pom.xml patient-service/
COPY note-service/pom.xml note-service/
COPY assessment-service/pom.xml assessment-service/
COPY front-end/pom.xml front-end/
RUN mvn -B -pl front-end -am dependency:go-offline -DskipTests
COPY front-end/src front-end/src
RUN mvn -B -pl front-end -am package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/front-end/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java","-jar","/app/app.jar"]
