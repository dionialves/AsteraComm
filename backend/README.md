# AsteraComm Backend

AsteraComm Backend é a aplicação principal do sistema — responsável pela interface web (Thymeleaf/HTMX) e pela comunicação com o Asterisk via AMI. Gerencia circuitos SIP, troncos, clientes, planos, DIDs e relatórios de chamadas.

## Tecnologias

- **Java 21** + **Spring Boot**
- **Thymeleaf + HTMX** — UI server-side rendering
- **Spring Security** — autenticação via sessão (form login)
- **PostgreSQL** — banco de dados principal
- **Flyway** — migrations de schema
- **JPA/Hibernate** — ORM
- **Maven** — build

## Repositório Principal

- [AsteraComm](https://github.com/dionialves/AsteraComm) — contém backend, infra (Docker, Asterisk, Nginx) e documentação completa.

## Como rodar localmente

Use os scripts na raiz do repositório principal:

```bash
./dev.sh        # Sobe todos os serviços com hot reload
./dev.sh build  # Rebuild + sobe
./dev.sh stop   # Para os serviços
./dev.sh logs backend  # Logs do backend
```

A aplicação estará disponível em `http://localhost:8090`.

### Credenciais padrão

- App: `admin@asteracomm.com` / `admin123`
- DB: `asteracomm` / `asteracomm`

## Profiles Spring

| Profile | Ativado por | Uso |
|---|---|---|
| `dev` | `SPRING_PROFILES_ACTIVE=dev` | Hot reload, cache desabilitado |
| `prod` | `SPRING_PROFILES_ACTIVE=prod` | Imagem compilada, ddl-auto=validate |

## Links úteis

- [Documentação do Asterisk](https://wiki.asterisk.org/)
- [HTMX](https://htmx.org/)
