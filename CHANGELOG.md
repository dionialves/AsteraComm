# Changelog — AsteraComm

## Em desenvolvimento

---

### FIX-001 — Corrigir carregamento de DIDs livres no modal de vínculo do circuito

**Descrição:**
O modal de vínculo de DID no formulário do circuito não exibia os DIDs disponíveis porque o frontend carregava todos os DIDs e filtrava no cliente via `!d.circuitNumber`, o que falhava silenciosamente quando o parâmetro `sort` era codificado incorretamente pelo `URLSearchParams` (vírgula → `%2C`), gerando erro no Pageable do Spring.

**Solução:**
- Backend: novo endpoint `GET /api/dids/free` retorna lista simples de DIDs sem circuito vinculado.
- Frontend: nova rota Astro `src/pages/api/did/free.ts` proxia o endpoint.
- `openModal()` em `circuits/[id].astro` atualizado para usar `/api/did/free` diretamente, eliminando o filtro cliente.

---

### FIX — Auditoria: circuito não encontrado ao processar

O seletor de circuito na página de auditoria usava `c.id` (PK numérica) como valor do `<option>`, mas o backend busca pelo campo `number` (string). Corrigido para `c.number` em `audit/index.astro`.

---

### US-022 — Campo `active` no circuito e regra de exclusão com fallback para desativação

**Titulo:** Campo `active` no circuito e regra de exclusão com fallback para desativação

**Descrição:**
Como administrador, quero que o circuito possua um campo `active` indicando se está em operação, e que a exclusão só seja permitida quando não houver DIDs nem Calls vinculadas — caso contrário, o sistema deve desativar o circuito em vez de excluí-lo. No frontend, o estado ativo deve ser visível na listagem e editável no formulário.

**Estimativa:** 3 story points

**Critérios de Aceite:**

1. **Campo `active` na entidade `Circuit`:** Booleano, padrão `true` na criação. Persistido como `active BOOLEAN NOT NULL DEFAULT TRUE` via migração Flyway.
2. **Regra de exclusão:** `DELETE /api/v1/circuits/{number}` verifica:
   - Se houver DIDs vinculados → `BusinessException` ("Desvincule os DIDs antes de excluir o circuito").
   - Se houver Calls vinculadas → não é possível excluir; o serviço **desativa** o circuito (`active = false`) e retorna `HTTP 200` com o circuito atualizado (em vez de `204`).
   - Se não houver DIDs nem Calls → exclui normalmente (`HTTP 204`).
3. **Endpoint de atualização:** `PUT /api/v1/circuits/{number}` passa a aceitar o campo `active` no body, permitindo ativar/desativar manualmente.
4. **Frontend — listagem:** Coluna **Ativo** adicionada à tabela de circuitos exibindo `Sim` (verde) ou `Não` (vermelho).
5. **Frontend — formulário (criação e edição):** Campo **Ativo** abaixo do campo Código, com radio buttons `( ) Sim  ( ) Não`. Padrão `Sim` na criação.
6. **Testes:** Cobrem os três cenários de exclusão (tem DID, tem Call, nenhum vínculo), atualização do campo `active` e criação com `active = true` por padrão.

---

### US-021 — Refatoração UX: página de detalhe do circuito e ID no domínio

**Titulo:** Refatoração UX: página de detalhe do circuito

**Descrição:**
Como administrador, quero que ao clicar em um circuito na lista eu seja levado a uma página de detalhe dedicada, onde posso visualizar e editar todas as informações do circuito e gerenciar os DIDs vinculados, em vez de editar inline na lista.

**Estimativa:** 3 story points

**Critérios de Aceite:**

1. **Lista de circuitos:** A coluna de Ações é removida. Clicar em qualquer linha navega para a página de detalhe do circuito (`/circuits/{id}`). Coluna **ID** adicionada como primeira coluna.
2. **Página de detalhe — cabeçalho:** Segue o padrão visual do sistema: título (nome do circuito) e subtítulo (ex.: "Editar circuito") na mesma linha dos botões **Voltar**, **Salvar** e **Deletar**.
3. **Página de detalhe — campos:** Abaixo do cabeçalho, cada campo é exibido em uma linha própria com o nome do campo à esquerda e o input à direita, empilhados verticalmente. Campos ID e Código sem placeholder no formulário de criação.
4. **Tabela de DIDs vinculados:** Abaixo dos campos, exibe uma tabela com os DIDs associados ao circuito.
5. **Ações da tabela de DIDs:** Acima da tabela, alinhados à direita, os botões **Adicionar** e **Desvincular**. O botão **Desvincular** aparece em vermelho apagado (desabilitado) e fica ativo (vermelho normal) somente quando ao menos um DID está selecionado na tabela.
6. **Consistência:** A experiência de navegação (voltar para a lista, salvar, deletar) funciona corretamente sem quebrar funcionalidades existentes.
7. **ID no domínio Circuit:** Campo `id` (Long, PK sequencial) adicionado à entidade `Circuit`. Migração `V3__add_id_to_circuits.sql` cuida da transição em bancos existentes. `findById` substituído por `findByNumber` em `AuditService`, `CallProcessingService` e `DIDService`. `@JoinColumn` em `Call` corrigido com `referencedColumnName = "number"` para manter compatibilidade de schema. 322 testes, 0 falhas.

