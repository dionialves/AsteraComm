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
4. [RF-080 — Upsert no padrão de atualização de status de troncos e endpoints](#rf-080)
5. [RF-081 — Remover double save em `CallProcessingService`](#rf-081)
6. [RF-082 — Transação por registro em `CallProcessingService`](#rf-082)
7. [RF-083 — Conexão persistente no `AmiService`](#rf-083)
8. [RF-084 — Substituir `System.err.println` por logger estruturado](#rf-084)
9. [RF-085 — Falha AMI rastreável no provisionamento](#rf-085)
10. [RF-086 — Eliminar lógica de custeio duplicada entre `AuditService` e `CallCostingService`](#rf-086)
11. [RF-087 — Substituir hack de formatação de moeda por `NumberFormat`](#rf-087)
12. [RF-088 — Substituir parsing frágil de texto AMI por consulta estruturada ao banco](#rf-088)
13. [RF-089 — Extrair valores hard-coded para constantes ou propriedades](#rf-089)
14. [RF-090 — Eliminar N+1 query na listagem de clientes](#rf-090)
15. [RF-091 — Adicionar cache ao dashboard](#rf-091)
16. [RF-092 — Padronizar `FetchType` em `Circuit`](#rf-092)
17. [RF-093 — Decompor serviços com múltiplas responsabilidades (`AuditService`, `CostPerCircuitService`)](#rf-093)
22. [RF-104 — Remover dependência Tailwind CSS e migrar para CSS puro](#rf-104)

### Bug Fixes (FIX)


---

### RF-075

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

### RF-080

**Titulo:** Upsert no padrão de atualização de status de troncos e endpoints

**Descrição:**
Como desenvolvedor, quero que a atualização de status de troncos e endpoints use upsert em vez de delete-then-insert, para que uma falha durante o refresh não resulte em perda total dos dados de status.

**Contexto técnico:**
`TrunkRegistrationStatusService:38` chama `statusRepository.deleteAll()` antes de inserir os novos registros. `EndpointStatusService:74` faz o mesmo com `endpointStatusRepository.deleteAll()`. Se o processo for interrompido entre o `deleteAll()` e os `save()` (timeout AMI, restart da aplicação, exceção), a tabela fica vazia. Além disso, durante a janela entre delete e insert, qualquer consulta à UI retorna status ausente.

**Estimativa:** 1 story point

**Critérios de Aceite:**

1. **Falha AMI preserva dados:** se `amiService.sendCommandWithResponse` retornar `null` ou lançar exceção, os registros de status existentes são mantidos intactos.
2. **Refresh normal atualiza dados:** registros presentes no novo snapshot são criados ou atualizados corretamente.
3. **Remoção de entidades:** registros cujas entidades não existem mais no novo snapshot são removidos após a inserção dos novos.
4. **Zero janela de dados ausentes:** em nenhum momento a tabela fica vazia durante um refresh normal.
5. **Testes:** cobrem AMI falha (dados preservados), refresh normal (dados atualizados) e entidade removida (registro deletado).

---

### RF-081

**Titulo:** Remover double save em `CallProcessingService`

**Descrição:**
Como desenvolvedor, quero que cada CDR processado gere exatamente um `save()` no banco, para evitar a persistência de registros `Call` incompletos e eliminar a operação de escrita redundante.

**Contexto técnico:**
`CallProcessingService:43-45` chama `callRepository.save(call)` duas vezes por CDR: a primeira antes de `callCostingService.applyCosting`, persistindo um `Call` sem status, sem custo e sem `minutesFromQuota`. Se `applyCosting` lançar exceção, o registro incompleto permanece no banco. A segunda chamada é a que de fato contém os dados completos.

**Estimativa:** 0,5 story point

**Critérios de Aceite:**

1. **Exatamente 1 `save()`** por CDR processado.
2. **Nenhum `Call` sem status** chega ao banco.
3. **Teste** verifica que `callRepository.save()` é chamado uma única vez por invocação do loop.

---

### RF-082

**Titulo:** Transação por registro em `CallProcessingService`

**Descrição:**
Como desenvolvedor, quero que o processamento de cada CDR seja atômico e independente, para que uma falha em um registro não comprometa os anteriores nem deixe o registro com falha em estado indeterminado.

**Contexto técnico:**
O método `process()` em `CallProcessingService` não é `@Transactional`. Se falhar no CDR #5 de uma lista de 10, os 4 anteriores já foram salvos (sem rollback possível), o #5 fica em estado parcial, e os CDRs #6-10 são abortados junto. Além disso, sem transação, não há garantia de isolamento durante a leitura do `cdrRepository.findUnprocessed()` e o `save()`.

**Estimativa:** 1 story point

**Critérios de Aceite:**

1. **Atomicidade por CDR:** cada registro é processado em sua própria transação com `propagation = REQUIRES_NEW`.
2. **Falha isolada:** exceção no CDR #2 de uma lista de 3 faz rollback apenas do #2; #1 e #3 são processados e salvos normalmente.
3. **CDR com falha reprocessável:** o CDR que falhou permanece como não processado e será tentado novamente na próxima execução do scheduler.
4. **Teste:** simula exceção no costing do CDR #2 e verifica que #1 e #3 foram persistidos.

---

### RF-083

**Titulo:** Conexão persistente no `AmiService`

**Descrição:**
Como desenvolvedor, quero que o `AmiService` mantenha uma única conexão AMI reutilizável, para eliminar o overhead de abrir e fechar uma conexão TCP a cada comando enviado.

**Contexto técnico:**
`AmiService.sendCommand` (linhas 29-33) e `sendCommandWithResponse` (linhas 39-45) criam `ManagerConnectionFactory`, abrem conexão, fazem login, enviam o comando e fazem logoff a cada chamada. Em um provisionamento de circuito completo, são 3 chamadas AMI separadas (`pjsip reload`, `dialplan reload`, status) — cada uma com seu próprio ciclo de conexão TCP.

**Estimativa:** 1 story point

**Critérios de Aceite:**

1. **Conexão singleton:** `AmiService` mantém uma única instância de `ManagerConnection` compartilhada entre todas as chamadas.
2. **Reconexão lazy:** se a conexão estiver fechada ou inativa no momento de um comando, reconecta automaticamente antes de enviar.
3. **Múltiplos comandos sequenciais** reutilizam a mesma conexão sem novo login/logoff.
4. **Teste:** verifica que `login()` é chamado apenas uma vez em uma sequência de 3 comandos consecutivos.

---

### RF-084

**Titulo:** Substituir `System.err.println` por logger estruturado

**Descrição:**
Como desenvolvedor, quero que erros de infraestrutura sejam registrados via SLF4J, para que possam ser controlados por nível de log, direcionados para arquivos e correlacionados com outros eventos do sistema.

**Contexto técnico:**
`AmiService:35,48` e `EndpointStatusService:34,65` usam `System.err.println` para reportar falhas. Isso contorna completamente o sistema de logging, não há nível de severidade configurável, não há stack trace estruturado, e não há integração com MDC ou correlação de requests.

**Estimativa:** 0,5 story point

**Critérios de Aceite:**

1. **Zero `System.err.println`** nos arquivos `AmiService` e `EndpointStatusService`.
2. **`@Slf4j`** adicionado nas classes afetadas.
3. **Erros** registrados com `log.error(mensagem, excecao)` incluindo a exceção para stack trace completo.
4. **Nível de log** configurável via `application.properties` para o pacote `asterisk`.

---

### RF-085

**Titulo:** Falha AMI rastreável no provisionamento

**Descrição:**
Como desenvolvedor, quero que falhas no AMI durante operações de provisionamento sejam propagadas como exceções rastreáveis, para que inconsistências entre o banco de dados e o Asterisk sejam detectáveis em vez de silenciosas.

**Contexto técnico:**
`AmiService.sendCommand` é `@Async` e captura todas as exceções internamente. `sendCommandWithResponse` retorna `null` em caso de falha. No fluxo de provisionamento de `AsteriskProvisioningService`, o `@Transactional` protege o banco, mas o `amiService.sendCommand("pjsip reload")` é disparado de forma assíncrona após o commit — se falhar, o banco reflete a mudança mas o Asterisk não. Essa inconsistência é invisível.

**Estimativa:** 1 story point

**Critérios de Aceite:**

1. **`sendCommand` síncrono no provisionamento:** o `@Async` é removido de `sendCommand` quando chamado pelo `AsteriskProvisioningService`, garantindo execução antes do retorno ao caller.
2. **Exceção em falha AMI crítica:** quando `sendCommandWithResponse` retornar `null` em operação de provisionamento, uma exceção rastreável é lançada com mensagem descritiva.
3. **Log `ERROR`** registrado com detalhes do comando que falhou.
4. **Teste:** simula falha AMI no provisionamento e verifica que exceção é propagada ao controller.

---

### RF-086

**Titulo:** Eliminar lógica de custeio duplicada entre `AuditService` e `CallCostingService`

**Descrição:**
Como desenvolvedor, quero que a lógica de resolução de tarifa e quota exista em um único lugar, para que alterações nas regras de custeio não precisem ser aplicadas em dois services separados e de forma independente.

**Contexto técnico:**
`AuditService` possui implementações próprias de `resolveRate`, `resolveUnifiedQuota`, `resolvePerCategoryQuota` e `decodeCost` que replicam o que `CallCostingService` já implementa. O método `calculateFractionCost` já é reaproveitado (`AuditService:90,130` chama `CallCostingService.calculateFractionCost`), mas a resolução de tarifa e quota permanece duplicada. Se a regra de tarifa mudar, precisa ser corrigida nos dois services.

**Estimativa:** 1 story point

**Critérios de Aceite:**

1. **Métodos compartilhados:** `resolveRate(Plan, CallType)` e os métodos de resolução de quota são `public static` em `CallCostingService` (ou extraídos para uma classe utilitária compartilhada).
2. **AuditService sem reimplementação:** `AuditService` não possui nenhuma implementação própria de resolução de tarifa ou quota — delega para `CallCostingService`.
3. **Resultado idêntico:** o resultado de `AuditService.simulate()` é idêntico antes e depois da refatoração para os três tipos de pacote (`NONE`, `UNIFIED`, `PER_CATEGORY`).
4. **Testes:** testes de `AuditService` e `CallCostingService` continuam passando sem alteração.

---

### RF-087

**Titulo:** Substituir hack de formatação de moeda por `NumberFormat`

**Descrição:**
Como desenvolvedor, quero que a formatação de valores monetários use a API padrão do Java, para eliminar a dependência frágil de locale implícito da JVM e o padrão de replace encadeado ilegível.

**Contexto técnico:**
`AuditService:213,250,251,270` e `CostPerCircuitService:144` usam `.replace(",","X").replace(".",",").replace("X",".")` para converter o separador decimal do formato JVM para o formato PT-BR. Esse padrão quebra silenciosamente se o locale padrão da JVM for diferente do esperado.

**Estimativa:** 0,5 story point

**Critérios de Aceite:**

1. **Zero ocorrências** do pattern `.replace(",","X")` no projeto.
2. **Método utilitário:** `formatBrl(BigDecimal)` extraído como método privado (ou classe utilitária) usando `NumberFormat.getInstance(new Locale("pt","BR"))` com scale fixo de 2 casas decimais.
3. **Saída idêntica:** formato `R$ 1.234,56` nos PDFs gerados idêntico ao atual.
4. **Comportamento independente** do locale padrão da JVM.

---

### RF-088

**Titulo:** Substituir parsing frágil de texto AMI por consulta estruturada ao banco

**Descrição:**
Como desenvolvedor, quero que o status de troncos e endpoints seja lido diretamente das tabelas do banco do Asterisk, para eliminar a fragilidade do parsing de saída de texto CLI que quebra com qualquer mudança de formatação do Asterisk.

**Contexto técnico:**
`TrunkRegistrationStatusService:47-52` faz split de strings na saída de `pjsip show registrations`. `EndpointStatusService:54-60` faz split de strings na saída de `pjsip show contacts`. Ambos assumem posições fixas de colunas no output CLI — qualquer mudança de versão do Asterisk quebra o parsing silenciosamente. O banco do Asterisk já é acessível via JPA (as tabelas `ps_endpoints`, `ps_aors` etc. já são lidas/escritas pelo sistema).

**Estimativa:** 2 story points

**Critérios de Aceite:**

1. **Nenhum parsing de texto CLI** nos dois services.
2. **Status de tronco** lido da tabela `ps_registrations` via entidade `@Immutable` + repositório JPA.
3. **Status de endpoint** lido da tabela `ps_contacts` via entidade `@Immutable` + repositório JPA.
4. **Sem chamadas AMI** para leitura de status — apenas para reload (`pjsip reload`, `dialplan reload`).
5. **Testes:** cobrem tronco registrado, não registrado e sem entrada na tabela.

---

### RF-089

**Titulo:** Extrair valores hard-coded para constantes ou propriedades

**Descrição:**
Como desenvolvedor, quero que literais mágicos espalhados pelo código sejam nomeados semanticamente ou configuráveis via properties, para facilitar manutenção e evitar inconsistências ao mudar convenções de nomenclatura.

**Contexto técnico:**
- `AsteriskProvisioningService`: prefixos `"internal-"` e `"pstn-"` repetidos 7 vezes no corpo dos métodos (linhas 67, 151, 152, 203, 242, 264, 288, 294).
- `DashboardService:145-146`, `AuditService:165-166`, `CostPerCircuitService:55-57`: arrays manuais de nomes de meses em PT-BR duplicados em 3 locais.
- `AsteriskProvisioningService`: literais de configuração PJSIP sem nome (`"60"`, `"1"`, `"yes"`, `"ulaw,alaw"`).

**Estimativa:** 1 story point

**Critérios de Aceite:**

1. **Prefixos de contexto** extraídos para `private static final String` com nome descritivo em `AsteriskProvisioningService`.
2. **Arrays de meses** substituídos por `Month.of(i).getDisplayName(TextStyle.FULL, new Locale("pt","BR"))` — sem array manual em nenhum arquivo.
3. **Literais de configuração PJSIP** extraídos para constantes nomeadas.
4. **Comportamento idêntico** em provisionamento e relatórios.
5. **Sem regressão** nos testes existentes.

---

### RF-090

**Titulo:** Eliminar N+1 query na listagem de clientes

**Descrição:**
Como desenvolvedor, quero que a listagem de clientes execute um número fixo de queries independente do tamanho da página, para evitar degradação de performance à medida que o volume de clientes cresce.

**Contexto técnico:**
`CustomerService:41` chama `circuitRepository.countByCustomerId(c.getId())` dentro de `page.map(this::toResponseDTO)`, resultando em 1 query por cliente da página. Com 20 clientes por página = 21 queries por request. Com 50 clientes = 51 queries.

**Estimativa:** 1 story point

**Critérios de Aceite:**

1. **Máximo 2 queries** por chamada a `CustomerService.getAll()` (dados + count para paginação), independente do tamanho da página.
2. **`circuitCount` correto** para clientes com 0, 1 e N circuitos.
3. **`CircuitRepository` removido** de `CustomerService` — a contagem é calculada na própria query de clientes via `LEFT JOIN ... GROUP BY`.
4. **Testes de integração** validam a contagem com H2.

---

### RF-091

**Titulo:** Adicionar cache ao dashboard

**Descrição:**
Como desenvolvedor, quero que os dados do dashboard sejam cacheados por um período configurável, para reduzir a carga no banco causada pelo polling frequente da interface.

**Contexto técnico:**
`DashboardService.getDashboard()` executa ~12 queries a cada chamada. O dashboard utiliza polling via HTMX, podendo gerar dezenas de chamadas por minuto com múltiplos usuários simultâneos. Caffeine é dependência transitiva do Spring Boot e está disponível sem adição de dependências.

**Estimativa:** 1 story point

**Critérios de Aceite:**

1. **Cache funcional:** segunda chamada ao dashboard dentro do TTL não executa queries no banco.
2. **Invalidação por circuito:** criação e deleção de circuito evictam o cache.
3. **Invalidação por chamadas:** processamento de novas chamadas evicta o cache.
4. **TTL configurável** via `dashboard.cache.ttl.seconds` em `application.properties`, com valor padrão de 60 segundos.
5. **Testes:** verificam que o repositório não é chamado na segunda invocação dentro do TTL.

---

### RF-092

**Titulo:** Padronizar `FetchType` em `Circuit`

**Descrição:**
Como desenvolvedor, quero que as associações de `Circuit` usem estratégia de fetch consistente e justificada, para evitar carregamento desnecessário de dados e potenciais `LazyInitializationException`.

**Contexto técnico:**
`Circuit.java:32`: `Customer` é `FetchType.LAZY`. `Circuit.java:36`: `Plan` é `FetchType.EAGER` sem justificativa técnica. O `EAGER` no `Plan` significa que toda listagem de circuitos (mesmo mostrando apenas número e status) carrega o plano completo com todas as suas colunas de tarifa.

**Estimativa:** 1 story point

**Critérios de Aceite:**

1. **`Plan` em `Circuit`** com `FetchType.LAZY`.
2. **Nenhum `LazyInitializationException`** em nenhum fluxo existente (listagem, detalhe, provisionamento, custeio, auditoria).
3. **Queries que precisam do plano** usam `@EntityGraph` ou `JOIN FETCH` explícito.
4. **Testes de integração** cobrem os fluxos que acessam `circuit.getPlan()`.

---

### RF-093

**Titulo:** Decompor serviços com múltiplas responsabilidades (`AuditService`, `CostPerCircuitService`)

**Descrição:**
Como desenvolvedor, quero que cada classe tenha uma única razão para mudar, extraindo a geração de PDF para classes dedicadas, para que alterações de layout de relatório não interfiram com a lógica de negócio.

**Contexto técnico:**
- `AuditService` (308 linhas): mistura lógica de simulação de custeio com geração de PDF. Mudança de layout do PDF e mudança de regra de custeio são razões independentes para alterar a mesma classe.
- `CostPerCircuitService` (146 linhas): mistura agregação de dados com geração de PDF. Ambas as classes têm imports de `com.lowagie` ao lado de imports de domínio.

**Estimativa:** 1 story point

**Critérios de Aceite:**

1. **`AuditPdfGenerator`** extraído de `AuditService`: recebe `AuditResultDTO` e retorna `byte[]`. `AuditService` não tem nenhum import de `com.lowagie`.
2. **`CostPerCircuitPdfGenerator`** extraído de `CostPerCircuitService`: recebe `CostPerCircuitResponseDTO` e retorna `byte[]`. `CostPerCircuitService` não tem nenhum import de `com.lowagie`.
3. **Geradores de PDF** não têm dependência de repositórios — trabalham apenas com DTOs.
4. **Comportamento dos PDFs** idêntico ao atual (layout, dados, formatação).
5. **Testes existentes** continuam passando sem alteração.

---

### RF-086

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

---

### RF-104 · Remover dependência Tailwind CSS e migrar para CSS puro

- **Tipo:** Refactor
- **Prioridade:** MÉDIA
- **US relacionada:** —
- **Sprint:** —
- **Arquivos:**
  - `backend/src/main/resources/static/css/input.css` (editar → renomear para `app.css`)
  - `backend/src/main/resources/static/css/output.css` (remover)
  - `backend/tools/tailwindcss` (remover)
  - `backend/tailwind.config.js` (remover)
  - `backend/tailwind-watch.sh` (remover)
  - `backend/pom.xml` (editar — remover exec-maven-plugin do Tailwind)
  - `backend/Dockerfile` (editar — remover step Tailwind)
  - `backend/src/main/resources/templates/layout/base.html` (editar — trocar referência CSS)
  - `backend/src/main/resources/templates/layout/base-login.html` (editar — trocar referência CSS)
  - `dev.sh` (editar — remover dica do tailwind-watch.sh)
  - `README.md` (editar — remover referência ao tailwind-watch.sh)
  - `AGENTS.md` (editar — atualizar descrição da stack e build)
  - Todos os 48 templates `.html` em `backend/src/main/resources/templates/` (editar — converter classes Tailwind para CSS puro)
- **Dependências:** —

#### Contexto / Problema

O Tailwind CSS foi introduzido no projeto via US-052 para padronizar o styling. Porém, hoje ele opera como um **binário standalone de 41 MB** (`backend/tools/tailwindcss`) que precisa ser executado offline para compilar o CSS gerado (`output.css`). Isso traz complexidade desnecessária:

1. **Build quebrado fora do Docker:** o passo de compilação Tailwind no `pom.xml` (phase `generate-resources` via `exec-maven-plugin`) invoca `./tools/tailwindcss`, que é um binário Linux x86_64. Em macOS (máquina do desenvolvedor), esse binário não roda — o Maven build falha localmente.
2. **Recompilação manual em dev:** alterações visuais só surtem efeito após rodar `./backend/tailwind-watch.sh` manualmente, eliminando o benefício do hot reload do devtools.
3. **Binário de 41 MB no repositório:** `backend/tools/tailwindcss` é um binário compilado committed no git, engordando o repositório e sem utilidade em plataformas não-Linux.
4. **Tailwind é supérfluo aqui:** o projeto usa Tailwind quase exclusivamente como **utility-first CSS** — não usa componentes, plugins, @apply, design system, nem PurgeCSS avançado. As ~406 classes únicas nos templates são triviais de converter para CSS puro (flex, grid, gap, padding, margin, cores, tipografia). As classes custom já existem no `input.css`.

#### Abordagem escolhida

Substituir toda a pipeline Tailwind (binário → `input.css` com `@tailwind` → compilação → `output.css`) por um **único arquivo CSS puro** (`app.css`) servido estaticamente. As classes utilitárias Tailwind nos templates são convertidas para classes semânticas nomeadas no CSS, mantendo o mesmo visual.

**Estratégia de conversão:**
- Classes de layout/estrutura (flex, grid, gap, items-, justify-) → classes semânticas por componente (ex.: `.sidebar`, `.sidebar-logo`, `.sidebar-link`, `.btn-primary`, `.card`, `.table-header`, etc.)
- Classes de espaçamento (p-*, m-*) → definições inline no CSS com os mesmos valores
- Classes de tipografia (text-*, font-*) → classes semânticas (`.text-heading`, `.text-body`, `.text-caption`, etc.)
- Classes de cor (bg-*, text-*, border-*) → CSS custom properties já existentes (`--color-primary`, `--color-text`, etc.) + novas variáveis conforme necessário
- Classes de borda, radius, shadow → definições no componente
- Classes de estado (hover:*, hidden, opacity-*) → CSS nativo (`:hover`, `display:none`, etc.)
- Classes existentes custom (`.modal-header`, `.trunk-row`, `.ss-trigger`, etc.) → mantidas como estão

**Alternativa descartada 1:** manter Tailwind via CDN (`<script src="https://cdn.tailwindcss.com">`). Motivo: troca um binário de build por uma dependência CDN em runtime — péssimo para performance e indisponibilidade offline.

**Alternativa descartada 2:** converter para PostCSS + plugin cssnano. Motivo: adiciona outra dependência de build com Node.js, indo na direção oposta à simplificação.

#### Passo-a-passo de implementação

> **NOTA:** Esta task tem escopo grande (48 templates + infra). O Codificador deve executar os passos na ordem exata. Compilar (`mvn compile`) após cada 5–8 templates convertidos para detectar regressões cedo.

1. **Criar** `backend/src/main/resources/static/css/app.css` — novo arquivo CSS puro contendo:
   - As custom properties `:root` já existentes no `input.css` (mantidas idênticas)
   - A animação `.toast-fadeout` (mantida idêntica)
   - Os estilos custom existentes do `input.css` (modal-*, *-row, ss-*, toggle-*, etc.) mantidos idênticos
   - **Novas classes semânticas** correspondentes a todas as classes Tailwind usadas nos templates, organizadas por seção:

   ```css
   /* ══════════════════════════════════════════════════════════════════════════
      AsteraComm — CSS Puro
      Substitui pipeline Tailwind (binário + input.css + output.css)
      ══════════════════════════════════════════════════════════════════════════ */

   /* ── Reset & Base ──────────────────────────────────────────────────────── */
   *, *::before, *::after { box-sizing: border-box; }
   body { margin: 0; font-family: ui-sans-serif, system-ui, -apple-system, sans-serif; color: var(--color-text); }

   /* ── Custom Properties ─────────────────────────────────────────────────── */
   :root {
     --color-primary: #1D9E75;
     --color-primary-dark: #085041;
     --color-danger: #791F1F;
     --color-text: #1a1a1a;
     --color-muted: #888;
     --color-border: #e0e0e0;
     --color-surface: #f5f5f5;
     --color-selected-bg: #E6F1FB;
     --color-selected-border: #378ADD;
     --color-white: #ffffff;
     --color-gray-50: #fafafa;
     --color-gray-100: #f5f5f5;
     --color-gray-200: #e5e7eb;
     --color-gray-300: #d1d5db;
     --color-gray-400: #9ca3af;
     --color-gray-500: #6b7280;
     --color-gray-600: #555555;
     --color-gray-700: #374151;
     --color-gray-900: #111827;
     --color-red-50: #fef2f2;
     --color-red-100: #fee2e2;
     --color-red-200: #fecaca;
     --color-red-300: #fca5a5;
     --color-red-500: #ef4444;
     --color-red-600: #dc2626;
     --color-red-700: #E24B4A;
     --color-amber-50: #FFF3E0;
     --color-amber-500: #E5A000;
     --color-amber-700: #E65100;
     --color-brown: #BA7517;
     --color-brown-dark: #854F0B;
     --color-blue-50: #E6F1FB;
     --color-blue-500: #185FA5;
     --color-sidebar: #1a1a1a;
     --color-sidebar-hover: rgba(255,255,255,0.05);
     --color-sidebar-active: rgba(255,255,255,0.08);
     --color-success-light: #E1F5EE;
     --color-warning-light: #FEF3C7;
     --color-danger-light: #FCEBEB;
     --shadow-sm: 0 1px 2px rgba(0,0,0,0.05);
     --shadow-md: 0 4px 6px -1px rgba(0,0,0,0.1);
     --shadow-lg: 0 10px 15px -3px rgba(0,0,0,0.1);
     --shadow-xl: 0 20px 25px -5px rgba(0,0,0,0.1);
   }
   ```

   O Codificador deve completar o arquivo `app.css` com **todas as classes semânticas necessárias**, mapeando cada classe Tailwind usada nos templates. O mapeamento segue os padrões abaixo (o Codificador deve criar TODAS — esta lista é um guia, não exaustiva):

   **Sidebar:**
   ```css
   .sidebar { position: fixed; top: 0; left: 0; width: 220px; height: 100vh; background: var(--color-sidebar); display: flex; flex-direction: column; z-index: 5; overflow-y: auto; }
   .sidebar-logo { padding: 20px 16px 12px; flex-shrink: 0; text-align: center; }
   .sidebar-logo-text { font-size: 24px; font-weight: 500; letter-spacing: -0.5px; }
   .sidebar-logo-accent { color: #5DCAA5; }
   .sidebar-logo-white { color: white; }
   .sidebar-version { font-size: 11px; color: rgba(255,255,255,0.3); margin-top: 4px; }
   .sidebar-nav { flex: 1; padding: 4px 8px; overflow-y: auto; }
   .sidebar-section-label { font-size: 10px; font-weight: 600; letter-spacing: 1px; color: rgba(255,255,255,0.35); text-transform: uppercase; padding: 14px 12px 4px; display: block; }
   .sidebar-link { display: flex; align-items: center; gap: 10px; padding: 9px 12px; border-radius: 6px; font-size: 13px; color: rgba(255,255,255,0.65); text-decoration: none; margin-bottom: 1px; cursor: pointer; transition: background 150ms, color 150ms; }
   .sidebar-link:hover { background: var(--color-sidebar-hover); color: rgba(255,255,255,0.9); }
   .sidebar-link.active { background: var(--color-sidebar-active); color: white; }
   .sidebar-submenu-btn { width: 100%; display: flex; align-items: center; justify-content: space-between; padding: 9px 12px; border-radius: 6px; border: none; background: transparent; font-size: 13px; color: rgba(255,255,255,0.65); cursor: pointer; margin-bottom: 1px; transition: background 150ms, color 150ms; }
   .sidebar-submenu-btn:hover { background: var(--color-sidebar-hover); color: rgba(255,255,255,0.9); }
   .sidebar-submenu-label { display: flex; align-items: center; gap: 10px; }
   .sidebar-sub-chevron { flex-shrink: 0; transition: transform 250ms ease; }
   .sidebar-sub-chevron.open { transform: rotate(180deg); }
   .sidebar-submenu { overflow: hidden; transition: max-height 250ms ease; }
   .sidebar-submenu.open { max-height: 200px; }
   .sidebar-submenu.closed { max-height: 0; }
   .sidebar-sublink { display: block; padding: 7px 12px 7px 38px; font-size: 13px; color: rgba(255,255,255,0.55); text-decoration: none; border-radius: 6px; margin-bottom: 1px; transition: background 150ms, color 150ms; }
   .sidebar-sublink:hover { background: var(--color-sidebar-hover); color: rgba(255,255,255,0.9); }
   .sidebar-sublink.active { color: white; }
   .sidebar-footer { flex-shrink: 0; padding: 8px; border-top: 1px solid rgba(255,255,255,0.07); position: relative; }
   .sidebar-profile-btn { width: 100%; display: flex; align-items: center; gap: 8px; padding: 8px 10px; border-radius: 6px; background: transparent; border: none; cursor: pointer; transition: background 150ms; }
   .sidebar-profile-btn:hover { background: var(--color-sidebar-hover); }
   .sidebar-profile-avatar { width: 28px; height: 28px; border-radius: 50%; background: var(--color-primary); color: white; font-size: 12px; font-weight: 600; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
   .sidebar-profile-info { flex: 1; min-width: 0; text-align: left; }
   .sidebar-profile-name { font-size: 12px; color: white; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; margin: 0; }
   .sidebar-profile-role { font-size: 11px; color: rgba(255,255,255,0.4); margin: 0; }
   .sidebar-profile-chevron { flex-shrink: 0; transition: transform 250ms ease; }
   .sidebar-profile-dropdown { position: absolute; bottom: calc(100% + 4px); left: 8px; right: 8px; background: #2a2a2a; border: 1px solid rgba(255,255,255,0.1); border-radius: 8px; overflow: hidden; }
   .sidebar-dropdown-btn { width: 100%; text-align: left; padding: 9px 14px; font-size: 13px; color: rgba(255,255,255,0.75); background: transparent; border: none; cursor: pointer; transition: background 120ms, color 120ms; display: block; }
   .sidebar-dropdown-btn:hover { background: rgba(255,255,255,0.06); color: white; }
   .sidebar-dropdown-btn.danger { color: #f87171; }
   .sidebar-dropdown-btn.danger:hover { background: rgba(239,68,68,0.12); }
   .sidebar-dropdown-divider { border-top: 1px solid rgba(255,255,255,0.1); }
   ```

   **Main content:**
   ```css
   .main-content { margin-left: 220px; min-height: 100vh; overflow-y: auto; padding: 32px; background: #fafaf8; }
   ```

   **Typography:**
   ```css
   .text-heading { font-size: 22px; font-weight: 500; color: var(--color-text); }
   .text-body { font-size: 13px; color: var(--color-text); }
   .text-caption { font-size: 12px; color: var(--color-muted); }
   .text-small { font-size: 11px; }
   .text-tiny { font-size: 10px; }
   .text-xxl { font-size: 24px; font-weight: 500; }
   .text-3xl { font-size: 36px; font-weight: 500; letter-spacing: -0.5px; }
   .text-mono { font-family: ui-monospace, SFMono-Regular, monospace; }
   .text-white { color: white; }
   .text-muted { color: var(--color-muted); }
   .text-primary { color: var(--color-primary); }
   .text-danger { color: var(--color-danger); }
   .text-danger-light { color: #f87171; }
   .text-gray-500 { color: var(--color-gray-500); }
   .text-gray-700 { color: var(--color-gray-700); }
   .text-gray-900 { color: var(--color-gray-900); }
   .text-semibold { font-weight: 600; }
   .text-medium { font-weight: 500; }
   .text-center { text-align: center; }
   .text-right { text-align: right; }
   .uppercase { text-transform: uppercase; }
   .whitespace-nowrap { white-space: nowrap; }
   .text-ellipsis { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
   .truncate { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
   .tracking-wide { letter-spacing: 0.025em; }
   .tracking-wider { letter-spacing: 0.05em; }
   .leading-relaxed { line-height: 1.625; }
   ```

   **Layout utilities:**
   ```css
   .flex { display: flex; }
   .flex-col { display: flex; flex-direction: column; }
   .flex-wrap { flex-wrap: wrap; }
   .flex-1 { flex: 1; }
   .flex-shrink-0 { flex-shrink: 0; }
   .grid { display: grid; }
   .items-center { align-items: center; }
   .items-start { align-items: flex-start; }
   .items-end { align-items: flex-end; }
   .items-baseline { align-items: baseline; }
   .items-stretch { align-items: stretch; }
   .justify-center { justify-content: center; }
   .justify-between { justify-content: space-between; }
   .justify-end { justify-content: flex-end; }
   .relative { position: relative; }
   .absolute { position: absolute; }
   .fixed { position: fixed; }
   .overflow-hidden { overflow: hidden; }
   .overflow-x-auto { overflow-x: auto; }
   .overflow-y-auto { overflow-y: auto; }
   .hidden { display: none; }
   .contents { display: contents; }
   .pointer-events-none { pointer-events: none; }
   .pointer-events-auto { pointer-events: auto; }
   .select-none { user-select: none; }
   .cursor-pointer { cursor: pointer; }
   .cursor-default { cursor: default; }
   .cursor-not-allowed { cursor: not-allowed; }
   .no-underline { text-decoration: none; }
   .shrink-0 { flex-shrink: 0; }
   .inline-block { display: inline-block; }
   .inline-flex { display: inline-flex; }
   .block { display: block; }
   ```

   **Spacing (manter como classes utilitárias — são práticas demais para eliminar):**
   ```css
   /* Padding */
   .p-0 { padding: 0; } .p-2 { padding: 8px; } .p-4 { padding: 16px; } .p-5 { padding: 20px; } .p-6 { padding: 24px; } .p-8 { padding: 32px; }
   .px-2 { padding-left: 8px; padding-right: 8px; } .px-3 { padding-left: 12px; padding-right: 12px; } .px-4 { padding-left: 16px; padding-right: 16px; } .px-5 { padding-left: 20px; padding-right: 20px; } .px-8 { padding-left: 32px; padding-right: 32px; }
   .py-1 { padding-top: 4px; padding-bottom: 4px; } .py-2 { padding-top: 8px; padding-bottom: 8px; } .py-3 { padding-top: 12px; padding-bottom: 12px; } .py-4 { padding-top: 16px; padding-bottom: 16px; }
   /* ... o Codificador deve criar TODAS as variantes de p/px/py/pt/pb/pl/pr/m/mx/my/mt/mb/ml/mr/gap usadas nos templates, consultando cada arquivo .html ... */

   /* Margin */
   .m-0 { margin: 0; } .mb-1 { margin-bottom: 4px; } .mb-2 { margin-bottom: 8px; } .mb-3 { margin-bottom: 12px; } .mb-4 { margin-bottom: 16px; } .mb-5 { margin-bottom: 20px; } .mb-6 { margin-bottom: 24px; } .mb-7 { margin-bottom: 28px; } .mb-8 { margin-bottom: 32px; } .mb-px { margin-bottom: 1px; }
   .mt-1 { margin-top: 4px; } .mt-2 { margin-top: 8px; } .mt-3 { margin-top: 12px; } .mt-4 { margin-top: 16px; }
   .ml-1 { margin-left: 4px; }
   .mr-auto { margin-right: auto; }

   /* Gap */
   .gap-1 { gap: 4px; } .gap-2 { gap: 8px; } .gap-3 { gap: 12px; } .gap-4 { gap: 16px; } .gap-8 { gap: 32px; }
   ```

   **Background, Border, Radius, Shadow:**
   ```css
   .bg-white { background: white; }
   .bg-surface { background: var(--color-surface); }
   .bg-primary { background: var(--color-primary); }
   .bg-danger { background: var(--color-danger); }
   .bg-danger-light { background: var(--color-danger-light); }
   .bg-success-light { background: var(--color-success-light); }
   .bg-warning-light { background: var(--color-warning-light); }
   .bg-transparent { background: transparent; }
   .bg-gray-50 { background: var(--color-gray-50); }
   .bg-gray-100 { background: var(--color-gray-100); }
   .border { border: 1px solid var(--color-border); }
   .border-t { border-top: 1px solid; }
   .border-b { border-bottom: 1px solid; }
   .border-b-0 { border-bottom: none; }
   .border-none { border: none; }
   .border-collapse { border-collapse: collapse; }
   .rounded { border-radius: 4px; }
   .rounded-md { border-radius: 6px; }
   .rounded-lg { border-radius: 8px; }
   .rounded-xl { border-radius: 12px; }
   .rounded-full { border-radius: 9999px; }
   .shadow-sm { box-shadow: var(--shadow-sm); }
   .shadow-md { box-shadow: var(--shadow-md); }
   .shadow-lg { box-shadow: var(--shadow-lg); }
   .shadow-xl { box-shadow: var(--shadow-xl); }
   .outline-none { outline: none; }
   ```

   **Sizing:**
   ```css
   .w-full { width: 100%; }
   .w-fit { width: fit-content; }
   .w-1\/2 { width: 50%; }
   .h-full { height: 100%; }
   .h-screen { height: 100vh; }
   .min-h-screen { min-height: 100vh; }
   .min-w-0 { min-width: 0; }
   .box-border { box-sizing: border-box; }
   ```

   **Interactivity (hover, transitions):**
   ```css
   .transition-all { transition: all 150ms; }
   .transition-colors { transition: color 150ms, background 150ms, border-color 150ms; }
   .transition-opacity { transition: opacity 150ms; }
   .transition-shadow { transition: box-shadow 150ms; }
   .transition-transform { transition: transform 150ms; }
   .opacity-30 { opacity: 0.3; }
   .opacity-25 { opacity: 0.25; }
   .opacity-75 { opacity: 0.75; }
   .animate-spin { animation: spin 1s linear infinite; }
   @keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
   ```

   O Codificador deve revisar **cada um dos 48 templates** e garantir que TODA classe Tailwind usada neles tem uma definição correspondente no `app.css`. Classes com valores arbitrários (ex.: `text-[13px]`, `bg-[#1D9E75]`, `w-[220px]`, `px-[14px]`) devem ser convertidas para classes semânticas nomeadas ou, quando extremamente específicas (1 uso), movidas para `style=""` inline no template.

   **Princípio: classes genéricas (flex, items-center, gap-2, mb-4) podem permanecer como utilities no CSS puro. Classes de cor e tipografia com valores arbitrários Tailwind (ex.: `text-[13px]`, `bg-[#1a1a1a]`) DEVEM ser convertidas para classes nomeadas.**

2. **Editar** `backend/src/main/resources/templates/layout/base.html`:
   - Trocar `<link rel="stylesheet" th:href="@{/css/output.css}"/>` por `<link rel="stylesheet" th:href="@{/css/app.css}"/>`
   - Converter as classes Tailwind no template para as novas classes semânticas:
     - `<body class="font-sans">` → `<body>`
     - `<aside class="fixed top-0 left-0 w-[220px] h-screen bg-[#1a1a1a] flex flex-col z-[5] overflow-y-auto">` → `<aside class="sidebar">`
     - Substituir classes Tailwind do logo, nav, links, seções, footer — tudo para as classes semânticas correspondentes definidas no passo 1
     - `<main class="ml-[220px] min-h-screen overflow-y-auto p-8 bg-stone-50">` → `<main class="main-content">`
     - `<div id="toast-container" class="fixed bottom-4 right-4 z-[300] flex flex-col gap-2 pointer-events-none">` → `<div id="toast-container" class="toast-container">` (adicionar `.toast-container` no `app.css`)
     - No JS inline, substituir `classList.toggle('max-h-0', ...)` / `classList.toggle('max-h-[200px]', ...)` por `classList.toggle('closed', ...)` / `classList.toggle('open', ...)` (ou adicionar/remover as classes `.sidebar-submenu.open` e `.sidebar-submenu.closed`)
     - No JS inline, substituir `classList.toggle('rotate-180', ...)` por `classList.toggle('open', ...)` para os chevrons (correspondendo a `.sidebar-sub-chevron.open`)

3. **Editar** `backend/src/main/resources/templates/layout/base-login.html`:
   - Trocar `<link rel="stylesheet" th:href="@{/css/output.css}"/>` por `<link rel="stylesheet" th:href="@{/css/app.css}"/>`
   - `<body class="font-sans bg-stone-50 min-h-screen flex items-center justify-center">` → `<body class="login-page">` (adicionar `.login-page { font-family: ...; background: #fafaf8; min-height: 100vh; display: flex; align-items: center; justify-content: center; }` no `app.css`)

4. **Editar** cada template de página/fragmento para converter classes Tailwind:
   
   Converter os 48 templates seguindo o mapeamento abaixo. O Codificador deve converter **todos** — esta lista indica a prioridade e ordem recomendada, mas TODOS devem ser convertidos:

   **Prioridade ALTA (layouts + páginas principais):**
   - `layout/base.html` (passo 2 acima)
   - `layout/base-login.html` (passo 3 acima)
   - `pages/login/index.html`
   - `pages/dashboard/index.html`
   - `fragments/pagination.html`
   - `fragments/toast.html`
   - `fragments/search-select.html`
   - `fragments/modal-close.html`
   - `fragments/confirm-delete.html`
   - `fragments/profile/modal.html`
   - `fragments/profile/password-modal.html`
   - `fragments/profile/response.html`

   **Prioridade MÉDIA (páginas de CRUD):**
   - `pages/circuits/list.html`
   - `pages/circuits/table.html`
   - `pages/circuits/modal.html`
   - `pages/circuits/tab-detalhes.html`
   - `pages/circuits/tab-dids.html`
   - `pages/circuits/tab-history.html`
   - `pages/customers/list.html`
   - `pages/customers/table.html`
   - `pages/customers/modal.html`
   - `pages/customers/tab-circuits.html`
   - `pages/customers/tab-history.html`
   - `pages/plans/list.html`
   - `pages/plans/table.html`
   - `pages/plans/modal.html`
   - `pages/plans/package-fields.html`
   - `pages/dids/list.html`
   - `pages/dids/table.html`
   - `pages/dids/modal.html`
   - `pages/trunks/list.html`
   - `pages/trunks/table.html`
   - `pages/trunks/modal.html`
   - `pages/users/list.html`
   - `pages/users/table.html`
   - `pages/users/modal.html`
   - `pages/cdrs/list.html`
   - `pages/cdrs/table.html`
   - `pages/cdrs/modal.html`

   **Prioridade MÉDIA (relatórios):**
   - `pages/reports/index.html`
   - `pages/reports/audit.html`
   - `pages/reports/audit-table.html`
   - `pages/reports/cost-per-circuit.html`
   - `pages/reports/cost-per-circuit-table.html`
   - `pages/reports/orphan-calls.html`
   - `pages/reports/orphan-calls-table.html`

   **Para cada template, o Codificador deve:**
   a. Identificar todas as classes Tailwind no `class=""` e no `th:classappend=""`
   b. Substituir por classes semânticas ou utilitárias puras definidas no `app.css`
   c. Classes `th:classappend` com condicionais Tailwind (ex.: `!bg-white/[0.08] !text-white`) devem ser substituídas por classes semânticas (ex.: `sidebar-link-active`)
   d. Após converter, rodar `mvn compile` a cada 5–8 templates para garantir que o Thymeleaf resolve os templates

5. **Remover** `backend/src/main/resources/static/css/output.css`

6. **Remover** `backend/src/main/resources/static/css/input.css`

7. **Remover** `backend/tools/tailwindcss`

8. **Remover** `backend/tailwind.config.js`

9. **Remover** `backend/tailwind-watch.sh`

10. **Editar** `backend/pom.xml` — remover o plugin `exec-maven-plugin` inteiro (linhas 130–154):
    ```xml
    <!-- REMOVER este bloco inteiro -->
    <plugin>
        <groupId>org.codehaus.mojo</groupId>
        <artifactId>exec-maven-plugin</artifactId>
        ...
    </plugin>
    ```

11. **Editar** `backend/Dockerfile` — remover as linhas de Tailwind:
    - Remover linha 21: `COPY tailwind.config.js .`
    - Remover linhas 22-23: `COPY tools/tailwindcss ...` e `RUN chmod +x ...`
    - Remover linhas 26-29: `RUN ./tools/tailwindcss ... --minify`
    - Atualizar comentário da linha 19 de "Copia código fonte, config Tailwind e binário" para "Copia código fonte"

12. **Editar** `dev.sh` — remover a dica do Tailwind:
    - Remover linha 85: `echo "  • Para recompilar o CSS: ./backend/tailwind-watch.sh"`

13. **Editar** `README.md` — remover referência ao `tailwind-watch.sh`

14. **Editar** `AGENTS.md`:
    - Na seção "Projeto", alterar "UI: Thymeleaf + HTMX + Tailwind CSS (CLI standalone)." para "UI: Thymeleaf + HTMX + CSS puro."
    - Na seção "Build", remover o bloco inteiro do Tailwind CSS:
      ```
      - **Tailwind CSS:** compilado por binário standalone (`backend/tools/tailwindcss`), não usa Node/npm.
        - Modo watch: `./backend/tailwind-watch.sh`
        - Build do CSS está acoplado ao Maven na fase `generate-resources` via `exec-maven-plugin`.
      ```
    - Na seção "Arquitetura", se houver referência a `tools/` ou `tailwind`, ajustar

15. **Rodar `./mvnw test`** e garantir que a suíte passa (nenhum teste deve regredir — não há testes de CSS).

#### Testes a criar/atualizar
- Nenhum teste novo necessário — CSS puro não é testado por unit tests. A verificação é visual/funcional.
- Se o `AsteraConnectApplicationTests.java` (ou `AsteraCommApplicationTests.java`) fizer assertion de conteúdo HTML, verificar que não quebra.

#### Critérios de aceitação
- [ ] Arquivos Tailwind removidos: `output.css`, `input.css`, `tailwind.config.js`, `tailwind-watch.sh`, `tools/tailwindcss`
- [ ] `pom.xml` não contém `exec-maven-plugin` do Tailwind
- [ ] `Dockerfile` não contém nenhum passo de Tailwind (COPY do config/binary, RUN tailwindcss)
- [ ] `dev.sh` não menciona `tailwind-watch.sh`
- [ ] `README.md` não menciona Tailwind
- [ ] `AGENTS.md` descreve UI como "Thymeleaf + HTMX + CSS puro" sem referência a build de CSS
- [ ] Templates referenciam `/css/app.css` (não `/css/output.css`)
- [ ] Nenhum template contém classes `@tailwind`-specific (ex.: `@apply`, `bg-[#xxx]` em estilo Tailwind, `text-[13px]`, etc.) — todas convertidas para classes semânticas ou utilitárias CSS puro
- [ ] Nenhum `output.css` é gerado durante o build Maven
- [ ] `mvn clean package -DskipTests` compila sem erro (verificar que o `generate-resources` sem Tailwind não falha)
- [ ] `mvn test` passa sem regressão
- [ ] Visual da aplicação é identico ao anterior (sidebar, login, dashboard, listagens, modais, relatórios, paginação, toasts)
- [ ] Commit no padrão `refactor(rf-104): remove tailwind css e migra para css puro`
- [ ] Entrada no `doc/CHANGELOG.md` seção `[Unreleased]` e descrição detalhada em `doc/release-notes/unreleased.md`
- [ ] Remoção da task do `doc/backlog.md` após conclusão

#### Riscos e observações
- **Escopo muito grande:** esta task toca 48 templates + infra. O Codificador deve ser metódico e converter template a template, compilando a cada lote. Se identificar que o escopo é insuficiente para uma única sessão do Codificador, o Arquiteto pode quebrar em RF-104a (infra + layouts) e RF-104b (templates de página). **Decisão:** manter como task única, mas o Codificador pode rodar `mvn compile` incrementalmente.
- **Classes arbitrárias Tailwind:** classes como `text-[13px]`, `w-[220px]`, `bg-[#1D9E75]`, `hover:bg-white/[0.05]` são o maior volume de trabalho. Elas não podem ser utilities genéricas — devem virar classes nomeadas no `app.css`. O Codificador deve ser criterioso: nomes semânticos para componentes (`.sidebar-link`, `.btn-primary`), nomes genéricos para utilities reutilizáveis (`.gap-2`, `.mb-4`, `.text-center`).
- **`th:classappend` com condicionais:** muitos templates usam `th:classappend` para adicionar classes condicionais (ex.: `active`, `row-selected`). O Codificador deve garantir que as classes condicionais existam no `app.css`.
- **JS inline que manipula classes Tailwind:** o `base.html` tem JS que adiciona/remove `max-h-0`, `max-h-[200px]`, `rotate-180`, `hidden`. Esses DEVEM ser substituídos por classes nomeadas no `app.css` e o JS atualizado para usar os novos nomes.
- **O Codificador NÃO deve alterar Java (controllers, services, repositories)** — esta task é exclusivamente de frontend (CSS + templates) e build infra.
- **O Codificador NÃO deve criar migration Flyway** — nenhuma alteração de schema.
- **O Codificador NÃO deve adicionar nenhuma dependência nova ao pom.xml.**
- **O binário `tools/tailwindcss` tem 41 MB** — certeza de remover do git. Considerar `git rm` para que seja removido do histórico no próximo garbage collect.

---

### RF-105 · PDF e tradução de status no Histórico de Ligações

**Status:** CONCLUÍDA
- **Prioridade:** ALTA
- **US relacionada:** US-080
- **Sprint:** —
- **Arquivos:**
  - `backend/src/main/java/com/dionialves/AsteraComm/report/callhistory/CallHistoryController.java` (editar)
  - `backend/src/main/java/com/dionialves/AsteraComm/report/callhistory/CallHistoryService.java` (editar)
  - `backend/src/main/java/com/dionialves/AsteraComm/report/callhistory/CallHistoryLineDTO.java` (editar)
  - `backend/src/main/resources/templates/pages/reports/call-history.html` (editar)
  - `backend/src/main/resources/templates/pages/reports/call-history-table.html` (editar)
- **Dependências:** RF-093 (extração de PDF de CostPerCircuitService)

#### Contexto / Problema

O relatório de Histórico de Ligações (`US-080`) exibe status de chamada em inglês diretamente do campo `Call.disposition` (ex.: `ANSWERED`, `NO ANSWER`, `BUSY`, `FAILED`). Como esse relatório pode ser compartilhado com clientes finais, os termos precisam estar em português para serem compreensíveis.

Além disso, o relatório de Custo por Circuito já oferece download em PDF via botão "Baixar PDF" no canto superior direito acima da tabela — o Histórico de Ligações precisa ter o mesmo padrão.

**Comportamento observado:**
- Template `call-history-table.html` mostra `${line.disposition}` crú (em inglês).
- Não há botão de download no template.
- Não há geração de PDF no backend.

**Comportamento esperado:**
- Status exibidos em português: `ANSWERED` → "Atendida", `NO ANSWER` → "Não Atendeu", `BUSY` → "Ocupado", `FAILED` → "Falhou".
- Botão "Baixar PDF" visível no canto superior direito acima da tabela, replicando o padrão de `cost-per-circuit-table.html`.
- Endpoint `/reports/call-history/pdf` que gera PDF idêntico à visualização da tabela (colunas: Data/Hora, Origem, Destino, Tipo, Direção, Duração, Status).

#### Abordagem escolhida

1. **Traduzir status no backend (não no SQL/query):** A tradução acontece no método `toLineDTO` do `CallHistoryService`, através de um método utilitário privado `translateDisposition(String)`. Isso mantém o DTO com o valor final em português e não altera a entidade `Call`. Alternativa de fazer tradução no template foi descartada porque o PDF consome o mesmo DTO — traduzir no backend garante consistência entre HTML e PDF.

2. **Gerar PDF via OpenPDF (`com.lowagie`):** Seguir o padrão já existente em `CostPerCircuitService.generateCostPerCircuitPdf` e `AuditService.generatePdf`. O método `generateCallHistoryPdf` será adicionado ao `CallHistoryService`, recebendo os mesmos parâmetros do relatório (`circuitNumber`, `month`, `year`).

3. **Botão no template:** O botão é adicionado ao fragmento `call-history-table.html`, replicando o elemento `<a>` com ícone SVG de download já existente no `cost-per-circuit-table.html`, posicionado no canto direito acima da tabela.

#### Passo-a-passo de implementação

1. **Editar** `backend/src/main/java/com/dionialves/AsteraComm/report/callhistory/CallHistoryLineDTO.java` — adicionar campo `directionLabel` e `dispositionLabel` para encapsular as strings em português
   ```java
   public record CallHistoryLineDTO(
           String        uniqueId,
           LocalDateTime callDate,
           String        callerNumber,
           String        dst,
           String        dispositionLabel,     // ex.: "Atendida" (traduzido)
           CallType      callType,
           CallDirection direction,
           String        directionLabel,       // ex.: "Efetuada" / "Recebida"
           int           billSeconds,
           Integer       durationSeconds
   ) {}
   ```
   - Atualizar também o construtor/invocação no `CallHistoryService`. O Codificador deve garantir que todos os lugares que instanciam `CallHistoryLineDTO` passem os novos parâmetros.

2. **Editar** `backend/src/main/java/com/dionialves/AsteraComm/report/callhistory/CallHistoryService.java` — adicionar tradução e geração de PDF
   - Adicionar os imports de OpenPDF no topo (já existem no projeto via OpenPDF/lowagie):
     ```java
     import com.lowagie.text.*;
     import com.lowagie.text.Font;
     import com.lowagie.text.Rectangle;
     import com.lowagie.text.pdf.PdfPCell;
     import com.lowagie.text.pdf.PdfPTable;
     import com.lowagie.text.pdf.PdfWriter;
     import java.awt.Color;
     import java.io.ByteArrayOutputStream;
     import java.time.format.DateTimeFormatter;
     ```
   - Substituir o método `toLineDTO` para popular os labels em português:
     ```java
     private CallHistoryLineDTO toLineDTO(Call call) {
         int billSec = call.getBillSeconds();
         Integer durationSec = billSec > 3 ? call.getDurationSeconds() : null;
         return new CallHistoryLineDTO(
                 call.getUniqueId(),
                 call.getCallDate(),
                 call.getCallerNumber(),
                 call.getDst(),
                 translateDisposition(call.getDisposition()),
                 call.getCallType(),
                 call.getDirection(),
                 call.getDirection() == CallDirection.OUTBOUND ? "Efetuada" : "Recebida",
                 billSec,
                 durationSec
         );
     }

     private String translateDisposition(String disposition) {
         if (disposition == null) return "—";
         return switch (disposition.trim().toUpperCase()) {
             case "ANSWERED"  -> "Atendida";
             case "NO ANSWER" -> "Não Atendeu";
             case "BUSY"      -> "Ocupado";
             case "FAILED"    -> "Falhou";
             default          -> disposition;
         };
     }
     ```
   - Adicionar método público `generatePdf` no `CallHistoryService`:
     ```java
     public byte[] generatePdf(String circuitNumber, int month, int year) {
         CallHistoryResultDTO result = getHistory(circuitNumber, month, year);
         DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm");

         String[] meses = {"Janeiro","Fevereiro","Março","Abril","Maio","Junho",
                           "Julho","Agosto","Setembro","Outubro","Novembro","Dezembro"};
         String mesNome = (month >= 1 && month <= 12) ? meses[month - 1] : String.valueOf(month);

         try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
             Document doc = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);
             PdfWriter.getInstance(doc, baos);
             doc.open();

             Font titleFont  = new Font(Font.HELVETICA, 14, Font.BOLD, Color.WHITE);
             Font headerFont = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
             Font bodyFont   = new Font(Font.HELVETICA, 9, Font.NORMAL, new Color(55, 65, 81));
             Font footFont   = new Font(Font.HELVETICA, 9, Font.BOLD,   new Color(55, 65, 81));

             // Título
             PdfPTable header = new PdfPTable(1);
             header.setWidthPercentage(100);
             PdfPCell titleCell = new PdfPCell(new Phrase(
                     "Histórico de Ligações   |   Circuito: " + circuitNumber + "   |   " + mesNome + " / " + year, titleFont));
             titleCell.setBackgroundColor(new Color(39, 39, 42));
             titleCell.setBorder(Rectangle.NO_BORDER);
             titleCell.setPadding(10);
             header.addCell(titleCell);
             doc.add(header);
             doc.add(new Paragraph(" "));

             // Tabela
             PdfPTable table = new PdfPTable(new float[]{2f, 1.5f, 1.5f, 1.2f, 1f, 1f, 1.2f});
             table.setWidthPercentage(100);
             Color headerBg = new Color(39, 39, 42);
             String[] cols = {"Data/Hora", "Origem", "Destino", "Tipo", "Direção", "Duração", "Status"};
             for (String col : cols) {
                 PdfPCell cell = new PdfPCell(new Phrase(col, headerFont));
                 cell.setBackgroundColor(headerBg);
                 cell.setBorder(Rectangle.NO_BORDER);
                 cell.setPadding(6);
                 cell.setHorizontalAlignment(Element.ALIGN_LEFT);
                 table.addCell(cell);
             }

             Color rowAlt = new Color(249, 250, 251);
             for (int i = 0; i < result.lines().size(); i++) {
                 CallHistoryLineDTO line = result.lines().get(i);
                 Color bg = (i % 2 == 1) ? rowAlt : Color.WHITE;
                 String tipo = switch (line.callType()) {
                     case FIXED_LOCAL          -> "Fixo Local";
                     case MOBILE_LOCAL         -> "Móvel Local";
                     case FIXED_LONG_DISTANCE  -> "Fixo LD";
                     case MOBILE_LONG_DISTANCE -> "Móvel LD";
                     default -> "—";
                 };

                 int sec = line.billSeconds();
                 int h = sec / 3600; int m = (sec % 3600) / 60; int s = sec % 60;
                 String duration = (h > 0 ? h + ":" : "") + (m < 10 ? "0" : "") + m + ":" + (s < 10 ? "0" : "") + s;

                 addPdfCell(table, line.callDate().format(dtf), bodyFont, Element.ALIGN_LEFT, bg);
                 addPdfCell(table, line.callerNumber() != null ? line.callerNumber() : "—", bodyFont, Element.ALIGN_LEFT, bg);
                 addPdfCell(table, line.dst(), bodyFont, Element.ALIGN_LEFT, bg);
                 addPdfCell(table, tipo, bodyFont, Element.ALIGN_LEFT, bg);
                 addPdfCell(table, line.directionLabel(), bodyFont, Element.ALIGN_LEFT, bg);
                 addPdfCell(table, duration, bodyFont, Element.ALIGN_RIGHT, bg);
                 addPdfCell(table, line.dispositionLabel(), bodyFont, Element.ALIGN_LEFT, bg);
             }
             doc.add(table);
             doc.close();
             return baos.toByteArray();
         } catch (Exception e) {
             throw new RuntimeException("Erro ao gerar PDF de histórico de ligações", e);
         }
     }

     private void addPdfCell(PdfPTable table, String text, Font font, int align, Color bg) {
         PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "—", font));
         cell.setBackgroundColor(bg);
         cell.setBorder(Rectangle.TOP);
         cell.setPadding(6);
         cell.setHorizontalAlignment(align);
         table.addCell(cell);
     }
     ```

3. **Editar** `backend/src/main/java/com/dionialves/AsteraComm/report/callhistory/CallHistoryController.java` — adicionar endpoint PDF
   ```java
   @GetMapping("/pdf")
   @ResponseBody
   public ResponseEntity<byte[]> pdf(@RequestParam String circuitNumber,
                                     @RequestParam int month,
                                     @RequestParam int year) {
       byte[] pdfBytes = service.generatePdf(circuitNumber, month, year);
       HttpHeaders headers = new HttpHeaders();
       headers.setContentType(MediaType.APPLICATION_PDF);
       headers.setContentDisposition(ContentDisposition.attachment()
               .filename("historico-ligacoes-" + circuitNumber + "-" + month + "-" + year + ".pdf")
               .build());
       return ResponseEntity.ok().headers(headers).body(pdfBytes);
   }
   ```
   - Adicionar imports:
     ```java
     import org.springframework.http.ContentDisposition;
     import org.springframework.http.HttpHeaders;
     import org.springframework.http.MediaType;
     import org.springframework.http.ResponseEntity;
     import org.springframework.web.bind.annotation.ResponseBody;
     ```

4. **Editar** `backend/src/main/resources/templates/pages/reports/call-history-table.html` — adicionar botão PDF e tradução de badge
   - Localizar a abertura do card de contexto (`<div class="bg-white rounded-xl p-4 mb-4 border border-[#e0e0e0]">`) e substituir o bloco interno por uma `flex` com o contexto à esquerda e o botão à direita:
     ```html
     <!-- Card de contexto + botão PDF -->
     <div class="bg-white rounded-xl p-4 mb-4 border border-[#e0e0e0]">
       <div class="flex items-center justify-between">
         <div class="flex gap-8">
           <div>
             <p class="text-[11px] font-medium uppercase tracking-wide text-[#888] mb-1">Circuito</p>
             <p class="text-[15px] font-medium text-[#1a1a1a] font-mono" th:text="${result.circuitNumber}">—</p>
           </div>
           <div>
             <p class="text-[11px] font-medium uppercase tracking-wide text-[#888] mb-1">Período</p>
             <p class="text-[15px] font-medium text-[#1a1a1a]"
                th:text="${#temporals.monthName(result.month)} + ' / ' + ${result.year}">—</p>
           </div>
         </div>
         <a id="btn-pdf-call-history" th:href="${'/reports/call-history/pdf?circuitNumber=' + result.circuitNumber + '&month=' + result.month + '&year=' + result.year}"
            class="flex items-center gap-1.5 bg-white border border-[#e0e0e0] text-[#1a1a1a] text-[13px] font-medium px-3 py-[9px] rounded-md hover:bg-[#f5f5f5] transition-colors no-underline">
           <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
                stroke-linecap="round" stroke-linejoin="round">
             <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
             <polyline points="7 10 12 15 17 10"/>
             <line x1="12" y1="15" x2="12" y2="3"/>
           </svg>
           Baixar PDF
         </a>
       </div>
     </div>
     ```
   - Remover o card antigo que era apenas os dois campos (manter o mesmo conteúdo visual, mas com o botão acima).
   - No badge de direção, manter a lógica Thymeleaf, mas agora o `directionLabel` já vem traduzido do DTO. Atualizar o `th:text` para usar o label:
     ```html
     <span class="rounded-[99px] text-[11px] px-[8px] py-[2px] font-medium whitespace-nowrap
                  ${line.direction?.name() == 'OUTBOUND' ? 'bg-[#E1F5EE] text-[#085041]' : 'bg-[#FAEEDA] text-[#854F0B]'}"
           th:text="${line.directionLabel}">
     </span>
     ```
   - Atualizar a célula de Status para usar `dispositionLabel` em vez de `disposition`:
     ```html
     <span class="text-[13px] font-mono text-[#1a1a1a]" th:text="${line.dispositionLabel}"></span>
     ```

5. **Editar** `backend/src/main/resources/templates/pages/reports/call-history.html` — incluir botão PDF na tabela (ou garantir que o HTML do fragmento carregado via HTMX contém o link). Como o botão está dentro do fragmento `call-history-table.html`, não há alteração necessária nesse arquivo. O Codificador deve apenas garantir que o `id="btn-pdf-call-history"` funcione — não há toggle ou JS a ser mantido para este relatório.

6. **Rodar `./mvnw test`** e garantir que a suíte passa (incluindo `CallHistoryServiceTest`).

#### Testes a criar/atualizar
- `src/test/java/.../report/callhistory/CallHistoryServiceTest.java` — novos cenários:
  - `translateDisposition_returnsPortugueseLabels` — verifica mapeamento `ANSWERED` → "Atendida", `NO ANSWER` → "Não Atendeu", caso nulo → "—".
  - `translateDisposition_returnsOriginalForUnknownValue` — valor não mapeado (`CANCELLED`) retorna o valor original.
  - `generatePdf_returnsNonEmptyBytes` — simula com calls e retorna `byte[]` não vazio (verificar via mock/stub do repositório).
- Atualizar helper `buildCall` no teste: o `CallHistoryLineDTO` agora terá mais parâmetros; os testes existentes que instanciam `CallHistoryLineDTO` diretamente precisam ser atualizados para passar os 2 novos campos (`dispositionLabel`, `directionLabel`).

#### Critérios de aceitação
- [ ] O relatório exibe os status das chamadas em português: Atendida, Não Atendeu, Ocupado, Falhou.
- [ ] Valores de `disposition` não mapeados são exibidos como recebidos do CDR (em inglês ou conforme o valor original).
- [ ] Botão "Baixar PDF" aparece no canto superior direito acima da tabela em `call-history-table`.
- [ ] O link do botão contém os parâmetros `circuitNumber`, `month`, `year` dinamicamente.
- [ ] O endpoint `/reports/call-history/pdf` retorna um PDF contendo a tabela de chamadas com colunas Data/Hora, Origem, Destino, Tipo, Direção, Duração, Status.
- [ ] O nome do arquivo PDF é `historico-ligacoes-{circuitNumber}-{month}-{year}.pdf`.
- [ ] O badge de direção ainda exibe "Efetuada" (OUTBOUND) / "Recebida" (INBOUND) com as cores corretas.
- [ ] Os campos `callType` exibidos no PDF estão em português (Fixo Local, Móvel Local, Fixo LD, Móvel LD).
- [ ] Nenhum teste existente regrediu.
- [ ] `./mvnw test` passa sem warnings de compilação.
- [ ] Commit no padrão `refactor(rf-105): pdf-e-traducao-de-status-no-historico-de-ligacoes`.
- [ ] Entrada no `doc/CHANGELOG.md` seção `[Unreleased]` e descrição detalhada em `doc/release_notes/unreleased.md`.
- [ ] Remoção da task do `doc/BACKLOG.md` após conclusão.

#### Riscos e observações
- **Inconsistência de `disposition`:** O Asterisk pode produzir outras variações de status além das 4 listadas. Se o campo for alinhado (`NOANSWER` sem espaço, `CONGESTION`, `CANCELLED`, etc.), a tradução não será feita. O método `translateDisposition` usa `toUpperCase()` e `trim()`, e o `default` retorna o valor original — isso é aceitável por enquanto.
- **Reutilização de OpenPDF:** O projeto usa o mesmo motor PDF (`com.lowagie`) em `CostPerCircuitService` e `AuditService`. A tabela do Histórico tem 7 colunas; o PDF deve usar `PageSize.A4.rotate()` (paisagem) para caber.
- **Formatação de data:** Usar o mesmo padrão da tela (`dd/MM/yy HH:mm`) no PDF, via `DateTimeFormatter`.
- **O Codificador NÃO deve:**
  - Alterar a entidade `Call`, migration ou tabela do banco.
  - Alterar os dados persistidos na coluna `disposition`.
  - Adicionar dependências novas ao `pom.xml` (OpenPDF já é transitiva).
  - Traduzir os labels no template Thymeleaf em vez do DTO — isso quebraria o PDF.
- **Dependência RF-093:** Caso `RF-093` (decomposição de serviços com PDF) já tenha sido executada, o PDF pode estar extraído para uma classe `CallHistoryPdfGenerator`. Se esse for o caso, o Codificador deve seguir o padrão de RF-093 e criar `CallHistoryPdfGenerator` separado em vez de deixar o método no `CallHistoryService`.
