# User Stories — AsteraComm

## Indice

1. [US-013 — Refatoração: múltiplos DIDs por circuito e seleção de CallerID](#us-013)
2. [US-017 — Snapshot de estado do circuito, DID e plano no processamento da ligação](#us-017)
3. [US-011 — Fatura mensal por circuito (Invoice)](#us-011)
4. [US-012 — Refatoração: reorganização de pacotes em `domain/`](#us-012)
5. [US-037 — Adicionar campo `linked_at` ao DID](#us-037)
6. [US-038 — Slide panel para edição de registros (Circuito e Cliente)](#us-038)

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

## US-038

**Titulo:** Slide panel para edição de registros (Circuito e Cliente)

**Descrição:**
Como administrador, quero que ao clicar em um registro na listagem (Circuitos, Clientes) um painel lateral deslizante se abra pela direita, mantendo a listagem visível ao fundo, em vez de navegar para uma página dedicada. O panel permite editar, salvar e deletar o registro sem sair da listagem, e oferece um botão "Expandir" para abrir a página inteira quando necessário.

**Estimativa:** 8 story points

**Critérios de Aceite:**

1. **Overlay:** Camada semi-transparente (`rgba(0,0,0,0.12)`) cobre a viewport ao abrir o panel; clicar nela fecha o panel. Transição de `0.3s ease` sincronizada com o panel.
2. **Slide panel — container:** Fixo à direita (`position: fixed; top: 0; right: 0; bottom: 0`), largura 520px, fundo branco, borda esquerda fina. Animação: `transform: translateX(100%)` → `translateX(0)` com `cubic-bezier(0.4, 0, 0.2, 1) 0.3s`. Z-index overlay: 5; panel: 10.
3. **Header (sticky):** Subtítulo do tipo da entidade + título com código do registro. Dois botões ícone à direita: **Expandir** (navega para a página inteira `/circuits/{id}`) e **Fechar** (fecha o panel). Abaixo, linha de tabs horizontais.
4. **Tabs — Circuito:** "Detalhes", "DIDs", "Histórico". Tabs — **Cliente:** "Detalhes", "Circuitos", "Histórico". Tab ativa: `border-bottom: 2px solid` + texto primário + font-weight 500. Tab inativa: borda transparente + cor secundária.
5. **Tab Detalhes (Circuito):** Seções separadas por dividers (sem cards com borda). Seção **Identificação**: grid 2 colunas (ID disabled, Código disabled) + toggle Ativo/Inativo. Seção **Autenticação**: input password + toggle visibilidade. Seção **Configuração**: grid 2 colunas (Tronco, Plano) + Cliente em largura parcial — todos com SearchSelect.
6. **Tab Detalhes (Cliente):** Seção **Identificação**: grid 2 colunas (ID disabled, Nome editável) + toggle Ativo/Inativo.
7. **Tab DIDs / Circuitos:** Botão "Adicionar" alinhado à direita. Grid com colunas adaptadas para 520px. DIDs: `50px 1fr 100px 36px` (ID, Número, Vinculado em, X). Circuitos: colunas essenciais (ID, Status, Código, Plano, Online, link externo) — omitir IP e RTT. Estado vazio com mensagem centralizada.
8. **Tab Histórico:** Timeline vertical com bolinha colorida, texto do evento (ex: "Plano alterado: Básico → Real") e metadata (data/hora — usuário). Dado alimentado pelo backend; exibir "Nenhum registro." se vazio.
9. **Footer (sticky):** Lado esquerdo: botão "Deletar" com confirmação em 2 cliques (primeiro muda texto/estilo, segundo executa). Lado direito: botões "Cancelar" (fecha sem salvar) e "Salvar" (AJAX, sem reload).
10. **Comportamentos:** Fechar via botão X, Cancelar, overlay ou `Escape`. Após salvar com sucesso: panel permanece aberto, linha na listagem atualizada in-place, toast de sucesso exibido. URL atualizada via `history.pushState` (ex: `/circuits?panel=4933311611`) para deep linking.
11. **Dimensões no panel:** Font-size de inputs 13px, padding `7px 10px`, ícones 13px, botões do footer 12px — levemente reduzidos em relação à página inteira para caber em 520px.
12. **Página inteira preservada:** `circuits/[id].astro` e `customers/[id].astro` permanecem funcionais como fallback acessível pelo botão "Expandir" e por URLs diretas.
13. **Testes:** Abertura/fechamento do panel, troca de tabs, salvamento via AJAX com atualização in-place da listagem, deletar com 2 cliques, fechar com Escape e overlay.

---
