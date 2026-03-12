# Changelog — AsteraComm

## Em desenvolvimento

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
