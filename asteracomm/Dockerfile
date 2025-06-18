FROM debian:bookworm-slim

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update && \
    apt-get install -y wget gnupg2 ca-certificates vim tar maven && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

ENV JAVA_VERSION=21.0.2
ENV JAVA_HOME=/opt/java/openjdk
ENV PATH="$JAVA_HOME/bin:$PATH"

RUN mkdir -p /opt/java && \
    wget -q https://download.java.net/java/GA/jdk21.0.2/f2283984656d49d69e91c558476027ac/13/GPL/openjdk-21.0.2_linux-x64_bin.tar.gz && \
    tar -xzf openjdk-21.0.2_linux-x64_bin.tar.gz -C /opt/java && \
    rm openjdk-21.0.2_linux-x64_bin.tar.gz && \
    mv /opt/java/jdk-$JAVA_VERSION $JAVA_HOME

EXPOSE 8090

WORKDIR /app
CMD ["sh", "-c", "mvn clean package && java -jar target/AsteraComm.jar"]
