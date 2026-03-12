# User Stories — AsteraComm

## Indice

1. [US-008 — Refatoração: EndpointStatusService usar AmiService](#us-008)

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

