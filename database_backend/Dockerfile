
FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace

# Copy dependency descriptors first to maximize Docker layer cache reuse.
COPY pom.xml .

# Pre-fetch Maven dependencies. The cache mount avoids re-downloading dependencies
# on every docker compose build when only source code changes.
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -DskipTests dependency:go-offline

# Copy application source after dependency resolution.
COPY src ./src

# Build the Spring Boot jar using the same Maven cache.
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -DskipTests package

# Normalize the final jar name so the runtime stage does not rely on a wildcard.
RUN set -eux; \
    JAR_FILE="$(find target -maxdepth 1 -type f -name '*.jar' ! -name '*sources.jar' ! -name '*javadoc.jar' | head -n 1)"; \
    test -n "$JAR_FILE"; \
    cp "$JAR_FILE" /workspace/app.jar

FROM eclipse-temurin:17-jre-jammy

RUN apt-get update \
    && apt-get install -y --no-install-recommends bedtools tabix ca-certificates \
    && rm -rf /var/lib/apt/lists/*

ENV BEDTOOLS_BIN=/usr/bin/bedtools

WORKDIR /app

COPY --from=build /workspace/app.jar /app/app.jar

RUN /usr/bin/bedtools --version

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
