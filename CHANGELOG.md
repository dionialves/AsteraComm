# Changelog — AsteraComm

## Em desenvolvimento

---

### FIX-005 — Refatorar fetches com limite hardcoded no frontend

Solução:

- Criado no backend endpoints que retornam todos os objetos de uma classe(Customer. Circuit, Trunk, Plan) sem paginação: `GET /api/[resource]/summary` (ex: `GET /api/customers/summary`).
- Nos endpoints retornam apenas objetos ativos e no no caso de DIDs os numeros livres.
- No Frontend, os fetches para popular os selects de Cliente, Tronco e Plano no modal de Circuitos foram alterados para usar os novos endpoints summary, eliminando os limite de 200 e 900 itens e a necessidade de paginação.

---

### FIX-003 — Botões do modal desalinhados no modo de criação

**Solução:**

- Frontend: `.modal-footer` alterado para `justify-content: flex-end`; `.btn-modal-delete` recebe `margin-right: auto` para empurrar os botões à direita quando visível e sumir do layout quando `hidden`.
- Frontend: `plans/index.astro` — botão Excluir trocado de `invisible` para `hidden` (HTML e JS) para ser removido do fluxo de layout.

---

### FIX-002 — Modais fora do tamanho correto nas páginas de Ligações e Planos

**Solução:**

- Frontend: modal de Planos convertido para `modal-main modal-plan`; `.modal-plan` sobrescreve para `height: auto; max-height: 85vh` mantendo largura 780px.
- Frontend: modal de Ligações convertido de CSS customizado (`width: 540px`) para `modal-main modal-cdr-detail`; overlay trocado para `modal-overlay` (padrão global); header/body/footer padronizados com as classes do sistema; footer adicionado com botão "Cancelar".

---

### FIX-001 — Ativação/desativação de usuário via edição

**Solução:**

- Backend: `UserService.update()` corrigido para aplicar os campos `name` e `enabled` do `UserUpdateDTO` antes de salvar (antes salvava sem alterar nada).
- Backend: endpoints `PATCH /{id}/disable` e `PATCH /{id}/enable` removidos do `UserController`.
- Backend: métodos `disable()` e `enable()` removidos do `UserService`.
- A ativação/desativação agora ocorre exclusivamente via `PUT /api/users/{id}`, alinhado com o que o frontend já envia.

---

### US-052 — Migrar frontend para 100% Tailwind CSS

**Solução:**

- Frontend: eliminados todos os atributos `style=""` estáticos de 14 arquivos Astro
  (`Layout.astro`, `index.astro`, `login/index.astro`, `cdrs/index.astro`,
  `circuits/index.astro`, `circuits/[id].astro`, `customers/index.astro`,
  `dids/index.astro`, `plans/index.astro`, `reports/audit.astro`,
  `reports/cost-per-circuit.astro`, `trunks/index.astro`, `users/index.astro`).
  Conversões JS (`style.display`, `style.opacity`, `style.cursor`,
  `style.visibility`) substituídas por `classList.toggle/add/remove`.
  Bloco `<style>` do `Layout.astro` (~95 linhas) removido integralmente.
  11 classes globais ad-hoc removidas do `global.css`. `@keyframes dropUp`
  movido para `global.css` e referenciado via `animate-[dropUp_0.15s_ease]`.
  Regra de CSS obrigatória documentada no `CLAUDE.md`.

---

### US-062 — Refatoração: organizar pacote `report` com sub-pacotes por relatório

**Solução:**

- Backend: arquivos de `report/` movidos para `report/costpercircuit/` com renomeação de `CallReportController` → `CostPerCircuitController`, `CallReportService` → `CostPerCircuitService` e `CallReportRepository` → `CostPerCircuitRepository`. Arquivos de Auditoria (`AuditController`, `AuditService`, `AuditResultDTO`, `AuditCallLineDTO`, `AuditSummaryDTO`) movidos de `call/` para `report/audit/`. `CallCostingService.calculateFractionCost` alterado para `public` (necessário pelo novo pacote de auditoria). Testes movidos e renomeados correspondentemente. Endpoints HTTP preservados.

---

### US-061 — Simplificação de roles: manter apenas ADMIN

**Solução:**

- Backend: `UserRole` reduzido a apenas `ADMIN`. `User.getAuthorities()` simplificado para retornar sempre `ROLE_ADMIN`. `SecurityConfigurations` remove restrição `hasRole("SUPER_ADMIN")`. `SuperUserInitializer` e `DevDataSeeder` atualizados para `UserRole.ADMIN`. `UserCreateDTO` e `UserUpdateDTO` removem campo `role`; `UserService` hardcoda `UserRole.ADMIN`. Migração `V8__simplify_roles.sql` atualiza registros existentes. Testes atualizados.
- Frontend: `users/index.astro` — seletor "Nível de Acesso" removido do modal; coluna "Role" e função `buildRoleBadge` removidas da tabela; campo `role` removido dos payloads de criação e atualização.

---

### US-060 — Excluir usuário pelo modal de edição

**Solução:**

- Frontend: `users/index.astro` — botão "Excluir" no rodapé do modal (apenas em modo edição) com padrão de dois cliques: 1º clique muda texto para "Confirmar exclusão" com timer de 3s para reverter automaticamente; 2º clique executa `DELETE /api/users/{id}`, fecha o modal e exibe toast de sucesso. Removidos `#confirm-body`, `#btn-confirm-delete` e toda a lógica de troca de painel (`showConfirmBody`/`showFormBody`/`confirmMode`).

---

### US-057 — Adicionar campo `active` ao Plano com filtro na listagem

**Solução:**

- Backend: migração `V7__add_active_to_plans.sql` adiciona coluna `active BOOLEAN NOT NULL DEFAULT TRUE` na tabela `asteracomm_plans`. Entidade `Plan` recebe campo `boolean active = true`. `PlanUpdateDTO` recebe campo `Boolean active`. `PlanRepository` ganha `findByActive()` e `findByActiveAndNameContainingIgnoreCase()`. `PlanService.getAll()` passa a aceitar `Boolean active` como parâmetro de filtro (null = todos, true = ativos, false = inativos). `PlanController` expõe o parâmetro `?active` no endpoint `GET /api/plans`.
- Frontend: `plans/index.astro` — coluna "Status" adicionada após "Nome" (bolinha colorida + texto, padrão de Circuitos); filtro Todos/Ativos/Inativos na toolbar (padrão `bg-white`); paginação sempre visível; modal reestruturado com tamanho padrão (`780×680`), linha 1 ID + Nome, linha 2 toggle Ativo/Inativo (disponível também na criação). `plans.ts` repassa parâmetro `active` ao backend.

---

### US-055 — Excluir DID livre pela página de listagem de DIDs

**Solução:**

- Backend: adicionada `ConflictException` (409) e handler no `GlobalExceptionHandler`. `DIDService.delete()` passa a lançar `ConflictException` em vez de `BusinessException` quando o DID está vinculado a um circuito.
- Frontend: `dids/index.astro` — modal reestruturado com painéis `#panel-create` e `#panel-view`. Clique na linha abre visualização com Número (monospace), Status (badge pill) e Circuito vinculado. Botão "Excluir" visível apenas para DIDs livres, com padrão de dois cliques (1º clique → "Confirmar exclusão", 2º clique → executa). Após exclusão: fade da linha, contador atualizado e toast de sucesso.

---

### US-059 — Adicionar coluna ID na listagem de Troncos

**Solução:**

- Backend: migração `V6__add_id_to_trunks.sql` adiciona campo `id BIGSERIAL` como nova PK da tabela `asteracomm_trunks`, com `name` tornando-se `UNIQUE`. Entidade `Trunk` atualizada com `@GeneratedValue(IDENTITY)`. `TrunkProjection` expõe `getId()`. `TrunkRepository` migra para `JpaRepository<Trunk, Long>` com novos métodos `findByName` e `existsByName`. `TrunkService` e `DevDataSeeder` atualizados.
- Frontend: `trunks/index.astro` — coluna `ID` adicionada como primeira coluna (`40px`, `font-mono text-[#888]`). Campo ID adicionado ao modal (desativado em criação e edição, populado com o valor real na edição).

---

### US-058 — Ajustar cor do texto dos IDs nas páginas de Clientes e DIDs

**Solução:**

- Frontend: `customers/index.astro` e `dids/index.astro` — células de ID alteradas de `text-[#1a1a1a]` para `text-[#888]`, alinhando ao padrão visual da página de Circuitos (`font-mono text-[#888]`).

---

### US-056 — Contador de registros no rodapé das listagens de Circuitos, Clientes e DIDs

**Solução:**

