#!/bin/bash

# =============================================================================
# AsteraComm - Script de Produção
# =============================================================================
# Gerencia o ambiente de produção com Docker Compose.
# Imagens contêm artefatos compilados (sem volumes de código-fonte).
#
# Uso:
#   ./prod.sh              # Inicia todos os serviços
#   ./prod.sh build        # Reconstrói as imagens e inicia
#   ./prod.sh stop         # Para todos os serviços
#   ./prod.sh logs         # Mostra logs de todos os serviços
#   ./prod.sh logs backend # Mostra logs do backend
#   ./prod.sh status       # Mostra status dos containers
#   ./prod.sh help         # Mostra esta ajuda
# =============================================================================

set -e

COMPOSE_FILE="docker-compose.yml"
PROJECT_NAME="asteracomm"

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_header() {
    echo -e "${BLUE}"
    echo "╔═══════════════════════════════════════════════════════════╗"
    echo "║            AsteraComm - Ambiente de Produção              ║"
    echo "╚═══════════════════════════════════════════════════════════╝"
    echo -e "${NC}"
}

print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[AVISO]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERRO]${NC} $1"
}

check_docker() {
    if ! command -v docker &>/dev/null; then
        print_error "Docker não está instalado. Por favor, instale o Docker primeiro."
        exit 1
    fi

    if ! sudo docker info &>/dev/null; then
        print_error "Docker não está rodando. Por favor, inicie o Docker."
        exit 1
    fi
}

check_submodules() {
    if [ ! -f "backend/pom.xml" ] || [ ! -f "frontend/package.json" ]; then
        print_warning "Submodules não inicializados. Inicializando..."
        git submodule update --init --recursive
    fi
}

start_services() {
    print_header
    check_docker
    check_submodules

    print_status "Iniciando serviços de produção..."

    if [ "$1" == "build" ]; then
        print_status "Reconstruindo imagens (isso pode levar alguns minutos)..."
        sudo docker compose -f $COMPOSE_FILE -p $PROJECT_NAME build --no-cache
    fi

    sudo docker compose -f $COMPOSE_FILE -p $PROJECT_NAME up -d

    echo ""
    print_status "Serviços iniciados com sucesso!"
    echo ""
    echo -e "${BLUE}Acesso aos serviços:${NC}"
    echo "  • Aplicação (NGINX):   http://localhost"
    echo "  • Frontend (direto):   http://localhost:4321"
    echo "  • Backend API:         http://localhost:8090"
    echo "  • API via NGINX:       http://localhost/api/"
    echo "  • PostgreSQL:          localhost:5432"
    echo "  • Asterisk AMI:        localhost:5038"
    echo ""
    echo -e "${YELLOW}Dicas:${NC}"
    echo "  • Use './prod.sh logs' para ver os logs"
    echo "  • Use './prod.sh status' para ver o estado dos containers"
    echo "  • Use './prod.sh stop' para parar os serviços"
    echo "  • Use './prod.sh build' para reconstruir as imagens após mudanças no código"
    echo ""
}

stop_services() {
    print_header
    print_status "Parando serviços de produção..."
    sudo docker compose -f $COMPOSE_FILE -p $PROJECT_NAME down
    print_status "Serviços parados."
}

show_logs() {
    if [ -n "$1" ]; then
        sudo docker compose -f $COMPOSE_FILE -p $PROJECT_NAME logs -f "$1"
    else
        sudo docker compose -f $COMPOSE_FILE -p $PROJECT_NAME logs -f
    fi
}

show_status() {
    print_header
    print_status "Status dos containers:"
    echo ""
    sudo docker compose -f $COMPOSE_FILE -p $PROJECT_NAME ps
}

show_help() {
    print_header
    echo "Uso: ./prod.sh [comando]"
    echo ""
    echo "Comandos:"
    echo "  (sem argumento)    Inicia todos os serviços"
    echo "  build              Reconstrói as imagens e inicia"
    echo "  stop               Para todos os serviços"
    echo "  logs [serviço]     Mostra logs (opcional: backend, frontend, postgres, asterisk, nginx)"
    echo "  status             Mostra status dos containers"
    echo "  help               Mostra esta ajuda"
    echo ""
}

# Main
case "${1:-start}" in
start)
    start_services
    ;;
build)
    start_services "build"
    ;;
stop)
    stop_services
    ;;
logs)
    show_logs "$2"
    ;;
status)
    show_status
    ;;
help | --help | -h)
    show_help
    ;;
*)
    print_error "Comando desconhecido: $1"
    show_help
    exit 1
    ;;
esac
