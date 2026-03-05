# User Stories — AsteraComm

## Indice

1. [US-005 — Refatoração: Modelo de Circuito SIP (softphone/ATA)](#us-005)
2. [US-001 — Cadastro de provedores VoIP para troncos de saída](#us-001)
3. [US-006 — Cadastro de DID (pool de números)](#us-006)
4. [US-007 — Vinculação DID-Circuito com provisionamento automático de Extensions](#us-007)
5. [US-002 — Pesquisa e visualização de ligações realizadas](#us-002)
6. [US-003 — Cadastro de minutagem (tarifas por tipo de ligação)](#us-003)
7. [US-004 — Cadastro de planos de minutagem](#us-004)

---

## US-005

**Titulo:** Refatoração: Modelo de Circuito SIP (softphone/ATA)

**Descrição:**
Como desenvolvedor, preciso refatorar o backend para que o conceito de **Circuito** seja a entidade central do domínio — representando um ramal SIP configurável para uso com softphone ou ATA — e que toda a interação com o Asterisk (ps_endpoints, ps_auths, ps_aors, extensions) seja encapsulada em uma camada de serviço, sem que entidades JPA do Asterisk vazem para o restante da aplicação.

**Estimativa:** 8 story points

**Critérios de Aceite:**

1. **Entidade `Circuito`:** Existe uma entidade de domínio própria (`Circuit`) com campos: número, senha, tipo (`SOFTPHONE` | `ATA`), e demais atributos necessários. Essa entidade é persistida em tabela própria da aplicação, não nas tabelas do Asterisk.
2. **Camada de integração Asterisk:** Um serviço dedicado (`AsteriskProvisioningService` ou similar) é responsável por traduzir o estado de um `Circuit` para as tabelas PJSIP (`ps_endpoints`, `ps_auths`, `ps_aors`) e para `extensions`, emitindo `pjsip reload` / `dialplan reload` via AMI após cada operação.
3. **Entidades Asterisk internas:** As entidades JPA das tabelas do Asterisk (`AorEntity`, `AuthEntity`, `EndpointEntity`, `ExtensionEntity`) existem apenas dentro da camada de provisionamento, sem exposição via controllers, DTOs públicos ou serviços de domínio.
4. **Comportamento preservado:** O CRUD de circuitos existente (`/api/circuits`) continua funcionando com o mesmo contrato de API — sem breaking change para o frontend.
5. **Testes atualizados:** Testes existentes de circuito continuam passando; a nova camada de provisionamento tem testes unitários próprios (mockando AMI/repositórios Asterisk).
6. **Base para troncos:** A arquitetura resultante é extensível para acomodar a US-001 (provedores VoIP/troncos de saída) sem duplicar a lógica de provisionamento Asterisk.

---

## US-001

**Titulo:** Cadastro de provedores VoIP para troncos de saída com autenticação usuário/senha

**Descrição:**
Como administrador do sistema, quero cadastrar provedores VoIP (trunks SIP) com autenticação por usuário e senha, para que o Asterisk possa realizar chamadas de saída através desses provedores.

**Estimativa:** 8 story points

**Critérios de Aceite:**

1. **Listagem:** A interface exibe a lista de provedores VoIP cadastrados (nome, host/domínio, usuário, status de registro).
2. **Criação:** É possível cadastrar um novo provedor informando:
   - Nome (identificador interno)
   - Host/domínio SIP do provedor
   - Usuário (username para autenticação)
   - Senha
3. **Edição:** É possível editar um provedor existente (todos os campos; senha em branco não altera a senha).
4. **Exclusão:** É possível excluir um provedor, removendo todas as configurações do Asterisk associadas.
5. **Persistência no Asterisk:** Ao criar/editar/excluir, as tabelas `ps_endpoints`, `ps_auths`, `ps_aors` e `ps_registrations` são atualizadas com as configurações de tronco e o Asterisk recebe um `pjsip reload`.
6. **Status de registro:** A interface exibe se o tronco está registrado ou não no provedor, consultando o estado via AMI.
7. **Validações:** Campos obrigatórios validados no frontend e backend; host deve ser não-vazio; nome único por sistema.

---

## US-002

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

## US-003

**Titulo:** Cadastro de minutagem (tarifas por tipo de ligação)

**Descrição:**
Como administrador, quero cadastrar tabelas de tarifas de minutagem, definindo o valor por minuto para ligações locais, de longa distância e internacionais, para que essas tarifas possam ser associadas a planos.

**Estimativa:** 3 story points

**Critérios de Aceite:**

1. **Listagem:** Exibe lista de minutagens cadastradas (nome, valor local/min, valor LD/min, valor internacional/min).
2. **Criação:** Cadastro com nome, valor/minuto local, valor/minuto longa distância e valor/minuto internacional.
3. **Edição:** Permite alterar qualquer campo da minutagem.
4. **Exclusão:** Impede exclusão se a minutagem estiver vinculada a algum plano ativo.
5. **Validações:** Valores monetários não negativos; nome único; campos obrigatórios.

---

## US-004

**Titulo:** Cadastro de planos de minutagem

**Descrição:**
Como administrador, quero cadastrar planos de minutagem associando uma tabela de tarifas e definindo minutos gratuitos por categoria de ligação, para que os planos possam ser atribuídos a clientes ou troncos.

**Estimativa:** 5 story points

**Critérios de Aceite:**

1. **Listagem:** Exibe lista de planos com nome, minutagem associada e minutos gratuitos por categoria.
2. **Criação:** Cadastro com nome, seleção de minutagem (dropdown), minutos gratuitos para local, LD e internacional (podem ser 0).
3. **Edição:** Permite alterar qualquer campo do plano.
4. **Exclusão:** Permite excluir plano não vinculado a troncos/clientes.
5. **Validações:** Minutagem obrigatória; minutos gratuitos >= 0; nome único.

---

## US-006

**Titulo:** Cadastro de DID (pool de números)

**Descrição:**
Como administrador, quero cadastrar DIDs (números de telefone) no sistema, formando um pool de números disponíveis para comercialização e uso, independentemente de estarem associados a um circuito.

**Estimativa:** 3 story points

**Critérios de Aceite:**

1. **Listagem:** Exibe DIDs cadastrados com: número, descrição, status (`LIVRE` | `EM USO`) e circuito vinculado (quando houver).
2. **Criação:** Cadastro com número (obrigatório) e descrição (opcional).
3. **Edição:** Permite editar número e descrição de um DID.
4. **Exclusão:** Impede exclusão de DID vinculado a um circuito.
5. **Validações:** Número único no sistema; formato válido; campos obrigatórios.

---

## US-007

**Titulo:** Vinculação DID-Circuito com provisionamento automático de Extensions

**Descrição:**
Como administrador, quero vincular um ou mais DIDs a um circuito, para que o Asterisk provisione automaticamente as Extensions correspondentes, habilitando o circuito a receber e realizar chamadas por aquele número.

**Estimativa:** 5 story points

**Critérios de Aceite:**

1. **Vinculação:** Na tela do circuito (ou do DID), é possível associar um DID livre ao circuito. Um circuito pode ter 1 ou mais DIDs.
2. **Provisionamento automático:** Ao vincular, a Extension correspondente ao número do DID é criada nas tabelas do Asterisk e um `dialplan reload` é disparado via AMI.
3. **Desvinculação:** É possível desvincular um DID de um circuito. Ao desvincular, a Extension é removida do Asterisk e o `dialplan reload` é disparado.
4. **Status do DID:** DID vinculado muda status para `EM USO`; ao desvincular volta para `LIVRE`.
5. **Restrição:** Um DID só pode estar vinculado a um circuito por vez.
6. **Exibição:** Na listagem de circuitos e na listagem de DIDs, o vínculo é exibido claramente.