---

### US-014 — Dashboard inicial com visão geral do sistema

**Titulo:** Dashboard inicial com visão geral do sistema

**Descrição:**
Como administrador, quero uma tela de dashboard que exiba um resumo operacional e financeiro do sistema, com painéis de circuitos, ligações, faturamento e troncos, para ter visibilidade rápida do estado da plataforma ao acessar o sistema.

**Estimativa:** 5 story points

**Critérios de Aceite:**

1. **Cards de resumo (4 colunas):**
   - **Circuitos:** total, online (indicador verde) e offline (indicador vermelho).
   - **Troncos:** total, registrados (verde) e não registrados (vermelho).
   - **Ligações do mês:** total, atendidas (verde), não atendidas (âmbar) e ocupado (vermelho).
   - **Faturamento do mês:** valor total em destaque, breakdown em assinaturas + excedentes.

2. **Gráfico de faturamento mensal:** Barras empilhadas com os últimos 12 meses — azul para assinaturas, âmbar para excedentes. Valores no eixo Y em reais (BRL).

3. **Gráfico de ligações diárias:** Barras empilhadas dos últimos 30 dias — atendidas (verde), não atendidas (âmbar), ocupado (vermelho) e falhas (cinza).

4. **Tabela "Circuitos com consumo próximo do limite":** Exibe até 10 circuitos com pacote de minutagem, ordenados do maior para o menor consumo percentual no mês corrente, **excluindo** circuitos cujo pacote já foi excedido. Cada linha mostra: número do circuito, plano, minutos utilizados, limite do pacote e barra de progresso colorida (verde abaixo de 85 %, âmbar de 85–94 %, vermelho a partir de 95 %).

5. **Gráfico de excedência (donut):** Card ao lado da tabela acima exibindo um gráfico circular com a proporção entre circuitos com pacote excedido vs. circuitos dentro do limite (`CircuitOverageStats`).

6. **Gráfico "Top 10 circuitos por consumo":** Barras horizontais com os 10 circuitos de maior consumo de minutos no mês, exibindo minutos utilizados vs. limite do pacote. Barras de consumo ficam vermelhas quando excedem o limite.

7. **API backend:** Endpoint `GET /api/dashboard` retorna todos os dados em um único payload (`CircuitStats`, `TrunkStats`, `CallStats`, `BillingStats`, `DailyCallStat[]`, `MonthlyBillingStat[]`, `CircuitConsumption[]`, `TopCircuit[]`, `CircuitOverageStats`).

8. **Atualização:** Dados carregados ao acessar a página, sem polling automático.

9. **Testes:** 18 testes unitários cobrindo: estatísticas de circuitos, troncos, ligações, faturamento, intervalos de data, valores zero/vazios, filtro de excedidos na near-limit list, ordenação por percentual e contagem de overage.

---

### US-015 — Relatórios: custo de ligações por circuito no período

**Titulo:** Relatórios: custo de ligações por circuito no período

**Descrição:**
Como administrador, quero acessar um menu de relatórios e gerar um relatório de custo de ligações por circuito, informando mês e ano, para visualizar o quanto cada circuito gerou de custo com ligações no mês selecionado.

**Estimativa:** 3 story points

**Critérios de Aceite:**

1. **Menu de Relatórios:** Nova entrada no menu lateral "Relatórios", contendo a listagem de relatórios disponíveis. Inicialmente exibe apenas o relatório de custo por circuito.
2. **Filtro por mês/ano:** O usuário seleciona mês e ano. Ambos os campos são obrigatórios.
3. **Resultado por circuito:** O relatório exibe uma linha por circuito com:
   - Nome do cliente vinculado ao circuito.
   - Nome do circuito.
   - Quantidade de ligações no período.
   - Total de minutos consumidos.
   - Custo total (R$) gerado pelas ligações (excluindo valor de plano/franquia — apenas custo excedente de ligações).
