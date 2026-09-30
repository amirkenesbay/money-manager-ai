FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle
COPY core/build.gradle.kts core/
COPY bot/build.gradle.kts bot/
COPY web/build.gradle.kts web/
COPY app/build.gradle.kts app/

RUN --mount=type=secret,id=gpr_user \
    --mount=type=secret,id=gpr_key \
    GITHUB_ACTOR=$(cat /run/secrets/gpr_user) \
    GITHUB_TOKEN=$(cat /run/secrets/gpr_key) \
    ./gradlew --no-daemon :app:dependencies || true

COPY core/src core/src
COPY bot/src bot/src
COPY web/src web/src
COPY app/src app/src

RUN --mount=type=secret,id=gpr_user \
    --mount=type=secret,id=gpr_key \
    GITHUB_ACTOR=$(cat /run/secrets/gpr_user) \
    GITHUB_TOKEN=$(cat /run/secrets/gpr_key) \
    ./gradlew --no-daemon --refresh-dependencies :app:bootJar -x test

FROM eclipse-temurin:21-jre

WORKDIR /app

RUN useradd --system --create-home --shell /bin/false app
USER app

COPY --from=builder /app/app/build/libs/*.jar app.jar

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]