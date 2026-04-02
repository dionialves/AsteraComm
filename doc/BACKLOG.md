# Backlog — AsteraComm

## Índice

### Features (US)
1. [US-011 — Fatura mensal por circuito (Invoice)](#us-011)
2. [US-013 — Refatoração: múltiplos DIDs por circuito e seleção de CallerID](#us-013)
3. [US-017 — Snapshot de estado do circuito, DID e plano no processamento da ligação](#us-017)
4. [US-037 — Adicionar campo `linked_at` ao DID](#us-037)
5. [US-054 — Criar circuito a partir do modal de cliente](#us-054)
6. [US-065 — Relatório: clientes sem circuitos vinculados](#us-065)
7. [US-078 — Controle de Firewall por IP](#us-078)
8. [US-079 — Integração com IXC Soft — Relatório de divergências](#us-079)

### Refactoring (RF)
1. [US-012 — Reorganização de pacotes em `domain/`](#us-012)
2. [US-066 — Menu lateral com seção "Operacional" e relatórios como links diretos](#us-066)
3. [RF-075 — Paridade de funcionalidades entre `dev.sh` e `prod.sh`](#rf-075)

---

### RF-075

**Titulo:** Paridade de funcionalidades entre `dev.sh` e `prod.sh`

**Descrição:**
Como desenvolvedor, quero que os scripts `dev.sh` e `prod.sh` tenham as mesmas funcionalidades disponíveis, e que ambos suportem parada de serviço individual, para simplificar a operação do ambiente sem precisar lembrar qual comando existe em qual script.

**Estimativa:** 1 story point

**Critérios de Aceite:**

1. **`prod.sh rebuild [serviço]`:** comando adicionado com o mesmo comportamento do `dev.sh` — para o serviço, remove o container, reconstrói sem cache e reinicia.
2. **`dev.sh status`:** comando adicionado exibindo o estado dos containers via `docker compose ps`.
3. **`stop [serviço]` em ambos:** quando um serviço for informado (`./dev.sh stop backend`), para apenas aquele container (`docker compose stop <serviço>`); sem argumento, mantém o comportamento atual (para tudo).
4. **`help` atualizado:** ambos os scripts refletem os novos comandos na saída de ajuda.
5. **Sem regressão:** comandos existentes (`start`, `build`, `logs`, `stop` sem argumento) continuam funcionando identicamente.

---

## Bug Fixes (FIX)
1. [FIX-073 — Group filter da página de Circuitos não permanece ativo após modificação de página](#fix-073)
2. [FIX-074 — Status de tronco com autenticação por IP não exibido corretamente](#fix-074)
3. [FIX-076 — Calls sem circuito associado para ligações entrantes](#fix-076)
4. [FIX-077 — Reconstruir circuitos excluídos e corrigir calls órfãs (Getel Telecom)](#fix-077)

---

## Features (US)

### US-011

**Titulo:** Fatura mensal por circuito (Invoice)

**Descrição:**
Como administrador, quero que cada circuito gere automaticamente uma fatura mensal, podendo visualizar a fatura em andamento como um resumo parcial e as faturas fechadas em uma lista identificada pelo mês, contendo o nome do cliente, o plano, e as ligações agrupadas por tipo de destino.

**Estimativa:** 5 story points

**Critérios de Aceite:**

1. **Campo `closing_day` no circuito:** O circuito passa a ter o campo `closing_day` (inteiro, obrigatório, valores de 1 a 28), que define o dia do mês em que a fatura é fechada.
2. **Geração automática:** Ao ser processada a primeira ligação de um novo período, uma fatura com status `OPEN` é criada automaticamente para o circuito, caso ainda não exista.
3. **Fechamento automático:** Um job agendado verifica diariamente os circuitos cujo `closing_day` corresponde ao dia atual e fecha as faturas `OPEN` do período, alterando o status para `CLOSED`.
4. **Fatura em andamento:** Enquanto `OPEN`, é possível acessar um endpoint que retorna o resumo parcial da fatura com todas as ligações já registradas no período.
5. **Lista de faturas fechadas:** Endpoint que retorna as faturas `CLOSED` de um circuito, identificadas pelo mês/ano de referência (ex.: `"Fevereiro 2026"`).
6. **Conteúdo da fatura:** A fatura exibe:
   - Nome do cliente vinculado ao circuito.
   - Nome do plano.
   - Ligações agrupadas por **tipo de destino**: Fixo Local, Fixo DDD, Móvel Local, Móvel DDD, Internacional — cada grupo com a lista de ligações (data/hora, destino, duração, valor) e subtotal do grupo.
   - Total geral de minutos consumidos e valor total da fatura.
7. **Classificação do tipo:** O tipo de destino é determinado pelo número discado (ex.: DDDs 11–99 para fixo/móvel local vs. DDD diferente, prefixo `00xx` para longa distância, `+` ou `00` seguido de código de país para internacional).
8. **Testes:** Testes unitários e de integração cobrem: criação automática de fatura, agrupamento por tipo de destino, fechamento pelo `closing_day` e consulta de fatura em andamento.

---

### US-013

**Titulo:** Refatoração: múltiplos DIDs por circuito e seleção de CallerID

**Descrição:**
Como administrador, quero que um circuito possa ter mais de um DID associado, e que na tela de edição do circuito eu possa escolher qual DID será utilizado nas chamadas saintes, preenchendo automaticamente o campo `callerid` com o número do DID selecionado. Ao vincular o primeiro DID ao circuito, ele deve ser automaticamente definido como `callerid`.

**Estimativa:** 3 story points

**Critérios de Aceite:**

1. **Relacionamento 1-N:** A entidade `DID` passa a ter uma FK opcional para `Circuit` (`circuit_id`), substituindo o vínculo anterior (se houver). Um circuito pode ter N DIDs; um DID pertence a no máximo um circuito.
2. **DID ativo (callerid):** O circuito possui um campo `active_did_id` (FK para `DID`, nullable) que indica qual DID é o CallerID atual para chamadas saintes.
3. **Preenchimento automático do `callerid`:** Sempre que `active_did_id` for alterado, o campo `callerid` do circuito é atualizado com o número do DID correspondente.
4. **Primeiro DID automático:** Ao vincular o primeiro DID a um circuito (que ainda não possui `active_did_id`), esse DID é automaticamente definido como `active_did_id` e o `callerid` é preenchido.
5. **Tela de edição do circuito:** Exibe a lista de DIDs associados ao circuito e um seletor para escolher o DID ativo. Ao selecionar, o campo `callerid` é atualizado visualmente e salvo no backend.
6. **API:**
   - `PATCH /api/v1/circuits/{id}/active-did` — altera o DID ativo do circuito.
   - `GET /api/v1/circuits/{id}` — retorna a lista de DIDs vinculados e o `active_did_id`.
7. **Testes:** Testes unitários cobrem: vínculo do primeiro DID (auto-set), troca de DID ativo e atualização do `callerid`.

---

### US-017

**Titulo:** Snapshot de estado do circuito, DID e plano no processamento da ligação

**Descrição:**
Como desenvolvedor, quero que cada ligação processada registre um snapshot dos dados relevantes do circuito, DID e plano vigentes no momento do processamento, para que auditorias futuras não dependam do estado atual dessas entidades, que podem ter sido alteradas desde então.

**Estimativa:** 2 story points

**Critérios de Aceite:**

1. **Campos de snapshot em `Call`:** A entidade `Call` passa a armazenar, no momento do processamento, os seguintes dados desnormalizados:
   - Do **plano**: nome, valor da franquia de minutos (se houver), tarifas por tipo de destino (fixo local, fixo DDD, móvel local, móvel DDD, internacional).
   - Do **circuito**: nome, `closing_day`.
   - Do **DID**: número.
2. **Preenchimento automático:** Ao processar uma ligação, o serviço de billing preenche os campos de snapshot com os valores vigentes naquele instante. Nenhuma ação manual é necessária.
3. **Imutabilidade:** Os campos de snapshot nunca são atualizados após a criação do registro — alterações posteriores no plano, circuito ou DID não afetam chamadas já registradas.
4. **Ferramenta de auditoria (US-016):** A ferramenta de auditoria passa a utilizar os dados do snapshot armazenados em `Call` para os cálculos, em vez de buscar o estado atual do plano/circuito.
5. **Migração:** Uma migração Flyway adiciona as novas colunas à tabela de calls, com valor `NULL` para registros históricos (aceito para chamadas anteriores à feature).
6. **Testes:** Testes unitários cobrem: snapshot preenchido corretamente no processamento, imutabilidade após alteração do plano, e que a auditoria usa os dados do snapshot e não os valores atuais.

---

### US-037

**Titulo:** Adicionar campo `linked_at` ao DID

**Descrição:**
Como administrador, quero que cada DID registre a data em que foi vinculado a um circuito, para que a página de detalhe do circuito exiba a coluna "Vinculado em" com a data correta.

**Estimativa:** 1 story point

**Critérios de Aceite:**

1. **Campo `linked_at`:** A entidade `DID` passa a ter o campo `linked_at` (timestamp, nullable), preenchido automaticamente com a data/hora atual sempre que o DID é vinculado a um circuito (`circuit_id` atribuído).
2. **Limpeza ao desvincular:** Ao desvincular o DID (circuito removido), `linked_at` é zerado (`NULL`).
3. **Migração:** Uma migração Flyway adiciona a coluna `linked_at` à tabela de DIDs, com valor `NULL` para registros existentes.
4. **API:** O endpoint `GET /api/v1/dids/by-circuit/{circuitId}` retorna o campo `linkedAt` no JSON de cada DID.
5. **Frontend:** Nenhuma alteração necessária — a página de detalhe do circuito já usa `did.linkedAt` na coluna "Vinculado em".

---

### US-054

**Titulo:** Criar circuito a partir do modal de cliente

**Descrição:**
Como administrador, quero poder criar um novo circuito diretamente pela aba "Circuitos" do modal de cliente, com o vínculo ao cliente já preenchido automaticamente, para agilizar o cadastro sem precisar sair da tela de clientes.

**Estimativa:** 2 story points

**Critérios de Aceite:**

1. **Botão "+ Novo circuito":** Na aba "Circuitos" do modal de cliente, um botão "+ Novo circuito" abre o sub-modal em modo criação (campos vazios, exceto "Cliente" já preenchido e bloqueado).
2. **Campo Cliente pré-preenchido:** O sub-modal em modo criação exibe o nome do cliente atual no chip de "Cliente" com estado fixo (não editável, sem botão de limpar).
3. **Campos do formulário:** Senha (opcional), Tronco (obrigatório) e Plano (obrigatório) — os mesmos do sub-modal de edição. O campo "Código" não é exibido (gerado automaticamente pelo backend).
4. **Validação:** Se Tronco ou Plano não estiverem selecionados ao salvar, exibe erro inline no sub-modal: "Tronco e Plano são obrigatórios."
5. **Salvamento:** Ao salvar, envia `POST /api/circuit/circuits` com `{ password?, trunkName, planId, customerId }`. Em caso de sucesso, fecha o sub-modal, recarrega a lista de circuitos da aba e atualiza o `circuitCount` da linha na listagem.
6. **Distinção visual:** O header do sub-modal exibe "Novo circuito" como título (em vez do código do circuito). O botão "Deletar" não é exibido no modo criação.
7. **Comportamento preservado:** O fluxo de edição de circuito existente (clique no ícone de seta na linha) continua funcionando sem alterações.

---

### US-065

**Titulo:** Relatório: clientes sem circuitos vinculados

**Descrição:**
Como administrador, quero acessar um relatório que lista todos os clientes que não possuem nenhum circuito vinculado, para identificar cadastros ociosos e tomar ações comerciais ou de limpeza.

**Estimativa:** 2 story points

**Critérios de Aceite:**

1. **Endpoint backend:** `GET /api/reports/customers-without-circuits` retorna lista paginada de clientes (`id`, `name`, `document`, `enabled`, `createdAt`) que não possuem nenhum circuito vinculado (independente do estado `active`).
2. **Parâmetros:** suporta `page`, `size`, `sort` e `search` (por nome ou documento).
3. **Página frontend:** nova rota `/reports/customers-without-circuits` com listagem no padrão de layout canônico (header, toolbar com busca + paginação, tabela CSS Grid).
4. **Menu lateral:** item "Sem circuito" adicionado dentro da seção "Operacional" (ver US-066).
5. **Testes:** testes unitários no backend cobrem: cliente sem circuito aparece; cliente com ao menos um circuito não aparece.

---

## Refactoring (RF)

### US-012

**Titulo:** Refatoração: reorganização de pacotes em `domain/`

**Descrição:**
Como desenvolvedor, quero reorganizar os pacotes do backend introduzindo um pacote raiz `domain/`, de forma que todos os domínios de negócio fiquem agrupados ali (com os domínios Asterisk dentro de `domain/asterisk/`), enquanto pacotes de suporte (`config/`, `infra/`, `exception/`) permanecem fora, tornando a estrutura mais legível e alinhada com os princípios de arquitetura de domínio.

**Estimativa:** 2 story points

**Critérios de Aceite:**

1. **Novo pacote `domain/`:** Criado em `com.dionialves.AsteraComm.domain`.
2. **Subpacote `domain/asterisk/`:** Todos os pacotes atuais de `asterisk/` (`aors`, `auth`, `dialplan`, `endpoint`, `extension`, `provisioning`, `registration`) são movidos para `domain/asterisk/`.
3. **Domínios de negócio em `domain/`:** Os pacotes `auth`, `cdr`, `circuit`, `did`, `plan`, `trunk`, `user` são movidos para `domain/`, no mesmo nível de `asterisk/`.
4. **Fora de `domain/`:** Os pacotes `config/`, `infra/`, `exception/` permanecem no nível raiz, sem alteração de estrutura interna.
5. **Sem quebra de funcionalidade:** Todos os imports são atualizados, a aplicação compila e os testes existentes passam.
6. **Sem alteração de comportamento:** Nenhuma lógica de negócio é alterada — apenas movimentação de pacotes/arquivos.

**Nova estrutura resultante:**

```
com.dionialves.AsteraComm/
├── AsteraCommApplication.java
├── domain/
│   ├── asterisk/
│   │   ├── aors/
│   │   ├── auth/
│   │   ├── dialplan/
│   │   ├── endpoint/
│   │   ├── extension/
│   │   ├── provisioning/
│   │   └── registration/
│   ├── auth/
│   ├── cdr/
│   ├── circuit/
│   ├── did/
│   ├── plan/
│   ├── trunk/
│   └── user/
├── config/
├── exception/
└── infra/
```

---

### US-066

**Titulo:** Refatoração: menu lateral com seção "Operacional" e relatórios como links diretos

**Descrição:**
Como administrador, quero que o menu lateral tenha uma seção "Operacional" que, ao clicar, expande e exibe os relatórios disponíveis como links diretos, eliminando a página de índice de relatórios com cards.

**Estimativa:** 2 story points

**Critérios de Aceite:**

1. **Item "Operacional" no menu:** exibido com ícone e seta indicando expansão. Ao clicar, expande/colapsa inline exibindo os relatórios disponíveis como links diretos:
   - Auditoria → `/reports/audit`
   - Custo por Circuito → `/reports/cost-per-circuit`
2. **Expansão automática:** se a rota atual for `/reports/*`, "Operacional" já inicia expandido.
3. **Estado ativo:** o link do relatório atual aparece destacado no padrão visual dos demais itens ativos do menu.
4. **Remoção da página de índice:** a página de índice de relatórios é excluída. Qualquer link que apontava para `/reports` é removido ou redirecionado.
5. **Escopo:** template de layout (menu lateral) + remoção da página de índice — zero impacto nas páginas de relatório individuais.

---

## Bug Fixes (FIX)

### FIX-073

**Titulo:** Group filter da página de Circuitos não permanece ativo após modificação de página

**Descrição:**
O button group (Todos / Online / Offline / Inativos) da listagem de Circuitos perde o estado ativo quando a página sofre uma modificação (ex.: edição de circuito, navegação entre páginas, reload da listagem). O filtro volta para "Todos" em vez de manter o selecionado.

**Causa provável:**
A re-renderização da listagem não restaura o estado visual do botão ativo nem reenvia o parâmetro de filtro para o fetch.

**Critérios de Aceite:**

1. Ao selecionar um filtro e editar/salvar um circuito, o filtro permanece ativo após o reload da listagem.
2. Ao navegar entre páginas (paginação), o filtro ativo é mantido.
3. O botão visualmente destacado corresponde sempre ao filtro em uso.

---

### FIX-074

**Titulo:** Status de tronco com autenticação por IP não exibido corretamente

**Descrição:**
Troncos com `authType = IP_AUTH` aparecem sempre como "Não registrado" na listagem. O mecanismo atual consulta registros SIP (`pjsip show registrations` via AMI), mas troncos IP Auth não utilizam registro — a autenticação é feita por IP de origem, sem handshake de registro com o provedor.

**Causa provável:**
O `EndpointStatusService` consulta status via registrations, que só existe para troncos `CREDENTIAL`. Troncos `IP_AUTH` não possuem entrada em `ps_registrations`, portanto nunca retornam status positivo.

**Critérios de Aceite:**

1. Troncos `CREDENTIAL` continuam exibindo "Registrado" / "Não registrado" como antes.
2. Troncos `IP_AUTH` exibem badge distinto (ex.: "IP Auth") sem tentar consultar registro SIP.
3. Nenhuma regressão no comportamento de troncos `CREDENTIAL` existentes.

---

### FIX-076

**Titulo:** Calls sem circuito associado para ligações entrantes

**Descrição:**
Existem registros em `Call` sem circuito associado provenientes de ligações entrantes. O `ChannelParser` extrai o código do circuito do campo `channel`, o que funciona para ligações saintes (`PJSIP/4933401714-xxxxx`), mas falha para entrantes (`PJSIP/operadora-xxxxx`), onde o channel contém o nome do tronco e não o código do circuito. Para ligações entrantes, o circuito deve ser identificado via o campo `dst` (número discado = DID vinculado ao circuito).

**Causa:**
`CallProcessingService` usa apenas o `channel` para associar o circuito. Ligações entrantes chegam com o nome do tronco no channel, não o código do circuito.

**Critérios de Aceite:**

1. Ligações saintes continuam associadas ao circuito via `channel` (sem regressão).
2. Ligações entrantes com DID cadastrado ficam associadas ao circuito vinculado ao DID via `dst`.
3. Ligações entrantes com DID não cadastrado mantêm `circuit = null` sem erro.
4. Testes unitários cobrem os três cenários: sainte, entrante com DID, entrante sem DID.

---

### FIX-077

**Titulo:** Reconstruir circuitos excluídos e corrigir calls órfãs (Getel Telecom)

**Descrição:**
O histórico de ligações da Getel Telecom contém calls com `circuit = null` porque os circuitos originais foram excluídos. O campo `channel` dessas calls ainda preserva o código do circuito original, permitindo identificar e recriar os circuitos para restabelecer o vínculo.

**Estratégia:**

- **Etapa 1 — Diagnóstico:** query para identificar calls com `circuit = null` e `channel` contendo código de circuito válido; extrair códigos únicos e apresentar relatório antes de qualquer alteração.
- **Etapa 2 — Correção via migração Flyway:** recriar os circuitos identificados com `active = false` (históricos, não operacionais) e atualizar as calls órfãs vinculando ao circuito recriado.

**Critérios de Aceite:**

1. Nenhuma `Call` com código de circuito identificável no `channel` permanece com `circuit = null`.
2. Circuitos recriados têm `active = false` indicando que são registros históricos.
3. Calls sem código de circuito identificável no `channel` permanecem com `circuit = null`.
4. Migração Flyway versionada e validada em homologação antes de rodar em produção.

---

### US-078

**Titulo:** Controle de Firewall por IP

**Descrição:**
Como administrador, quero gerenciar quais IPs podem autenticar no servidor e que IPs com falhas de autenticação repetidas sejam bloqueados automaticamente, para proteger o sistema contra tentativas de uso indevido.

**Estimativa:** 5 story points

**Critérios de Aceite:**

1. Administrador pode adicionar/remover regras de bloqueio ou permissão por IP manualmente.
2. IPs com N falhas de autenticação consecutivas (configurável via `firewall.max.attempts`) são bloqueados automaticamente via `iptables`.
3. Falhas por circuito inativo e circuito inexistente ambas contam para o contador de tentativas.
4. Ao reiniciar o sistema, todas as regras BLOCK ativas são reaplicadas no `iptables`.
5. Desbloquear um IP remove a regra do banco e desfaz o bloqueio no `iptables`.
6. Badge visual distingue bloqueios automáticos (`AUTO`) de manuais (`MANUAL`).
7. Testes unitários cobrem: incremento de contador, disparo do auto-bloqueio e reaplicação no startup.

**Componentes:**
- Entidade `FirewallRule`: `ip`, `type` (`ALLOW` | `BLOCK`), `reason`, `createdAt`, `autoBlocked`
- `IxcSoftClient`: serviço que monitora eventos de falha AMI e acumula tentativas por IP
- Endpoints: `GET /api/firewall`, `POST /api/firewall`, `DELETE /api/firewall/{id}`
- Frontend: página `/firewall` com tabela de regras, badge por tipo e botão desbloquear

---

### US-079

**Titulo:** Integração com IXC Soft — Relatório de divergências

**Descrição:**
Como administrador, quero um relatório que compare clientes e planos do IXC Soft com os do AsteraComm, exibindo as divergências, para manter os dois sistemas sincronizados.

**Estimativa:** 3 story points

**Critérios de Aceite:**

1. Credenciais do IXC Soft configuráveis via `application.properties` (`ixcsoft.url`, `ixcsoft.token`) sem hardcode.
2. Relatório exibe três categorias para clientes e planos:
   - `Apenas no IXC Soft` — existe no IXC mas não no AsteraComm
   - `Apenas no AsteraComm` — existe no AsteraComm mas não no IXC
   - `Divergentes` — existe nos dois mas com dados diferentes (ex.: nome)
3. Se a API do IXC Soft estiver indisponível, retorna erro claro sem derrubar o sistema.
4. Exportação em CSV com todas as divergências.
5. Testes unitários cobrem: item só no IXC, só no AsteraComm, divergente e API indisponível.

**Componentes:**
- `IxcSoftClient`: cliente HTTP que busca clientes e planos da API do IXC Soft
- `IxcSoftComparisonService`: compara dados e produz resultado por categoria
- Endpoint: `GET /api/reports/ixcsoft-comparison`
- Frontend: página `/reports/ixcsoft-comparison` com seções por categoria e botão exportar CSV
