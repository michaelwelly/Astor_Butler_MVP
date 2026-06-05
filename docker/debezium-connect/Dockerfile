FROM maven:3.9-eclipse-temurin-21 AS avro-converter

WORKDIR /build
COPY pom.xml .
RUN mvn -q dependency:copy-dependencies \
    -DincludeScope=runtime \
    -DoutputDirectory=/build/confluent-avro

FROM debezium/connect:2.7.3.Final

COPY --from=avro-converter /build/confluent-avro /kafka/connect/confluent-avro
