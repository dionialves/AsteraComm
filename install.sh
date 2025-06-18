#!/bin/bash

set -e # Encerra o script se qualquer comando falhar

# Clonar o repositório principal
echo "Clonando o repositório AsteraComm..."
git clone https://github.com/dionialves/AsteraComm.git
cd AsteraComm

# Baixar e extrair o frontend
echo "Baixando o frontend..."
mkdir frontend/app
cd frontend/app
git clone https://github.com/dionialves/AsteraComm-frontend.git .
cd ../../

# Baixar o backend
echo "Baixando o backend..."
mkdir -p asteracomm/app
cd asteracomm/app
git clone https://github.com/dionialves/AsteraComm-backend.git .
cd ../..

# Build e up do Docker
echo "Construindo e subindo os containers Docker..."
docker compose build --no-cache
docker compose up -d

echo "✅ Instalação e inicialização concluídas com sucesso!"
