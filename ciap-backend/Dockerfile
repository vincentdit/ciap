FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -q dependency:go-offline -B
COPY src ./src
RUN mvn -q clean package -DskipTests -B
FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app
RUN groupadd --system ciap && useradd --system --gid ciap ciap
COPY --from=build /build/target/*.jar app.jar
RUN mkdir -p /var/lib/ciap/documents && chown -R ciap:ciap /var/lib/ciap /app
ENV CIAP_STORAGE_ROOT=/var/lib/ciap/documents
USER ciap
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