- Frontend: `circuits/index.astro`, `customers/index.astro`, `dids/index.astro` — adicionado `<p id="counter">` abaixo da tabela (alinhado à direita, `text-[12px] text-[#888] mt-3`).
- Cada página rastreia `totalElements` a partir da resposta paginada e exibe singular/plural formatado em pt-BR (`toLocaleString('pt-BR')`).
- Contador atualiza a cada fetch (carga inicial, filtros, busca e navegação de página).

---

### US-053 — Botão "Novo circuito" abre modal de criação

**Solução:**

- Frontend: `circuits/index.astro` — botão "Novo circuito" abre o modal existente em modo criação (campos vazios) via `openCircuitModal(null, null)`, sem navegação de página.
- Abas "DIDs" e "Histórico" ocultadas no modo criação (irrelevantes antes do circuito existir); restauradas ao abrir em modo edição.
- Após `POST` bem-sucedido, o modal é reaberto automaticamente em modo edição com os dados do circuito recém-criado (em vez de fechar a tela).
- Labels dos campos obrigatórios (Senha, Tronco, Plano, Cliente) recebem `*` vermelho.
- Validação client-side antes do envio: lista os campos obrigatórios não preenchidos via `showModalError`.

---

### US-064 — Modais de perfil e senha no padrão do sistema

**Solução:**

- Frontend: `Layout.astro` — modais "Dados cadastrais" e "Alterar senha" migrados para classes canônicas do `global.css` (`.modal-overlay`, `.modal-main`, `.modal-header`, `.modal-body`, `.modal-footer`, `.form-group`, `.form-input`, `.input-password-wrap`, `.btn-eye`, `.modal-error`).
- CSS customizado (`.layout-modal`, `.lm-*`) removido do `Layout.astro`.
- Z-index dos modais do layout sobrescrito para 200/210 (acima do sidebar).
- Form de dados cadastrais reorganizado em grid 2 colunas.

---

### US-050 — Reestruturação do sidebar de navegação

**Solução:**

- Frontend: `Layout.astro` reescrito com sidebar fixo 220px (`#1a1a1a`), logo centralizada 24px (`#5DCAA5` + branco).
- Seções OPERACIONAL (Cadastro colapsável: Circuitos, Clientes, DIDs, Planos; Configuração colapsável: Troncos) e FINANCEIRO (Ligações, Relatórios).
- Seção ADMINISTRAÇÃO (Usuários) renderizada via SSR apenas para `SUPER_ADMIN` e `ADMIN` (fetch server-side no frontmatter).
- Menus colapsáveis com chevron animado (rotate 180°) e `max-height` (0.25s ease). Auto-expansão e `.active` via JS baseado na URL atual.
- Rodapé: avatar 28px, nome, role, dropdown para cima (`#2a2a2a`) com Dados cadastrais, Alterar senha, Sair.
- Modais Dados cadastrais (580px) e Alterar senha (420px) com campos no padrão `global.css`.
- Proxy `api/auth/me/password.ts`: handler PUT adicionado com `{ currentPassword, newPassword }`.

---

### US-063 — Modal de detalhes do circuito a partir da página de Auditoria

**Solução:**

- Frontend: `reports/audit.astro` — botão de link do `SearchSelect` de circuito passa a abrir modal de detalhes do circuito diretamente na página, sem navegar para `/circuits?modal=...`.
- Adicionados overlay (`#circuit-modal-overlay`), CSS para modal de seleção de DID e modal DID (`csub-modal-did`).
- Modal com tabs Detalhes/DIDs/Histórico, `SearchSelect` de Tronco, Plano e Cliente, toggle de status, toggle de senha.
- Botões Salvar (`PUT /api/circuit/{number}`), Deletar (dois cliques, timer 3s), Cancelar/✕ e Escape.
- Tab DIDs: lista de DIDs vinculados, desvincular com confirmação, adicionar DID livre via modal de seleção.

---

### US-049 — Reestruturação da página de Auditoria

**Solução:**

- Backend: `AuditSimulationService` e `AuditController` com endpoint `GET /api/audit/simulate?circuitNumber&month&year`.
- Frontend: `reports/audit.astro` reescrito com header (botão voltar + título + subtítulo), card de filtros (SearchSelect de circuito, Mês, Ano, botão Processar), card de contexto (Circuito/Plano/Período), toggle "Apenas chamadas relevantes", botão "Baixar PDF", tabela CSS Grid 8 colunas somente leitura e 5 totalizadores semânticos.
- Geração de PDF client-side via jsPDF + autoTable.
- Proxy Astro `api/audit/simulate.ts` criado.

---

### US-048 — Reestruturação da página de listagem de Usuários

**Solução:**

- Frontend: `users/index.astro` reescrito com 100% Tailwind + classes canônicas do `global.css`.
- Header com título "Usuários" + subtítulo + botão "Novo usuário" (`#1D9E75`).
- Toolbar com busca (debounce 300ms, client-side por nome e username), filtros Todos/Ativos/Inativos e paginação condicional (oculta quando ≤ 20 registros).
- Tabela CSS Grid 7 colunas: ID (font-mono), Status (bolinha 7px + texto colorido), Nome, Username, Role (badge pill por nível), Criado em, Atualizado em. Sort client-side por coluna com seta indicadora.
- Linha inativa `opacity: 0.55`, linha selecionada `#E6F1FB` + `border-left: 2px solid #378ADD`.
- Rodapé contador singular/plural alinhado à direita.
- Modal no padrão canônico dos Circuitos: `.modal-overlay` animado, `.modal-main` (780px, `height: auto`), `.modal-header` com subtítulo "Usuário" + título dinâmico + botão fechar, `.modal-body` com campos `form-input`/`form-label`, `.modal-footer` com Excluir (edit only) à esquerda e Cancelar/Salvar à direita.
- Campos do modal: Nome, Username (disabled no edit), Senha (apenas criação), Nível de Acesso (select), toggle Ativo/Inativo (apenas edit).
- Exclusão com confirmação inline: form-body oculto, confirm-body exibido com "Confirmar exclusão" / "Voltar".
- Sem alteração no backend.

---

### US-047 — Reestruturação do sistema de Relatórios

**Solução:**

- Backend: dependência OpenPDF adicionada. Novos records `CostPerCircuitSummaryDTO` e `CostPerCircuitResponseDTO`. `CallReportService` com métodos `getCostPerCircuit()` e `generateCostPerCircuitPdf()` (geração server-side). `CallReportController` com endpoints `GET /api/reports/cost-per-circuit` e `GET /api/reports/cost-per-circuit/pdf`.
- Frontend: `ReportCard.astro` criado em `src/components/` como componente reutilizável para cards do índice. `reports/index.astro` redesenhado com header + grid 3 colunas. `reports/cost-per-circuit.astro` criado com header (botão voltar + título + subtítulo), card de filtros (Mês, Ano, checkbox), 4 totalizadores em grid, botão "Baixar PDF" e tabela somente leitura CSS Grid 5 colunas. Proxies Astro `api/reports/cost-per-circuit.ts` e `cost-per-circuit-pdf.ts` criados.
- Endpoints antigos (`/api/reports/call-cost`, `/reports/call-cost`) mantidos sem alteração.

---

### US-046 — Reestruturação da página de listagem de Planos

**Solução:**

- Frontend: `plans/index.astro` reescrito com 100% Tailwind — header com botão "Novo plano", toolbar com busca (debounce 300ms) à esquerda e paginação condicional (visível apenas quando totalPages > 1) à direita.
- Tabela CSS Grid 8 colunas: ID · Nome · Mensalidade · Fixo Local · Fixo LD · Móvel Local · Móvel LD · Pacote.
- Colunas de tarifa em `font-mono`, 4 casas decimais; Mensalidade em `font-mono font-medium` com prefixo `R$`.
- Coluna Pacote: mini badges pill FL / FI / ML / MLD — ativo (`#E6F1FB` / `#0C447C`) ou inativo (`#f5f5f5` / `#888`) conforme franquia > 0.
- Ordenação por coluna: chevron SVG exibido apenas na coluna ativa (padrão Circuitos).
- Modal no padrão do sistema: overlay + scale/opacity, subtítulo "Plano", footer com Excluir (dois cliques para confirmar) à esquerda e Cancelar + Salvar à direita.
- Linha selecionada: `.row-selected` com `#E6F1FB` + `border-left: 2px solid #378ADD`.
- Contador de rodapé à direita: "1 plano cadastrado" / "N planos cadastrados".
- Sem alteração no backend.

---

### US-045 — Reestruturação da página de listagem de Ligações (CDR)

**Solução:**

