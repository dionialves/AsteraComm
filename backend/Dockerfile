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

# Copia código fonte, config Tailwind e binário
COPY src ./src
COPY tailwind.config.js .
COPY tools/tailwindcss ./tools/tailwindcss
RUN chmod +x ./tools/tailwindcss

# Gera o CSS antes do build Maven (output vai para static/css/ dentro do JAR)
RUN ./tools/tailwindcss \
    -i src/main/resources/static/css/input.css \
    -o src/main/resources/static/css/output.css \
    --minify

# Compila o projeto
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime (imagem leve, apenas JRE)
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /build/target/AsteraComm.jar ./AsteraComm.jar

EXPOSE 8090

CMD ["java", "-jar", "AsteraComm.jar"]
