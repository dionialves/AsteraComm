# AsteraComm

Plataforma web de gerenciamento e monitoramento do [Asterisk PBX](https://www.asterisk.org/), desenvolvida como projeto de portfólio.

## Tecnologias

| Camada | Tecnologia |
|--------|-----------|
| Backend | Java 21 + Spring Boot 4 + Thymeleaf + HTMX |
| Estilização | Tailwind CSS (standalone CLI) |
| Banco de dados | PostgreSQL 18 |
| Telefonia | Asterisk 22 (ODBC → PostgreSQL para CDR/CEL) |
| Proxy reverso | NGINX (produção) |
| Containerização | Docker + Docker Compose |

## Arquitetura

```
NGINX (porta 80 — produção)
  /*  →  backend:8090

Backend (Spring Boot — porta 8090)
  Thymeleaf templates + HTMX
  ↕ AMI (porta 5038)
Asterisk 22
  ↕ ODBC
PostgreSQL 18 (porta 5432)
```

Em desenvolvimento o NGINX não é utilizado — acesse diretamente `http://localhost:8090`.

## Desenvolvimento

```bash
./dev.sh          # Inicia todos os serviços (backend + postgres + asterisk)
./dev.sh build    # Reconstrói os containers antes de iniciar
./dev.sh stop     # Para todos os serviços
./dev.sh logs [serviço]  # Acompanha logs (serviços: backend, postgres, asterisk)
```

Para recompilar o CSS em modo watch:
```bash
./backend/tailwind-watch.sh
```

Acesso após iniciar:
- Aplicação: http://localhost:8090
- PostgreSQL: localhost:5432
- Asterisk AMI: localhost:5038

Credenciais padrão: `admin@asteracomm.com` / `admin123`

## Produção

```bash
./prod.sh         # Inicia todos os serviços
./prod.sh build   # Reconstrói as imagens
./prod.sh stop    # Para os serviços
./prod.sh status  # Status dos containers
```

Acesso após iniciar: http://localhost

## Estrutura do repositório

```
AsteraComm/
├── backend/        # Spring Boot — código fonte, templates, CSS
├── asterisk/       # Dockerfile + configurações do Asterisk
├── nginx/          # Configuração do NGINX (produção)
├── postgres/       # Scripts de init do banco
├── doc/            # Documentação do projeto (backlog, changelog, roadmap)
├── docker-compose.yml      # Produção
├── docker-compose.dev.yml  # Desenvolvimento
├── dev.sh
└── prod.sh
```

## Documentação

- [Backlog de migração](doc/BacklogMigrationsThymeleaf.md)
- [Changelog](doc/CHANGELOG.md)
- [Roadmap](doc/ROADMAP.md)
- [User Stories](doc/UserStory.md)