- Frontend: `cdrs/index.astro` reescrito com 100% Tailwind — header sem botão de ação, card de filtros com labels acima dos campos, 6 campos (Origem, Destino, Circuito, Status, Data início, Data fim) em duas linhas, botões "Filtrar" e "Limpar".
- Filtro Status como button group (padrão dos Circuitos): Todos / Atendida / Sem resp. / Ocupado / Falha — aplicado apenas ao clicar "Filtrar".
- Paginação com chevrons SVG dentro do card de filtros.
- Tabela CSS Grid 10 colunas, `cursor:pointer`, hover `#fafafa`, `.row-selected` com `#E6F1FB` + `border-left: 2px solid #378ADD`.
- Header da tabela com colunas ordenáveis (`sort-col`, seta SVG indicando campo e direção ativas).
- Badges: Tipo (4 cores), Status (3 cores), Custeio (badge p/ PROCESSED/PENDING/ERROR, texto simples p/ sem circuito/plano).
- Duração: `font-weight:500` se > 0, `#888` se = 0. Formato `Xs`, `Xm Xs`, `Xh Xm Xs`.
- Modal de detalhe (somente leitura) no padrão do sistema: overlay + scale/opacity, subtítulo "Ligação" + título `#ID`, grid 2 colunas com badges.
- Contador de rodapé à direita: "{n} ligações encontradas".

---

### US-044 — Reestruturação da página de listagem de Troncos

**Solução:**

- Frontend: `trunks/index.astro` reescrito com 100% Tailwind — header com botão "Novo tronco" (`#1D9E75`), busca com debounce 300ms e ícone integrado, paginação condicional com chevrons SVG (oculta quando `totalPages <= 1`).
- Tabela em CSS Grid 4 colunas (Nome, Host, Usuário, Registro); badge pill "Registrado" (`#E1F5EE`/`#085041`) e "Não registrado" (`#FCEBEB`/`#791F1F`); linha selecionada `#E6F1FB` + `border-left: 2px solid #378ADD`.
- Contador de rodapé: "{n} troncos cadastrados" (singular/plural), alinhado à direita.

---

### US-042 — Reestruturação da página de listagem de Clientes

**Solução:**

- Backend: `CustomerResponseDTO` criado com campo `circuitCount` (count de circuitos vinculados por cliente via `CircuitRepository.countByCustomerId()`).
- Backend: `CustomerRepository` recebeu `findByEnabled()` e `findByEnabledAndNameContainingIgnoreCase()` para filtro por status.
- Backend: `CustomerService.getAll()` atualizado para receber `Boolean enabled` e retornar `Page<CustomerResponseDTO>`.
- Backend: `CustomerController.findAll()` aceita `?status=ACTIVE|INACTIVE`, mapeia para `Boolean` e delega ao serviço.
- Frontend: `customers.ts` atualizado para repassar `status` ao backend.
- Frontend: `customers/index.astro` reescrito com 100% Tailwind — header, busca com debounce 300ms e ícone integrado, button group Todos/Ativos/Inativos, paginação com chevrons SVG.
- Tabela em CSS Grid 6 colunas (ID, Nome, Status com bolinha colorida, Circuitos com count, Criado em, Atualizado em); header clicável com setas de sort; linha selecionada `#E6F1FB` + `border-left: 2px solid #378ADD`; linha inativa `opacity: 0.55`.
- Botão "Novo cliente" abre modal em modo criação (POST); `atualizarLinhaListagem` e `removerLinhaDaListagem` para atualização incremental da UI.

---

### US-043 — Reestruturação da página de listagem de DIDs

**Solução:**

- Backend: `DIDCircuitDTO` e `DIDResponseDTO` criados como records (campos: `id`, `number`, `status`, `circuit`).
- Backend: `DIDRepository` recebeu 4 novos métodos paginados: `findByCircuitIsNull`, `findByCircuitIsNotNull`, e variantes com `NumberContaining` para busca.
- Backend: `DIDService.getAll()` atualizado para retornar `Page<DIDResponseDTO>` com status computado (`IN_USE`/`FREE`) e `DIDCircuitDTO` aninhado.
- Backend: `DIDController.findAll()` aceita `?status=IN_USE|FREE` e usa `@PageableDefault(size=20)`.
- Frontend: `dids.ts` atualizado para repassar `status` ao backend.
- Frontend: `dids/index.astro` reescrito com 100% Tailwind — header, busca com debounce 300ms e ícone integrado, button group Todos/Em uso/Livres, paginação com chevrons SVG.
- Tabela em CSS Grid 4 colunas (ID, Número, Status badge pill, Circuito vinculado); header clicável com setas de sort; linha selecionada `#E6F1FB` + `border-left: 2px solid #378ADD`.
- Badge pills diferenciados: Em uso (`#E1F5EE`/`#085041`), Livre (`#E6F1FB`/`#0C447C`).
- Botão "Novo DID" abre modal em modo criação (POST) com máscara `(XX) XXXX-XXXX`; exclusão com confirmação em dois cliques; `removerLinhaDaListagem` para atualização incremental.

---

### US-041 — Reestruturação da página de listagem de Circuitos

**Solução:**

- Backend: novo endpoint `GET /api/circuits/summary` → `{ total, active, online, inactive }`.
- Backend: `findAllCircuits` atualizado com filtros nullable `online` e `active` via `CAST(:param AS boolean) IS NULL`; `ORDER BY` hardcoded removido, sort delegado ao `Pageable`.
- Backend: `CircuitSummaryDTO` criado como record; `CircuitService.getAll()` e `CircuitController.findAll()` atualizados com parâmetros `online` e `status`.
- Frontend: `circuits/index.astro` reescrito com 100% Tailwind — 4 cards de resumo, busca com debounce 300ms e ícone integrado, button group Todos/Online/Offline/Inativos à esquerda, paginação com chevrons SVG.
- Tabela em CSS Grid; header clicável com setas de sort; linhas com bg branco, status com bolinha colorida, Online com badge pill, linha selecionada highlight azul (`#E6F1FB` + `border-left: 2px solid #378ADD`), linha inativa `opacity: 0.55`.
- Botão "Novo circuito" abre modal em modo criação com campos vazios.
- Proxy `summary.ts` criado; `circuits.ts` atualizado para repassar params `online` e `status`.

---

### US-051 — Reestruturação da tela de Login

**Solução:**

- `login/index.astro` reescrito: fundo `#f5f5f5`, card branco 480px centralizado, `border: 0.5px solid #e0e0e0`, `border-radius 12px`.
- Logo "Astera" (`#1D9E75`) + "Comm" (preto), font-size 36px, sem imagem PNG.
- Campos Usuário e Senha com labels 12px e inputs padding 10px 12px, font-size 14px, borda `#d0d0d0`.
- Toggle de visibilidade da senha com ícones olho/olho-riscado SVG.
- Botão "Entrar" verde `#1D9E75`, largura 100%, com estado disabled e texto "Entrando..." durante o fetch.
- Área de erro inline (`#FCEBEB`/`#791F1F`) para 401 e erro de rede, sem `alert()`.
- Submissão por tecla Enter.
- Redirecionamento para `/dashboard` em caso de sucesso.

---

### US-039 — Sistema de modais empilhados para edição de registros (Circuito e Cliente)

**Solução:**

- `ModalSystem` class (inline) com dois níveis de overlay/modal, animações CSS, URL sync via `history.pushState` e suporte a Escape.
- `SearchSelect` substituindo `ChipSelect` nos chips do modal — modo chip visual (`.cs-chip`) ao selecionar, modo busca ao limpar; visibilidade dos botões gerenciada via `style.display` para evitar conflito de classes.
- **Página de Circuitos:** clique na linha abre modal principal com abas Detalhes / DIDs / Histórico. Detalhes: campos editáveis (senha com toggle olho, tronco/plano/cliente com SearchSelect). DIDs: lista com desvincular (confirm) + botão "+ Adicionar" que filtra apenas DIDs livres via `/api/did/free`. Chips com `setLinkClickHandler` abrindo sub-modal para edição de Tronco, Plano e Cliente.
- **Sub-modal Cliente** (dentro de Circuitos): abas Detalhes / Circuitos / Histórico; footer Salvar/Deletar ocultos nas abas não-Detalhes.
- **Página de Clientes:** clique na linha abre modal principal com abas Detalhes / Circuitos / Histórico. Aba Circuitos: botão ícone external-link por linha abre sub-modal completo do circuito (todas as abas, edição completa, modal DID para vinculação).
- CSS em `global.css`: sistema completo de classes `.modal-overlay`, `.modal-main`, `.modal-sub`, `.cs-chip-*`, `.did-grid-*`, `.history-*`, `.modal-footer`, `.btn-modal-*`, `.toggle-switch`, `.form-input`, `.section-title` etc.

