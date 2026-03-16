# User Stories — AsteraComm

## Indice

1. [US-011 — Fatura mensal por circuito (Invoice)](#us-011)
2. [US-012 — Refatoração: reorganização de pacotes em `domain/`](#us-012)
3. [US-013 — Refatoração: múltiplos DIDs por circuito e seleção de CallerID](#us-013)
4. [US-014 — Dashboard inicial com visão geral do sistema](#us-014)
5. [US-015 — Relatórios: custo de ligações por circuito no período](#us-015)
6. [US-017 — Snapshot de estado do circuito, DID e plano no processamento da ligação](#us-017)

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

## US-014

**Titulo:** Dashboard inicial com visão geral do sistema

**Descrição:**
Como administrador, quero uma tela de dashboard que exiba um resumo operacional e financeiro do sistema, com painéis de circuitos, ligações, faturamento e troncos, para ter visibilidade rápida do estado da plataforma ao acessar o sistema.

**Estimativa:** 5 story points

**Critérios de Aceite:**

1. **Painel de Circuitos:**
   - Total de circuitos cadastrados.
   - Circuitos registrados (online) vs. offline, com indicador visual de status.

2. **Painel de Troncos:**
   - Total de troncos cadastrados.
   - Troncos com registro ativo vs. sem registro, com indicador visual.

3. **Painel de Ligações:**
   - Total de ligações do mês corrente.
   - Total de minutos consumidos no mês.
   - Breakdown por status: atendidas, não atendidas, ocupado.

4. **Painel de Faturamento:**
   - Valor total faturado no mês corrente (soma das faturas `OPEN` em andamento).
   - Valor total de faturas `CLOSED` no mês anterior.

5. **API backend:** Endpoint `GET /api/v1/dashboard` retorna todos os dados acima em um único payload, minimizando roundtrips.

6. **Atualização:** Os dados do dashboard são carregados ao acessar a página. Não é necessário polling automático nesta versão.

7. **Testes:** Testes unitários cobrem o serviço de agregação do dashboard (contagens e totalizações).

---

## US-015

**Titulo:** Relatórios: custo de ligações por circuito no período

**Descrição:**
Como administrador, quero acessar um menu de relatórios e gerar um relatório de custo de ligações por circuito, informando um período (data início e data fim), para visualizar o quanto cada circuito gerou de custo com ligações no intervalo selecionado.

**Estimativa:** 3 story points

**Critérios de Aceite:**

1. **Menu de Relatórios:** Nova entrada no menu lateral "Relatórios", contendo a listagem de relatórios disponíveis. Inicialmente exibe apenas o relatório de custo por circuito.
2. **Filtro de período:** O usuário informa data de início e data de fim. Ambos os campos são obrigatórios.
3. **Resultado por circuito:** O relatório exibe uma linha por circuito com:
   - Nome do cliente vinculado ao circuito.
   - Nome do circuito.
   - Quantidade de ligações no período.
   - Total de minutos consumidos.
   - Custo total (R$) gerado pelas ligações (excluindo valor de plano/franquia — apenas custo excedente de ligações).
4. **Apenas ligações:** O relatório considera exclusivamente registros da entidade `Call`; faturas, planos e outros valores não entram no cálculo.
5. **Circuitos sem ligações:** Circuitos sem nenhuma ligação no período não aparecem no resultado.
6. **API backend:** Endpoint `GET /api/v1/reports/call-cost?from=YYYY-MM-DD&to=YYYY-MM-DD` retorna os dados agregados por circuito.
7. **Testes:** Testes unitários cobrem a query de agregação e os casos: período sem ligações, múltiplos circuitos com custos distintos.

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

