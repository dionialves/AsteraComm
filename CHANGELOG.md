# Changelog — AsteraComm

## Em desenvolvimento

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