4. **Apenas ligações:** O relatório considera exclusivamente registros da entidade `Call`; faturas, planos e outros valores não entram no cálculo.
5. **Circuitos sem ligações:** Circuitos sem nenhuma ligação no período não aparecem no resultado.
6. **API backend:** Endpoint `GET /api/v1/reports/call-cost?month=MM&year=YYYY&onlyWithCost=false` retorna os dados agregados por circuito.
7. **Layout da tela:** A página exibe, em ordem: título do relatório, descrição explicativa, seletores de mês e ano, checkbox "Apenas com custo", botão "Processar" e tabela de resultados com totalizador.
8. **Filtro "Apenas com custo":** Checkbox que, quando marcada, exclui do resultado circuitos com `totalCost = 0`. Filtro aplicado no backend via `onlyWithCost=true`.
9. **Download PDF:** Botão "Baixar PDF" gera um arquivo com header (logo, título, filtros aplicados, data de geração) e tabela com totais via `jsPDF` + `jspdf-autotable`. Download iniciado automaticamente sem diálogo de impressão.
10. **Testes:** 12 testes (7 service + 5 controller), 0 falhas. Cobrem: mês sem ligações, múltiplos circuitos com custos distintos, filtro `onlyWithCost` ativo vs. inativo, parâmetros obrigatórios.

---

### US-020 — Ajuste do consumo de pacote de minutagem para frações de 30 segundos

**Titulo:** Ajuste do consumo de pacote de minutagem para frações de 30 segundos

**Descrição:**
Como operador, quero que o consumo do pacote de minutagem use a mesma base de faturamento (frações de 30 segundos), para que cada minuto de pacote cubra exatamente um minuto de custo faturável, sem penalizar o cliente com consumo excessivo de pacote por arredondamento para o minuto inteiro.

**Estimativa:** 2 story points

**Critérios de Aceite:**

1. **Nova lógica de consumo:** O consumo do pacote passa a usar `ceil(billSeconds / 30.0)` frações de 30s, idêntico à base de cálculo de custo. Uma ligação de 61s consome 3 frações (1,5 min) do pacote — o mesmo valor que custaria se fosse cobrada.
2. **Campo `minutes_from_quota`:** Passa a armazenar o número de **frações de 30 segundos** consumidas do pacote (não minutos inteiros). O nome da coluna é mantido por retrocompatibilidade, mas o valor semântico muda: `2 = 1 minuto`.
3. **Comparação com o pacote:** O total disponível é convertido para frações na comparação: `packageTotalMinutes × 2`. As queries `sumQuotaMinutes` e `sumQuotaMinutesByType` continuam somando `minutes_from_quota` normalmente — o ajuste está na comparação.
4. **`CallCostingService`:** A lógica de `applyWithQuota` é atualizada: `durationMinutes` → `durationFractions = ceil(billSeconds / 30.0)`. O cálculo de excedente usa `billableSeconds = billSeconds - (remaining × 30)`.
5. **`AuditService`:** Atualizado da mesma forma, mantendo paridade com a produção.
6. **Registros históricos:** Ligações já processadas com a lógica antiga não são reprocessadas. A mudança vale apenas para novos processamentos.
7. **Testes:** Testes cobrem: ligação de 61s consome 3 frações (não 2 min), ligação parcialmente coberta com excedente correto, pacote esgotado no meio de uma ligação, verificação de que auditoria e produção produzem o mesmo resultado.

---

### US-016 — Ferramenta de auditoria de custo de ligações por circuito

**Titulo:** Ferramenta de auditoria de custo de ligações por circuito

**Descrição:**
Como desenvolvedor, quero uma ferramenta que, dado um circuito, mês e ano, simule o cálculo de custo de cada ligação do período de forma transparente e passo a passo, exibindo o raciocínio de cada cálculo (custo unitário, desconto por pacote de minutagem e consumo acumulado), para que eu possa auditar e validar se a lógica de billing está correta.

**Estimativa:** 3 story points

**Critérios de Aceite:**

