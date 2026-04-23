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
9. [US-080 — Histórico de ligações por circuito e mês](#us-080)

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
18. [RF-096 — Normalizar número de DID no lookup inbound do processamento de ligações](#rf-096)
19. [RF-097 — Reverter elementos desconexos da Auditoria (direção, filtro, totalizadores)](#rf-097)
20. [RF-100 — Vincular circuito no processamento via channel e dstChannel](#rf-100)
21. [RF-103 — Relatório de chamadas órfãs: paginação, loader funcional e vinculação](#rf-103)
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

### RF-097 · Reverter elementos desconexos da Auditoria (direção, filtro, totalizadores)

> **⚠️ Esta task anula parcialmente a RF-094.** A RF-094 adicionou `direction` no domínio `Call` (correto), mas introduziu elementos desconexos na ferramenta de Auditoria: coluna "Direção", toggle "Apenas ligações efetuadas", e totalizadores de chamadas recebidas com custo. O propósito da Auditoria é exclusivamente de **custeio de chamadas efetuadas** (OUTBOUND). Esses elementos poluem a interface e distorcem o resumo para o usuário.

- **Tipo:** Refactor
- **Prioridade:** ALTA
- **US relacionada:** US-072 (Auditoria de custeio)
- **Sprint:** —
- **Arquivos:**
  - `backend/src/main/java/com/dionialves/AsteraComm/report/audit/AuditService.java` (editar)
  - `backend/src/main/java/com/dionialves/AsteraComm/report/audit/AuditCallLineDTO.java` (editar)
  - `backend/src/main/java/com/dionialves/AsteraComm/report/audit/AuditResultDTO.java` (editar)
  - `backend/src/main/java/com/dionialves/AsteraComm/report/audit/AuditSummaryDTO.java` (editar)
  - `backend/src/main/java/com/dionialves/AsteraComm/report/ReportViewController.java` (editar)
  - `backend/src/test/java/com/dionialves/AsteraComm/report/audit/AuditServiceTest.java` (editar)
  - `backend/src/main/resources/templates/pages/reports/audit-table.html` (editar)
  - `backend/src/main/resources/templates/pages/reports/audit.html` (editar)
- **Dependências:** US-080 (novo relatório de histórico, para onde a coluna "Direção" e o filtro migrarão)

#### Contexto / Problema
Após a RF-094, o relatório de Auditoria passou a exibir:
1. Coluna "Direção" (badge Recebida / Efetuada) na tabela detalhada e no PDF — irrelevante para custeio.
2. Toggle "Apenas ligações efetuadas" — a Auditoria deveria mostrar **apenas** OUTBOUND por definição.
3. Chamadas INBOUND apareciam na simulação com custo, quota e acumulador — distorce o resumo de consumo do plano, pois a franquia se aplica apenas a chamadas efetuadas.
4. Totalizadores de resumo passaram a incluir chamadas recebidas no cálculo.

#### Abordagem escolhida
Remover da `AuditService` e dos DTOs de auditoria qualquer referência a `direction`. O `CallService` (`findByCircuitNumberAndPeriod`) internamente já filtra OUTBOUND apenas, mas o simulador não. Em vez de adicionar o filtro no `AuditService`, usar o repository existente e manter o comportamento original de só exibir custo de chamadas efetuadas (revertendo para o estado anterior ao adicionamento do `CallDirection` no relatório). A entidade `Call` continua com o campo `direction`, mas a ferramenta de Auditoria não lê mais esse campo. O filtro `onlyOutgoing` é removido completamente da API e templates.

**Alternativa descartada:** manter `onlyOutgoing=true` como default e manter a coluna. Rejeitado porque a UI ficaria poluída e o PDF continuaria despropositado para o usuário final.

#### Passo-a-passo de implementação

1. **Editar** `backend/src/main/java/com/dionialves/AsteraComm/report/audit/AuditCallLineDTO.java` — remover o campo `direction` (último parâmetro do record).
   ```java
   public record AuditCallLineDTO(
           String        uniqueId,
           LocalDateTime callDate,
           String        dst,
           CallType      callType,
           int           billSeconds,
           BigDecimal    ratePerMinute,
           BigDecimal    quotaUsedThisCall,
           BigDecimal    quotaAccumulated,
           BigDecimal    cost
   ) {}
   ```

2. **Editar** `backend/src/main/java/com/dionialves/AsteraComm/report/audit/AuditService.java`:
   - Em `simulate(String circuitNumber, int month, int year)`, remover o overload com `boolean onlyOutgoing`.
   - Em `buildResult(...)`, remover o parâmetro `boolean onlyOutgoing`. Remover o `if (onlyOutgoing)` e a variável `filteredLines` — usar `lines` diretamente.
   - No cálculo de `summary`, usar `lines` (não `filteredLines`).
   - No `lines.add(new AuditCallLineDTO(...))`, remover o último argumento `call.getDirection()`.
   - No método `generatePdf(...)`, remover os overloads e o parâmetro `onlyOutgoing`. Remover a passagem de `onlyOutgoing` para `simulate`.
   - No PDF, remover o cabeçalho "Direção" (se houver — o PDF ainda não foi alterado para mostrar, mas verificar).

3. **Editar** `backend/src/main/java/com/dionialves/AsteraComm/report/ReportViewController.java`:
   - Em `auditSimulation(...)`, remover o parâmetro `@RequestParam(defaultValue = "false") boolean onlyOutgoing`.
   - Remover `model.addAttribute("onlyOutgoing", onlyOutgoing)`.
   - Na chamada `auditService.simulate(circuitNumber, month, year, onlyOutgoing)`, voltar para `auditService.simulate(circuitNumber, month, year)`.
   - Em `auditPdf(...)`, remover o parâmetro `@RequestParam(defaultValue = "false") boolean onlyOutgoing` e a passagem para `generatePdf`.

4. **Editar** `backend/src/main/resources/templates/pages/reports/audit.html`:
   - Remover o `hx-include` que referencia os toggles (deixar apenas `#audit-form` para o botão Processar).

5. **Editar** `backend/src/main/resources/templates/pages/reports/audit-table.html`:
   - Remover o toggle "Apenas ligações efetuadas" (div com `id="toggle-outgoing-table"` e seu span).
   - Remover a coluna "Direção" do header (`<span>...Direção...</span>`) e das linhas (badge span th:text Recebida/Efetuada).
   - Remover `th:attr="..., data-outgoing=${line.direction.name()}"`.
   - Ajustar o `grid-cols-audit` de volta para 8 colunas (remover a coluna Direção). Verificar CSS: `grid-template-columns: repeat(9, minmax(100px, 1fr))` voltar para `repeat(8, ...)`.
   - No JavaScript inline, remover o bloco de código inteiro do toggle `toggle-outgoing-table` e seu `addEventListener` (linhas ~209-229).
   - No `setParam(href, 'onlyOutgoing', ...)`, remover essa linha (se existir no script do PDF).
   - No link do PDF `btn-pdf-audit`, remover o parâmetro `onlyOutgoing`.

6. **Editar** `backend/src/test/java/com/dionialves/AsteraComm/report/audit/AuditServiceTest.java`:
   - Remover os testes de filtragem por direção (`simulate_returnsOnlyOutbound_whenOnlyOutgoingTrue`, `simulate_returnsAll_whenOnlyOutgoingFalse`, `simulate_inboundCallsAppearInResult`).
   - Ajustar os testes restantes para não criarem chamadas com `CallDirection` diferente de OUTBOUND.
   - O `buildCall` helper ainda pode criar com direction, mas o teste não verifica direção no resultado. Ou simplificar o helper para não aceitar mais o parâmetro de direction.

#### Testes a criar/atualizar
- `backend/src/test/java/com/dionialves/AsteraComm/report/audit/AuditServiceTest.java` — remover os 3 testes de direção.
- Verificar que todos os demais testes de `AuditServiceTest` ainda passam (sem referência a `direction` nos asserts).

#### Critérios de aceitação
- [ ] O relatório de Auditoria não exibe a coluna "Direção".
- [ ] O relatório de Auditoria não possui o toggle "Apenas ligações efetuadas".
- [ ] O PDF de Auditoria não exibe a coluna "Direção" (se o PDF já tiver sido feito; senão, garantir que o generator não tenta).
- [ ] O método `AuditService.simulate()` não aceita mais o parâmetro `onlyOutgoing`.
- [ ] Chamadas em testes de auditoria são consideradas OUTBOUND por padrão (sem assert inbound).
- [ ] `./mvnw test` passa sem regressão nos testes restantes.
- [ ] Commit no padrão `refactor(rf-097): remove direcao e filtro de ligacoes da auditoria`.
- [ ] Entrada no `doc/changelog.md` seção `[Unreleased]` e descrição detalhada em `doc/release_notes/unreleased.md`.
- [ ] Remoção da task do `doc/backlog.md` após conclusão.

#### Riscos e observações
- **RF-093 (decomposição de PDF):** se o Codificador executar RF-097 antes de RF-093, o `AuditPdfGenerator` precisará também ser ajustado. Ordenação recomendada: executar RF-097 primeiro, depois RF-093 (que já está no backlog).
- **CallProcessingServiceTest:** os testes que validam `INBOUND`/`OUTBOUND` no processamento de CDR devem permanecer intactos — a entidade `Call` ainda carrega `direction`. A task toca apenas na camada de relatório.
- **Audit.html header:** o link de voltar (`th:href="@{/reports}"`) será removido quando US-066 for executada.

---

### US-080 · Histórico de ligações por circuito e mês

- **Tipo:** User Story
- **Prioridade:** ALTA
- **US relacionada:** —
- **Sprint:** —
- **Arquivos:**
  - `backend/src/main/java/com/dionialves/AsteraComm/report/callhistory/CallHistoryService.java` (novo)
  - `backend/src/main/java/com/dionialves/AsteraComm/report/callhistory/CallHistoryController.java` (novo)
  - `backend/src/main/java/com/dionialves/AsteraComm/report/callhistory/CallHistoryLineDTO.java` (novo)
  - `backend/src/main/java/com/dionialves/AsteraComm/report/callhistory/CallHistoryResultDTO.java` (novo)
  - `backend/src/test/java/com/dionialves/AsteraComm/report/callhistory/CallHistoryServiceTest.java` (novo)
  - `backend/src/main/resources/templates/pages/reports/call-history.html` (novo)
  - `backend/src/main/resources/templates/pages/reports/call-history-table.html` (novo)
- **Dependências:** RF-097 (remoção dos elementos desconexos da Auditoria)

#### Contexto / Problema
O usuário precisa visualizar, para um circuito e um mês específicos, **todas as ligações efetuadas e recebidas daquele circuito**, sem qualquer cálculo de custo, franquia ou tarifa. É um histórico operacional — diferente da Auditoria, que é ferramenta de custeio.

#### Abordagem escolhida
Criar um novo endpoint `/reports/call-history` com serviço, controller e templates dedicados. Reutilizar a query `CallRepository.findByCircuitNumberAndPeriod` para buscar as calls do circuito no período. O DTO deve conter apenas informações descritivas da chamada (data, destino, duração, tipo, direção, status/disposição). Sem interação com `Plan`, `CallCostingService` ou campos de quota/custo. Interface seguindo o padrão visual da Auditoria, mas com colunas relevantes ao histórico.

#### Passo-a-passo de implementação

1. **Criar** `backend/src/main/java/com/dionialves/AsteraComm/report/callhistory/CallHistoryLineDTO.java`
   ```java
   public record CallHistoryLineDTO(
       String        uniqueId,
       LocalDateTime callDate,
       String        callerNumber,
       String        dst,
       String        disposition,
       CallType      callType,
       CallDirection direction,
       int           billSeconds,
       Integer       durationSeconds
   ) {}
   ```

2. **Criar** `backend/src/main/java/com/dionialves/AsteraComm/report/callhistory/CallHistoryResultDTO.java`
   ```java
   public record CallHistoryResultDTO(
       String                circuitNumber,
       int                   month,
       int                   year,
       int                   totalCalls,
       BigDecimal            totalMinutes,
       List<CallHistoryLineDTO> lines
   ) {}
   ```
   Totalizadores: total de chamadas e tempo total (em minutos, arredondado como em `AuditSummaryDTO`, mas sem custo/quota).

3. **Criar** `backend/src/main/java/com/dionialves/AsteraComm/report/callhistory/CallHistoryService.java`
   ```java
   @RequiredArgsConstructor
   @Service
   public class CallHistoryService {
       private final CircuitRepository circuitRepository;
       private final CallRepository    callRepository;

       public CallHistoryResultDTO getHistory(String circuitNumber, int month, int year) {
           Circuit circuit = circuitRepository.findByNumber(circuitNumber)
               .orElseThrow(() -> new NotFoundException("Circuito não encontrado: " + circuitNumber));

           List<Call> calls = callRepository.findByCircuitNumberAndPeriod(circuitNumber, month, year);

           List<CallHistoryLineDTO> lines = calls.stream()
               .map(c -> new CallHistoryLineDTO(...))
               .toList();

           int totalCalls = lines.size();
           int totalBillSec = lines.stream().mapToInt(CallHistoryLineDTO::billSeconds).sum();
           BigDecimal totalMinutes = BigDecimal.valueOf(Math.ceil(totalBillSec / 30.0))
               .divide(BigDecimal.valueOf(2), 1, RoundingMode.UNNECESSARY);

           return new CallHistoryResultDTO(circuitNumber, month, year, totalCalls, totalMinutes, lines);
       }
   }
   ```

4. **Criar** `backend/src/main/java/com/dionialves/AsteraComm/report/callhistory/CallHistoryController.java`
   ```java
   @Controller
   @RequiredArgsConstructor
   @RequestMapping("/reports/call-history")
   public class CallHistoryController {
       private final CallHistoryService service;

       @GetMapping
       public String index(Model model) {
           LocalDate now = LocalDate.now();
           model.addAttribute("currentMonth", now.getMonthValue());
           model.addAttribute("currentYear",  now.getYear());
           return "pages/reports/call-history";
       }

       @GetMapping("/table")
       public String table(@RequestParam String circuitNumber, @RequestParam int month, @RequestParam int year, Model model) {
           model.addAttribute("month", month);
           model.addAttribute("year", year);
           if (circuitNumber == null || circuitNumber.isBlank()) {
               model.addAttribute("errorMsg", "Selecione um circuito.");
           } else {
               model.addAttribute("result", service.getHistory(circuitNumber, month, year));
           }
           return "pages/reports/call-history-table :: table";
       }
   }
   ```

5. **Criar** template `backend/src/main/resources/templates/pages/reports/call-history.html` e `call-history-table.html` seguindo o padrão visual de Audit (`audit.html` + `audit-table.html`), mas sem:
   - Contexto de plano (Plano).
   - Colunas de tarifa, franquia, acumulador, custo.
   - Toggle de "relevantes".
   - Botão PDF.

   Colunas da tabela: Data/Hora | Origem | Destino | Tipo | Direção | Duração | Status (Disposition).
   Badge de direção: recebida (INBOUND) e efetuada (OUTBOUND).

6. **Adicionar** item no menu lateral (`layout/base.html`) na seção FINANCEIRO → "Histórico de Ligações" após "Auditoria". URL `/reports/call-history`. Destacar quando `currentPath` começar com `/reports/call-history`.

7. **Criar teste** `backend/src/test/java/com/dionialves/AsteraComm/report/callhistory/CallHistoryServiceTest.java`:
   - Cenário: `getHistory_throwsNotFound_whenCircuitNotFound`.
   - Cenário: `getHistory_returnsEmpty_whenNoCalls`.
   - Cenário: `getHistory_returnsCorrectLinesForMixedDirections` — calls OUTBOUND e INBOUND na lista.
   - Cenário: `getHistory_calculatesTotalMinutesCorrectly`.

#### Testes a criar/atualizar
- `CallHistoryServiceTest` — cenários acima.

#### Critérios de aceitação
- [ ] Endpoint `/reports/call-history` abre página com SearchSelect de circuito, mês e ano, e botão "Processar".
- [ ] O resultado exibe todas as ligações (efetuadas e recebidas) do circuito no mês/ano, com as colunas: Data/Hora, Origem, Destino, Tipo, Direção, Duração, Status.
- [ ] Badge "Efetuada" / "Recebida" corretamente colorido como no layout da Auditoria anterior à RF-097.
- [ ] Resumo exibe apenas total de chamadas e minutos totais (sem custo, franquia ou excedente).
- [ ] `./mvnw test` passa (testes novos + existentes sem regressão).
- [ ] Commit no padrão `feat(us-080): adiciona relatorio historico de ligacoes por circuito`.
- [ ] Entrada no `doc/changelog.md` e `doc/release_notes/unreleased.md`.
- [ ] Remoção da task do `doc/backlog.md` após conclusão.

#### Riscos e observações
- O Codificador não deve alterar o `AuditService` além do que está especificado na RF-097. Não é para fatorar a query comum entre os dois relatórios — cada um usa seu próprio service.
- O totalizador de minutos do histórico pode usar a mesma fórmula de arredondamento do histórico (ceil(billSeconds/30)/2) para consistência visual com a Auditoria.
- A query `findByCircuitNumberAndPeriod` internamente consulta a tabela de DID via JOIN; como o histórico quer **todas** as ligações (inclusive inbound via dst→did), essa query já serve perfeitamente — nenhuma alteração no repository é necessária.

---

### RF-100 · Vincular circuito no processamento via channel e dstChannel

- **Tipo:** Refactor
- **Prioridade:** ALTA
- **US relacionada:** —
- **Sprint:** —
- **Arquivos:**
  - `backend/src/main/java/com/dionialves/AsteraComm/call/CallProcessingService.java` (editar)
  - `backend/src/test/java/com/dionialves/AsteraComm/call/CallProcessingServiceTest.java` (editar)
- **Dependências:** —

#### Contexto / Problema

O `CallProcessingService.process()` hoje faz 2 tentativas para vincular o circuito a uma chamada:

1. **Via `channel`** — extrai o código com `ChannelParser.parse(cdr.getChannel())` e busca em `circuitRepository`. Funciona para ligações **outbound** (o `channel` contém `PJSIP/4933401714-xxx`).
2. **Via `dst → DID`** — quando a tentativa 1 falha, busca `didRepository.findByNumber(cdr.getDst())` e, se encontrar um DID com circuito, usa `did.getCircuit()`. Funciona apenas quando o `dst` está cadastrado como DID.

**O problema:** A tentativa 2 depende da tabela de DID estar completa e atualizada. Se o `dst` não estiver cadastrado como DID, a ligação fica órfã — mesmo que o `dstChannel` do CDR contenha claramente o circuito que atendeu a chamada (ex.: `PJSIP/4933401714-xxx`).

**Forma do CDR no Asterisk:**

| Cenário | `channel` (quem originou) | `dstChannel` (quem atendeu) |
|---|---|---|
| **Outbound** | `PJSIP/4933401714-xxx` (circuito) | `PJSIP/operadora-xxx` (tronco) |
| **Inbound** | `PJSIP/operadora-xxx` (tronco) | `PJSIP/4933401714-xxx` (circuito) |

Com essa tabela, a estratégia correta e suficiente é:
- **Tentativa 1 = `channel`** → resolve outbound.
- **Tentativa 2 = `dstChannel`** → resolve inbound.

O `dstChannel` é a **fonte de verdade do Asterisk** para "quem atendeu a ligação". Não depende de cadastro, é preenchido automaticamente pelo PBX. O `OrphanCallReportService` já usa exatamente essa lógica (channel → dstChannel) — a diferença é que o faz offline, somente no relatório.

Ao corrigir o `CallProcessingService`, a vinculação acontecerá automaticamente no processamento e o volume de chamadas órfãs será drasticamente reduzido.

#### Abordagem escolhida

Substituir a tentativa 2 (via `dst → DID → circuit`) por tentativa via `dstChannel` (usando o mesmo `ChannelParser`). A direção será determinada pela tentativa que resolveu:
- Se vinculou via `channel` → `OUTBOUND`.
- Se vinculou via `dstChannel` → `INBOUND`.
- Se nenhuma vinculou → `OUTBOUND` (default) e circuit permanece `null`.

Remover a dependência de `DIDRepository` do `CallProcessingService`, pois não será mais necessária.

**Alternativa descartada:** Manter as 3 tentativas (channel, dst→DID, dstChannel). Motivo: a tentativa via DID é redundante — se o `dstChannel` contém o circuito, ele será encontrado diretamente; se o `dstChannel` está vazio (ligação não atendida, disposition != ANSWERED), o circuito não poderia ser vinculado de qualquer forma. O DID adiciona uma query extra e uma dependência de cadastro sem ganho real.

#### Passo-a-passo de implementação

1. **Editar** `backend/src/main/java/com/dionialves/AsteraComm/call/CallProcessingService.java` — Substituir a tentativa 2 e a lógica de direção:

   Código atual (linhas 41-55) a ser substituído:
   ```java
   String circuitCode = channelParser.parse(cdr.getChannel());
   if (!circuitCode.isEmpty()) {
       circuitRepository.findByNumber(circuitCode).ifPresent(call::setCircuit);
   }
   // Tentativa 2: association via dst -> DID (inbound)
   // Direction: se dst casou com DID → INBOUND; caso contrário → OUTBOUND
   CallDirection direction = CallDirection.OUTBOUND;
   if (call.getCircuit() == null && cdr.getDst() != null && !cdr.getDst().isBlank()) {
       var didOpt = didRepository.findByNumber(cdr.getDst()).filter(did -> did.getCircuit() != null);
       if (didOpt.isPresent()) {
           call.setCircuit(didOpt.get().getCircuit());
           direction = CallDirection.INBOUND;
       }
   }
   call.setDirection(direction);
   ```

   Novo código:
   ```java
   // Tentativa 1: via channel → outbound (quem originou a ligação)
   String channelCode = channelParser.parse(cdr.getChannel());
   CallDirection direction = CallDirection.OUTBOUND;
   if (!channelCode.isEmpty()) {
       circuitRepository.findByNumber(channelCode).ifPresent(call::setCircuit);
   }
   // Tentativa 2: via dstChannel → inbound (quem atendeu a ligação)
   if (call.getCircuit() == null && cdr.getDstchannel() != null && !cdr.getDstchannel().isBlank()) {
       String dstChannelCode = channelParser.parse(cdr.getDstchannel());
       if (!dstChannelCode.isEmpty()) {
           circuitRepository.findByNumber(dstChannelCode).ifPresent(c -> {
               call.setCircuit(c);
               direction = CallDirection.INBOUND;
           });
       }
   }
   call.setDirection(direction);
   ```

2. **Editar** `backend/src/main/java/com/dionialves/AsteraComm/call/CallProcessingService.java` — Remover import e campo de `DIDRepository`:

   - Remover linha 6: `import com.dionialves.AsteraComm.did.DIDRepository;`
   - Remover linha 25: `private final DIDRepository didRepository;`

3. **Editar** `backend/src/test/java/com/dionialves/AsteraComm/call/CallProcessingServiceTest.java` — Atualizar testes:

   a. **Remover** o mock de `DIDRepository` (linhas 48-49):
   ```java
   @Mock
   private DIDRepository didRepository;
   ```

   b. **Remover** teste `process_shouldAssociateCircuitViaDstDid_whenInboundCall` (linhas 166-190) — cenário agora coberto pelo novo teste via dstChannel.

   c. **Remover** teste `process_shouldLeaveCircuitNull_whenDidNotFound` (linhas 192-209) — cenário substituído pelo novo teste de fallback.

   d. **Remover** teste `process_setsDirectionInbound_whenDstIsDid` (linhas 211-235) — substituído pelo novo teste de direção via dstChannel.

   e. **Remover** teste `process_setsDirectionOutbound_whenDstIsNotDid` (linhas 237-254) — o teste existente `process_shouldLeaveCircuitNull_whenChannelCodeNotFound` já cobre o caso de circuit not found; adicionar assert de direção nesse teste existente.

   f. **Adicionar** novo teste — vinculação via dstChannel:
   ```java
   @Test
   void process_shouldAssociateCircuitViaDstChannel_whenInboundCall() {
       Circuit circuit = new Circuit();
       circuit.setNumber("4933401714");

       CdrRecord cdr = buildCdr("1000.30", "ANSWERED", "PJSIP/operadora-0001");
       cdr.setDstchannel("PJSIP/4933401714-0001");

       when(cdrRepository.findUnprocessed()).thenReturn(List.of(cdr));
       when(callerIdParser.parse(any())).thenReturn("11933334444");
       when(callTypeClassifier.classify(any())).thenReturn(CallType.FIXED_LOCAL);
       when(channelParser.parse("PJSIP/operadora-0001")).thenReturn("operadora");
       when(circuitRepository.findByNumber("operadora")).thenReturn(Optional.empty());
       when(channelParser.parse("PJSIP/4933401714-0001")).thenReturn("4933401714");
       when(circuitRepository.findByNumber("4933401714")).thenReturn(Optional.of(circuit));

       callProcessingService.process();

       ArgumentCaptor<Call> captor = ArgumentCaptor.forClass(Call.class);
       verify(callRepository, times(2)).save(captor.capture());
       assertThat(captor.getAllValues().get(0).getCircuit()).isEqualTo(circuit);
       assertThat(captor.getAllValues().get(0).getDirection()).isEqualTo(CallDirection.INBOUND);
   }
   ```

   g. **Adicionar** novo teste — dstChannel não resolve, circuit permanece null:
   ```java
   @Test
   void process_shouldLeaveCircuitNull_whenBothChannelsFail() {
       CdrRecord cdr = buildCdr("1000.31", "ANSWERED", "PJSIP/operadora-0001");
       cdr.setDstchannel("PJSIP/unknown-0001");

       when(cdrRepository.findUnprocessed()).thenReturn(List.of(cdr));
       when(callerIdParser.parse(any())).thenReturn("11933334444");
       when(callTypeClassifier.classify(any())).thenReturn(CallType.FIXED_LOCAL);
       when(channelParser.parse("PJSIP/operadora-0001")).thenReturn("operadora");
       when(circuitRepository.findByNumber("operadora")).thenReturn(Optional.empty());
       when(channelParser.parse("PJSIP/unknown-0001")).thenReturn("unknown");
       when(circuitRepository.findByNumber("unknown")).thenReturn(Optional.empty());

       callProcessingService.process();

       ArgumentCaptor<Call> captor = ArgumentCaptor.forClass(Call.class);
       verify(callRepository, times(2)).save(captor.capture());
       assertThat(captor.getAllValues().get(0).getCircuit()).isNull();
       assertThat(captor.getAllValues().get(0).getDirection()).isEqualTo(CallDirection.OUTBOUND);
   }
   ```

   h. **Adicionar** novo teste — dstChannel vazio/nulo:
   ```java
   @Test
   void process_shouldLeaveCircuitNull_whenDstChannelIsEmpty() {
       CdrRecord cdr = buildCdr("1000.32", "ANSWERED", "PJSIP/operadora-0001");
       cdr.setDstchannel("");

       when(cdrRepository.findUnprocessed()).thenReturn(List.of(cdr));
       when(callerIdParser.parse(any())).thenReturn("11933334444");
       when(callTypeClassifier.classify(any())).thenReturn(CallType.FIXED_LOCAL);
       when(channelParser.parse("PJSIP/operadora-0001")).thenReturn("operadora");
       when(circuitRepository.findByNumber("operadora")).thenReturn(Optional.empty());

       callProcessingService.process();

       ArgumentCaptor<Call> captor = ArgumentCaptor.forClass(Call.class);
       verify(callRepository, times(2)).save(captor.capture());
       assertThat(captor.getAllValues().get(0).getCircuit()).isNull();
   }
   ```

   i. **Adicionar** assert de direção no teste existente `process_shouldAssociateCircuit_whenChannelMatchesExistingCircuit` (após o assert de circuit na linha 106):
   ```java
   assertThat(captor.getAllValues().get(0).getDirection()).isEqualTo(CallDirection.OUTBOUND);
   ```

   j. **Adicionar** assert de direção no teste existente `process_shouldLeaveCircuitNull_whenChannelCodeNotFound` (após o assert de circuit null na linha 122):
   ```java
   assertThat(captor.getAllValues().get(0).getDirection()).isEqualTo(CallDirection.OUTBOUND);
   ```

4. **Atualizar** o helper `buildCdr` no teste — adicionar `cdr.setDstchannel("");` como valor default após a linha 62, garantindo que todos os testes existentes que usam helper sem dstChannel não tenham o campo nulo.

5. **Rodar `./mvnw test`** e garantir que a suíte passa (incluindo os novos testes).

#### Testes a criar/atualizar
- `CallProcessingServiceTest` — **remover** 4 testes: `process_shouldAssociateCircuitViaDstDid_whenInboundCall`, `process_shouldLeaveCircuitNull_whenDidNotFound`, `process_setsDirectionInbound_whenDstIsDid`, `process_setsDirectionOutbound_whenDstIsNotDid`.
- `CallProcessingServiceTest` — **adicionar** 3 testes: `process_shouldAssociateCircuitViaDstChannel_whenInboundCall`, `process_shouldLeaveCircuitNull_whenBothChannelsFail`, `process_shouldLeaveCircuitNull_whenDstChannelIsEmpty`.
- `CallProcessingServiceTest` — **adicionar** assert de direção em 2 testes existentes: `process_shouldAssociateCircuit_whenChannelMatchesExistingCircuit` (OUTBOUND) e `process_shouldLeaveCircuitNull_whenChannelCodeNotFound` (OUTBOUND).
- `CallProcessingServiceTest` — **atualizar** helper `buildCdr` para setar `cdr.setDstchannel("")` como default.

#### Critérios de aceitação
- [ ] `CallProcessingService` não possui mais dependência de `DIDRepository` (import e campo removidos).
- [ ] Tentativa 1 usa `channel` para vincular circuito (comportamento inalterado para outbound).
- [ ] Tentativa 2 usa `dstChannel` para vincular circuito (novo comportamento para inbound).
- [ ] Direção é `OUTBOUND` quando o circuito é vinculado via `channel`.
- [ ] Direção é `INBOUND` quando o circuito é vinculado via `dstChannel`.
- [ ] Direção é `OUTBOUND` (default) quando nenhuma tentativa vincula o circuito.
- [ ] Chamadas inbound cujo `dstChannel` contém o código do circuito são automaticamente vinculadas — sem depender da tabela de DID.
- [ ] O `CallCostingService.applyCosting()` não é alterado — a lógica de `OUT_OF_SCOPE` para inbound com circuito vinculado continua correta.
- [ ] Todos os testes novos passam e nenhum teste existente regrediu.
- [ ] `./mvnw test` passa sem warnings de compilação.
- [ ] Commit no padrão `refactor(rf-100): vincula circuito via channel e dstchannel no processamento`.
- [ ] Entrada no `doc/changelog.md` seção `[Unreleased]` e descrição detalhada em `doc/release_notes/unreleased.md`.
- [ ] Remoção da task do `doc/backlog.md` após conclusão.

#### Riscos e observações
- **`dstChannel` vazio em CDRs não atendidos:** quando a ligação não é atendida (`NO ANSWER`, `BUSY`, `FAILED`), o Asterisk pode deixar o `dstChannel` vazio ou incompleto. Nesses casos, a tentativa 2 não resolve e a chamada fica órfã — igual ao comportamento atual. Não é regressão.
- **`dstChannel` preenchido também em outbound:** no CDR de uma ligação outbound, o `dstChannel` contém o nome do tronco (ex.: `PJSIP/operadora-xxx`). O `ChannelParser` extrairá `operadora`, e `circuitRepository.findByNumber("operadora")` retornará `empty` (pois troncos não são circuitos). Logo, a tentativa 2 não interfere no custeio outbound.
- **`OrphanCallReportService` NÃO deve ser alterado nesta task** — ele já usa a lógica correta (channel → dstChannel). Apenas o `CallProcessingService` precisa ser alinhado.
- **O Codificador NÃO deve alterar o `CallCostingService`** — a distinção OUTBOUND vs INBOUND para custeio permanece via `dcontext` + `EndpointRepository`, que já funciona corretamente.
- **O Codificador NÃO deve criar migration Flyway** — nenhuma alteração de schema é necessária.
- **Impacto na RF-103:** com esta correção, o volume de chamadas órfãs será drasticamente menor. O botão "Vincular circuitos" (RF-103) continuará útil para chamadas históricas e cenários excepcionais.

---

### RF-103 · Relatório de chamadas órfãs: paginação, loader funcional e vinculação

- **Tipo:** Refactor
- **Prioridade:** ALTA
- **US relacionada:** —
- **Sprint:** —
- **Arquivos:**
  - `backend/src/main/java/com/dionialves/AsteraComm/call/OrphanCallReportService.java` (editar)
  - `backend/src/main/java/com/dionialves/AsteraComm/call/OrphanCallReportController.java` (editar)
  - `backend/src/main/java/com/dionialves/AsteraComm/call/CallRepository.java` (editar)
  - `backend/src/main/resources/templates/pages/reports/orphan-calls.html` (editar)
  - `backend/src/main/resources/templates/pages/reports/orphan-calls-table.html` (editar)
  - `backend/src/test/java/com/dionialves/AsteraComm/call/OrphanCallReportServiceTest.java` (editar)
  - `backend/src/test/java/com/dionialves/AsteraComm/call/OrphanCallReportControllerTest.java` (editar)
  - `doc/CHANGELOG.md` (editar — remover RF-099 do `[Unreleased]`)
- **Dependências:** FIX-101 (NonUniqueResultException), RF-102 (N+1 queries)

#### Contexto / Problema

A RF-099 foi implementada fielmente ao plano original, mas o veredito do Revisor foi **BLOQUEADO** devido a três problemas críticos que impedem o uso do relatório em dados reais:

1. **Bug A — NonUniqueResultException (coberto pela FIX-101).** O `findByUniqueId` falha com CDRs duplicados.
2. **Bug B — N+1 massivo (coberto pela RF-102).** 23,5s para 12 chamadas; inutilizável com 10k+ registros.
3. **Sem paginação (novo).** Meses com 10.000+ chamadas órfãs geram uma resposta HTML monolítica que o navegador demora para renderizar. O endpoint `findOrphanCallsByPeriod` devolve `List<Call>` sem limitação — todos os registros do mês são carregados na memória e enviados ao cliente em uma única resposta HTML.
4. **Loader dos botões não funciona (novo).** Os scripts de `htmx:beforeRequest`/`htmx:afterRequest` estão dentro de blocos `<script>` que rodam apenas no `htmx:afterSwap`, mas os event listeners são registrados **depois** que o botão já existe no DOM. O problema: o listener de `beforeRequest` só é adicionado após o primeiro `afterSwap`, o que significa que **a primeira requisição nunca ativa o loader**. Para o botão "Vincular", o script está no fragmento da tabela que é re-renderizado a cada swap — os listeners se perdem porque o botão é destruído e recriado, mas os listeners só são reanexados no próximo `afterSwap`.

Esta task é o **replanejamento da RF-099** — absorve as funcionalidades originais (card na index, loader, vinculação de circuitos) e corrige os problemas de paginação e loader.

#### Abordagem escolhida

1. **Paginação no backend:** `CallRepository.findOrphanCallsByPeriod` passa a aceitar `Pageable` e retornar `Page<Call>`. O controller recebe `page` e `size` como parâmetros, com default de 50 registros por página. O template usa o fragmento `fragments/pagination :: pagination` (já existente no projeto) para navegação entre páginas.

2. **Loader via `htmx-indicator` (padrão nativo HTMX):** em vez de scripts `addEventListener` frágeis, usar a diretiva nativa `hx-indicator` do HTMX. Adicionar `hx-indicator="#spinner-process"` no botão "Processar" e `hx-indicator="#spinner-link"` no botão "Vincular". Os spinners recebem a classe `htmx-indicator` (que por padrão é `display:none` e fica visível durante a requisição). Isso elimina completamente os scripts `beforeRequest`/`afterRequest` e garante que o loader funcione na primeira requisição e após re-renderizações do fragmento.

3. **Mantidos da RF-099:** card "Chamadas Órfãs" na `index.html`, botão "Vincular circuitos (N)" no fragmento, `linkOrphanCalls` no service, feedback de sucesso/sem-resolvíveis no template, CSRF no form POST.

**Alternativa descartada para loader:** corrigir os `addEventListener` para usar `document.addEventListener('htmx:beforeRequest', ...)` com delegation. Motivo: mais complexo, propenso a leaks, e não resolve o caso da primeira requisição. O `hx-indicator` é a solução canônica do HTMX.

#### Passo-a-passo de implementação

1. **Editar** `backend/src/main/java/com/dionialves/AsteraComm/call/CallRepository.java` — alterar `findOrphanCallsByPeriod` para aceitar `Pageable`:
   ```java
   @Query(value = """
       SELECT * FROM asteracomm_calls
       WHERE circuit_number IS NULL
       AND EXTRACT(MONTH FROM call_date) = :month
       AND EXTRACT(YEAR  FROM call_date) = :year
       ORDER BY call_date DESC
       """,
       countQuery = """
       SELECT COUNT(*) FROM asteracomm_calls
       WHERE circuit_number IS NULL
       AND EXTRACT(MONTH FROM call_date) = :month
       AND EXTRACT(YEAR  FROM call_date) = :year
       """,
       nativeQuery = true)
   Page<Call> findOrphanCallsByPeriod(@Param("month") int month, @Param("year") int year, Pageable pageable);
   ```

2. **Editar** `backend/src/main/java/com/dionialves/AsteraComm/call/OrphanCallReportService.java` — atualizar `findOrphanCalls` para aceitar `Pageable`:

   a. Alterar a assinatura:
   ```java
   public Page<OrphanCallReportDTO> findOrphanCalls(int month, int year, Pageable pageable) {
   ```

   b. Substituir `List<Call> orphans = callRepository.findOrphanCallsByPeriod(month, year);` por:
   ```java
   Page<Call> orphansPage = callRepository.findOrphanCallsByPeriod(month, year, pageable);
   List<Call> orphans = orphansPage.getContent();
   ```

   c. No final, envelopar o resultado em `Page`:
   ```java
   return new PageImpl<>(result, pageable, orphansPage.getTotalElements());
   ```

   Adicionar imports:
   ```java
   import org.springframework.data.domain.Page;
   import org.springframework.data.domain.PageImpl;
   import org.springframework.data.domain.Pageable;
   ```

   d. O método `countResolvable` deve ser ajustado para não chamar `findOrphanCalls` (que agora é paginado). Criar um novo método `countResolvable(int month, int year)` que busca todos os orphans do período (sem paginação) para contagem. Usar a query `countOrphanCallsByPeriod` existente e uma busca não paginada interna:
   ```java
   public long countResolvable(int month, int year) {
       // Busca todos os orphans (sem paginação) para contagem de resolvíveis
       List<Call> allOrphans = callRepository.findOrphanCallsByPeriod(month, year, Pageable.unpaged()).getContent();
       // ... mesmo processamento batch dos CDRs e circuits ...
   }
   ```

   **Alternativa mais simples:** não buscar todos os orphans para contagem. Em vez disso, calcular `resolvableCount` a partir da página atual. A contagem total de resolvíveis é aproximada (só reflete a página corrente). Na prática, isso é suficiente: o botão "Vincular" vincula TODAS as chamadas resolvíveis do período (não apenas as da página), e o número no botão reflete a página atual. Se necessário, o controller pode fazer uma query `count` separada. **Decisão:** optar pela abordagem simples — `resolvableCount` reflete a página corrente. O `linkOrphanCalls` já opera sobre todos os resolvíveis, independente da página.

   Na verdade, repensando: o `linkOrphanCalls` atualmente chama `findOrphanCalls(month, year)` para obter a lista completa de resolvíveis. Se `findOrphanCalls` agora retorna `Page`, o `linkOrphanCalls` precisa de uma variante não paginada ou usar `Pageable.unpaged()`. Decisão: criar um método privado `findAllOrphanCallDTOs` que faz o processamento batch sem paginação (para `linkOrphanCalls` e `countResolvable`), e manter `findOrphanCalls(month, year, pageable)` como o método público paginado.

   Refatorar o `OrphanCallReportService` para ter:
   ```java
   // Método público paginado
   public Page<OrphanCallReportDTO> findOrphanCalls(int month, int year, Pageable pageable) {
       Page<Call> orphansPage = callRepository.findOrphanCallsByPeriod(month, year, pageable);
       List<OrphanCallReportDTO> dtos = buildReportDTOs(orphansPage.getContent(), month, year);
       return new PageImpl<>(dtos, pageable, orphansPage.getTotalElements());
   }

   // Método público para contagem
   public long countResolvable(int month, int year) {
       List<OrphanCallReportDTO> allDtos = findAllOrphanCallDTOs(month, year);
       return allDtos.stream().filter(OrphanCallReportDTO::resolvable).count();
   }

   // Método público para vinculação (usa lista completa)
   @Transactional
   public int linkOrphanCalls(int month, int year) {
       List<OrphanCallReportDTO> allDtos = findAllOrphanCallDTOs(month, year);
       int linked = 0;
       for (OrphanCallReportDTO dto : allDtos) {
           if (dto.resolvable() && dto.circuitCode() != null && !dto.circuitCode().isBlank()) {
               callRepository.linkCircuitByUniqueId(dto.uniqueId(), dto.circuitCode());
               linked++;
           }
       }
       return linked;
   }

   // Método privado: busca todos os orphans do período (sem página) e monta DTOs
   private List<OrphanCallReportDTO> findAllOrphanCallDTOs(int month, int year) {
       List<Call> orphans = callRepository.findOrphanCallsByPeriod(month, year, Pageable.unpaged()).getContent();
       return buildReportDTOs(orphans, month, year);
   }

   // Método privado: lógica de montagem dos DTOs (reutilizada)
   private List<OrphanCallReportDTO> buildReportDTOs(List<Call> orphans, int month, int year) {
       // ... lógica batch da RF-102 (cdrByUniqueId, circuitByNumber, etc.)
   }
   ```

3. **Editar** `backend/src/main/java/com/dionialves/AsteraComm/call/OrphanCallReportController.java` — atualizar endpoints:

   a. `table` — aceitar parâmetros de paginação:
   ```java
   @GetMapping("/table")
   public String table(@RequestParam int month, @RequestParam int year,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "50") int size,
                       Model model) {
       Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "callDate"));
       Page<OrphanCallReportDTO> orphansPage = reportService.findOrphanCalls(month, year, pageable);
       model.addAttribute("month", month);
       model.addAttribute("year", year);
       model.addAttribute("orphans", orphansPage);
       model.addAttribute("resolvableCount", reportService.countResolvable(month, year));
       return "pages/reports/orphan-calls-table :: table";
   }
   ```

   Adicionar imports:
   ```java
   import org.springframework.data.domain.Page;
   import org.springframework.data.domain.PageRequest;
   import org.springframework.data.domain.Pageable;
   import org.springframework.data.domain.Sort;
   ```

   b. `linkOrphanCalls` — após vincular, recarregar a primeira página:
   ```java
   @PostMapping("/link")
   public String linkOrphanCalls(@RequestParam int month, @RequestParam int year, Model model) {
       int linked = reportService.linkOrphanCalls(month, year);
       model.addAttribute("month", month);
       model.addAttribute("year", year);
       Pageable pageable = PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "callDate"));
       model.addAttribute("orphans", reportService.findOrphanCalls(month, year, pageable));
       model.addAttribute("resolvableCount", reportService.countResolvable(month, year));
       model.addAttribute("linkResult", linked);
       return "pages/reports/orphan-calls-table :: table";
   }
   ```

4. **Editar** `backend/src/main/resources/templates/pages/reports/orphan-calls-table.html` — reescrever para suportar paginação e loader via `hx-indicator`:

   a. Substituir o `<div th:fragment="table">` inteiro. Estrutura resultante:

   ```html
   <div th:fragment="table">

     <!-- Vazio -->
     <div th:if="${orphans == null or orphans.empty}"
          class="py-10 text-center text-[13px] text-[#aaa]">
       Selecione o período e clique em Processar.
     </div>

     <th:block th:if="${orphans != null}">

       <!-- Card de contexto + botão Vincular -->
       <div class="flex items-center justify-between mb-4">
         <div class="bg-white rounded-xl p-4 border border-[#e0e0e0]">
           <p class="text-[13px] text-[#1a1a1a]">
             Período: <strong th:text="${month + '/' + year}">—</strong>
             — <strong th:text="${orphans.totalElements}">0</strong> chamada(s) órfã(s) encontrada(s)
           </p>
         </div>
         <th:block th:if="${resolvableCount != null and resolvableCount > 0}">
           <form th:attr="hx-post=@{/reports/orphan-calls/link}" hx-target="#orphan-calls-table" hx-swap="innerHTML"
                 hx-include="#orphan-form" style="display:inline">
             <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}"/>
             <input type="hidden" name="month" th:value="${month}"/>
             <input type="hidden" name="year" th:value="${year}"/>
             <button type="submit" id="btn-link-orphans"
                     class="htmx-indicator-class-btn flex items-center gap-1.5 bg-[#E5A000] text-white text-[13px] font-medium px-4 py-[9px] rounded-md hover:opacity-90 transition-opacity">
               <svg class="htmx-indicator animate-spin h-4 w-4 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                 <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                 <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
               </svg>
               <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="htmx-indicator-hide">
                 <path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"/>
                 <path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"/>
               </svg>
               Vincular circuitos (<span th:text="${resolvableCount}">0</span>)
             </button>
           </form>
         </th:block>
       </div>

       <!-- Feedback de vinculação -->
       <div th:if="${linkResult != null and linkResult > 0}" class="bg-[#E1F5EE] border border-[#085041] text-[#085041] text-[13px] font-medium rounded-lg px-4 py-3 mb-4">
         <strong th:text="${linkResult}">0</strong> circuito(s) vinculado(s) com sucesso.
       </div>

       <!-- Feedback sem resolvable -->
       <div th:if="${resolvableCount != null and resolvableCount == 0 and not #lists.isEmpty(orphans.content)}" class="bg-[#f5f5f5] border border-[#e0e0e0] text-[#888] text-[13px] rounded-lg px-4 py-3 mb-4">
         Nenhuma chamada órfã resolvível para vincular neste período.
       </div>

       <!-- Tabela de chamadas -->
       <div class="rounded-xl border border-[#e0e0e0] overflow-x-auto mb-4">
         <!-- Header -->
         <div class="orphan-row bg-white border-b border-[#e0e0e0]">
           <span class="text-[11px] font-medium text-[#888] uppercase tracking-wide w-[80px] shrink-0">Unique ID</span>
           <span class="text-[11px] font-medium text-[#888] uppercase tracking-wide w-[110px] shrink-0">Data</span>
           <span class="text-[11px] font-medium text-[#888] uppercase tracking-wide w-[100px] shrink-0">Destino</span>
           <span class="text-[11px] font-medium text-[#888] uppercase tracking-wide shrink-0">Canal</span>
           <span class="text-[11px] font-medium text-[#888] uppercase tracking-wide shrink-0">Canal Destino</span>
           <span class="text-[11px] font-medium text-[#888] uppercase tracking-wide w-[100px] shrink-0">Código Canal</span>
           <span class="text-[11px] font-medium text-[#888] uppercase tracking-wide text-center w-[110px] shrink-0">Status</span>
         </div>

         <!-- Vazio -->
         <div th:if="${orphans.content.isEmpty()}" class="py-10 text-center text-[13px] text-[#aaa]">
           Nenhuma chamada órfã encontrada para este período.
         </div>

         <!-- Linhas -->
         <div th:each="orphan : ${orphans.content}"
               class="orphan-row bg-white border-b border-[#f0f0f0]">
           <span class="text-[13px] font-mono text-[#1a1a1a] w-[80px] shrink-0 overflow-hidden text-ellipsis"
                 th:text="${orphan.uniqueId}"></span>
           <span class="text-[13px] font-mono text-[#1a1a1a] w-[110px] shrink-0"
                 th:text="${#temporals.format(orphan.callDate, 'dd/MM/yy HH:mm')}"></span>
           <span class="text-[13px] font-mono text-[#1a1a1a] w-[100px] shrink-0 overflow-hidden text-ellipsis"
                 th:text="${orphan.dst != null ? orphan.dst : '—'}"></span>
           <span class="text-[13px] font-mono text-[#555] shrink-0 overflow-hidden text-ellipsis"
                 th:text="${orphan.channel != null ? orphan.channel : '—'}"></span>
           <span class="text-[13px] font-mono text-[#555] shrink-0 overflow-hidden text-ellipsis"
                 th:text="${orphan.dstChannel != null ? orphan.dstChannel : '—'}"></span>
           <span class="text-[13px] font-mono text-[#1a1a1a] w-[100px] shrink-0"
                 th:text="${orphan.circuitCode != null ? orphan.circuitCode : '—'}"></span>
           <span class="flex justify-center w-[110px] shrink-0">
             <span th:class="|rounded-[99px] text-[11px] px-[8px] py-[2px] font-medium whitespace-nowrap
                              ${orphan.resolvable ? 'bg-[#E1F5EE] text-[#085041]' : 'bg-[#FEE2E2] text-[#dc2626]'}|"
                   th:text="${orphan.resolvable ? 'Resolvível' : 'Não resolvível'}">
             </span>
           </span>
         </div>
       </div>

       <!-- Paginação -->
       <div id="pagination-toolbar-orphan" class="flex items-center gap-2">
         <div th:replace="~{fragments/pagination :: pagination(
             page=${orphans},
             hxGet=${'/reports/orphan-calls/table?month=' + month + '&year=' + year},
             hxTarget='#orphan-calls-table',
             hxInclude='#orphan-form')}"></div>
       </div>

     </th:block>

   </div>
   ```

   **Nota sobre `hx-indicator`**: O HTMX nativamente torna visível elementos com a classe `.htmx-indicator` durante a requisição. Para os botões "Processar" e "Vincular", a estratégia é usar CSS customizado para controlar a visibilidade dos spinners e ícones. Adicionar o seguinte bloco `<style>` no template `orphan-calls.html` (não no fragmento, para evitar duplicação):

   b. Remover o bloco `<script>` inteiro (linhas 109-129) do `orphan-calls-table.html`.

5. **Editar** `backend/src/main/resources/templates/pages/reports/orphan-calls.html` — refazer o loader do botão "Processar":

   a. Adicionar bloco `<style>` dentro do `<head>`:
   ```html
   <style>
     /* Loader HTMX para botão Processar */
     #btn-process-orphan .htmx-indicator { display: none; }
     #btn-process-orphan.htmx-request .htmx-indicator { display: inline-block; }
     #btn-process-orphan.htmx-request .htmx-indicator-hide { display: none; }
     #btn-process-orphan.htmx-request { opacity: 0.7; cursor: not-allowed; }

     /* Loader HTMX para botão Vincular */
     #btn-link-orphans .htmx-indicator { display: none; }
     #btn-link-orphans.htmx-request .htmx-indicator { display: inline-block; }
     #btn-link-orphans.htmx-request .htmx-indicator-hide { display: none; }
     #btn-link-orphans.htmx-request { opacity: 0.7; cursor: not-allowed; }
   </style>
   ```

   b. Refazer o botão "Processar" para usar classes HTMX em vez de JS:
   ```html
   <button type="button" id="btn-process-orphan"
       class="flex items-center gap-1.5 bg-[#1D9E75] text-white text-[13px] font-medium px-4 py-[9px] rounded-md hover:opacity-90 transition-opacity"
       th:attr="hx-get=@{/reports/orphan-calls/table}" hx-target="#orphan-calls-table" hx-swap="innerHTML"
       hx-include="#orphan-form">
       <svg class="htmx-indicator animate-spin h-4 w-4 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
           <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
           <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
       </svg>
       <span class="htmx-indicator-hide">Processar</span>
       <span class="htmx-indicator">Processando...</span>
   </button>
   ```

   c. Remover o bloco `<script>` inteiro (linhas 81-99) do `orphan-calls.html`.

6. **Editar** `backend/src/test/java/com/dionialves/AsteraComm/call/OrphanCallReportServiceTest.java` — atualizar testes para usar `Pageable`:

   a. Em todos os testes que chamam `service.findOrphanCalls(month, year)`, atualizar para `service.findOrphanCalls(month, year, PageRequest.of(0, 50))`.

   b. Atualizar mocks: `when(callRepository.findOrphanCallsByPeriod(3, 2026))` → `when(callRepository.findOrphanCallsByPeriod(eq(3), eq(2026), any(Pageable.class)))`, retornando `new PageImpl<>(list, PageRequest.of(0, 50), list.size())`.

   c. Atualizar asserts para `result.getContent()` em vez de `result` direto.

   Adicionar imports:
   ```java
   import org.springframework.data.domain.Page;
   import org.springframework.data.domain.PageImpl;
   import org.springframework.data.domain.PageRequest;
   import org.springframework.data.domain.Pageable;
   import static org.mockito.ArgumentMatchers.any;
   import static org.mockito.ArgumentMatchers.eq;
   ```

7. **Editar** `backend/src/test/java/com/dionialves/AsteraComm/call/OrphanCallReportControllerTest.java` — atualizar testes para refletir a assinatura paginada:

   a. Teste `link_postSetsModelAttributes` — a chamada interna a `findOrphanCalls` agora recebe `Pageable`. Mockar adequadamente.

   b. Teste `link_postReturnsTableFragment` — sem mudança no assert de view name.

8. **Editar** `doc/CHANGELOG.md` — remover a linha `RF-099` do `[Unreleased]` (a RF-099 foi BLOQUEADA e será substituída por FIX-101 + RF-102 + RF-103).

9. **Rodar `./mvnw test`** e garantir que a suíte passa (incluindo os novos testes).

#### Testes a criar/atualizar
- `OrphanCallReportServiceTest` — atualizar todos os testes para usar `Pageable` e `Page<>`.
- `OrphanCallReportServiceTest` — adicionar teste `findOrphanCalls_returnsPaginatedResult` com 100 orphans, page size 10, verificando que `result.getContent().size() == 10` e `result.getTotalElements() == 100`.
- `OrphanCallReportControllerTest` — atualizar mocks para `Pageable`.
- `OrphanCallReportControllerTest` — adicionar teste `table_returnsPaginatedModel` verificando que `model.containsAttribute("orphans")` e que o tipo é `Page`.

#### Critérios de aceitação
- [ ] `GET /reports/orphan-calls/table?month=X&year=Y` retorna no máximo 50 registros por página (default), com toolbar de paginação.
- [ ] Clicar em "Processar" exibe spinner e texto "Processando..." enquanto a requisição está em andamento (na primeira e nas subsequentes).
- [ ] Clicar em "Vincular" exibe spinner enquanto a requisição está em andamento (na primeira e nas subsequentes).
- [ ] Após "Vincular", a tabela recarrega a primeira página e exibe feedback verde com o número de circuitos vinculados.
- [ ] Meses com 10.000+ chamadas órfãs carregam sem erro 500 e sem timeout (paginação + batch queries).
- [ ] A navegação entre páginas (anterior/próximo) funciona sem reprocessar todo o relatório.
- [ ] Card "Chamadas Órfãs" permanece na `index.html` (funcionalidade da RF-099 mantida).
- [ ] Botão "Vincular circuitos (N)" aparece quando há resolvíveis (funcionalidade da RF-099 mantida).
- [ ] Todos os testes novos passam e nenhum teste existente regrediu.
- [ ] `./mvnw test` passa.
- [ ] Commit no padrão `refactor(rf-103): adiciona paginacao e loader-funcional ao relatorio de chamadas-orfas`.
- [ ] Entrada no `doc/changelog.md` seção `[Unreleased]` e descrição detalhada em `doc/release_notes/unreleased.md`.
- [ ] Remoção da task do `doc/backlog.md` após conclusão.
- [ ] RF-099 removida do `[Unreleased]` no `doc/changelog.md`.

#### Riscos e observações
- **`Pageable.unpaged()` em `findAllOrphanCallDTOs`:** usar esta abordagem para `linkOrphanCalls` e `countResolvable` significa que, para a vinculação, todos os orphans ainda são carregados em memória. Isso é aceitável porque a vinculação é uma ação manual esporádica. Para o futuro, pode-se implementar batch UPDATE (ver recomendação do Revisor).
- **Estilo `htmx-indicator`:** o CSS precisa ser definido no template pai (`orphan-calls.html`), não no fragmento, pois os estilos dos spinners precisam existir desde o carregamento inicial da página (antes do primeiro HTMX swap).
- **O Codificador NÃO deve criar migration Flyway** — nenhuma alteração de esquema.
- **O Codificador NÃO deve alterar `SecurityConfigurations`** — o endpoint já está protegido.
- **Linha duplicada no release notes:** se existir entrada duplicada de `findOrphanCalls_returnsNotResolvable_whenCdrMissing` em `doc/release-notes/unreleased.md`, corrigir removendo a duplicata.
- **Ordem de execução:** esta task depende de FIX-101 e RF-102. O Codificador deve executar FIX-101 primeiro, depois RF-102, e finalmente RF-103.

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
