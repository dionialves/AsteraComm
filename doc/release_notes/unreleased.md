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
