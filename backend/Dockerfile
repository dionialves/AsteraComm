# =============================================================================
# AsteraComm Backend - Dockerfile de Produção (Multi-stage Build)
# =============================================================================

# Stage 1: Build
FROM eclipse-temurin:21-jdk AS builder

RUN apt-get update && \
    apt-get install -y maven && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /build

# Copia apenas o pom.xml primeiro para cache de dependências
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copia código fonte e compila
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime (imagem leve, apenas JRE)
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /build/target/AsteraComm.jar ./AsteraComm.jar

EXPOSE 8090

CMD ["java", "-jar", "AsteraComm.jar"]
