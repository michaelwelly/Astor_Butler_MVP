# ========= BUILD =========
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Кэшируем зависимости
COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline

# Собираем jar
COPY src src
RUN mvn -B -DskipTests package

# ========= RUN =========
FROM eclipse-temurin:21-jre
WORKDIR /app

# Опциональные JVM-флаги (безопасно для контейнера)
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseG1GC" \
    TZ=UTC

# Имя артефакта проекта сейчас astor-butler-<version>.jar.
COPY --from=build /workspace/target/*.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
