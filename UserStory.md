# User Stories — AsteraComm

## Indice

1. [US-003 — Cadastro de minutagem (tarifas por tipo de ligação)](#us-003)
4. [US-004 — Cadastro de planos de minutagem](#us-004)
5. [US-008 — Refatoração: EndpointStatusService usar AmiService](#us-008)

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

