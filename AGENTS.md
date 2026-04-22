# AGENTS.md — AsteraComm

> Idioma padrão de interação: **pt-br**. Responda ao usuário sempre em português brasileiro.
>
> O fluxo oficial de trabalho com 3 agentes descrito abaixo. Cada sessão deve operar como um único agente por vez, usando os artefatos do projeto como contrato.

## Projeto
Aplicação web com Spring Boot 4 para gerenciamento do Asterisk PBX. Java 21. UI: Thymeleaf + HTMX + Tailwind CSS (CLI standalone). Banco: PostgreSQL. Migrations: Flyway.

## Build
- **Tailwind CSS:** compilado por binário standalone (`backend/tools/tailwindcss`), não usa Node/npm.
  - Modo watch: `./backend/tailwind-watch.sh`
  - Build do CSS está acoplado ao Maven na fase `generate-resources` via `exec-maven-plugin`.
- **Maven:** `mvn clean package -DskipTests` (produção sempre ignora tests; CI deploy constrói via Docker).

## Execução / Desenvolvimento
- **Ambiente dev depende do Docker:** `./dev.sh [build|rebuild [serviço]|stop|logs [serviço]]`
  - Sobe PostgreSQL, Asterisk e backend com hot reload via `spring-boot-devtools` + `entrypoint-dev.sh`.
  - Alterações no código Java/config são detectadas e recompiladas automaticamente dentro do container; não é necessário reiniciar manualmente.
  - App: `http://localhost:8090` | DB: `localhost:5432` | AMI: `localhost:5038`
- **Produção:** `./prod.sh [build|stop|logs [serviço]|status]` (usa `docker-compose.yml` com NGINX).

## Testes
- Testes são **unitários com JUnit 5 + Mockito** (`@ExtendWith(MockitoExtension.class)`). Não há infraestrutura de testes de integração fora do Docker.
- `AsteraConnectApplicationTests.java` é o único `@SpringBootTest`.
- Filtro padrão do Maven: `mvn test -Dtest=NomeDaClasse`.

## Arquitetura
- **Fonte principal:** `backend/src/main/java/com/dionialves/asteracomm/`
- **Testes:** `backend/src/test/java/com/dionialves/asteracomm/`
- **Migrations Flyway:** `backend/src/main/resources/db/migration/`
- **Templates Thymeleaf:** `backend/src/main/resources/templates/`
- **Assets estáticos (CSS):** `backend/src/main/resources/static/css/`
- **Config Asterisk:** `asterisk/config/` | **Volume dialplan gerado:** `dialplan-generated`

## Estilo e Convenções
- Estilo de commits (Codificador): Conventional Commits simplificado.
  - Padrão: `tipo(código): titulo-curto`
  - **Tipo:** `feat` (User Story), `fix` (Bug Fix), `refactor` (Refactor), `chore` (tarefa), `docs` (documentação)
  - **Código:** identificador da task entre parênteses — `(us-xxx)`, `(rf-xxx)`, `(fix-xxx)`
  - **Título curto:** descrição em português do que foi feito
  - Exemplos: `feat(us-067): adiciona monitoramento de troncos`, `fix(fix-073): corrige persistência de filtro de grupo`
- Não altere versão no `pom.xml`, crie tags nem faça push sem confirmação explícita do usuário.

## Fluxo de Trabalho entre Agentes (Arquiteto → Codificador → Revisor)
Cada task segue um ciclo de três agentes com responsabilidades mutuamente exclusivas:

```
┌───────────┐   task em    ┌──────────────┐   commit +   ┌───────────┐
│ Arquiteto │ ── doc/ ───▶ │ Codificador  │ ── docs ──▶ │  Revisor  │
│  (plano)  │   BACKLOG.md │ (execução)   │  att.      │ (parecer) │
└─────┬─────┘              └──────┬───────┘            └─────┬─────┘
      ▲                           │                          │
      │  (replano)                │  (dúvida / lacuna do    │
      └───────────────────────────┴──────────────────────────┘
                        plano)         devoluções possíveis
```

