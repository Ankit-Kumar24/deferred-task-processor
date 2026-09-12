FROM maven:3.9.11-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B package -DskipTests

WORKDIR /layers
RUN java -Djarmode=tools \
    -jar /workspace/target/*.jar extract --layers --launcher --destination /layers

FROM eclipse-temurin:21-jre-jammy AS runtime

RUN useradd --system --create-home --uid 10001 chronos
USER chronos
WORKDIR /app

COPY --from=build --chown=chronos:chronos /layers/dependencies/ ./
COPY --from=build --chown=chronos:chronos /layers/spring-boot-loader/ ./
COPY --from=build --chown=chronos:chronos /layers/snapshot-dependencies/ ./
COPY --from=build --chown=chronos:chronos /layers/application/ ./

EXPOSE 8080

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
