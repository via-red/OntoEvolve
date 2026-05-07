FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

COPY onto-domain-education/target/*.jar app.jar
COPY data/student-data data/student-data

EXPOSE 8088

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD wget -qO- http://localhost:8088/actuator/health || exit 1

CMD java -jar app.jar
