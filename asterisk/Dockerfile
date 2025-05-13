FROM debian:bookworm-slim

ENV DEBIAN_FRONTEND=noninteractive

# Instala dependências
RUN apt-get update && apt-get install -y \
    vim curl wget net-tools iputils-ping dnsutils \
    build-essential unixodbc unixodbc-dev odbc-postgresql \
    libpq-dev libssl-dev libjansson-dev libxml2-dev \
    libedit-dev git pkg-config uuid-dev \
    sqlite3 libsqlite3-dev \
    libxslt1-dev && \
    apt-get clean

# Baixa e instala o Asterisk
RUN wget https://downloads.asterisk.org/pub/telephony/asterisk/asterisk-22-current.tar.gz && \
    tar -xvzf asterisk-22-current.tar.gz && \
    rm -rf asterisk-22-current.tar.gz &&  \
    mv asterisk-* /usr/src/asterisk && \
    cd /usr/src/asterisk && \
    ./configure && \
    make -j$(nproc) && \
    make install && \
    make samples && \
    make config && \
    rm -rf /var/lib/apt/lists/*

# Configura ODBC
COPY odbc.ini /etc/odbc.ini
COPY odbcinst.ini /etc/odbcinst.ini

# Inicia o Asterisk em primeiro plano
CMD ["asterisk", "-f"]

