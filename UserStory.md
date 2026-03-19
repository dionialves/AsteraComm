# User Stories — AsteraComm

## Indice

1. [US-013 — Refatoração: múltiplos DIDs por circuito e seleção de CallerID](#us-013)
2. [US-017 — Snapshot de estado do circuito, DID e plano no processamento da ligação](#us-017)
3. [US-011 — Fatura mensal por circuito (Invoice)](#us-011)
4. [US-012 — Refatoração: reorganização de pacotes em `domain/`](#us-012)
5. [US-037 — Adicionar campo `linked_at` ao DID](#us-037)
6. [US-040 — Refatoração: extrair scripts de modal para arquivos `.ts` importáveis nas páginas Astro](#us-040)
7. [US-041 — Reestruturação da página de listagem de Circuitos](#us-041)

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

## US-041

**Titulo:** Reestruturação da página de listagem de Circuitos

**Descrição:**
Como administrador, quero que a página de listagem de Circuitos seja redesenhada com cards de resumo de métricas, filtros rápidos por segmento e indicadores visuais melhorados na tabela, mantendo a integração com o sistema de modais empilhados para edição.

**Estimativa:** 5 story points

**Critérios de Aceite:**

1. **Cards de resumo (4):** Grid 4 colunas com cards de fundo `#f5f5f5`, padding 14px 16px, border-radius 8px — exibindo Total de circuitos (texto primário), Ativos (verde `#085041`), Online agora (verde `#085041`) e Inativos (vermelho `#791F1F`). Label 12px / valor 22px font-weight 500. Dados vindos de `GET /api/circuits/summary` → `{ total, active, online, inactive }`.
2. **Barra de ferramentas:** Campo de busca com ícone de lupa integrado (debounce 300ms, max-width 320px) + button group de 4 filtros (Todos / Online / Offline / Inativos) + paginação com chevrons SVG substituindo os textos "Anterior/Próxima".
3. **Button group de filtros:** Botões sem gap, bordas unificadas. Ativo: fundo `#ffffff`, texto `#1a1a1a`, font-weight 500. Inativo: fundo transparente, texto `#888`. Filtros mapeados para query params: Todos (sem filtro), Online (`online=true`), Offline (`online=false&status=ACTIVE`), Inativos (`status=INACTIVE`).
4. **Tabela em grid CSS:** Container com `border-radius 12px`, `border: 0.5px solid #e0e0e0`, `overflow: hidden`. Header com fundo `#f5f5f5`, font-size 11px. Linhas com `grid-template-columns: 48px 68px minmax(0,1fr) minmax(0,1fr) minmax(0,1fr) minmax(0,1fr) 64px 110px 70px`, gap 6px, padding 11px 16px.
5. **Coluna Status:** bolinha 7px (`#1D9E75` ativo / `#E24B4A` inativo) + texto (`#085041` / `#791F1F`).
6. **Coluna Online:** badge pill (`border-radius: 99px`) — "OK" com fundo `#E1F5EE` / texto `#085041`; "Off" com fundo `#f5f5f5` / texto `#888`.
7. **Linha selecionada:** ao abrir modal, linha recebe fundo `#E6F1FB` e `border-left: 2px solid #378ADD`; ao fechar modal, volta ao normal.
8. **Linha inativa:** `opacity: 0.55` (hover: `0.75`).
9. **Campos monospace:** Código, IP e RTT renderizados em `font-family: monospace`.
10. **Botão "Novo circuito":** fundo `#1D9E75`, ícone `+` SVG + texto, abre modal em modo criação (campos vazios).
11. **Endpoint de resumo:** `GET /api/circuits/summary` no backend retornando `{ total, active, online, inactive }` calculados a partir da base.