---

### US-036 — Redesenhar layout da página de detalhe do circuito

**Solução:**

- Container principal: `max-w-[1100px] mx-auto`.
- Header: breadcrumb (`Circuitos › {código}`) + `h1` + botões Voltar/Salvar. Deletar removido do header.
- Card Identificação: grid 3 colunas — ID (disabled), Código (disabled), Status com toggle button group colorido (Ativo = verde, Inativo = vermelho).
- Card Autenticação: `input[type=password]` + font-mono + botão toggle olho/olho-cortado.
- Card Configuração: Tronco e Plano em grid 2 colunas; Cliente em `max-w-[400px]`, todos com SearchSelect.
- Card DIDs vinculados: grid CSS por linha (`60px 1fr 130px 44px`), colunas ID/Número/Vinculado em + botão X individual para desvincular (sem checkbox).
- Footer: botão "Deletar circuito" com confirmação em 2 cliques (4s de timeout), timestamp "Última modificação".

---

### US-035 — Travar seleção e exibir ações ao selecionar item no SearchSelect

**Solução:**

- Ao selecionar um item, o trigger do SearchSelect fica travado (não abre o dropdown).
- Ícone **X** aparece ao lado: limpa a seleção e reabre o dropdown automaticamente.
- Ícone **seta** aparece ao lado quando `onNavigate` está definido no config: navega para a URL retornada com `?from=[caminho atual]` para que o "Voltar" funcione.
- Quando `setDisabled(true)` está ativo, o ícone X fica oculto (campo bloqueado externamente).
- Campo Cliente em `circuits/new.astro` e `circuits/[id].astro` configurado com `onNavigate: (id) => \`customers/${id}\``.
- `customers/[id].astro`: botão "Voltar" passa a suportar o parâmetro `?from`.
- 17 novos testes cobrindo travamento, X e navegação.

---

### US-033 — Botão "Adicionar circuito" na página de detalhe do cliente

**Solução:**

- Botão "Adicionar" inserido no cabeçalho da tabela de circuitos em `customers/[id].astro`; navega para `circuits/new?customerId=${customerId}`.
- Em `circuits/new.astro`, se `customerId` estiver presente na URL: campo Cliente é pré-selecionado via `setValue()` e desabilitado via `setDisabled(true)`.
- Após salvar com sucesso, redireciona para `customers/${customerId}` em vez de `circuits/${created.number}`.
- Método `setDisabled(boolean)` adicionado ao componente `SearchSelect`.
- Comportamento padrão (sem `customerId`) preservado integralmente.

---

### US-032 — Navegação contextual: voltar para cliente ao acessar circuito via página de cliente

**Solução:**

- Em `customers/[id].astro`, o clique em uma linha de circuito passa `?from=customers/${customerId}` na URL de destino.
- Em `circuits/[id].astro`, o botão "Voltar" lê o parâmetro `from`: se presente, navega para `/${from}`; caso contrário, mantém o comportamento padrão (`/circuits`).

---

### US-034 — Ajustar colunas da tabela de DIDs vinculados no circuito

**Solução:**

- Tabela de DIDs em `circuits/[id].astro` passa a exibir apenas as colunas `ID` e `Número`, removendo a coluna `Status`.
- Coluna `ID` com largura fixa `w-16`; coluna `Número` sem largura fixa, ocupando o restante da tabela.
- Checkboxes de seleção e comportamento do botão Desvincular preservados.

---

### US-031 — Componente de seleção com pesquisa integrada (SearchSelect)

**Solução:**

- Componente `SearchSelect` implementado em `src/lib/search-select.ts` como classe TypeScript reutilizável.
- Aparência fechada simula `<select>` nativo com ícone de seta e label do item selecionado (ou placeholder em cinza).
- Ao clicar, abre dropdown com campo de pesquisa no topo e lista de até 10 opções filtradas em tempo real (case-insensitive, client-side).
- API pública: `getValue()`, `setValue(value)`, `setOptions(options)`.
- Clicar fora do componente fecha o dropdown sem alterar a seleção.
- Integrado em `circuits/new.astro` (tronco, cliente, plano) e `circuits/[id].astro` (tronco, cliente, plano e DID no modal de vínculo).
- Selects excluídos: `#selectPackageType` em `plans/index.astro` (3 opções fixas de controle).
- Payloads HTTP enviados ao backend permanecem idênticos aos anteriores.
- 19 testes unitários cobrindo: estado fechado, abertura, filtragem, seleção, fechamento, `setValue()` e `setOptions()`.

---

### US-030 — Página de detalhe do cliente

**Solução:**

- Clicar em uma linha da listagem de clientes navega para `customers/[id]`.
- Página de detalhe exibe: ID, Nome (editável), Ativo (radio Sim/Não, editável), Criado em, Atualizado em.
- Botão Salvar persiste Nome e Ativo via PUT sem recarregar a página.
- Tabela de circuitos vinculados com colunas: ID, Status, Código, Plano, Tronco, Online, IP, RTT — clicável para `circuits/[number]`.
- Novo cliente criado em `customers/new` (mesma estrutura da página de edição); após salvar redireciona para `customers/[id]`.
- Modal de criação/edição removido da listagem.

---

### US-029 — Padronizar largura de colunas nas listagens do sistema

**Solução:**

- Colunas sem largura fixa: usam `whitespace-nowrap`, layout determina o espaço conforme conteúdo.
- Alinhamento à esquerda; espaço excedente fica à direita da última coluna.
- Tabela usa `w-full` sem `table-fixed`.
- Coluna ID adicionada como primeira coluna com ordenação inicial decrescente em todas as páginas.
- Ordenação por coluna clicável com cursor `pointer` nos cabeçalhos.
- Escopo: circuitos, clientes, DIDs, planos, troncos, usuários, CDRs.
- Backend: ordenação padrão `id DESC` adicionada ao endpoint de planos.

---

### US-028 — Reorganizar colunas e padronizar status na listagem de clientes

**Solução:**

- Coluna ID adicionada como primeira coluna.
- Ordem das colunas: `ID | Nome | Status | Criado em | Atualizado em`.
- Coluna Status padronizada: `Ativo` (verde, `text-green-600`) / `Inativo` (vermelho, `text-red-600`) com `font-semibold`, removendo o badge anterior.
- Ordenação inicial alterada para ID decrescente (`sort=id,desc`).

---

### US-024 — Padronizar estilo dos botões de ação em todo o sistema

**Solução:**

- Verde (Adicionar, Salvar, Vincular): `bg-green-600 hover:bg-green-700` — corrigido em circuits, customers, dids, plans, trunks e users.
- Cinza (Cancelar, Voltar, Fechar, Anterior, Próximo): `bg-gray-200 hover:bg-gray-300` — corrigido nos modais de todas as páginas e no `.listing-page-btn` do CSS global (de `#f3f4f6` → `#e5e7eb`).
- Azul (Filtrar, Editar): `bg-blue-500 hover:bg-blue-600` — corrigido em cdrs e plans.
- Vermelho (Excluir, Deletar): já estava correto em todas as páginas.

---

### US-023 — Reordenar e ajustar colunas da listagem de circuitos

**Solução:**

- Colunas reordenadas para: `ID | Status | Código | Cliente | Plano | Tronco | Online | IP | RTT`.
- Coluna `Senha` removida da listagem.
- Coluna Status exibe `Ativo` (verde) / `Inativo` (vermelho) baseado no campo `active`.
- Coluna Online exibe apenas `OK` (verde) ou `Offline` (vermelho), sem RTT embutido.
- Colunas IP e RTT exibidas separadamente.
- Ordenação inicial alterada para ID decrescente (maior ID primeiro), tanto no backend (`@PageableDefault`) quanto no frontend (sort padrão `id,desc`).

---

### US-027 — Refatoração: DID referencia circuito por ID em vez de número

**Solução:**

- `DID` passa a ter `@ManyToOne Circuit` com `@JoinColumn(name = "circuit_id")`, expondo `circuitId` e `circuitNumber` via `@JsonProperty`.
- Migração Flyway V5 converte `circuit_number` (VARCHAR) para `circuit_id` (BIGINT FK), preservando vínculos existentes via JOIN por número.
- Endpoints ajustados: `GET /api/dids/free`, `GET /api/dids/by-circuit/{circuitNumber}` e `PUT /api/dids/{id}/link/{circuitNumber}`.
- `03-seed-real.sql` atualizado para resolver `circuit_id` via `JOIN asteracomm_circuits ON number`.
- Frontend: `carregarDIDs()` usa endpoint dedicado, eliminando filtro client-side com limite de 200 registros; payloads de vínculo atualizados.

