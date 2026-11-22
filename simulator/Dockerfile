FROM maven:3-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:copy-dependencies -DoutputDirectory=target/lib
COPY src ./src
RUN mvn -q compile

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/classes ./classes
COPY --from=build /app/target/lib ./lib
CMD ["java", "-cp", "classes:lib/*", "com.iot.simulator.SimulatorApp"]