| Agente | Pode | Não pode |
|---|---|---|
| **Arquiteto** | Ler todo o código; registrar/remover task em `doc/BACKLOG.md`; planejar em detalhes | Editar código de produção (`backend/src/**`, `pom.xml`, templates, etc.) |
| **Codificador** | Editar código e testes conforme o plano; atualizar docs de task | Desviar do plano; refatorar fora do escopo; decidir arquitetura; aprovar próprio trabalho |
| **Revisor** | Ler todo o código e docs; rodar `mvn test`; emitir parecer | Editar código de produção; alterar o plano |

### Etapas
1. **Arquiteto planeja**
   - Classifica a demanda como `US-xxx`, `RF-xxx` ou `FIX-xxx`.
   - Investiga o código-fonte afetado.
   - Registra a task completa em `doc/BACKLOG.md` com: Contexto/Problema, Abordagem, Passo-a-passo detalhado, Testes a criar/atualizar, Critérios de aceitação, Riscos.
   - Entrega ao Codificador.
2. **Codificador executa**
   - Lê a task em `doc/BACKLOG.md`.
   - Valida se o plano é executável — se não for, devolve ao Arquiteto com o ponto exato da falha.
   - Executa os passos na ordem exata, sem inventar ou melhorar nada.
   - Compila incrementalmente (`mvn compile`).
   - Ao fim, roda `mvn test` inteiro — precisa estar verde.
   - Atualiza docs: remove do `doc/BACKLOG.md`, adiciona em `[Unreleased]` no `doc/CHANGELOG.md`, detalha em `doc/release-notes/unreleased.md`.
   - Cria commit no padrão `tipo(código): titulo-curto`.
   - Entrega ao Revisor.
3. **Revisor audita**
   - Lê a task original, o diff do commit e as atualizações de docs.
   - Audita em 3 camadas:
     - **A.** Aderência ao plano — passos 1-a-1, migrations, testes previstos.
     - **B.** Qualidade técnica — qualidade de código, acoplamento, legibilidade, segurança, testes.
     - **C.** Critérios de aceitação — evidência objetiva para cada item.
   - Emite parecer:
     - `APROVADO` — fecha o ciclo; recomendações viram novas tasks `RF-xxx`.
     - `AJUSTES NECESSÁRIOS` — devolve ao Codificador com lista numerada.
     - `BLOQUEADO` — devolve ao Arquiteto (falha de segurança, violação de proibição, plano inviável).
   - O parecer é entregue ao usuário (PO).

### Regras do ciclo
- Uma task = um ciclo completo. Nenhuma task muda de estágio sem passar pelo agente anterior.
- Apenas o Arquiteto registra e reescreve tasks em `doc/BACKLOG.md`.
- Apenas o Codificador altera `backend/src/**`, `pom.xml`, templates, static, properties.
- Apenas o Revisor emite o parecer de aprovação.
- Devolução segue a origem do problema:
  - Falha de execução (passo errado, teste não criado, etc.) → Codificador.
  - Falha de planejamento (passo ambíguo, critério não verificável, plano inviável) → Arquiteto.
- Critérios de aceitação são contrato. Só o Arquiteto pode reescrevê-los.
- Recomendações não-bloqueantes do Revisor viram novas tasks `RF-xxx` criadas pelo Arquiteto — nunca são "anexadas" ao commit em revisão.
- Proibições do projeto (ver Checklist) são bloqueios automáticos na revisão.
- `Definition of Done` é o piso do Revisor: todos os itens precisam ser satisfeitos para um `APROVADO`.
- Rodar tudo em uma sessão única contamina as fronteiras — só é aceitável para protótipos descartáveis. Cada sessão parte de contexto limpo, lê o artefato que precisa e entrega o próximo.