---

### FIX-001 — Corrigir carregamento de DIDs livres no modal de vínculo do circuito

**Descrição:**
O modal de vínculo de DID no formulário do circuito não exibia os DIDs disponíveis porque o frontend carregava todos os DIDs e filtrava no cliente via `!d.circuitNumber`, o que falhava silenciosamente quando o parâmetro `sort` era codificado incorretamente pelo `URLSearchParams` (vírgula → `%2C`), gerando erro no Pageable do Spring.

**Solução:**

- Backend: novo endpoint `GET /api/dids/free` retorna lista simples de DIDs sem circuito vinculado.
- Frontend: nova rota Astro `src/pages/api/did/free.ts` proxia o endpoint.
- `openModal()` em `circuits/[id].astro` atualizado para usar `/api/did/free` diretamente, eliminando o filtro cliente.

---

### FIX — Auditoria: circuito não encontrado ao processar

O seletor de circuito na página de auditoria usava `c.id` (PK numérica) como valor do `<option>`, mas o backend busca pelo campo `number` (string). Corrigido para `c.number` em `audit/index.astro`.

---

### US-022 — Campo `active` no circuito e regra de exclusão com fallback para desativação

**Titulo:** Campo `active` no circuito e regra de exclusão com fallback para desativação

**Descrição:**
Como administrador, quero que o circuito possua um campo `active` indicando se está em operação, e que a exclusão só seja permitida quando não houver DIDs nem Calls vinculadas — caso contrário, o sistema deve desativar o circuito em vez de excluí-lo. No frontend, o estado ativo deve ser visível na listagem e editável no formulário.

**Estimativa:** 3 story points

**Critérios de Aceite:**

1. **Campo `active` na entidade `Circuit`:** Booleano, padrão `true` na criação. Persistido como `active BOOLEAN NOT NULL DEFAULT TRUE` via migração Flyway.
2. **Regra de exclusão:** `DELETE /api/v1/circuits/{number}` verifica:
   - Se houver DIDs vinculados → `BusinessException` ("Desvincule os DIDs antes de excluir o circuito").
   - Se houver Calls vinculadas → não é possível excluir; o serviço **desativa** o circuito (`active = false`) e retorna `HTTP 200` com o circuito atualizado (em vez de `204`).
   - Se não houver DIDs nem Calls → exclui normalmente (`HTTP 204`).
3. **Endpoint de atualização:** `PUT /api/v1/circuits/{number}` passa a aceitar o campo `active` no body, permitindo ativar/desativar manualmente.
4. **Frontend — listagem:** Coluna **Ativo** adicionada à tabela de circuitos exibindo `Sim` (verde) ou `Não` (vermelho).
5. **Frontend — formulário (criação e edição):** Campo **Ativo** abaixo do campo Código, com radio buttons `( ) Sim  ( ) Não`. Padrão `Sim` na criação.
6. **Testes:** Cobrem os três cenários de exclusão (tem DID, tem Call, nenhum vínculo), atualização do campo `active` e criação com `active = true` por padrão.

---

### US-021 — Refatoração UX: página de detalhe do circuito e ID no domínio

**Titulo:** Refatoração UX: página de detalhe do circuito

**Descrição:**
Como administrador, quero que ao clicar em um circuito na lista eu seja levado a uma página de detalhe dedicada, onde posso visualizar e editar todas as informações do circuito e gerenciar os DIDs vinculados, em vez de editar inline na lista.

**Estimativa:** 3 story points

**Critérios de Aceite:**

1. **Lista de circuitos:** A coluna de Ações é removida. Clicar em qualquer linha navega para a página de detalhe do circuito (`/circuits/{id}`). Coluna **ID** adicionada como primeira coluna.
2. **Página de detalhe — cabeçalho:** Segue o padrão visual do sistema: título (nome do circuito) e subtítulo (ex.: "Editar circuito") na mesma linha dos botões **Voltar**, **Salvar** e **Deletar**.
3. **Página de detalhe — campos:** Abaixo do cabeçalho, cada campo é exibido em uma linha própria com o nome do campo à esquerda e o input à direita, empilhados verticalmente. Campos ID e Código sem placeholder no formulário de criação.
4. **Tabela de DIDs vinculados:** Abaixo dos campos, exibe uma tabela com os DIDs associados ao circuito.
5. **Ações da tabela de DIDs:** Acima da tabela, alinhados à direita, os botões **Adicionar** e **Desvincular**. O botão **Desvincular** aparece em vermelho apagado (desabilitado) e fica ativo (vermelho normal) somente quando ao menos um DID está selecionado na tabela.
6. **Consistência:** A experiência de navegação (voltar para a lista, salvar, deletar) funciona corretamente sem quebrar funcionalidades existentes.
7. **ID no domínio Circuit:** Campo `id` (Long, PK sequencial) adicionado à entidade `Circuit`. Migração `V3__add_id_to_circuits.sql` cuida da transição em bancos existentes. `findById` substituído por `findByNumber` em `AuditService`, `CallProcessingService` e `DIDService`. `@JoinColumn` em `Call` corrigido com `referencedColumnName = "number"` para manter compatibilidade de schema. 322 testes, 0 falhas.

---

### US-014 — Dashboard inicial com visão geral do sistema

**Titulo:** Dashboard inicial com visão geral do sistema

**Descrição:**
Como administrador, quero uma tela de dashboard que exiba um resumo operacional e financeiro do sistema, com painéis de circuitos, ligações, faturamento e troncos, para ter visibilidade rápida do estado da plataforma ao acessar o sistema.

**Estimativa:** 5 story points

**Critérios de Aceite:**

1. **Cards de resumo (4 colunas):**
   - **Circuitos:** total, online (indicador verde) e offline (indicador vermelho).
   - **Troncos:** total, registrados (verde) e não registrados (vermelho).
   - **Ligações do mês:** total, atendidas (verde), não atendidas (âmbar) e ocupado (vermelho).
   - **Faturamento do mês:** valor total em destaque, breakdown em assinaturas + excedentes.

2. **Gráfico de faturamento mensal:** Barras empilhadas com os últimos 12 meses — azul para assinaturas, âmbar para excedentes. Valores no eixo Y em reais (BRL).

3. **Gráfico de ligações diárias:** Barras empilhadas dos últimos 30 dias — atendidas (verde), não atendidas (âmbar), ocupado (vermelho) e falhas (cinza).

4. **Tabela "Circuitos com consumo próximo do limite":** Exibe até 10 circuitos com pacote de minutagem, ordenados do maior para o menor consumo percentual no mês corrente, **excluindo** circuitos cujo pacote já foi excedido. Cada linha mostra: número do circuito, plano, minutos utilizados, limite do pacote e barra de progresso colorida (verde abaixo de 85 %, âmbar de 85–94 %, vermelho a partir de 95 %).

5. **Gráfico de excedência (donut):** Card ao lado da tabela acima exibindo um gráfico circular com a proporção entre circuitos com pacote excedido vs. circuitos dentro do limite (`CircuitOverageStats`).

6. **Gráfico "Top 10 circuitos por consumo":** Barras horizontais com os 10 circuitos de maior consumo de minutos no mês, exibindo minutos utilizados vs. limite do pacote. Barras de consumo ficam vermelhas quando excedem o limite.

7. **API backend:** Endpoint `GET /api/dashboard` retorna todos os dados em um único payload (`CircuitStats`, `TrunkStats`, `CallStats`, `BillingStats`, `DailyCallStat[]`, `MonthlyBillingStat[]`, `CircuitConsumption[]`, `TopCircuit[]`, `CircuitOverageStats`).

8. **Atualização:** Dados carregados ao acessar a página, sem polling automático.

9. **Testes:** 18 testes unitários cobrindo: estatísticas de circuitos, troncos, ligações, faturamento, intervalos de data, valores zero/vazios, filtro de excedidos na near-limit list, ordenação por percentual e contagem de overage.

---

### US-015 — Relatórios: custo de ligações por circuito no período

**Titulo:** Relatórios: custo de ligações por circuito no período

**Descrição:**
Como administrador, quero acessar um menu de relatórios e gerar um relatório de custo de ligações por circuito, informando mês e ano, para visualizar o quanto cada circuito gerou de custo com ligações no mês selecionado.

**Estimativa:** 3 story points

**Critérios de Aceite:**

