# Changelog — AsteraComm

## Em desenvolvimento

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