1. **Filtro de seleção:** Seletor de circuito, mês e ano com botão "Processar".
2. **Processamento cronológico:** Ligações do período exibidas em ordem cronológica, uma por linha.
3. **Linha por ligação:** Data/hora, destino, tipo, duração, tarifa, minutos do pacote consumidos, acumulado do pacote e custo calculado.
4. **Contador de pacote:** Acumula progressivamente os minutos consumidos do pacote; congela ao esgotar.
5. **Custo zerado por pacote:** Ligações cobertas exibem R$ 0,00 com o acréscimo no contador.
6. **Filtro dinâmico:** Toggle "Exibir apenas chamadas relevantes" filtra no client sem nova requisição, mostrando contador de linhas visíveis.
7. **Resumo ao final:** Total de ligações, minutos, minutos de pacote usados, minutos excedentes e custo total.
8. **Acesso restrito:** Disponível apenas para `SUPER_ADMIN`.
9. **Bug fix — `CURRENT_DATE`:** Corrigido bug em `sumQuotaMinutesThisMonth` / `sumQuotaMinutesThisMonthByType` que usavam `CURRENT_DATE` nas queries, fazendo com que ligações processadas em mês diferente do mês da chamada calculassem a cota com saldo incorreto. Métodos renomeados para `sumQuotaMinutes` / `sumQuotaMinutesByType` e passam a receber `month` e `year` explícitos derivados de `call.getCallDate()`.
10. **Testes:** `AuditServiceTest` (11 casos) + `CallCostingServiceTest` atualizado (15 casos), 0 falhas.

---

### US-010 — Custeio de ligações por circuito com franquia e tarifação

**Titulo:** Custeio de ligações por circuito com franquia e tarifação

**Descrição:**
Como sistema, quero que cada `Call` criada a partir de um registro CDR seja associada ao circuito de origem, tenha seu tipo classificado e seu custo calculado, de forma que ligações originadas no servidor tenham seu valor apurado considerando a franquia de minutos do plano vinculado ao circuito.

**Estimativa:** 5 story points

**Critérios de Aceite:**

1. Escopo de custeio: apenas ligações de contextos de saída são custeadas; demais recebem `OUT_OF_SCOPE` e `cost = 0,00`.
2. Vínculo `Call → Circuit → Plan`; sem plano vinculado → `NO_PLAN` e `cost = 0,00`.
3. Franquia prioriza pacote exclusivo do `callType`; fallback para pacote geral; sem pacote → vai direto à tarifação.
4. Minutos acumulados por circuito no mês calendário corrente.
5. Custo em frações de 30 s (cada fração iniciada cobrada integralmente = 50% do valor/min).
6. Cenários: dentro da franquia (cost=0), esgotamento parcial, totalmente fora da franquia.
7. Idempotência: `Call` com `status != null` não é reprocessada.
8. Campos: `callStatus` (enum `PROCESSED/NO_CIRCUIT/NO_PLAN/OUT_OF_SCOPE`), `minutesFromQuota` (int), `cost` (decimal 2 casas).
9. Frontend: colunas "Custeio" e "Valor" na tabela e modal de ligações.

---

### US-019 — Migrações de schema com Flyway

**Titulo:** Migrações de schema com Flyway

**Descrição:**
Substituição do `ddl-auto=create-drop` por migrações versionadas com Flyway. O schema da aplicação (`asteracomm_*`) passa a ser gerenciado pelo Flyway via `V1__create_schema.sql`. As tabelas Asterisk (`cdr`, `cel`, `ps_*`, `extensions`) permanecem no `postgres/init/01-create-tables.sql`. Um novo arquivo `postgres/init/02-initial-flyway.sql` cria as tabelas `asteracomm_*` com `IF NOT EXISTS` para garantir que o seed real (`03-seed-real.sql`) funcione antes do backend subir.

**Estimativa:** 2 story points

**Critérios de Aceite:**

1. Flyway (`flyway-core` + `flyway-database-postgresql`) adicionado ao `pom.xml`.
2. `V1__create_schema.sql` em `db/migration/` com todas as tabelas `asteracomm_*` usando `IF NOT EXISTS`.
3. `ddl-auto=validate` nos profiles `dev` e `prod`; `baseline-on-migrate=true`.
4. `02-initial-flyway.sql` no `postgres/init/` cria tabelas `asteracomm_*` para o seed funcionar.
5. `application-test.properties`: `flyway.enabled=false` + `ddl-auto=create-drop`.
6. Divergências entre entidades JPA e schema corrigidas (`nullable`, `optional=false`).

---

### US-018 — Vínculo de plano de cobrança ao circuito e seleção de cliente no frontend

**Titulo:** Vínculo de plano de cobrança ao circuito e seleção de cliente no frontend

**Descrição:**
Plano de cobrança vinculado diretamente ao circuito (obrigatório). O circuito passa a ser o ponto central do sistema: `Call → Circuit → Plan` e `Call → Circuit → Customer`. A tela de circuitos exibe e permite selecionar cliente e plano. O DataSeeder cria um cliente único por circuito, um plano compartilhado e um DID por circuito.

**Estimativa:** 3 story points

**Critérios de Aceite:**

