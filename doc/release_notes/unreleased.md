# [Unreleased] — 2026-04-22

## Bug Fixes

### FIX-076: Calls sem circuito associado para ligações entrantes

**Problema:** O `CallProcessingService` associava a ligação ao circuito apenas via campo `channel` do CDR, extraindo o código após a barra (ex.: `PJSIP/4933401714-xxxxx` → `4933401714`). Isso funciona para ligações saintes (outbound), mas falha para ligações entrantes (inbound), pois o `channel` contém o nome do tronco (ex.: `PJSIP/operadora-xxxxx`) e não o circuito. Consequentemente, ligações recebidas ficavam com `circuit = null` e não apareciam na auditoria de custeio.

**Solução:**
- Adicionado `findByNumber(String number)` em `DIDRepository` para buscar DID por número exato.
- Adicionada segunda tentativa de vínculo em `CallProcessingService`: quando o circuito é `null` após a tentativa via `channel`, o serviço agora busca o `dst` (número discado) na tabela de DIDs. Se encontrar um DID vinculado a um circuito, associa esse circuito à chamada.

**Arquivos alterados:**
- `backend/src/main/java/com/dionialves/AsteraComm/call/CallProcessingService.java` — lógica de associação dual via channel e dst→DID
- `backend/src/main/java/com/dionialves/AsteraComm/did/DIDRepository.java` — novo método `findByNumber`
- `backend/src/test/java/com/dionialves/AsteraComm/call/CallProcessingServiceTest.java` — 2 novos cenários de teste

**Testes:**
- `process_shouldAssociateCircuitViaDstDid_whenInboundCall` — verifica vínculo via dst quando channel não resolve
- `process_shouldLeaveCircuitNull_whenDidNotFound` — verifica que circuit permanece null quando DID não existe

### FIX-078: Corrige testes de CircuitServiceTest com comportamento alterado

**Problema:** O commit `14ca9f6` introduziu duas mudanças no `CircuitService` que quebraram testes existentes:
1. `CircuitService.delete()` agora lança `BusinessException` quando há chamadas associadas (em vez de desativar e retornar o circuito).
2. `CircuitService.update()` com `dto.active() == null` agora seta `active = false` (em vez de preservar o estado anterior).

**Solução:** Atualizados os dois testes para refletir o novo comportamento real do código:
- `update_shouldSetActiveFalse_whenDtoHasActiveNull` — espera que circuito seja desativado quando DTO tem `active = null`
- `delete_shouldThrowBusinessException_whenCallsExist` — espera que `BusinessException` seja lançada quando há chamadas associadas

**Arquivos alterados:**
- `backend/src/test/java/com/dionialves/AsteraComm/circuit/CircuitServiceTest.java` — 2 testes corrigidos

### FIX-101: Corrige NonUniqueResultException em CdrRepository.findByUniqueId

**Problema:** `CdrRepository.findByUniqueId(String uniqueId)` retornava `Optional<CdrRecord>`, fazendo o Spring Data JPA usar internamente `getSingleResultOrNull()`. Na tabela `cdr` do Asterisk, o campo `uniqueid` **não é único** — cenários de retry, forwarding e retorno de ligação produzem múltiplos registros com o mesmo `uniqueid`. Quando há 2+ registros, o JPA lança `IncorrectResultSizeDataAccessException: Query did not return a unique result: 2 results were returned`, causando erro 500 no relatório de chamadas órfãs.

**Solução:**
- Criado novo método `findFirstByUniqueId(String uniqueId)` em `CdrRepository` que retorna `Optional<CdrRecord>`. O Spring Data JPA aplica `LIMIT 1` automaticamente via `findFirst...`, eliminando a exceção mesmo quando há múltiplos registros.
- `OrphanCallReportService` usa `findFirstByUniqueId` — mesmo comportamento seguro do ponto de vista do relatório (qualquer CDR com o mesmo `uniqueid` serve).
- Semanticamente correto: não fingimos que há um único quando pode haver vários; o método `findFirst` deixa isso explícito.

**Arquivos alterados:**
- `backend/src/main/java/com/dionialves/AsteraComm/cdr/CdrRepository.java` — novo método `findFirstByUniqueId`
- `backend/src/main/java/com/dionialves/AsteraComm/call/OrphanCallReportService.java` — chama `findFirstByUniqueId` em vez de `findByUniqueId`
- `backend/src/test/java/com/dionialves/AsteraComm/call/OrphanCallReportServiceTest.java` — 8 mocks atualizados para `findFirstByUniqueId`, 1 novo teste de duplicatas

**Testes novos:**
- `findOrphanCalls_handlesDuplicateCdrRecords_withoutError` — verifica que duplicatas não causam exceção

---

## Refactoring

### RF-095: Script de reconciliação de ligações sem circuito via canal CDR

**Problema:** Ligações históricas no banco possuem `circuit = null` e não aparecem na auditoria de custeio. Reprocessar esses CDRs recalcularia o custo usando o plano atual do circuito, alterando dados históricos incorretamente.

