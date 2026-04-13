FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

ARG GPR_USER
ARG GPR_KEY
ENV GITHUB_ACTOR=${GPR_USER}
ENV GITHUB_TOKEN=${GPR_KEY}

COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle

RUN ./gradlew --no-daemon dependencies || true

COPY src ./src

RUN ./gradlew --no-daemon bootJar -x test

FROM eclipse-temurin:21-jre

WORKDIR /app

RUN useradd --system --create-home --shell /bin/false app
USER app

COPY --from=builder /app/build/libs/*.jar app.jar

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]