# syntax=docker/dockerfile:1.7

FROM maven:3.9.11-amazoncorretto-25 AS build
WORKDIR /workspace

COPY pom.xml /workspace/
COPY libs/common-domain/pom.xml /workspace/libs/common-domain/
COPY libs/common-security/pom.xml /workspace/libs/common-security/
COPY services/catalog-service/pom.xml /workspace/services/catalog-service/

RUN mvn -pl services/catalog-service -am dependency:go-offline

COPY libs /workspace/libs
COPY services/catalog-service /workspace/services/catalog-service

RUN mvn -pl services/catalog-service -am package -DskipTests

FROM amazoncorretto:25-alpine
WORKDIR /app

COPY --from=build /workspace/services/catalog-service/target/catalog-service-*.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