1. **Entidade `Circuit`:** Campo `plan` adicionado (FK `NOT NULL` para `Plan`). Plano é obrigatório — `BusinessException` lançada quando `planId` é nulo.
2. **API — criação e edição:** `POST /api/circuits` e `PUT /api/circuits/{number}` aceitam `planId` (obrigatório). `planId` inválido lança `NotFoundException`.
3. **API — leitura:** Listagem retorna `planName`; `GET /api/circuits/{number}` retorna o objeto `plan` completo.
4. **Frontend — formulário:** Seletor de **cliente** (obrigatório) e seletor de **plano** (obrigatório) no modal de criação/edição. Validação no frontend impede salvar sem plano.
5. **Frontend — listagem:** Coluna **Plano** adicionada na tabela de circuitos.
6. **SQL:** `plan_id BIGINT NOT NULL` em `asteracomm_circuits`; `asteracomm_plans` movida antes de `asteracomm_circuits` no script de init.
7. **DataSeeder:** Cria 1 plano "Plano Dev", 1 cliente por circuito (nomes de empresas) e 1 DID por circuito (4933333333, 4933334444, ... +1111).
8. **Testes:** 264 testes, 0 falhas — cobrem criação com/sem plano, troca de plano, exceção quando planId nulo/inválido.

---

### US-017 — Enriquecimento de ligações com circuito via channel

**Titulo:** Enriquecimento de ligações com circuito via channel

**Descrição:**
Ao processar registros do CDR, a entidade `Call` é enriquecida com o circuito de origem. O campo `channel` (ex: `PJSIP/4933401714-000045f0`) é parseado para extrair o código do circuito (`4933401714`), que é usado para associar a entidade `Circuit` correspondente. A tela de ligações exibe o circuito em coluna dedicada e permite filtrar por número de circuito.

**Estimativa:** 3 story points

**Critérios de Aceite:**

1. O campo `channel` é parseado corretamente para extrair o código do circuito (formato: `<PROTOCOLO>/<codigo>-<sufixo>`).
2. O código extraído é usado para buscar o `Circuit` correspondente pelo número do circuito.
3. A entidade `Call` expõe o `Circuit` associado quando encontrado (campo `circuit` com FK `circuit_number`).
4. Quando nenhum circuito é encontrado para o código extraído, a ligação ainda é retornada sem falhar (circuito nulo/ausente).
5. O parsing trata variações de protocolo além de `PJSIP` (ex: `SIP/`, `DAHDI/`) de forma genérica.
6. A tela de ligações exibe coluna "Circuito" e campo de filtro por número de circuito.
7. Testes unitários: 9 casos em `ChannelParserTest`, 6 casos em `CallProcessingServiceTest` — 259 testes, 0 falhas.

---

### US-016 — Entidade Call: mapeamento das ligações do CDR

**Titulo:** Entidade Call: mapeamento das ligações do CDR

**Descrição:**
Como sistema, quero processar automaticamente todos os registros da tabela `cdr` e transformá-los em objetos `Call`, enriquecendo cada ligação com data/hora, origem (callerid), destino, e categoria de destino classificada automaticamente, de forma que a tela de "Ligações" passe a consultar `Call` em vez de acessar o CDR diretamente.

**Estimativa:** 3 story points

**Critérios de Aceite:**

1. **Processamento de todos os registros:** Todas as ligações são mapeadas independentemente do status — ANSWERED, NO ANSWER, BUSY, FAILED e demais valores de `disposition`.
2. **Campos da entidade `Call`:**
   - `id` (PK)
   - `uniqueId` — CDR.uniqueid (índice único, garante idempotência)
   - `callDate` — CDR.calldate
   - `callerNumber` — extraído do CDR.clid (formato `"Nome" <número>` → extrair apenas o número)
   - `dst` — CDR.dst
   - `durationSeconds` — CDR.duration
   - `billSeconds` — CDR.billsec
   - `disposition` — CDR.disposition (mantido como string)
   - `callType` — enum classificado pelo `dst` (ver critério 3)
   - `processedAt` — timestamp de quando o registro foi criado em `Call`
3. **Classificação do `callType`** — validação do dígito inicial do assinante e suporte a prefixos brasileiros:
   - `FIXED_LOCAL` — 8 dígitos, começa com 2–8
   - `MOBILE_LOCAL` — 9 dígitos, começa com 9
   - `FIXED_LONG_DISTANCE` — DDD(2)+fixo(8), 0+DDD+fixo(11d) ou 0+CSP+DDD+fixo(13d)
   - `MOBILE_LONG_DISTANCE` — DDD(2)+móvel(9), 0+DDD+móvel(12d) ou 0+CSP+DDD+móvel(14d)
   - `UNKNOWN` — demais casos
