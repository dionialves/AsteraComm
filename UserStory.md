# User Stories — AsteraComm

## Indice

1. [US-003 — Cadastro de planos de cobrança](#us-003)
2. [US-008 — Refatoração: EndpointStatusService usar AmiService](#us-008)

---

## US-003

**Titulo:** Cadastro de planos de cobrança

**Descrição:**
Como administrador, quero cadastrar planos de cobrança definindo o valor mensal, as tarifas por minuto para cada categoria de ligação fixa e, opcionalmente, um pacote de minutos inclusos — por categoria ou unificado — para que os planos possam ser atribuídos a clientes.

**Estimativa:** 5 story points

**Critérios de Aceite:**

1. **Listagem:** Exibe lista de planos com: nome, valor mensal, tipo de pacote e tarifas por categoria.
2. **Criação:** Cadastro com:
   - `name` — obrigatório, único
   - `monthlyPrice` — obrigatório, >= 0
   - `fixedLocal`, `fixedLongDistance`, `mobileLocal`, `mobileLongDistance` — tarifas R$/min, obrigatórias, >= 0, 4 casas decimais
   - `packageType`: `NONE` | `UNIFIED` | `PER_CATEGORY`
     - `NONE`: sem pacote de minutos
     - `UNIFIED`: campo `packageTotalMinutes` obrigatório (> 0); pool compartilhado entre todas as categorias
     - `PER_CATEGORY`: campos `packageFixedLocal`, `packageFixedLongDistance`, `packageMobileLocal`, `packageMobileLongDistance` obrigatórios (>= 0); buckets independentes por categoria
3. **Edição:** Permite alterar qualquer campo do plano.
4. **Exclusão:** Permite excluir plano não vinculado a clientes.
5. **Validações:** Consistência do pacote conforme `packageType`; nome único; campos obrigatórios.

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

