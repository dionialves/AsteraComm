#!/bin/bash

# =============================================================================
# AsteraComm - Script de Desenvolvimento
# =============================================================================
# Inicia o ambiente de desenvolvimento com hot reload
#
# Uso:
#   ./dev.sh          # Inicia todos os serviços
#   ./dev.sh build    # Reconstrói os containers antes de iniciar
#   ./dev.sh stop     # Para todos os serviços
#   ./dev.sh logs     # Mostra logs de todos os serviços
#   ./dev.sh logs backend   # Mostra logs do backend
#   ./dev.sh logs frontend  # Mostra logs do frontend
# =============================================================================

set -e

COMPOSE_FILE="docker-compose.dev.yml"
PROJECT_NAME="asteracomm-dev"

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_header() {
    echo -e "${BLUE}"
    echo "╔═══════════════════════════════════════════════════════════╗"
    echo "║           AsteraComm - Ambiente de Desenvolvimento        ║"
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

    print_status "Iniciando serviços de desenvolvimento..."

    if [ "$1" == "build" ]; then
        print_status "Reconstruindo containers..."
        sudo docker compose -f $COMPOSE_FILE -p $PROJECT_NAME build --no-cache
    fi

    sudo docker compose -f $COMPOSE_FILE -p $PROJECT_NAME up -d

    echo ""
    print_status "Serviços iniciados com sucesso!"
    echo ""
    echo -e "${BLUE}Acesso aos serviços:${NC}"
    echo "  • Frontend (Astro):    http://localhost:4321"
    echo "  • Backend (API):       http://localhost:8090"
    echo "  • PostgreSQL:          localhost:5432"
    echo "  • Asterisk AMI:        localhost:5038"
    echo ""
    echo -e "${YELLOW}Dicas:${NC}"
    echo "  • Alterações no frontend refletem automaticamente (hot reload)"
    echo "  • Alterações no backend requerem que spring-boot-devtools esteja no pom.xml"
    echo "  • Use './dev.sh logs' para ver os logs"
    echo "  • Use './dev.sh stop' para parar os serviços"
    echo ""
}

stop_services() {
    print_header
    print_status "Parando serviços de desenvolvimento..."
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

show_help() {
    echo "Uso: ./dev.sh [comando]"
    echo ""
    echo "Comandos:"
    echo "  (sem argumento)    Inicia todos os serviços"
    echo "  build              Reconstrói os containers e inicia"
    echo "  stop               Para todos os serviços"
    echo "  logs [serviço]     Mostra logs (opcional: backend, frontend, postgres, asterisk)"
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
help | --help | -h)
    show_help
    ;;
*)
    print_error "Comando desconhecido: $1"
    show_help
    exit 1
    ;;
esac