## Fluxo Completo de uma Task
1. **CRIAR** → Arquiteto adiciona ao `doc/BACKLOG.md` com número e descrição completa; aguardar aprovação do usuário para iniciar execução.
2. **EXECUTAR** → Codificador desenvolve seguindo o plano aprovado (testes → código → refatoração).
3. **REVISAR** → Revisor audita o entregue e emite parecer (APROVADO / AJUSTES / BLOQUEADO).
4. **CONCLUIR** → Após `APROVADO`: remover do `doc/BACKLOG.md`; adicionar `TIPO-### · Título` em `[Unreleased]` no `doc/CHANGELOG.md`; adicionar descrição detalhada em `doc/release-notes/unreleased.md`.
5. **PUBLICAR** → Após o usuário solicitar explicitamente: renomear `doc/release-notes/unreleased.md` → `vX.Y.Z.md`; criar novo `unreleased.md` vazio; substituir `[Unreleased]` por `[X.Y.Z] - DATA` no `doc/CHANGELOG.md`.

## Criação de Versão (Release)
Quando o usuário solicitar "cria versão X.Y.Z":
1. Atualizar `<version>` no `pom.xml` (ex.: `1.2.0`, sem prefixo `v`)
2. Mover `[Unreleased]` do `doc/CHANGELOG.md` para `[X.Y.Z] - YYYY-MM-DD`
3. Criar `doc/release-notes/vX.Y.Z.md` com detalhamento técnico dos itens
4. Commitar: `chore: bump version to X.Y.Z`
5. Criar tag: `git tag vX.Y.Z`
6. Push: `git push && git push --tags`

## Documentação do Projeto
| Arquivo | Propósito |
|---|---|
| `doc/BACKLOG.md` | Itens pendentes (Features, Refactoring, Bug Fixes) |
| `doc/CHANGELOG.md` | Histórico resumido de versões |
| `doc/release-notes/vX.Y.Z.md` | Detalhamento técnico por versão |
| `doc/ROADMAP.md` | Visão de longo prazo do produto |

## Checklist do Agente — Antes de Implementar (Codificador)
1. Ler a task inteira em `doc/BACKLOG.md`, `doc/CHANGELOG.md` e este `AGENTS.md`.
2. Rodar `mvn test` (ou `mvn test -Dtest=NomeDaClasse`) antes de modificar qualquer coisa para ter baseline.
3. Criar migration Flyway **antes** da entidade JPA. Nunca usar `ddl-auto=update` ou `create` em nenhum perfil.
4. Garantir token CSRF em todos os formulários Thymeleaf (`<form th:action>` ou `<input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}"/>`).
5. Validação de entrada: Jakarta Validation em DTOs.

## Definition of Done (Simples)
Uma US/RF/FIX só é considerada concluída quando **todos** forem verdadeiros. Este é o piso mínimo do Revisor:

1. Código compila sem warnings do compilador.
2. `mvn test` passa (inclui novos testes da task).
3. Não há `TODO`/`FIXME` sem issue correspondente.
4. Migrations aplicam-se do zero em banco limpo.
5. Commits criados com mensagens no padrão `tipo(código): titulo-curto`.

## Versionamento Semântico
O projeto segue SemVer: `MAJOR.MINOR.PATCH`

| Componente | Quando incrementar |
|---|---|
| `MAJOR` | Mudança incompatível com versões anteriores |
| `MINOR` | Nova funcionalidade compatível (`US`) |
| `PATCH` | Correção de bug (`FIX`) ou refactor sem impacto funcional |

## Deploy
- A GitHub Action `.github/workflows/deploy.yml` dispara no push para `main`, acessa o servidor de produção via SSH e executa `./prod.sh build`.

## Anti-padrões a evitar

- ❌ Arquiteto escrevendo código de produção "só para acelerar".
- ❌ Codificador "corrigindo" o plano em silêncio no commit.
- ❌ Codificador fazendo cleanup / refactor oportunista fora do escopo da task.
- ❌ Revisor aprovando sem rodar `mvn test`.
- ❌ Revisor reprovando com base em gosto pessoal, sem apontar arquivo/linha ou critério objetivo.
- ❌ Devolução para o agente errado (ex.: mandar Codificador corrigir algo que é falha do plano).
- ❌ Task iniciada sem número (`US-xxx`, `FIX-xxx`, `RF-xxx`).
- ❌ Commit tocando código fora do escopo listado na task.
