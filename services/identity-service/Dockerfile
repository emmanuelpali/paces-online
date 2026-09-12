FROM eclipse-temurin:25-jdk AS build

WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

RUN chmod +x mvnw \
    && ./mvnw -B dependency:go-offline

COPY src/ src/

RUN ./mvnw -B clean package -DskipTests

FROM eclipse-temurin:25-jre

WORKDIR /app

COPY --from=build --chown=10001:10001 \
    /workspace/target/identity-service-*.jar \
    identity-service.jar

USER 10001:10001

EXPOSE 8080

ENTRYPOINT [ "java", "-jar", "/app/identity-service.jar" ]