1. **Menu de Relatórios:** Nova entrada no menu lateral "Relatórios", contendo a listagem de relatórios disponíveis. Inicialmente exibe apenas o relatório de custo por circuito.
2. **Filtro por mês/ano:** O usuário seleciona mês e ano. Ambos os campos são obrigatórios.
3. **Resultado por circuito:** O relatório exibe uma linha por circuito com:
   - Nome do cliente vinculado ao circuito.
   - Nome do circuito.
   - Quantidade de ligações no período.
   - Total de minutos consumidos.
   - Custo total (R$) gerado pelas ligações (excluindo valor de plano/franquia — apenas custo excedente de ligações).
4. **Apenas ligações:** O relatório considera exclusivamente registros da entidade `Call`; faturas, planos e outros valores não entram no cálculo.
5. **Circuitos sem ligações:** Circuitos sem nenhuma ligação no período não aparecem no resultado.
6. **API backend:** Endpoint `GET /api/v1/reports/call-cost?month=MM&year=YYYY&onlyWithCost=false` retorna os dados agregados por circuito.
7. **Layout da tela:** A página exibe, em ordem: título do relatório, descrição explicativa, seletores de mês e ano, checkbox "Apenas com custo", botão "Processar" e tabela de resultados com totalizador.
8. **Filtro "Apenas com custo":** Checkbox que, quando marcada, exclui do resultado circuitos com `totalCost = 0`. Filtro aplicado no backend via `onlyWithCost=true`.
9. **Download PDF:** Botão "Baixar PDF" gera um arquivo com header (logo, título, filtros aplicados, data de geração) e tabela com totais via `jsPDF` + `jspdf-autotable`. Download iniciado automaticamente sem diálogo de impressão.
10. **Testes:** 12 testes (7 service + 5 controller), 0 falhas. Cobrem: mês sem ligações, múltiplos circuitos com custos distintos, filtro `onlyWithCost` ativo vs. inativo, parâmetros obrigatórios.

---

### US-020 — Ajuste do consumo de pacote de minutagem para frações de 30 segundos

**Titulo:** Ajuste do consumo de pacote de minutagem para frações de 30 segundos

**Descrição:**
Como operador, quero que o consumo do pacote de minutagem use a mesma base de faturamento (frações de 30 segundos), para que cada minuto de pacote cubra exatamente um minuto de custo faturável, sem penalizar o cliente com consumo excessivo de pacote por arredondamento para o minuto inteiro.

**Estimativa:** 2 story points

**Critérios de Aceite:**

1. **Nova lógica de consumo:** O consumo do pacote passa a usar `ceil(billSeconds / 30.0)` frações de 30s, idêntico à base de cálculo de custo. Uma ligação de 61s consome 3 frações (1,5 min) do pacote — o mesmo valor que custaria se fosse cobrada.
2. **Campo `minutes_from_quota`:** Passa a armazenar o número de **frações de 30 segundos** consumidas do pacote (não minutos inteiros). O nome da coluna é mantido por retrocompatibilidade, mas o valor semântico muda: `2 = 1 minuto`.
3. **Comparação com o pacote:** O total disponível é convertido para frações na comparação: `packageTotalMinutes × 2`. As queries `sumQuotaMinutes` e `sumQuotaMinutesByType` continuam somando `minutes_from_quota` normalmente — o ajuste está na comparação.
4. **`CallCostingService`:** A lógica de `applyWithQuota` é atualizada: `durationMinutes` → `durationFractions = ceil(billSeconds / 30.0)`. O cálculo de excedente usa `billableSeconds = billSeconds - (remaining × 30)`.
5. **`AuditService`:** Atualizado da mesma forma, mantendo paridade com a produção.
6. **Registros históricos:** Ligações já processadas com a lógica antiga não são reprocessadas. A mudança vale apenas para novos processamentos.
7. **Testes:** Testes cobrem: ligação de 61s consome 3 frações (não 2 min), ligação parcialmente coberta com excedente correto, pacote esgotado no meio de uma ligação, verificação de que auditoria e produção produzem o mesmo resultado.

---

### US-016 — Ferramenta de auditoria de custo de ligações por circuito

**Titulo:** Ferramenta de auditoria de custo de ligações por circuito

**Descrição:**
Como desenvolvedor, quero uma ferramenta que, dado um circuito, mês e ano, simule o cálculo de custo de cada ligação do período de forma transparente e passo a passo, exibindo o raciocínio de cada cálculo (custo unitário, desconto por pacote de minutagem e consumo acumulado), para que eu possa auditar e validar se a lógica de billing está correta.

**Estimativa:** 3 story points

**Critérios de Aceite:**

1. **Filtro de seleção:** Seletor de circuito, mês e ano com botão "Processar".
2. **Processamento cronológico:** Ligações do período exibidas em ordem cronológica, uma por linha.
3. **Linha por ligação:** Data/hora, destino, tipo, duração, tarifa, minutos do pacote consumidos, acumulado do pacote e custo calculado.
4. **Contador de pacote:** Acumula progressivamente os minutos consumidos do pacote; congela ao esgotar.
5. **Custo zerado por pacote:** Ligações cobertas exibem R$ 0,00 com o acréscimo no contador.
6. **Filtro dinâmico:** Toggle "Exibir apenas chamadas relevantes" filtra no client sem nova requisição, mostrando contador de linhas visíveis.
7. **Resumo ao final:** Total de ligações, minutos, minutos de pacote usados, minutos excedentes e custo total.
8. **Acesso restrito:** Disponível apenas para `SUPER_ADMIN`.
9. **Bug fix — `CURRENT_DATE`:** Corrigido bug em `sumQuotaMinutesThisMonth` / `sumQuotaMinutesThisMonthByType` que usavam `CURRENT_DATE` nas queries, fazendo com que ligações processadas em mês diferente do mês da chamada calculassem a cota com saldo incorreto. Métodos renomeados para `sumQuotaMinutes` / `sumQuotaMinutesByType` e passam a receber `month` e `year` explícitos derivados de `call.getCallDate()`.
10. **Testes:** `AuditServiceTest` (11 casos) + `CallCostingServiceTest` atualizado (15 casos), 0 falhas.

---

### US-010 — Custeio de ligações por circuito com franquia e tarifação

**Titulo:** Custeio de ligações por circuito com franquia e tarifação

**Descrição:**
Como sistema, quero que cada `Call` criada a partir de um registro CDR seja associada ao circuito de origem, tenha seu tipo classificado e seu custo calculado, de forma que ligações originadas no servidor tenham seu valor apurado considerando a franquia de minutos do plano vinculado ao circuito.

**Estimativa:** 5 story points

**Critérios de Aceite:**

1. Escopo de custeio: apenas ligações de contextos de saída são custeadas; demais recebem `OUT_OF_SCOPE` e `cost = 0,00`.
2. Vínculo `Call → Circuit → Plan`; sem plano vinculado → `NO_PLAN` e `cost = 0,00`.
3. Franquia prioriza pacote exclusivo do `callType`; fallback para pacote geral; sem pacote → vai direto à tarifação.
4. Minutos acumulados por circuito no mês calendário corrente.
5. Custo em frações de 30 s (cada fração iniciada cobrada integralmente = 50% do valor/min).
6. Cenários: dentro da franquia (cost=0), esgotamento parcial, totalmente fora da franquia.
7. Idempotência: `Call` com `status != null` não é reprocessada.
8. Campos: `callStatus` (enum `PROCESSED/NO_CIRCUIT/NO_PLAN/OUT_OF_SCOPE`), `minutesFromQuota` (int), `cost` (decimal 2 casas).
9. Frontend: colunas "Custeio" e "Valor" na tabela e modal de ligações.

---

### US-019 — Migrações de schema com Flyway

**Titulo:** Migrações de schema com Flyway

**Descrição:**
Substituição do `ddl-auto=create-drop` por migrações versionadas com Flyway. O schema da aplicação (`asteracomm_*`) passa a ser gerenciado pelo Flyway via `V1__create_schema.sql`. As tabelas Asterisk (`cdr`, `cel`, `ps_*`, `extensions`) permanecem no `postgres/init/01-create-tables.sql`. Um novo arquivo `postgres/init/02-initial-flyway.sql` cria as tabelas `asteracomm_*` com `IF NOT EXISTS` para garantir que o seed real (`03-seed-real.sql`) funcione antes do backend subir.

**Estimativa:** 2 story points

**Critérios de Aceite:**

1. Flyway (`flyway-core` + `flyway-database-postgresql`) adicionado ao `pom.xml`.
2. `V1__create_schema.sql` em `db/migration/` com todas as tabelas `asteracomm_*` usando `IF NOT EXISTS`.
3. `ddl-auto=validate` nos profiles `dev` e `prod`; `baseline-on-migrate=true`.
4. `02-initial-flyway.sql` no `postgres/init/` cria tabelas `asteracomm_*` para o seed funcionar.
5. `application-test.properties`: `flyway.enabled=false` + `ddl-auto=create-drop`.
6. Divergências entre entidades JPA e schema corrigidas (`nullable`, `optional=false`).