4. **Polling periódico:** Um job agendado (intervalo configurável via `application.properties`) consulta registros do CDR cujo `uniqueid` ainda não existe em `asteracomm_calls` e os processa.
5. **Idempotência:** Um mesmo `uniqueid` do CDR nunca gera dois registros em `Call`.
6. **Substituição da tela de Ligações:** O endpoint `/api/cdrs` é substituído por `/api/calls`, retornando registros de `Call`. Os filtros existentes (origem, destino, disposition, período) são mantidos. A tela de frontend é atualizada com coluna Tipo e campos do novo JSON.
7. **Testes:** 247 testes, 0 falhas — cobrindo extração de callerid, classificação de callType (23 casos) e processamento com diferentes dispositions.

---

### US-009 — Cadastro de clientes e vínculo obrigatório com circuito

**Titulo:** Cadastro de clientes e vínculo obrigatório com circuito

**Descrição:**
Como administrador, quero cadastrar clientes com apenas o nome, e vincular cada circuito a um cliente, sendo esse vínculo obrigatório, para que eu possa identificar a qual cliente cada circuito pertence.

**Estimativa:** 3 story points

**Critérios de Aceite:**

1. **CRUD de clientes:** Endpoints REST para criar, listar, buscar por ID, atualizar e remover clientes. O cliente possui `nome` (obrigatório, único), `enabled`, `createdAt` e `updatedAt`.
2. **Vínculo no circuito:** A entidade `Circuit` possui FK obrigatória para `Customer` (`customer_id NOT NULL`).
3. **Validação:** Não é possível criar ou atualizar um circuito sem informar um `customerId` válido.
4. **Exclusão restrita:** Não é permitido excluir um cliente que possua circuitos vinculados — apenas desativação (`PATCH /api/customers/{id}/disable`).
5. **Frontend:** Página `/customers` com CRUD completo (tabela paginada, modal criar/editar, botão desativar, botão excluir). Formulário de circuito inclui seletor de cliente obrigatório.
6. **Testes:** 228 testes, 0 falhas.

---

### US-008 — Refatoração: EndpointStatusService usar AmiService

**Titulo:** Refatoração: EndpointStatusService usar AmiService

**Descrição:**
Como desenvolvedor, quero que o `EndpointStatusService` utilize o `AmiService` para se conectar ao Asterisk via AMI, eliminando a duplicação de lógica de conexão que hoje existe entre os dois serviços.

**Estimativa:** 2 story points

**Critérios de Aceite:**

1. **AmiService estendido:** `AmiService` expõe um método `sendCommandWithResponse(String command) → CommandResponse` para comandos que necessitam retorno.
2. **EndpointStatusService refatorado:** Injeta `AmiService` e delega a conexão AMI ao novo método, removendo o código inline de `ManagerConnectionFactory`/`ManagerConnection`.
3. **Comportamento preservado:** O polling de status (`pjsip show contacts`) continua funcionando da mesma forma.
4. **Testes atualizados:** `EndpointStatusServiceTest` passa a mockar `AmiService` em vez de usar `ReflectionTestUtils` para os parâmetros de conexão.

---

### US-003 — Cadastro de planos de cobrança

**Titulo:** Cadastro de planos de cobrança

**Descrição:**
Como administrador, quero cadastrar planos de cobrança definindo o valor mensal, as tarifas por minuto para cada categoria de ligação fixa e, opcionalmente, um pacote de minutos inclusos — por categoria ou unificado — para que os planos possam ser atribuídos a clientes.

**Estimativa:** 5 story points

**Critérios de Aceite:**

1. **Listagem:** Exibe lista de planos com: nome, valor mensal, tipo de pacote e tarifas por categoria.
2. **Criação:** Cadastro com nome, mensalidade, tarifas R$/min (4 casas decimais) e pacote de minutos (`NONE` | `UNIFIED` | `PER_CATEGORY`).
3. **Edição:** Permite alterar qualquer campo do plano.
4. **Exclusão:** Permite excluir plano não vinculado a clientes.
5. **Validações:** Consistência do pacote conforme `packageType` via `@ValidPackage`; nome único; campos obrigatórios; 200 testes, 0 falhas.

---

### US-002 — Pesquisa e visualização de ligações realizadas

**Titulo:** Pesquisa e visualização de ligações realizadas

**Descrição:**
Como usuário do sistema, quero pesquisar e visualizar as ligações que foram realizadas, para que eu possa consultar histórico de chamadas, auditar uso e obter informações sobre origem, destino, duração e resultado de cada ligação.

**Estimativa:** 5 story points

**Critérios de Aceite:**

