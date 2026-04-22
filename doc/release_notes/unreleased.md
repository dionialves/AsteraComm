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

---

## Refactoring

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