**Solução:**
- Criado `CallCircuitReconciliationService` que enumera chamadas sem circuito via `findByCircuitIsNull()`, analisa o `channel` do CDR original via `ChannelParser`, e atualiza **apenas** a FK `circuit_number` — preservando `cost`, `minutes_from_quota`, `call_status` e `call_type`.
- Criado `CallCircuitReconciliationController` com GET (visualização) e POST (execução) em `/admin/reconcile-calls`.
- Criado template `reconcile-calls.html` com tabela de órfãs, badge de resolvibilidade e botão de execução com CSRF.
- Criados DTOs `OrphanCallDTO` e `ReconciliationResultDTO` como records.
- Adicionado `findByCircuitIsNull()` no `CallRepository` para evitar carregar todos os registros em memória (performance).

**Arquivos alterados:**
- `backend/src/main/java/com/dionialves/AsteraComm/call/CallCircuitReconciliationService.java` — novo
- `backend/src/main/java/com/dionialves/AsteraComm/call/CallCircuitReconciliationController.java` — novo
- `backend/src/main/java/com/dionialves/AsteraComm/call/OrphanCallDTO.java` — novo
- `backend/src/main/java/com/dionialves/AsteraComm/call/ReconciliationResultDTO.java` — novo
- `backend/src/main/java/com/dionialves/AsteraComm/call/CallRepository.java` — novo método `findByCircuitIsNull`
- `backend/src/main/resources/templates/pages/admin/reconcile-calls.html` — novo
- `backend/src/test/java/com/dionialves/AsteraComm/call/CallCircuitReconciliationServiceTest.java` — novo

**Testes novos:**
- `findOrphanCalls_returnsResolvable_whenChannelMatchesCircuit`
- `findOrphanCalls_returnsNotResolvable_whenChannelEmpty`
- `findOrphanCalls_returnsNotResolvable_whenCircuitMissing`
- `reconcile_linksOnlyResolvableCalls`
- `reconcile_preservesCostAndStatus`

---

### RF-094: Relatório de auditoria com direção de chamada e filtro de ligações efetuadas

**Problema:** O relatório de auditoria exibe chamadas de entrada (inbound) e saída (outbound) sem distinção visual, e não havia filtro para exibir apenas ligações efetuadas (outbound) — as únicas que consomem minutos do plano e geram custo.

**Solução:**
- Criado enum `CallDirection` (`INBOUND`, `OUTBOUND`) persistido na tabela `asteracomm_calls` via coluna `direction`.
- Detecção automática no `CallProcessingService`: se o `dst` casar com um DID cadastrado, a chamada é classificada como `INBOUND`; caso contrário, `OUTBOUND`.
- Repositório expandido com JOIN na tabela de DIDs para buscar tanto chamadas saídas (via `circuit_number`) quanto recebidas (via `dst → DID`).
- `AuditCallLineDTO` recebe campo `direction`; `AuditService` adiciona parâmetro `onlyOutgoing` com filtro e versão sobrecarregada de `simulate` e `generatePdf`.
- Controller recebe `onlyOutgoing` nos endpoints de simulação e PDF.
- UI adiciona segundo toggle "Apenas ligações efetuadas" com badge "Recebida"/"Efetuada" por linha.

**Arquivos alterados:**
- `backend/src/main/java/com/dionialves/AsteraComm/call/CallDirection.java` — novo enum
- `backend/src/main/resources/db/migration/V11__add_direction_to_calls.sql` — nova migration
- `backend/src/main/java/com/dionialves/AsteraComm/call/Call.java` — campo `direction`
- `backend/src/main/java/com/dionialves/AsteraComm/call/CallProcessingService.java` — detecção de direção
- `backend/src/main/java/com/dionialves/AsteraComm/call/CallRepository.java` — query expandida com LEFT JOIN
- `backend/src/main/java/com/dionialves/AsteraComm/report/audit/AuditCallLineDTO.java` — campo `direction`
- `backend/src/main/java/com/dionialves/AsteraComm/report/audit/AuditService.java` — filtro `onlyOutgoing` + overloads
- `backend/src/main/java/com/dionialves/AsteraComm/report/ReportViewController.java` — parâmetro `onlyOutgoing`
- `backend/src/main/resources/templates/pages/reports/audit.html` — toggle + checkbox
- `backend/src/main/resources/templates/pages/reports/audit-table.html` — coluna direção + badge + JS toggle
- `backend/src/test/java/com/dionialves/AsteraComm/report/audit/AuditServiceTest.java` — 3 novos cenários
- `backend/src/test/java/com/dionialves/AsteraComm/call/CallProcessingServiceTest.java` — 2 novos cenários

**Testes novos:**
- `simulate_returnsOnlyOutbound_whenOnlyOutgoingTrue` — filtro retorna apenas OUTBOUND
- `simulate_returnsAll_whenOnlyOutgoingFalse` — sem filtro retorna todos
- `simulate_inboundCallsAppearInResult` — chamada INBOUND incluída no resultado
- `process_setsDirectionInbound_whenDstIsDid` — direção INBOUND quando dst é DID
- `process_setsDirectionOutbound_whenDstIsNotDid` — direção OUTBOUND quando dst não é DID