1. **Listagem:** A interface exibe uma tabela paginada com as ligações registradas no CDR, contendo: data/hora, origem, destino, duração, status (ANSWERED, NO ANSWER, BUSY, FAILED).
2. **Filtros:** É possível filtrar por:
   - Período (data inicial e data final)
   - Origem (número chamador)
   - Destino (número chamado)
   - Status da chamada
3. **Ordenação:** A tabela permite ordenar por data/hora e duração.
4. **Paginação:** Os resultados são paginados, com controle de itens por página.
5. **Detalhes:** Ao clicar em uma ligação, exibe detalhes completos (contexto, canal, tempo de espera, ID único).
6. **Acesso:** Disponível para usuários com role `ADMIN` ou superior.

---

### US-007 — Vinculação DID-Circuito com provisionamento automático de Extensions

**Titulo:** Vinculação DID-Circuito com provisionamento automático de Extensions

**Descrição:**
Como administrador, quero vincular um ou mais DIDs a um circuito, para que o Asterisk provisione automaticamente as Extensions correspondentes, habilitando o circuito a receber e realizar chamadas por aquele número.

**Estimativa:** 5 story points

**Critérios de Aceite:**

1. **Vinculação:** Na tela do circuito, é possível associar um DID livre ao circuito. Um circuito pode ter 1 ou mais DIDs.
2. **Provisionamento automático:** Ao vincular, a Extension correspondente ao número do DID é criada nas tabelas do Asterisk (`pstn-{trunkName}`: Dial → Hangup) e um `dialplan reload` é disparado via AMI.
3. **Desvinculação:** É possível desvincular um DID de um circuito. Ao desvincular, a Extension é removida do Asterisk e o `dialplan reload` é disparado.
4. **Status do DID:** DID vinculado muda status para `EM USO`; ao desvincular volta para `LIVRE`.
5. **Restrição:** Um DID só pode estar vinculado a um circuito por vez. Circuito com DIDs vinculados não pode ser excluído.
6. **Exibição:** Na listagem de circuitos e na listagem de DIDs, o vínculo é exibido claramente.
7. **Testes:** 157 testes, 0 falhas.

---

### US-010 — Geração dinâmica de `extensions_trunks.conf` para contextos de tronco

**Titulo:** Geração dinâmica de `extensions_trunks.conf` para contextos de tronco

**Descrição:**
Como administrador, quero que ao cadastrar ou remover um tronco o Asterisk reconheça automaticamente os contextos de dialplan correspondentes (`internal-<tronco>` e `pstn-<tronco>`), sem necessidade de edição manual de arquivos de configuração.

**Estimativa:** 5 story points

**Critérios de Aceite:**

1. Volume nomeado `dialplan-generated` compartilhado entre `backend` (`/dialplan-generated`) e `asterisk` (`/etc/asterisk/generated`) em `docker-compose.dev.yml` e `docker-compose.yml`.
2. `extensions.conf` simplificado para apenas `#tryinclude generated/extensions_trunks.conf`.
3. `DialplanGeneratorService` gera o arquivo com stubs `switch => Realtime/@` para cada par `[internal-<tronco>]`/`[pstn-<tronco>]` e dispara `dialplan reload` via AMI.
4. Geração automática na inicialização via `@EventListener(ApplicationReadyEvent.class)`.
5. `provisionTrunk` e `deprovisionTrunk` invocam `generateAndReload()`; `reprovisionTrunk` não.
6. Propriedade `asterisk.dialplan.generated-path=/dialplan-generated` nos dois perfis.
7. 125 testes, 0 falhas.

---

### US-001 — Cadastro de provedores VoIP para troncos de saída

**Titulo:** Cadastro de provedores VoIP para troncos de saída com autenticação usuário/senha

**Descrição:**
Como administrador do sistema, quero cadastrar provedores VoIP (trunks SIP) com autenticação por usuário e senha, para que o Asterisk possa realizar chamadas de saída através desses provedores.

**Estimativa:** 8 story points

**Critérios de Aceite:**

1. **Listagem:** A interface exibe a lista de provedores VoIP cadastrados (nome, host/domínio, usuário, status de registro).
2. **Criação:** É possível cadastrar um novo provedor informando nome, host/domínio SIP, usuário e senha.
3. **Edição:** É possível editar um provedor existente (todos os campos; senha em branco não altera a senha).
4. **Exclusão:** É possível excluir um provedor, removendo todas as configurações do Asterisk associadas.
5. **Persistência no Asterisk:** Ao criar/editar/excluir, as tabelas `ps_endpoints`, `ps_auths`, `ps_aors` e `ps_registrations` são atualizadas e o Asterisk recebe um `pjsip reload`.
6. **Status de registro:** A interface exibe se o tronco está registrado ou não no provedor, consultando o estado via AMI.
7. **Validações:** Campos obrigatórios validados no frontend e backend; host deve ser não-vazio; nome único por sistema.

