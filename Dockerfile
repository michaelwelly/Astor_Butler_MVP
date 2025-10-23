# ========= BUILD =========
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Кэшируем зависимости
COPY mvnw ./
COPY .mvn .mvn
COPY pom.xml .
RUN ./mvnw -B -DskipTests dependency:go-offline

# Собираем jar
COPY src src
RUN ./mvnw -B -DskipTests package

# ========= RUN =========
FROM eclipse-temurin:21-jre
WORKDIR /app

# Опциональные JVM-флаги (безопасно для контейнера)
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseG1GC" \
    TZ=UTC

# Имя артефакта — любой *-SNAPSHOT.jar / *-RELEASE.jar
COPY --from=build /workspace/target/*-SNAPSHOT.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]