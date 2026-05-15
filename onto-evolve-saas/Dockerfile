# Single-stage: expects pre-built JAR from local mvn package
FROM eclipse-temurin:17-jre-jammy
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY target/onto-evolve-saas-*.jar app.jar
EXPOSE 8090
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD curl -f http://localhost:8090/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