---

### US-005 — Refatoração: Modelo de Circuito SIP

**Titulo:** Refatoração: Modelo de Circuito SIP (softphone/ATA)

**Descrição:**
Como desenvolvedor, preciso refatorar o backend para que o conceito de **Circuito** seja a entidade central do domínio — representando um ramal SIP configurável para uso com softphone ou ATA — e que toda a interação com o Asterisk (ps_endpoints, ps_auths, ps_aors, extensions) seja encapsulada em uma camada de serviço, sem que entidades JPA do Asterisk vazem para o restante da aplicação.

**Estimativa:** 8 story points

**Critérios de Aceite:**

1. **Entidade `Circuito`:** Existe uma entidade de domínio própria (`Circuit`) com campos: número, senha. Essa entidade é persistida em tabela própria da aplicação (`asteracomm_circuits`), não nas tabelas do Asterisk.
2. **Camada de integração Asterisk:** `AsteriskProvisioningService` traduz o estado de um `Circuit` para as tabelas PJSIP (`ps_endpoints`, `ps_auths`, `ps_aors`) e para `extensions`, emitindo `pjsip reload` / `dialplan reload` via AMI após cada operação.
3. **Entidades Asterisk internas:** As entidades JPA das tabelas do Asterisk existem apenas dentro da camada de provisionamento, sem exposição via controllers, DTOs públicos ou serviços de domínio.
4. **Comportamento preservado:** O CRUD de circuitos (`/api/circuits`) continua funcionando com o mesmo contrato de API.
5. **Testes:** `CircuitServiceTest`, `CircuitControllerTest`, `AsteriskProvisioningServiceTest` — 73 testes, 0 falhas.
6. **Base para troncos:** Arquitetura extensível para US-001 (provedores VoIP/troncos) sem duplicar lógica de provisionamento.

---

### US-006 — Cadastro de DID (pool de números)

**Titulo:** Cadastro de DID (pool de números)

**Descrição:**
Como administrador, quero cadastrar DIDs (números de telefone) no sistema, formando um pool de números disponíveis para comercialização e uso, independentemente de estarem associados a um circuito.

**Estimativa:** 3 story points

**Critérios de Aceite:**

1. **Listagem:** Exibe DIDs cadastrados com: número, status (`LIVRE` | `EM USO`) e circuito vinculado (quando houver).
2. **Criação:** Cadastro com número obrigatório no formato `(XX) XXXX-XXXX` (exatamente 10 dígitos).
3. **Exclusão:** Impede exclusão de DID vinculado a um circuito.
4. **Validações:** Número único; apenas dígitos; exatamente 10 dígitos.
5. **Vinculação/Desvinculação:** Na tela do circuito, é possível vincular um DID livre e desvincular DIDs já associados (sem provisionamento Asterisk — ver US-007).
6. **Código do circuito auto-gerado:** Circuitos não aceitam mais número no cadastro; código gerado sequencialmente a partir de 100000.
7. **Testes:** 148 testes, 0 falhas.

---

### US-009 — Vinculação Circuito-Tronco com provisionamento automático de contextos

**Titulo:** Vinculação Circuito-Tronco com provisionamento automático de contextos

**Descrição:**
Como administrador do sistema, quero que cada circuito esteja obrigatoriamente vinculado a um tronco VoIP, para que o Asterisk configure automaticamente os contextos de discagem — permitindo que o circuito realize chamadas de saída pelo tronco e receba chamadas de entrada por ele.

**Estimativa:** 5 story points

**Critérios de Aceite:**

1. **Trunk com prefixo:** O tronco tem campo `prefix` opcional. Usado na composição do número discado: `PJSIP/<prefix>${EXTEN}@<tronco>,60`.
2. **Rota de saída auto-criada:** Ao criar um tronco, extensions de saída provisionadas em `internal-<nome>`: priority 1 NoOp, priority 2 Dial, priority 3 Hangup.
3. **Contexto de entrada do tronco:** `ps_endpoints` do tronco usa `context = "pstn-<nome>"`.
4. **Tronco obrigatório no circuito:** `trunkName NOT NULL`. `ps_endpoints` do circuito usa `context = "internal-<tronco>"`. Extensions de entrada criadas em `pstn-<tronco>`.
5. **Troca de tronco:** Reprovisionamento remove extensions do tronco antigo e cria no novo.
6. **Correções de provisão:** `provision(Circuit)` não usa mais `from-pstn`. `reprovisionTrunk` recria extensions de saída quando prefix ou host mudam.
