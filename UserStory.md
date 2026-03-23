# User Stories — AsteraComm

## Indice

1. [US-013 — Refatoração: múltiplos DIDs por circuito e seleção de CallerID](#us-013)
2. [US-017 — Snapshot de estado do circuito, DID e plano no processamento da ligação](#us-017)
3. [US-011 — Fatura mensal por circuito (Invoice)](#us-011)
4. [US-012 — Refatoração: reorganização de pacotes em `domain/`](#us-012)
5. [US-037 — Adicionar campo `linked_at` ao DID](#us-037)
6. [US-040 — Refatoração: extrair scripts de modal para arquivos `.ts` importáveis nas páginas Astro](#us-040)
7. [FIX-001 — Erro ao desativar/ativar usuário](#fix-001)
8. [FIX-002 — Modais fora do tamanho correto nas páginas de Ligações e Planos](#fix-002)
9. [FIX-003 — Botões do modal desalinhados no modo de criação](#fix-003)
10. [FIX-005 — Refatorar fetches com limite hardcoded no frontend](#fix-005)
11. [FIX-006 — Modal de Planos com tamanho incorreto](#fix-006)
12. [US-062 — Refatoração: organizar pacote `report` com sub-pacotes por relatório](#us-062)
13. [US-052 — Migrar frontend para 100% Tailwind CSS](#us-052)
14. [US-054 — Criar circuito a partir do modal de cliente](#us-054)
15. [US-060 — Excluir usuário pelo modal de edição](#us-060)
16. [US-061 — Refatoração: controle de acesso ao menu por nível de usuário](#us-061)
17. [FIX-007 — btn-prev habilitado na primeira página da listagem de Circuitos](#fix-007)
18. [FIX-008 — Código gerado automaticamente ao criar circuito usa número de telefone em vez de sequência 100000+](#fix-008)
19. [FIX-009 — Permitir criação de cliente sem nome](#fix-009)
20. [FIX-010 — Planos e clientes inativos aparecendo nos seletores do modal de Circuito](#fix-010)
21. [FIX-011 — Desativar cliente automaticamente quando seu último circuito ativo for desativado](#fix-011)

---

## US-011

**Titulo:** Fatura mensal por circuito (Invoice)

**Descrição:**
Como administrador, quero que cada circuito gere automaticamente uma fatura mensal, podendo visualizar a fatura em andamento como um resumo parcial e as faturas fechadas em uma lista identificada pelo mês, contendo o nome do cliente, o plano, e as ligações agrupadas por tipo de destino.

**Estimativa:** 5 story points

**Critérios de Aceite:**

1. **Campo `closing_day` no circuito:** O circuito passa a ter o campo `closing_day` (inteiro, obrigatório, valores de 1 a 28), que define o dia do mês em que a fatura é fechada.
2. **Geração automática:** Ao ser processada a primeira ligação de um novo período, uma fatura com status `OPEN` é criada automaticamente para o circuito, caso ainda não exista.
3. **Fechamento automático:** Um job agendado verifica diariamente os circuitos cujo `closing_day` corresponde ao dia atual e fecha as faturas `OPEN` do período, alterando o status para `CLOSED`.
4. **Fatura em andamento:** Enquanto `OPEN`, é possível acessar um endpoint que retorna o resumo parcial da fatura com todas as ligações já registradas no período.
5. **Lista de faturas fechadas:** Endpoint que retorna as faturas `CLOSED` de um circuito, identificadas pelo mês/ano de referência (ex.: `"Fevereiro 2026"`).
6. **Conteúdo da fatura:** A fatura exibe:
   - Nome do cliente vinculado ao circuito.
   - Nome do plano.
   - Ligações agrupadas por **tipo de destino**: Fixo Local, Fixo DDD, Móvel Local, Móvel DDD, Internacional — cada grupo com a lista de ligações (data/hora, destino, duração, valor) e subtotal do grupo.
   - Total geral de minutos consumidos e valor total da fatura.
7. **Classificação do tipo:** O tipo de destino é determinado pelo número discado (ex.: DDDs 11–99 para fixo/móvel local vs. DDD diferente, prefixo `00xx` para longa distância, `+` ou `00` seguido de código de país para internacional).
8. **Testes:** Testes unitários e de integração cobrem: criação automática de fatura, agrupamento por tipo de destino, fechamento pelo `closing_day` e consulta de fatura em andamento.

---

## US-012

**Titulo:** Refatoração: reorganização de pacotes em `domain/`

**Descrição:**
Como desenvolvedor, quero reorganizar os pacotes do backend introduzindo um pacote raiz `domain/`, de forma que todos os domínios de negócio fiquem agrupados ali (com os domínios Asterisk dentro de `domain/asterisk/`), enquanto pacotes de suporte (`config/`, `infra/`, `exception/`) permanecem fora, tornando a estrutura mais legível e alinhada com os princípios de arquitetura de domínio.

**Estimativa:** 2 story points

**Critérios de Aceite:**

1. **Novo pacote `domain/`:** Criado em `com.dionialves.AsteraComm.domain`.
2. **Subpacote `domain/asterisk/`:** Todos os pacotes atuais de `asterisk/` (`aors`, `auth`, `dialplan`, `endpoint`, `extension`, `provisioning`, `registration`) são movidos para `domain/asterisk/`.
3. **Domínios de negócio em `domain/`:** Os pacotes `auth`, `cdr`, `circuit`, `did`, `plan`, `trunk`, `user` são movidos para `domain/`, no mesmo nível de `asterisk/`.
4. **Fora de `domain/`:** Os pacotes `config/`, `infra/`, `exception/` permanecem no nível raiz, sem alteração de estrutura interna.
5. **Sem quebra de funcionalidade:** Todos os imports são atualizados, a aplicação compila e os testes existentes passam.
6. **Sem alteração de comportamento:** Nenhuma lógica de negócio é alterada — apenas movimentação de pacotes/arquivos.

**Nova estrutura resultante:**

```
com.dionialves.AsteraComm/
├── AsteraCommApplication.java
├── domain/
│   ├── asterisk/
│   │   ├── aors/
│   │   ├── auth/
│   │   ├── dialplan/
│   │   ├── endpoint/
│   │   ├── extension/
│   │   ├── provisioning/
│   │   └── registration/
│   ├── auth/
│   ├── cdr/
│   ├── circuit/
│   ├── did/
│   ├── plan/
│   ├── trunk/
│   └── user/
├── config/
├── exception/
└── infra/
```

---

## US-013

**Titulo:** Refatoração: múltiplos DIDs por circuito e seleção de CallerID

**Descrição:**
Como administrador, quero que um circuito possa ter mais de um DID associado, e que na tela de edição do circuito eu possa escolher qual DID será utilizado nas chamadas saintes, preenchendo automaticamente o campo `callerid` com o número do DID selecionado. Ao vincular o primeiro DID ao circuito, ele deve ser automaticamente definido como `callerid`.

**Estimativa:** 3 story points

**Critérios de Aceite:**

1. **Relacionamento 1-N:** A entidade `DID` passa a ter uma FK opcional para `Circuit` (`circuit_id`), substituindo o vínculo anterior (se houver). Um circuito pode ter N DIDs; um DID pertence a no máximo um circuito.
2. **DID ativo (callerid):** O circuito possui um campo `active_did_id` (FK para `DID`, nullable) que indica qual DID é o CallerID atual para chamadas saintes.
3. **Preenchimento automático do `callerid`:** Sempre que `active_did_id` for alterado, o campo `callerid` do circuito é atualizado com o número do DID correspondente.
4. **Primeiro DID automático:** Ao vincular o primeiro DID a um circuito (que ainda não possui `active_did_id`), esse DID é automaticamente definido como `active_did_id` e o `callerid` é preenchido.
5. **Tela de edição do circuito:** Exibe a lista de DIDs associados ao circuito e um seletor para escolher o DID ativo. Ao selecionar, o campo `callerid` é atualizado visualmente e salvo no backend.
6. **API:**
   - `PATCH /api/v1/circuits/{id}/active-did` — altera o DID ativo do circuito.
   - `GET /api/v1/circuits/{id}` — retorna a lista de DIDs vinculados e o `active_did_id`.
7. **Testes:** Testes unitários cobrem: vínculo do primeiro DID (auto-set), troca de DID ativo e atualização do `callerid`.

---

## US-017

**Titulo:** Snapshot de estado do circuito, DID e plano no processamento da ligação

**Descrição:**
Como desenvolvedor, quero que cada ligação processada registre um snapshot dos dados relevantes do circuito, DID e plano vigentes no momento do processamento, para que auditorias futuras não dependam do estado atual dessas entidades, que podem ter sido alteradas desde então.

**Estimativa:** 2 story points

**Critérios de Aceite:**

1. **Campos de snapshot em `Call`:** A entidade `Call` passa a armazenar, no momento do processamento, os seguintes dados desnormalizados:
   - Do **plano**: nome, valor da franquia de minutos (se houver), tarifas por tipo de destino (fixo local, fixo DDD, móvel local, móvel DDD, internacional).
   - Do **circuito**: nome, `closing_day`.
   - Do **DID**: número.
2. **Preenchimento automático:** Ao processar uma ligação, o serviço de billing preenche os campos de snapshot com os valores vigentes naquele instante. Nenhuma ação manual é necessária.
3. **Imutabilidade:** Os campos de snapshot nunca são atualizados após a criação do registro — alterações posteriores no plano, circuito ou DID não afetam chamadas já registradas.
4. **Ferramenta de auditoria (US-016):** A ferramenta de auditoria passa a utilizar os dados do snapshot armazenados em `Call` para os cálculos, em vez de buscar o estado atual do plano/circuito.
5. **Migração:** Uma migração Flyway adiciona as novas colunas à tabela de calls, com valor `NULL` para registros históricos (aceito para chamadas anteriores à feature).
6. **Testes:** Testes unitários cobrem: snapshot preenchido corretamente no processamento, imutabilidade após alteração do plano, e que a auditoria usa os dados do snapshot e não os valores atuais.

---

## US-037

**Titulo:** Adicionar campo `linked_at` ao DID

**Descrição:**
Como administrador, quero que cada DID registre a data em que foi vinculado a um circuito, para que a página de detalhe do circuito exiba a coluna "Vinculado em" com a data correta.

**Estimativa:** 1 story point

**Critérios de Aceite:**

1. **Campo `linked_at`:** A entidade `DID` passa a ter o campo `linked_at` (timestamp, nullable), preenchido automaticamente com a data/hora atual sempre que o DID é vinculado a um circuito (`circuit_id` atribuído).
2. **Limpeza ao desvincular:** Ao desvincular o DID (circuito removido), `linked_at` é zerado (`NULL`).
3. **Migração:** Uma migração Flyway adiciona a coluna `linked_at` à tabela de DIDs, com valor `NULL` para registros existentes.
4. **API:** O endpoint `GET /api/v1/dids/by-circuit/{circuitId}` retorna o campo `linkedAt` no JSON de cada DID.
5. **Frontend:** Nenhuma alteração necessária — a página de detalhe do circuito já usa `did.linkedAt` na coluna "Vinculado em".

---

## US-040

**Titulo:** Refatoração: extrair scripts de modal para arquivos `.ts` importáveis nas páginas Astro

**Descrição:**
Como desenvolvedor, quero que a lógica dos modais (`ModalSystem`, `ChipSelect` e os scripts de abertura/população de modal) seja extraída dos blocos `<script>` inline das páginas Astro para arquivos TypeScript dedicados, eliminando a duplicação de código entre `circuits/index.astro` e `customers/index.astro` e tornando a adição de novos modais mais simples.

**Estimativa:** 2 story points

**Critérios de Aceite:**

1. **Resolução do alias `@/`:** O bundler Vite resolve corretamente imports `@/lib/*` nos scripts das páginas Astro (sem `is:inline` e sem `type="module"`), eliminando o erro "bare specifier".
2. **`ModalSystem` e `ChipSelect` importados:** As classes deixam de ser copiadas inline em cada página e passam a ser importadas de `src/lib/modal-system.ts` e `src/lib/chip-select.ts`.
3. **Scripts de página extraídos:** A lógica de cada modal (população de campos, save, delete, tabs) é movida para arquivos como `src/lib/modals/circuit-modal.ts` e `src/lib/modals/customer-modal.ts`, importados nas respectivas páginas.
4. **Sem duplicação:** O sub-modal de cliente (aberto a partir do circuito) reutiliza a lógica de `customer-modal.ts`.
5. **Comportamento preservado:** Todos os critérios da US-039 continuam funcionando após a refatoração.
6. **Testes:** Os testes existentes de `ModalSystem` e `ChipSelect` continuam passando.

---

## FIX-001

**Titulo:** Erro ao desativar/ativar usuário

**Descrição:**
Como administrador, ao clicar em "Desativar usuário" ou "Ativar usuário" no modal de edição, o sistema retorna erro porque as rotas Astro `PATCH /api/users/[id]/disable` e `PATCH /api/users/[id]/enable` não existem no frontend.

**Estimativa:** 1 story point

**Critérios de Aceite:**

1. **Rota disable:** Criar `frontend/src/pages/api/users/[id]/disable.ts` com handler `PATCH` que proxia para `PATCH /api/users/{id}/disable` no backend com o token de autenticação.
2. **Rota enable:** Criar `frontend/src/pages/api/users/[id]/enable.ts` com handler `PATCH` que proxia para `PATCH /api/users/{id}/enable` no backend com o token de autenticação.
3. **Comportamento:** O toggle Ativar/Desativar no modal funciona sem erro, a tabela atualiza imediatamente e exibe toast de sucesso.

---

## FIX-002

**Titulo:** Modais fora do tamanho correto nas páginas de Ligações e Planos

**Descrição:**
Como administrador, os modais das páginas de Ligações (`/calls`) e Planos (`/plans`) estão com dimensões incorretas em relação ao padrão visual do sistema, causando inconsistência de layout.

**Estimativa:** 1 story point

**Critérios de Aceite:**

1. **Modal de Planos:** largura e altura ajustadas para seguir o padrão canônico (`max-w-md` ou `max-w-lg` conforme quantidade de campos), sem overflow nem compressão de conteúdo.
2. **Modal de Ligações:** idem — dimensões corrigidas para exibir todos os campos sem scroll desnecessário ou espaço excessivo.
3. **Sem alteração** de conteúdo, lógica ou comportamento dos modais — apenas dimensões e espaçamentos corrigidos.

---

## FIX-003

**Titulo:** Botões do modal desalinhados no modo de criação

**Descrição:**
Ao abrir o modal para adicionar um novo registro, o botão "Excluir" (visível apenas no modo edição) ocupa espaço no layout mesmo quando oculto, fazendo com que os botões "Cancelar" e "Salvar" não fiquem totalmente à direita.

**Estimativa:** 1 story point

**Critérios de Aceite:**

1. **Modo criação:** os botões "Cancelar" e "Salvar" ficam alinhados à direita (`justify-content: flex-end`) quando não há botão "Excluir" visível.
2. **Modo edição:** o layout permanece com "Excluir" à esquerda e "Cancelar"/"Salvar" à direita (`space-between`), sem alteração.
3. **Escopo:** corrigir em todas as páginas que utilizam o padrão de modal com footer (`modal-footer`) — Circuitos, Clientes, Planos, DIDs, Troncos e Usuários.

---

## FIX-005

**Titulo:** Refatorar fetches com limite hardcoded no frontend

**Descrição:**
Vários fetches no frontend usam `size=200` ou `size=9999` para carregar listas completas (ex.: clientes no modal de circuito, planos, troncos). Esse padrão é frágil e não escala. A abordagem correta é usar endpoints dedicados de listagem simplificada (sem paginação) no backend, retornando apenas os campos necessários para populares os selects.

**Estimativa:** 3 story points

**Critérios de Aceite:**

1. **Mapeamento:** Identificados todos os fetches com `size ≥ 200` ou `size=9999` nas páginas Astro e rotas de API do frontend.
2. **Endpoints backend:** Para cada recurso afetado, criado endpoint `GET /api/{recurso}/all` retornando lista plana (sem `Page`) com apenas os campos necessários para o select (ex.: `id`, `name`, `number`).
3. **Rotas frontend:** Criadas ou atualizadas rotas Astro correspondentes para os novos endpoints.
4. **Fetches atualizados:** Todas as chamadas com limite hardcoded substituídas pelos novos endpoints.
5. **Comportamento preservado:** A filtragem/busca no `SearchSelect` continua funcionando client-side sobre a lista retornada.

---

## FIX-006

**Titulo:** Modal de Planos com tamanho incorreto

**Descrição:**
O modal da página de Planos (`/plans`) está com dimensões incorretas em relação ao padrão visual do sistema, causando inconsistência de layout.

**Estimativa:** 1 story point

**Critérios de Aceite:**

1. **Dimensões corrigidas:** Largura e altura ajustadas para seguir o padrão canônico dos demais modais do sistema.
2. **Sem regressão:** Nenhuma alteração de conteúdo, lógica ou comportamento — apenas dimensões e espaçamentos corrigidos.

---

## US-062

**Titulo:** Refatoração: organizar pacote `report` com sub-pacotes por relatório

**Descrição:**
Como desenvolvedor, quero reorganizar o backend para que o pacote `report` contenha um sub-pacote dedicado para cada relatório, e que os arquivos de Auditoria — atualmente dispersos no pacote `call/` — sejam movidos para `report/audit/`. Isso torna a estrutura mais coesa, facilita a adição de novos relatórios e elimina a mistura de responsabilidades no pacote `call/`.

**Estimativa:** 2 story points

**Critérios de Aceite:**

1. **Sub-pacote `report/costpercircuit/`:** Os 7 arquivos atuais de `report/` são movidos para `report/costpercircuit/`. As classes `CallReportController`, `CallReportService` e `CallReportRepository` são renomeadas para `CostPerCircuitController`, `CostPerCircuitService` e `CostPerCircuitRepository`.
2. **Sub-pacote `report/audit/`:** Os 5 arquivos de Auditoria (`AuditController`, `AuditService`, `AuditResultDTO`, `AuditCallLineDTO`, `AuditSummaryDTO`) são movidos de `call/` para `report/audit/`.
3. **Imports atualizados:** Todos os imports afetados no código de produção e de teste são atualizados para os novos pacotes.
4. **Testes movidos:** `CallReportControllerTest` e `CallReportServiceTest` movidos para `report/costpercircuit/` (renomeados); `AuditServiceTest` movido de `call/` para `report/audit/`.
5. **Endpoints preservados:** Os mapeamentos HTTP (`/api/audit/cost-simulation`, `/api/reports/*`) permanecem inalterados — apenas a estrutura de pacotes muda.
6. **Sem quebra de funcionalidade:** A aplicação compila e todos os testes existentes passam.

**Estrutura resultante:**

```
report/
├── audit/
│   ├── AuditCallLineDTO.java
│   ├── AuditController.java
│   ├── AuditResultDTO.java
│   ├── AuditService.java
│   └── AuditSummaryDTO.java
└── costpercircuit/
    ├── CallCostReportDTO.java
    ├── CallCostReportRow.java
    ├── CostPerCircuitController.java
    ├── CostPerCircuitRepository.java
    ├── CostPerCircuitResponseDTO.java
    ├── CostPerCircuitService.java
    └── CostPerCircuitSummaryDTO.java
```

---

## US-052

**Titulo:** Migrar frontend para 100% Tailwind CSS

**Descrição:**
Como desenvolvedor, quero eliminar todo CSS inline e classes globais desnecessárias das páginas Astro, migrando 100% para Tailwind CSS utility classes, para manter consistência visual e facilitar manutenção futura.

**Estimativa:** 5 story points

**Critérios de Aceite:**

1. **Zero CSS inline:** Nenhum atributo `style=""` nas páginas Astro migradas.
2. **Zero classes globais ad-hoc:** Nenhuma classe CSS adicionada ao `global.css` para uso pontual em uma única página.
3. **Tailwind arbitrary values:** Cores e dimensões específicas do design system (`#1D9E75`, `#085041`, etc.) expressas via Tailwind arbitrary values (ex: `text-[#1D9E75]`, `bg-[#f5f5f5]`).
4. **Cobertura:** Todas as páginas implementadas nas USs de reestruturação (US-041 a US-051) devem estar em conformidade.
5. **Comportamento preservado:** Nenhuma alteração visual perceptível — layout, cores, espaçamentos e interações idênticos ao estado anterior.

---

## US-054

**Titulo:** Criar circuito a partir do modal de cliente

**Descrição:**
Como administrador, quero poder criar um novo circuito diretamente pela aba "Circuitos" do modal de cliente, com o vínculo ao cliente já preenchido automaticamente, para agilizar o cadastro sem precisar sair da tela de clientes.

**Estimativa:** 2 story points

**Critérios de Aceite:**

1. **Botão "+ Novo circuito":** Na aba "Circuitos" do modal de cliente, um botão "+ Novo circuito" abre o sub-modal em modo criação (campos vazios, exceto "Cliente" já preenchido e bloqueado).
2. **Campo Cliente pré-preenchido:** O sub-modal em modo criação exibe o nome do cliente atual no chip de "Cliente" com estado fixo (não editável, sem botão de limpar).
3. **Campos do formulário:** Senha (opcional), Tronco (obrigatório) e Plano (obrigatório) — os mesmos do sub-modal de edição. O campo "Código" não é exibido (gerado automaticamente pelo backend).
4. **Validação:** Se Tronco ou Plano não estiverem selecionados ao salvar, exibe erro inline no sub-modal: "Tronco e Plano são obrigatórios."
5. **Salvamento:** Ao salvar, envia `POST /api/circuit/circuits` com `{ password?, trunkName, planId, customerId }`. Em caso de sucesso, fecha o sub-modal, recarrega a lista de circuitos da aba e atualiza o `circuitCount` da linha na listagem.
6. **Distinção visual:** O header do sub-modal exibe "Novo circuito" como título (em vez do código do circuito). O botão "Deletar" não é exibido no modo criação.
7. **Comportamento preservado:** O fluxo de edição de circuito existente (clique no ícone de seta na linha) continua funcionando sem alterações.

---

## US-060

**Titulo:** Excluir usuário pelo modal de edição

**Descrição:**
Como administrador, quero poder excluir um usuário diretamente pelo modal de edição, com confirmação antes de executar a ação, para remover contas que não serão mais utilizadas.

**Estimativa:** 1 story point

**Critérios de Aceite:**

1. **Botão "Excluir":** exibido apenas no modal de edição, posicionado à esquerda no rodapé do header do modal (separado dos botões Salvar/Cancelar), texto `#791F1F`, hover `bg-[#FEE2E2]`, border `#e0e0e0`.
2. **Confirmação por dois cliques:** ao clicar em "Excluir", o botão muda seu texto para "Confirmar exclusão" e adiciona a classe `confirm`. Um timer de 3 segundos redefine o botão ao estado original se não houver segundo clique. Somente ao clicar novamente a exclusão é executada. O `confirm-body`, `btn-confirm-delete` e toda a lógica de troca de painel são removidos.
3. **Execução:** confirmar chama `DELETE /api/users/{id}`, fecha o modal, remove o usuário da tabela e exibe toast de sucesso.
4. **Proteção:** o botão "Excluir" é oculto quando o usuário logado é o mesmo que está sendo editado (sem auto-exclusão).

---

## US-061

**Titulo:** Refatoração: controle de acesso ao menu por nível de usuário

**Descrição:**
Como administrador, quero que os itens do sidebar de navegação sejam exibidos de acordo com o nível de acesso do usuário logado, ocultando seções restritas para perfis com menos privilégios.

**Estimativa:** 2 story points

**Critérios de Aceite:**

1. **Leitura do perfil logado:** O frontend obtém o `role` do usuário autenticado (via endpoint existente ou JWT decodificado no middleware) e o disponibiliza para as decisões de visibilidade do menu.
2. **Regras de visibilidade por role:**
   - `SUPER_ADMIN`: acesso a todos os itens do menu.
   - `ADMIN`: acesso a todos os itens, exceto o menu de **Usuários**.
   - `USER` (Operador): acesso apenas a itens operacionais (Circuitos, Ligações, Auditoria) — sem acesso a Usuários, Clientes, Planos, Troncos ou DIDs.
3. **Proteção de rota:** Além da visibilidade no menu, o middleware Astro (`src/middleware.ts`) nega acesso direto via URL a rotas restritas, redirecionando para `/dashboard` com mensagem de permissão insuficiente.
4. **Sem impacto visual:** itens visíveis permanecem com aparência idêntica ao atual; apenas itens inacessíveis são ocultados.

---

## FIX-007

**Titulo:** btn-prev habilitado na primeira página da listagem de Circuitos

**Descrição:**
Na página de listagem de Circuitos, o botão de página anterior (`btn-prev`) aparece habilitado mesmo quando o usuário está na primeira página (`currentPage === 0`), permitindo clique sem efeito.

**Estimativa:** 0,5 story points

**Critérios de Aceite:**

1. **Estado inicial:** ao carregar a listagem, `btn-prev` está desabilitado (`disabled`) quando `currentPage === 0`.
2. **Navegação:** ao avançar para a página 2 ou além, `btn-prev` é habilitado corretamente.
3. **Consistência:** o comportamento de `btn-next` (já correto) serve de referência — `btn-prev` deve seguir a mesma lógica simétrica.

---

## FIX-008

**Titulo:** Código gerado automaticamente ao criar circuito usa número de telefone em vez de sequência 100000+

**Descrição:**
Ao criar um novo circuito, o campo `number` (código do circuito) deveria ser gerado pelo backend como um sequencial a partir de `100000`. Porém, está sendo atribuído um valor no formato de número de telefone (ex: `49xxxxxxxx`), provavelmente reutilizando lógica errada ou lendo de outra fonte.

**Estimativa:** 1 story point

**Critérios de Aceite:**

1. **Geração sequencial:** o `number` do primeiro circuito criado é `100000`; o segundo `100001`; e assim por diante, independentemente de outros campos.
2. **Sem colisão:** dois circuitos nunca recebem o mesmo `number`.
3. **Sem interferência de DID/telefone:** nenhum dado de DID, tronco ou cliente influencia o valor gerado.
4. **Exibição correta no modal:** após salvar, o modal reaberto exibe o `number` correto (ex: `100000`) no título e no campo de código.

---

## FIX-009

**Titulo:** Permitir criação de cliente sem nome

**Descrição:**
Atualmente o sistema aceita cadastrar um cliente sem informar o nome, tanto pelo frontend (sem feedback visual de obrigatoriedade) quanto pelo backend (sem validação). O campo Nome deve ser obrigatório nas duas camadas.

**Estimativa:** 1 story point

**Critérios de Aceite:**

1. **Frontend — indicação visual:** o label "Nome" exibe `*` vermelho, sinalizando campo obrigatório.
2. **Frontend — validação client-side:** ao tentar salvar sem nome preenchido (ou apenas espaços), exibe mensagem de erro inline no modal: "O campo Nome é obrigatório." O envio ao backend não ocorre.
3. **Backend — validação server-side:** o endpoint `POST /api/customer/customers` (e `PUT` de edição, se aplicável) rejeita requisições com `name` nulo, vazio ou somente espaços, retornando `400 Bad Request` com mensagem descritiva.
4. **Comportamento de edição preservado:** a validação não interfere no fluxo de edição de clientes que já possuem nome.

---

## FIX-010

**Titulo:** Planos e clientes inativos aparecendo nos seletores do modal de Circuito

**Descrição:**
No modal de criação/edição de Circuito, os seletores de Plano e Cliente exibem todos os registros, incluindo planos com `active = false` e clientes com `enabled = false`. Registros inativos não devem ser selecionáveis.

**Estimativa:** 1 story point

**Critérios de Aceite:**

1. **Seletor de Plano:** o endpoint utilizado para popular o seletor (`GET /api/plans/all` ou equivalente) passa a retornar apenas planos com `active = true`.
2. **Seletor de Cliente:** o endpoint utilizado para popular o seletor (`GET /api/customer/customers/all` ou equivalente) passa a retornar apenas clientes com `enabled = true`.
3. **Comportamento:** planos e clientes inativos não aparecem na lista de opções do modal de Circuito (criação e edição).
4. **Sem impacto em circuitos existentes:** um circuito já vinculado a um plano/cliente que foi posteriormente desativado continua exibindo corretamente esses dados em modo de visualização/edição — apenas a seleção de novos registros é restrita.

---

## FIX-011

**Titulo:** Desativar cliente automaticamente quando seu último circuito ativo for desativado

**Descrição:**
Quando um circuito é desativado (`active = false`), o sistema deve verificar se o cliente associado ainda possui outros circuitos ativos. Caso não possua, o cliente deve ser desativado automaticamente (`enabled = false`).

**Estimativa:** 1 story point

**Critérios de Aceite:**

1. **Gatilho:** a verificação ocorre sempre que um circuito é desativado via `PUT /api/circuits/{id}` com `active = false`.
2. **Lógica:** após desativar o circuito, o serviço verifica se o cliente vinculado ainda possui ao menos um circuito com `active = true`. Se não houver, o cliente é desativado automaticamente.
3. **Sem efeito colateral:** reativar um circuito (`active = true`) não reativa automaticamente o cliente — a reativação do cliente é sempre manual.
4. **Clientes sem circuito:** circuitos sem cliente vinculado (`customerId = null`) não disparam nenhuma verificação.
5. **Testes:** testes unitários cobrem: desativação do cliente ao desativar o último circuito ativo; não desativação quando ainda existem outros circuitos ativos; não reativação ao reativar um circuito.