---

### RF-098: Refatorar relatório de chamadas órfãs com filtro por período e card no dashboard

**Problema:** O relatório antigo em `/admin/reconcile-calls` carregava 100% das chamadas órfãs sem filtro de período, causando tela em branco/timeout com alto volume. O acesso estava escondido sem link direto no dashboard.

**Solução:**
- Substituído o serviço `CallCircuitReconciliationService` pelo novo `OrphanCallReportService` com filtro obrigatório de mês/ano.
- Novo endpoint `/reports/orphan-calls` com formulário de filtro (mês/ano) e tabela HTMX.
- Novo card "Chamadas órfãs (mês)" no dashboard com badge "Atenção" quando count > 0.
- Link "Chamadas Órfãs" adicionado ao menu lateral dentro da seção Relatórios.
- Query limitada ao período selecionado: `findOrphanCallsByPeriod(month, year)` com `EXTRACT(MONTH/YEAR)`.

**Arquivos alterados:**
- `backend/src/main/java/com/dionialves/AsteraComm/call/OrphanCallReportDTO.java` — novo record
- `backend/src/main/java/com/dionialves/AsteraComm/call/OrphanCallReportService.java` — novo service com filtro por período
- `backend/src/main/java/com/dionialves/AsteraComm/call/OrphanCallReportController.java` — novo controller em `/reports/orphan-calls`
- `backend/src/main/java/com/dionialves/AsteraComm/call/CallRepository.java` — novo método `findOrphanCallsByPeriod`
- `backend/src/main/java/com/dionialves/AsteraComm/dashboard/DashboardDTO.java` — novo campo `orphanCalls`
- `backend/src/main/java/com/dionialves/AsteraComm/dashboard/DashboardService.java` — populador do campo `orphanCalls`
- `backend/src/main/resources/templates/pages/reports/orphan-calls.html` — novo template de filtro
- `backend/src/main/resources/templates/pages/reports/orphan-calls-table.html` — novo fragmento de tabela
- `backend/src/main/resources/templates/pages/dashboard/index.html` — card de chamadas órfãs
- `backend/src/main/resources/templates/layout/base.html` — item de menu em Relatórios

**Arquivos removidos:**
- `backend/src/main/java/com/dionialves/AsteraComm/call/CallCircuitReconciliationController.java`
- `backend/src/main/java/com/dionialves/AsteraComm/call/CallCircuitReconciliationService.java`
- `backend/src/main/java/com/dionialves/AsteraComm/call/OrphanCallDTO.java`
- `backend/src/main/java/com/dionialves/AsteraComm/call/ReconciliationResultDTO.java`
- `backend/src/main/resources/templates/pages/admin/reconcile-calls.html`
- `backend/src/test/java/com/dionialves/AsteraComm/call/CallCircuitReconciliationServiceTest.java`

**Testes novos:**
- `findOrphanCalls_returnsEmpty_whenNoOrphansForPeriod`
- `findOrphanCalls_returnsResolvable_whenChannelMatchesExistingCircuit`
- `findOrphanCalls_returnsNotResolvable_whenCircuitMissing`
- `findOrphanCalls_returnsNotResolvable_whenCdrMissing`
- `countOrphanCallsCurrentMonth_returnsCountForCurrentMonth`

---

### RF-100: Vincular circuito no processamento via channel e dstChannel

**Problema:** O `CallProcessingService` fazia a 2ª tentativa de vinculação via `dst → DID` (tabela de cadastro), que falhava quando o DID não estava cadastrado, gerando chamadas órfãs mesmo quando o `dstChannel` do CDR continha o código do circuito que atendeu a ligação.

**Solução:**
- Substituída a tentativa 2 (via `dst → DID → circuit`) por tentativa via `dstChannel` (campo do CDR preenchido pelo Asterisk com o endpoint que atendeu a ligação).
- Direção determinada pela tentativa que resolveu: `channel` → OUTBOUND, `dstChannel` → INBOUND.
- Removida a dependência de `DIDRepository` do `CallProcessingService`.
- A lógica fica idêntica ao `OrphanCallReportService` (que já usava channel → dstChannel).

**Arquivos alterados:**
- `backend/src/main/java/com/dionialves/AsteraComm/call/CallProcessingService.java` — tentativa 2 via dstChannel, remoção de DIDRepository
- `backend/src/test/java/com/dionialves/AsteraComm/call/CallProcessingServiceTest.java` — 4 testes removidos, 3 novos, 2 asserts adicionados

**Testes novos:**
- `process_shouldAssociateCircuitViaDstChannel_whenInboundCall` — vincula via dstChannel com direção INBOUND
- `process_shouldLeaveCircuitNull_whenBothChannelsFail` — nenhuma tentativa resolve
- `process_shouldLeaveCircuitNull_whenDstChannelIsEmpty` — dstChannel vazio não interfere