---

### US-018 — Vínculo de plano de cobrança ao circuito e seleção de cliente no frontend

**Titulo:** Vínculo de plano de cobrança ao circuito e seleção de cliente no frontend

**Descrição:**
Plano de cobrança vinculado diretamente ao circuito (obrigatório). O circuito passa a ser o ponto central do sistema: `Call → Circuit → Plan` e `Call → Circuit → Customer`. A tela de circuitos exibe e permite selecionar cliente e plano. O DataSeeder cria um cliente único por circuito, um plano compartilhado e um DID por circuito.

**Estimativa:** 3 story points

**Critérios de Aceite:**

1. **Entidade `Circuit`:** Campo `plan` adicionado (FK `NOT NULL` para `Plan`). Plano é obrigatório — `BusinessException` lançada quando `planId` é nulo.
2. **API — criação e edição:** `POST /api/circuits` e `PUT /api/circuits/{number}` aceitam `planId` (obrigatório). `planId` inválido lança `NotFoundException`.
3. **API — leitura:** Listagem retorna `planName`; `GET /api/circuits/{number}` retorna o objeto `plan` completo.
4. **Frontend — formulário:** Seletor de **cliente** (obrigatório) e seletor de **plano** (obrigatório) no modal de criação/edição. Validação no frontend impede salvar sem plano.
5. **Frontend — listagem:** Coluna **Plano** adicionada na tabela de circuitos.
6. **SQL:** `plan_id BIGINT NOT NULL` em `asteracomm_circuits`; `asteracomm_plans` movida antes de `asteracomm_circuits` no script de init.
7. **DataSeeder:** Cria 1 plano "Plano Dev", 1 cliente por circuito (nomes de empresas) e 1 DID por circuito (4933333333, 4933334444, ... +1111).
8. **Testes:** 264 testes, 0 falhas — cobrem criação com/sem plano, troca de plano, exceção quando planId nulo/inválido.

---

### US-017 — Enriquecimento de ligações com circuito via channel

**Titulo:** Enriquecimento de ligações com circuito via channel

**Descrição:**
Ao processar registros do CDR, a entidade `Call` é enriquecida com o circuito de origem. O campo `channel` (ex: `PJSIP/4933401714-000045f0`) é parseado para extrair o código do circuito (`4933401714`), que é usado para associar a entidade `Circuit` correspondente. A tela de ligações exibe o circuito em coluna dedicada e permite filtrar por número de circuito.

**Estimativa:** 3 story points

**Critérios de Aceite:**

1. O campo `channel` é parseado corretamente para extrair o código do circuito (formato: `<PROTOCOLO>/<codigo>-<sufixo>`).
2. O código extraído é usado para buscar o `Circuit` correspondente pelo número do circuito.
3. A entidade `Call` expõe o `Circuit` associado quando encontrado (campo `circuit` com FK `circuit_number`).
4. Quando nenhum circuito é encontrado para o código extraído, a ligação ainda é retornada sem falhar (circuito nulo/ausente).
5. O parsing trata variações de protocolo além de `PJSIP` (ex: `SIP/`, `DAHDI/`) de forma genérica.
6. A tela de ligações exibe coluna "Circuito" e campo de filtro por número de circuito.
7. Testes unitários: 9 casos em `ChannelParserTest`, 6 casos em `CallProcessingServiceTest` — 259 testes, 0 falhas.

---

### US-016 — Entidade Call: mapeamento das ligações do CDR

**Titulo:** Entidade Call: mapeamento das ligações do CDR

**Descrição:**
Como sistema, quero processar automaticamente todos os registros da tabela `cdr` e transformá-los em objetos `Call`, enriquecendo cada ligação com data/hora, origem (callerid), destino, e categoria de destino classificada automaticamente, de forma que a tela de "Ligações" passe a consultar `Call` em vez de acessar o CDR diretamente.

**Estimativa:** 3 story points

**Critérios de Aceite:**

1. **Processamento de todos os registros:** Todas as ligações são mapeadas independentemente do status — ANSWERED, NO ANSWER, BUSY, FAILED e demais valores de `disposition`.
2. **Campos da entidade `Call`:**
   - `id` (PK)
   - `uniqueId` — CDR.uniqueid (índice único, garante idempotência)
   - `callDate` — CDR.calldate
   - `callerNumber` — extraído do CDR.clid (formato `"Nome" <número>` → extrair apenas o número)
   - `dst` — CDR.dst
   - `durationSeconds` — CDR.duration
   - `billSeconds` — CDR.billsec
   - `disposition` — CDR.disposition (mantido como string)
   - `callType` — enum classificado pelo `dst` (ver critério 3)
   - `processedAt` — timestamp de quando o registro foi criado em `Call`
3. **Classificação do `callType`** — validação do dígito inicial do assinante e suporte a prefixos brasileiros:
   - `FIXED_LOCAL` — 8 dígitos, começa com 2–8
   - `MOBILE_LOCAL` — 9 dígitos, começa com 9
   - `FIXED_LONG_DISTANCE` — DDD(2)+fixo(8), 0+DDD+fixo(11d) ou 0+CSP+DDD+fixo(13d)
   - `MOBILE_LONG_DISTANCE` — DDD(2)+móvel(9), 0+DDD+móvel(12d) ou 0+CSP+DDD+móvel(14d)
   - `UNKNOWN` — demais casos
4. **Polling periódico:** Um job agendado (intervalo configurável via `application.properties`) consulta registros do CDR cujo `uniqueid` ainda não existe em `asteracomm_calls` e os processa.
5. **Idempotência:** Um mesmo `uniqueid` do CDR nunca gera dois registros em `Call`.
6. **Substituição da tela de Ligações:** O endpoint `/api/cdrs` é substituído por `/api/calls`, retornando registros de `Call`. Os filtros existentes (origem, destino, disposition, período) são mantidos. A tela de frontend é atualizada com coluna Tipo e campos do novo JSON.
7. **Testes:** 247 testes, 0 falhas — cobrindo extração de callerid, classificação de callType (23 casos) e processamento com diferentes dispositions.

---

### US-009 — Cadastro de clientes e vínculo obrigatório com circuito

**Titulo:** Cadastro de clientes e vínculo obrigatório com circuito

**Descrição:**
Como administrador, quero cadastrar clientes com apenas o nome, e vincular cada circuito a um cliente, sendo esse vínculo obrigatório, para que eu possa identificar a qual cliente cada circuito pertence.

**Estimativa:** 3 story points

**Critérios de Aceite:**

1. **CRUD de clientes:** Endpoints REST para criar, listar, buscar por ID, atualizar e remover clientes. O cliente possui `nome` (obrigatório, único), `enabled`, `createdAt` e `updatedAt`.
2. **Vínculo no circuito:** A entidade `Circuit` possui FK obrigatória para `Customer` (`customer_id NOT NULL`).
3. **Validação:** Não é possível criar ou atualizar um circuito sem informar um `customerId` válido.
4. **Exclusão restrita:** Não é permitido excluir um cliente que possua circuitos vinculados — apenas desativação (`PATCH /api/customers/{id}/disable`).
5. **Frontend:** Página `/customers` com CRUD completo (tabela paginada, modal criar/editar, botão desativar, botão excluir). Formulário de circuito inclui seletor de cliente obrigatório.
6. **Testes:** 228 testes, 0 falhas.

---

### US-008 — Refatoração: EndpointStatusService usar AmiService

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

### US-003 — Cadastro de planos de cobrança

**Titulo:** Cadastro de planos de cobrança

**Descrição:**
Como administrador, quero cadastrar planos de cobrança definindo o valor mensal, as tarifas por minuto para cada categoria de ligação fixa e, opcionalmente, um pacote de minutos inclusos — por categoria ou unificado — para que os planos possam ser atribuídos a clientes.

**Estimativa:** 5 story points

**Critérios de Aceite:**

1. **Listagem:** Exibe lista de planos com: nome, valor mensal, tipo de pacote e tarifas por categoria.
2. **Criação:** Cadastro com nome, mensalidade, tarifas R$/min (4 casas decimais) e pacote de minutos (`NONE` | `UNIFIED` | `PER_CATEGORY`).
3. **Edição:** Permite alterar qualquer campo do plano.
4. **Exclusão:** Permite excluir plano não vinculado a clientes.
5. **Validações:** Consistência do pacote conforme `packageType` via `@ValidPackage`; nome único; campos obrigatórios; 200 testes, 0 falhas.

