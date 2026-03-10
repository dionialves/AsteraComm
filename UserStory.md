# User Stories — AsteraComm

## Indice

1. [US-007 — Vinculação DID-Circuito com provisionamento automático de Extensions](#us-007)
2. [US-002 — Pesquisa e visualização de ligações realizadas](#us-002)
3. [US-003 — Cadastro de minutagem (tarifas por tipo de ligação)](#us-003)
4. [US-004 — Cadastro de planos de minutagem](#us-004)
5. [US-008 — Refatoração: EndpointStatusService usar AmiService](#us-008)

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

---

## US-008

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

