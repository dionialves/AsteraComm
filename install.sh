#!/bin/bash

set -e # Encerra o script se qualquer comando falhar

# Clonar o repositório principal
echo "Clonando o repositório AsteraComm..."
git clone https://github.com/dionialves/AsteraComm.git
cd AsteraComm

# Baixar e extrair o frontend
echo "Baixando o frontend..."
mkdir -p nginx/www
cd nginx/www
wget https://github.com/dionialves/AsteraComm-frontend/releases/download/v0.1.1/AsteraComm-0.1.1.zip
unzip AsteraComm-0.1.1.zip
rm -f AsteraComm-0.1.1.zip
cd ../../

# Baixar o backend
echo "Baixando o backend..."
mkdir -p asteracomm/app
cd asteracomm/app
wget https://github.com/dionialves/AsteraComm-backend/releases/download/v0.1.2/AsteraComm-0.1.2.jar
mv AsteraComm-*.jar AsteraComm.jar
cd ../..

# Build e up do Docker
echo "Construindo e subindo os containers Docker..."
docker compose build --no-cache
docker compose up -d

echo "✅ Instalação e inicialização concluídas com sucesso!"