---

### US-002 — Pesquisa e visualização de ligações realizadas

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

### US-007 — Vinculação DID-Circuito com provisionamento automático de Extensions

**Titulo:** Vinculação DID-Circuito com provisionamento automático de Extensions

**Descrição:**
Como administrador, quero vincular um ou mais DIDs a um circuito, para que o Asterisk provisione automaticamente as Extensions correspondentes, habilitando o circuito a receber e realizar chamadas por aquele número.

**Estimativa:** 5 story points

**Critérios de Aceite:**

1. **Vinculação:** Na tela do circuito, é possível associar um DID livre ao circuito. Um circuito pode ter 1 ou mais DIDs.
2. **Provisionamento automático:** Ao vincular, a Extension correspondente ao número do DID é criada nas tabelas do Asterisk (`pstn-{trunkName}`: Dial → Hangup) e um `dialplan reload` é disparado via AMI.
3. **Desvinculação:** É possível desvincular um DID de um circuito. Ao desvincular, a Extension é removida do Asterisk e o `dialplan reload` é disparado.
4. **Status do DID:** DID vinculado muda status para `EM USO`; ao desvincular volta para `LIVRE`.
5. **Restrição:** Um DID só pode estar vinculado a um circuito por vez. Circuito com DIDs vinculados não pode ser excluído.
6. **Exibição:** Na listagem de circuitos e na listagem de DIDs, o vínculo é exibido claramente.
7. **Testes:** 157 testes, 0 falhas.

---

### US-010 — Geração dinâmica de `extensions_trunks.conf` para contextos de tronco

**Titulo:** Geração dinâmica de `extensions_trunks.conf` para contextos de tronco

**Descrição:**
Como administrador, quero que ao cadastrar ou remover um tronco o Asterisk reconheça automaticamente os contextos de dialplan correspondentes (`internal-<tronco>` e `pstn-<tronco>`), sem necessidade de edição manual de arquivos de configuração.

**Estimativa:** 5 story points

**Critérios de Aceite:**

1. Volume nomeado `dialplan-generated` compartilhado entre `backend` (`/dialplan-generated`) e `asterisk` (`/etc/asterisk/generated`) em `docker-compose.dev.yml` e `docker-compose.yml`.
2. `extensions.conf` simplificado para apenas `#tryinclude generated/extensions_trunks.conf`.
3. `DialplanGeneratorService` gera o arquivo com stubs `switch => Realtime/@` para cada par `[internal-<tronco>]`/`[pstn-<tronco>]` e dispara `dialplan reload` via AMI.
4. Geração automática na inicialização via `@EventListener(ApplicationReadyEvent.class)`.
5. `provisionTrunk` e `deprovisionTrunk` invocam `generateAndReload()`; `reprovisionTrunk` não.
6. Propriedade `asterisk.dialplan.generated-path=/dialplan-generated` nos dois perfis.
7. 125 testes, 0 falhas.

---

### US-001 — Cadastro de provedores VoIP para troncos de saída

**Titulo:** Cadastro de provedores VoIP para troncos de saída com autenticação usuário/senha

**Descrição:**
Como administrador do sistema, quero cadastrar provedores VoIP (trunks SIP) com autenticação por usuário e senha, para que o Asterisk possa realizar chamadas de saída através desses provedores.

**Estimativa:** 8 story points

**Critérios de Aceite:**

1. **Listagem:** A interface exibe a lista de provedores VoIP cadastrados (nome, host/domínio, usuário, status de registro).
2. **Criação:** É possível cadastrar um novo provedor informando nome, host/domínio SIP, usuário e senha.
3. **Edição:** É possível editar um provedor existente (todos os campos; senha em branco não altera a senha).
4. **Exclusão:** É possível excluir um provedor, removendo todas as configurações do Asterisk associadas.
5. **Persistência no Asterisk:** Ao criar/editar/excluir, as tabelas `ps_endpoints`, `ps_auths`, `ps_aors` e `ps_registrations` são atualizadas e o Asterisk recebe um `pjsip reload`.
6. **Status de registro:** A interface exibe se o tronco está registrado ou não no provedor, consultando o estado via AMI.
7. **Validações:** Campos obrigatórios validados no frontend e backend; host deve ser não-vazio; nome único por sistema.

---

### US-005 — Refatoração: Modelo de Circuito SIP

**Titulo:** Refatoração: Modelo de Circuito SIP (softphone/ATA)

**Descrição:**
Como desenvolvedor, preciso refatorar o backend para que o conceito de **Circuito** seja a entidade central do domínio — representando um ramal SIP configurável para uso com softphone ou ATA — e que toda a interação com o Asterisk (ps_endpoints, ps_auths, ps_aors, extensions) seja encapsulada em uma camada de serviço, sem que entidades JPA do Asterisk vazem para o restante da aplicação.

**Estimativa:** 8 story points

**Critérios de Aceite:**

1. **Entidade `Circuito`:** Existe uma entidade de domínio própria (`Circuit`) com campos: número, senha. Essa entidade é persistida em tabela própria da aplicação (`asteracomm_circuits`), não nas tabelas do Asterisk.
2. **Camada de integração Asterisk:** `AsteriskProvisioningService` traduz o estado de um `Circuit` para as tabelas PJSIP (`ps_endpoints`, `ps_auths`, `ps_aors`) e para `extensions`, emitindo `pjsip reload` / `dialplan reload` via AMI após cada operação.
3. **Entidades Asterisk internas:** As entidades JPA das tabelas do Asterisk existem apenas dentro da camada de provisionamento, sem exposição via controllers, DTOs públicos ou serviços de domínio.
4. **Comportamento preservado:** O CRUD de circuitos (`/api/circuits`) continua funcionando com o mesmo contrato de API.
5. **Testes:** `CircuitServiceTest`, `CircuitControllerTest`, `AsteriskProvisioningServiceTest` — 73 testes, 0 falhas.
6. **Base para troncos:** Arquitetura extensível para US-001 (provedores VoIP/troncos) sem duplicar lógica de provisionamento.

---

### US-006 — Cadastro de DID (pool de números)

**Titulo:** Cadastro de DID (pool de números)

**Descrição:**
Como administrador, quero cadastrar DIDs (números de telefone) no sistema, formando um pool de números disponíveis para comercialização e uso, independentemente de estarem associados a um circuito.

**Estimativa:** 3 story points

**Critérios de Aceite:**

1. **Listagem:** Exibe DIDs cadastrados com: número, status (`LIVRE` | `EM USO`) e circuito vinculado (quando houver).
2. **Criação:** Cadastro com número obrigatório no formato `(XX) XXXX-XXXX` (exatamente 10 dígitos).
3. **Exclusão:** Impede exclusão de DID vinculado a um circuito.
4. **Validações:** Número único; apenas dígitos; exatamente 10 dígitos.
5. **Vinculação/Desvinculação:** Na tela do circuito, é possível vincular um DID livre e desvincular DIDs já associados (sem provisionamento Asterisk — ver US-007).
6. **Código do circuito auto-gerado:** Circuitos não aceitam mais número no cadastro; código gerado sequencialmente a partir de 100000.
7. **Testes:** 148 testes, 0 falhas.

---

### US-009 — Vinculação Circuito-Tronco com provisionamento automático de contextos

**Titulo:** Vinculação Circuito-Tronco com provisionamento automático de contextos

**Descrição:**
Como administrador do sistema, quero que cada circuito esteja obrigatoriamente vinculado a um tronco VoIP, para que o Asterisk configure automaticamente os contextos de discagem — permitindo que o circuito realize chamadas de saída pelo tronco e receba chamadas de entrada por ele.

**Estimativa:** 5 story points

**Critérios de Aceite:**

1. **Trunk com prefixo:** O tronco tem campo `prefix` opcional. Usado na composição do número discado: `PJSIP/<prefix>${EXTEN}@<tronco>,60`.
2. **Rota de saída auto-criada:** Ao criar um tronco, extensions de saída provisionadas em `internal-<nome>`: priority 1 NoOp, priority 2 Dial, priority 3 Hangup.
3. **Contexto de entrada do tronco:** `ps_endpoints` do tronco usa `context = "pstn-<nome>"`.
4. **Tronco obrigatório no circuito:** `trunkName NOT NULL`. `ps_endpoints` do circuito usa `context = "internal-<tronco>"`. Extensions de entrada criadas em `pstn-<tronco>`.
5. **Troca de tronco:** Reprovisionamento remove extensions do tronco antigo e cria no novo.
6. **Correções de provisão:** `provision(Circuit)` não usa mais `from-pstn`. `reprovisionTrunk` recria extensions de saída quando prefix ou host mudam.
