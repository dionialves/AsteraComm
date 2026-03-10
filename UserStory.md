# User Stories — AsteraComm

## Indice

1. [US-010 — Geração dinâmica de `extensions_trunks.conf` para contextos de tronco](#us-010)
2. [US-006 — Cadastro de DID (pool de números)](#us-006)
3. [US-007 — Vinculação DID-Circuito com provisionamento automático de Extensions](#us-007)
4. [US-002 — Pesquisa e visualização de ligações realizadas](#us-002)
5. [US-003 — Cadastro de minutagem (tarifas por tipo de ligação)](#us-003)
6. [US-004 — Cadastro de planos de minutagem](#us-004)
7. [US-008 — Refatoração: EndpointStatusService usar AmiService](#us-008)

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

## US-010

**Titulo:** Geração dinâmica de `extensions_trunks.conf` para contextos de tronco

**Descrição:**
Como administrador, quero que ao cadastrar ou remover um tronco o Asterisk reconheça automaticamente os contextos de dialplan correspondentes (`internal-<tronco>` e `pstn-<tronco>`), sem necessidade de edição manual de arquivos de configuração, para que as chamadas de saída e entrada dos circuitos vinculados ao tronco funcionem imediatamente após o provisionamento.

**Estimativa:** 5 story points

**Contexto técnico (raiz do problema):**

O `AsteriskProvisioningService` já grava as extensions dos troncos e circuitos na tabela `extensions` (ARA/Realtime). O Asterisk consulta essa tabela via `switch => Realtime/@` — mas somente para contextos que possuam esse stub em `extensions.conf`. Como `extensions.conf` contém apenas stubs para `from-internal` e `from-pstn`, os contextos `internal-<tronco>` e `pstn-<tronco>` criados dinamicamente nunca são encontrados pelo Asterisk.

O backend (container `backend`) não monta o volume do Asterisk (`./asterisk/config`), impossibilitando escrita direta em `extensions.conf`. A solução é um volume Docker compartilhado entre os containers `backend` e `asterisk`, onde o backend escreve um arquivo `extensions_trunks.conf` gerado dinamicamente.

**Critérios de Aceite:**

1. **Volume Docker compartilhado:** Um volume nomeado `dialplan-generated` é adicionado a `docker-compose.dev.yml` e `docker-compose.yml`:
   - Montado no `asterisk` em `/etc/asterisk/generated`
   - Montado no `backend` em `/dialplan-generated`

2. **`extensions.conf` simplificado:** Os stubs `[from-pstn]` e `[from-internal]` com `switch => Realtime/@` são **removidos**. O arquivo passa a conter apenas:
   ```ini
   #tryinclude generated/extensions_trunks.conf
   ```
   O `#tryinclude` tolera ausência do arquivo no primeiro boot sem gerar erro de inicialização do Asterisk.

3. **`DialplanGeneratorService`:** Novo serviço criado em `asterisk/dialplan/` com dois métodos:
   - `generateTrunkContexts()`: consulta todos os troncos em `asteracomm_trunks`, gera o arquivo com um stub `switch => Realtime/@` para cada contexto `internal-<tronco>` e `pstn-<tronco>`, e escreve em `/dialplan-generated/extensions_trunks.conf`. Exemplo de saída:
     ```ini
     ; AUTO-GENERATED by AsteraComm — NÃO EDITE MANUALMENTE
     ; Gerado em: <timestamp>

     [internal-vivo]
     switch => Realtime/@

     [pstn-vivo]
     switch => Realtime/@
     ```
   - `generateAndReload()`: chama `generateTrunkContexts()` e em seguida envia `dialplan reload` via `AmiService`.
   - O caminho do diretório de saída é configurável via propriedade `asterisk.dialplan.generated-path` (default: `/dialplan-generated`).

4. **Geração na inicialização:** `DialplanGeneratorService.generateAndReload()` é chamado via `@EventListener(ApplicationReadyEvent.class)` na inicialização do backend, garantindo que o arquivo reflita o estado atual do banco mesmo após restart.

5. **Integração com `AsteriskProvisioningService`:** Os métodos a seguir passam a invocar `dialplanGeneratorService.generateAndReload()` após sua lógica atual:
   - `provisionTrunk(Trunk)` — novo tronco exige novo par de stubs no arquivo
   - `deprovisionTrunk(Trunk)` — tronco removido deve ter seus stubs eliminados do arquivo
   - `reprovisionTrunk` **não** regenera o arquivo (nome do tronco é imutável por ser PK)

6. **Configuração por perfil:**
   - `application-dev.properties`: `asterisk.dialplan.generated-path=/dialplan-generated`
   - `application-prod.properties` (ou variável de ambiente): `asterisk.dialplan.generated-path=/dialplan-generated`

7. **Testes unitários — `DialplanGeneratorServiceTest`:**
   - Geração com zero troncos: arquivo contém apenas o cabeçalho, sem blocos de contexto.
   - Geração com um tronco: arquivo contém exatamente `[internal-<nome>]` e `[pstn-<nome>]`.
   - Geração com múltiplos troncos: todos os contextos esperados presentes, sem duplicatas.
   - `generateAndReload` invoca `amiService.sendCommand("dialplan reload")` após a escrita.
   - Falha de escrita (diretório inexistente ou sem permissão): exceção descritiva lançada, não engolida silenciosamente.
   - Testes usam `@TempDir` para não depender do filesystem real.

8. **Testes de integração — `AsteriskProvisioningServiceTest` (ajuste):**
   - `provisionTrunk` invoca `dialplanGeneratorService.generateAndReload()`.
   - `deprovisionTrunk` invoca `dialplanGeneratorService.generateAndReload()`.
   - `reprovisionTrunk` **não** invoca `generateAndReload`.

**Fora de escopo:**
- Rotas de saída com seleção de padrão de discagem — coberto por US-009
- Vinculação DID-Circuito — coberto por US-007
- Geração de dialplan completo sem Realtime (modelo FreePBX full-generation)


