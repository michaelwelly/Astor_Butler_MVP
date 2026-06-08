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

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl ffmpeg python3 python3-pip \
    && pip3 install --break-system-packages --no-cache-dir faster-whisper==1.1.1 requests \
    && rm -rf /var/lib/apt/lists/*

# Опциональные JVM-флаги (безопасно для контейнера)
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseG1GC" \
    TZ=UTC

# Имя артефакта проекта сейчас astor-butler-<version>.jar.
COPY --from=build /workspace/target/*.jar /app/app.jar
COPY scripts/stt_faster_whisper.py /app/stt_faster_whisper.py

EXPOSE 8088
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
