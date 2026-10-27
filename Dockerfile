FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY Proyecto1 ./Proyecto1
RUN mvn -B -ntp -f Proyecto1/pom.xml verify

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/Proyecto1/target/Proyecto1-0.0.1-SNAPSHOT.jar app.jar
RUN mkdir -p /app/runtime-data
VOLUME ["/app/runtime-data"]
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
