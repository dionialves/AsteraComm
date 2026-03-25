# Backlog — Migração Astro → Thymeleaf + HTMX

> Tarefas organizadas por prioridade. A ordem do índice define a sequência de execução.
> Cada tarefa é atômica e testável de forma independente.

---

## Índice (por prioridade)

### Fase 0 — Infraestrutura
1. [T-001 — Adicionar dependências Thymeleaf ao pom.xml](#t-001)
2. [T-002 — Configurar Spring Security com form login + session auth](#t-002)
3. [T-003 — Criar CurrentPathInterceptor para active state da sidebar](#t-003)
4. [T-004 — Configurar Tailwind CSS standalone + script de build](#t-004)
5. [T-005 — Criar estrutura de diretórios em resources/templates e resources/static](#t-005)
6. [T-006 — Incluir htmx.min.js em resources/static/js](#t-006)
7. [T-007 — Criar layout base.html (sidebar + main + toast container + modal container)](#t-007)
8. [T-008 — Criar layout base-login.html (sem sidebar)](#t-008)
9. [T-009 — Criar fragment toast.html com suporte a HTMX OOB swap](#t-009)
10. [T-010 — Criar fragment pagination.html reutilizável](#t-010)
11. [T-011 — Criar fragment confirm-delete.html](#t-011)
12. [T-012 — Criar fragment search-select.html + endpoint /fragments/search-options](#t-012)

### Fase 1 — Páginas simples
13. [T-013 — Migrar página de Login](#t-013)
14. [T-014 — Migrar Dashboard (cards server-side + Chart.js)](#t-014)
15. [T-015 — Migrar Reports index (grid estático)](#t-015)
16. [T-016 — Criar modais de Perfil e Alteração de Senha no layout](#t-016)

### Fase 2 — CRUDs simples
17. [T-017 — Migrar Troncos: list.html + table.html (fragment)](#t-017)
18. [T-018 — Migrar Troncos: modal.html (criar/editar) + toggle de senha](#t-018)
19. [T-019 — Criar TrunkViewController com endpoints página, tabela, modal, CRUD](#t-019)
20. [T-020 — Migrar Planos: list.html + table.html (fragment)](#t-020)
21. [T-021 — Migrar Planos: modal.html com campos condicionais por tipo de pacote](#t-021)
22. [T-022 — Criar endpoint /plans/package-fields para campos condicionais via HTMX](#t-022)
23. [T-023 — Criar PlanViewController com endpoints página, tabela, modal, CRUD](#t-023)
24. [T-024 — Migrar DIDs: list.html + table.html (fragment)](#t-024)
25. [T-025 — Migrar DIDs: modal.html com máscara de telefone](#t-025)
26. [T-026 — Criar DidViewController com endpoints página, tabela, modal, CRUD](#t-026)
27. [T-027 — Migrar Usuários: list.html + table.html (fragment) com paginação server-side](#t-027)
28. [T-028 — Migrar Usuários: modal.html com filtro por role + reset de senha admin](#t-028)
29. [T-029 — Criar UserViewController com endpoints página, tabela, modal, CRUD](#t-029)

### Fase 3 — CRUDs complexos
30. [T-030 — Migrar Circuitos: list.html + table.html (fragment) com filtros status/online](#t-030)
31. [T-031 — Migrar Circuitos: modal.html com estrutura de 3 tabs](#t-031)
32. [T-032 — Migrar Circuitos: tab-detalhes.html com SearchSelect para tronco/plano/cliente](#t-032)
33. [T-033 — Migrar Circuitos: tab-dids.html com link/unlink de DIDs](#t-033)
34. [T-034 — Migrar Circuitos: tab-history.html (read-only)](#t-034)
35. [T-035 — Criar CircuitViewController com endpoints página, tabela, modal, tabs, CRUD](#t-035)
36. [T-036 — Migrar Clientes: list.html + table.html (fragment)](#t-036)
37. [T-037 — Migrar Clientes: modal.html com estrutura de tabs](#t-037)
38. [T-038 — Migrar Clientes: tab-circuits.html (listagem de circuitos do cliente)](#t-038)
39. [T-039 — Migrar Clientes: tab-history.html (read-only)](#t-039)
40. [T-040 — Criar CustomerViewController com endpoints página, tabela, modal, tabs, CRUD](#t-040)

### Fase 4 — CDRs e Relatórios
41. [T-041 — Migrar CDRs: list.html + table.html com 6 filtros via HTMX](#t-041)
42. [T-042 — Migrar CDRs: detail-modal.html (read-only)](#t-042)
43. [T-043 — Criar CdrViewController com endpoints página, tabela, modal](#t-043)
44. [T-044 — Migrar formatações JS de CDR para o backend (formatDuration, formatCost, badges)](#t-044)
45. [T-045 — Migrar Report Cost per Circuit: seletores + tabela HTMX + PDF server-side existente](#t-045)
46. [T-046 — Migrar Report Call Cost: seletores + tabela HTMX + migrar PDF do client para backend](#t-046)
47. [T-047 — Migrar Report Audit: SearchSelect circuito + simulação HTMX + PDF server-side](#t-047)
48. [T-048 — Criar ReportViewController com endpoints para os 3 relatórios](#t-048)

### Fase 5 — Cleanup
49. [T-049 — Remover diretório frontend/ do docker-compose e do repositório de submodules](#t-049)
50. [T-050 — Atualizar Dockerfile do backend para incluir build do Tailwind CSS](#t-050)
51. [T-051 — Remover ou simplificar configuração Nginx (proxy para frontend Node.js)](#t-051)
52. [T-052 — Atualizar README e documentação de infraestrutura](#t-052)

---

## Padrões de implementação

### Padrão de Modal

Todo modal do sistema **deve** seguir este padrão. A referência canônica é `pages/dids/modal.html`.

#### Estrutura obrigatória

```html
<!-- Backdrop -->
<div th:fragment="modal"
     class="fixed inset-0 bg-black/40 z-[100] flex items-center justify-center"
     onclick="closeModal()">

  <!-- Card -->
  <div class="bg-white rounded-xl shadow-xl w-full max-w-[780px] h-[700px] flex flex-col"
       onclick="event.stopPropagation()">

    <!-- Header (fixo, nunca rola) -->
    <div class="flex-shrink-0 flex items-center justify-between px-5 pt-4 pb-4 border-b border-[#e5e7eb]">
      <div>
        <p class="text-[11px] text-[#888] uppercase tracking-wide font-medium">{Entidade}</p>
        <h2 class="text-[18px] font-medium text-[#1a1a1a] mt-0.5"
            th:text="${obj != null} ? ${obj.name} : 'Novo {Objeto}'">...</h2>
      </div>
      <button type="button" onclick="closeModal()"
              class="w-7 h-7 flex items-center justify-center text-[#888] hover:text-[#1a1a1a] rounded-md transition-colors">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
             stroke-linecap="round" stroke-linejoin="round">
          <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
        </svg>
      </button>
    </div>

    <!-- Form unificado: th:attr hx-post/hx-put — nunca dois forms separados com th:if -->
    <form th:attr="hx-post=${obj == null ? '/entidade' : null},
                  hx-put=${obj != null ? '/entidade/' + obj.id : null}"
          hx-target="#table-container" hx-swap="innerHTML"
          class="flex flex-col flex-1 min-h-0">
      <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}"/>

      <!-- Body (rola quando o conteúdo excede o espaço) -->
      <div class="flex-1 overflow-y-auto p-5 flex flex-col gap-4">
        <!-- campos do formulário -->
      </div>

      <!-- Footer (fixo, nunca rola) -->
      <div class="modal-footer">
        <!-- Botão deletar: apenas em modo edição, alinhado à esquerda via mr-auto no CSS -->
        <!-- ATENÇÃO: se o botão for condicional (th:if), o wrapper deve ter class="contents" -->
        <div th:if="${obj != null}" class="contents">
          <div th:replace="~{fragments/confirm-delete :: confirm-delete(
              deleteUrl=${'/entidade/' + obj.id},
              hxTarget='#table-container',
              hxInclude='.entidade-filter',
              label=${'a entidade ' + obj.name})}">
          </div>
        </div>
        <button type="button" onclick="closeModal()" class="btn-modal-cancel">Cancelar</button>
        <button type="submit" class="btn-modal-save"
                th:text="${obj != null} ? 'Salvar' : 'Criar {Objeto}'">Criar</button>
      </div>
    </form>

  </div>
</div>
```

#### Regras

- **Tamanho fixo:** `max-w-[780px] h-[700px]` — nunca alterar por modal de CRUD.
- **Scroll apenas no body:** header e footer são `flex-shrink-0`; o body usa `flex-1 overflow-y-auto`.
- **Form unificado:** usar `th:attr hx-post/hx-put` — nunca dois forms separados com `th:if`.
- **Form envolve body + footer:** a `<form>` deve ter `class="flex flex-col flex-1 min-h-0"` para participar do layout flex corretamente.
- **Seções no body:** separar grupos de campos com `<p class="text-[12px] font-semibold text-[#888] uppercase tracking-wide">Título</p>` e `<hr class="border-t border-[#e5e7eb]"/>`.
- **Script dentro do fragment:** todo `<script>` do modal deve estar **dentro** do `<div th:fragment="modal">`. Scripts fora do fragment são descartados pelo HTMX ao carregar via AJAX.
- **Painel read-only** (sem form): usar `<div class="flex flex-col flex-1 min-h-0">` no lugar da `<form>`.

#### Botões do footer

Definidos como classes CSS em `input.css` (não Tailwind inline):

| Classe | Uso | Visual |
|---|---|---|
| `.btn-modal-cancel` | Cancelar / Fechar | Branco, borda cinza, texto cinza |
| `.btn-modal-save` | Salvar / Criar | Verde `#1D9E75`, texto branco |
| `.btn-modal-delete` | Deletar (modo edição) | Branco, borda vermelha `#fca5a5`, texto `#ef4444`, `mr-auto` |
| `.btn-modal-delete.confirm` | Estado após 1º clique | Fundo `#ef4444`, texto branco |

#### Padrão de exclusão (duplo clique)

O botão "Deletar" usa o fragment `confirm-delete` com `hx-trigger="confirmed"` e `onclick="handleModalDelete(this)"`. **Não usar `hx-confirm`**.

- **1º clique:** texto muda para "Confirmar exclusão?", fundo fica vermelho. Auto-reset em 3s.
- **2º clique:** dispara `htmx.trigger(btn, 'confirmed')` → executa o DELETE.
- A função `handleModalDelete(btn)` está definida globalmente em `layout/base.html`.

```html
<!-- Fragment confirm-delete.html — uso via th:replace -->
<button th:fragment="confirm-delete(deleteUrl, hxTarget, hxInclude, label)"
        type="button"
        th:attr="hx-delete=${deleteUrl}, hx-target=${hxTarget},
                 hx-swap='innerHTML', hx-include=${hxInclude}"
        hx-trigger="confirmed"
        onclick="handleModalDelete(this)"
        class="btn-modal-delete">
  Deletar
</button>
```

#### Armadilha: wrapper `th:if` quebra o `mr-auto`

O `.btn-modal-delete` usa `margin-right: auto` para se alinhar à esquerda no flex do footer. Se o botão estiver dentro de um `<div th:if>`, o wrapper se torna o flex child e o `mr-auto` perde efeito. **Solução: `class="contents"`** torna o wrapper invisível ao layout flex.

```html
<!-- ERRADO: mr-auto não funciona -->
<div th:if="${obj != null}">
  <div th:replace="~{fragments/confirm-delete :: ...}"></div>
</div>

<!-- CORRETO: class="contents" torna o wrapper transparente ao flex -->
<div th:if="${obj != null}" class="contents">
  <div th:replace="~{fragments/confirm-delete :: ...}"></div>
</div>
```

#### CSS compartilhado (input.css)

As classes de modal estão definidas em `src/main/resources/static/css/input.css`:

```css
.modal-footer { padding: 14px 20px; border-top: 1px solid #e5e7eb; display: flex; align-items: center; justify-content: flex-end; gap: 8px; flex-shrink: 0; }
.btn-modal-delete { padding: 7px 16px; background: #fff; border: 1px solid #fca5a5; color: #ef4444; border-radius: 6px; margin-right: auto; cursor: pointer; font-size: 13px; }
.btn-modal-delete.confirm { background: #ef4444; color: #fff; border-color: #ef4444; }
.btn-modal-cancel { padding: 7px 16px; border: 1px solid #d1d5db; background: #fff; border-radius: 6px; color: #374151; cursor: pointer; font-size: 13px; }
.btn-modal-save { padding: 7px 16px; background: #1D9E75; color: #fff; border-radius: 6px; border: none; cursor: pointer; font-size: 13px; font-weight: 500; }
```

---

### Padrão de Sort

**Nunca** usar `th:attr="hx-get=..."` nas colunas de sort. O `hx-include` enviaria o parâmetro `sort` duplicado (URL + hidden input), causando comportamento imprevisível.

#### `table.html` (fragment)

```html
<!-- hidden input com id — lido e atualizado pelo JS -->
<input type="hidden" id="current-sort" name="sort" th:value="${sort}" class="{obj}-filter"/>

<!-- coluna sortável: só data-field, sem hx-* -->
<span data-field="campo"
      class="sort-col text-[11px] font-medium text-[#888] uppercase tracking-wide cursor-pointer select-none flex items-center gap-1">
  Coluna
</span>
<!-- coluna não sortável: sem data-field, sem cursor-pointer -->
<span class="text-[11px] font-medium text-[#888] uppercase tracking-wide">Coluna</span>
```

#### `list.html` (página)

```javascript
var currentSortField = 'id';   // campo padrão
var currentSortDir   = 'desc'; // direção padrão
var currentStatus    = '';     // rastreado pelo setFilter()

var SORT_ASC  = '<svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="18 15 12 9 6 15"/></svg>';
var SORT_DESC = '<svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="6 9 12 15 18 9"/></svg>';

function updateSortIcons() {
  document.querySelectorAll('.sort-col[data-field]').forEach(function (el) {
    var existing = el.querySelector('svg');
    if (existing) existing.remove();
    if (el.dataset.field === currentSortField)
      el.insertAdjacentHTML('beforeend', currentSortDir === 'asc' ? SORT_ASC : SORT_DESC);
  });
}

function triggerTableLoad(page) {
  var search = (document.querySelector('input[name="search"].{obj}-filter') || {}).value || '';
  var params = 'sort=' + currentSortField + ',' + currentSortDir
             + '&page=' + page + '&size=20'
             + '&search=' + encodeURIComponent(search);
  if (currentStatus) params += '&status=' + currentStatus;
  htmx.ajax('GET', '/{objetos}/table?' + params, {target: '#table-container', swap: 'innerHTML'});
}

document.addEventListener('htmx:afterSwap', function (e) {
  if (e.detail.target.id === 'table-container') {
    // sincronizar vars com o que o servidor retornou
    var si = document.getElementById('current-sort');
    if (si) { var p = si.value.split(','); currentSortField = p[0]; currentSortDir = p[1] || 'asc'; }
    updateSortIcons();

    // click handlers nas colunas sortáveis
    document.querySelectorAll('.sort-col[data-field]').forEach(function (el) {
      el.addEventListener('click', function () {
        if (currentSortField === el.dataset.field)
          currentSortDir = currentSortDir === 'asc' ? 'desc' : 'asc';
        else { currentSortField = el.dataset.field; currentSortDir = 'asc'; }
        var si2 = document.getElementById('current-sort');
        if (si2) si2.value = currentSortField + ',' + currentSortDir;
        updateSortIcons();
        triggerTableLoad(0);
      });
    });

    // click handlers nas linhas (seleção)
    document.querySelectorAll('.{obj}-row[data-id]').forEach(function (row) {
      row.addEventListener('click', function () {
        document.querySelectorAll('.{obj}-row.row-selected').forEach(function (r) { r.classList.remove('row-selected'); });
        row.classList.add('row-selected');
      });
    });
  }
  // remover seleção ao fechar modal
  if (e.detail.target.id === 'modal-container' && !e.detail.target.innerHTML.trim()) {
    document.querySelectorAll('.{obj}-row.row-selected').forEach(function (r) { r.classList.remove('row-selected'); });
  }
});
```

Quando a página usa filtros por status/tipo, atualizar `currentStatus` na função de filtro **antes** de o HTMX disparar, para que `triggerTableLoad` passe o valor correto.

---

### Padrão de Paginação no Toolbar

A paginação fica na **mesma linha dos filtros**, alinhada à direita. **Nunca** colocar abaixo da tabela.

#### `list.html` — toolbar (slot vazio)

```html
<div class="flex items-center justify-between mb-3 gap-3 flex-wrap">
  <div class="flex items-center gap-3"><!-- busca + filtros --></div>
  <!-- slot: preenchido via OOB a cada swap do fragment -->
  <div id="pagination-toolbar" class="flex items-center gap-2"></div>
</div>
```

#### `table.html` — OOB dentro do fragmento

```html
<div th:fragment="table">

  <!-- ... hidden inputs, tabela ... -->

  <!-- OOB: popula o slot de paginação no toolbar -->
  <div id="pagination-toolbar" hx-swap-oob="innerHTML">
    <div th:replace="~{fragments/pagination :: pagination(
        page=${items},
        hxGet=${'/objetos/table?sort=' + sort + (!#strings.isEmpty(statusFilter) ? '&status=' + statusFilter : '')},
        hxTarget='#table-container',
        hxInclude='.{obj}-filter')}">
    </div>
  </div>

</div>
```

> **Regra crítica:** o `hx-swap-oob` deve estar **dentro** do `th:fragment`. Thymeleaf descarta tudo fora do fragmento ao usar o seletor `:: table`, então o OOB nunca chegaria ao cliente se estivesse fora.

A referência canônica é `pages/dids/list.html` + `pages/dids/table.html`.

---

### Padrão de Contador de Registros

Toda página de listagem deve exibir o total de registros abaixo da tabela, atualizado via OOB.

#### `list.html` — slot vazio
```html
<div id="table-container" hx-get="/{objetos}/table" hx-trigger="load" hx-swap="innerHTML"></div>
<p id="{obj}-counter" class="text-[12px] text-[#888] text-right mt-3"></p>
```

#### `table.html` — OOB dentro do fragment
```html
<p id="{obj}-counter" hx-swap-oob="innerHTML"
   th:text="${items.totalElements == 1 ? '1 {objeto} cadastrado' : items.totalElements + ' {objetos} cadastrados'}"></p>
```

---

### Padrão de Toggle Switch

Para campos booleanos (ativo, habilitado) em modais, usar as classes CSS do `input.css`:

```html
<div class="flex items-center gap-2">
  <label class="toggle-switch">
    <input type="checkbox" name="active" value="true" th:checked="${obj?.active}"/>
    <span class="toggle-slider"></span>
  </label>
  <span id="toggle-label" class="toggle-label"
        th:text="${obj != null and obj.active} ? 'Ativo' : 'Inativo'">Ativo</span>
</div>
```

Script de atualização do label (dentro do fragment):
```javascript
var chk = document.getElementById('campo');
var lbl = document.getElementById('toggle-label');
if (chk && lbl) {
  chk.addEventListener('change', function () { lbl.textContent = chk.checked ? 'Ativo' : 'Inativo'; });
}
```

Classes no `input.css`: `.toggle-switch`, `.toggle-slider`, `.toggle-label` — não recriar inline.

---

### Padrão de Input Masking

Para campos monetários e telefone: campo de exibição com máscara (sem `name`) + hidden input com valor limpo (com `name`). O `htmx:configRequest` sobrescreve o parâmetro antes do envio.

```html
<!-- Exibição com máscara (sem name, com required para validação) -->
<input id="display-price" type="text" placeholder="0,00" required class="..."/>
<!-- Valor numérico enviado ao servidor (sem required) -->
<input id="hidden-price" name="monthlyPrice" type="hidden"/>
```

```javascript
// Aplicar máscara no input event
var dP = document.getElementById('display-price');
if (dP) {
  dP.addEventListener('input', function () {
    var d = dP.value.replace(/\D/g, '');
    var n = parseInt(d || '0', 10);
    dP.value = (n / 100).toLocaleString('pt-BR', {minimumFractionDigits: 2, maximumFractionDigits: 2});
  });
}

// Enviar valor numérico real via htmx:configRequest (não htmx:beforeRequest)
form.addEventListener('htmx:configRequest', function (evt) {
  function parseCurrency(val) { var d = val.replace(/\D/g, ''); return d ? (parseInt(d,10)/100).toFixed(2) : ''; }
  if (dP) evt.detail.parameters['monthlyPrice'] = parseCurrency(dP.value);
});
```

> **Armadilha:** usar `htmx:beforeRequest` não funciona — o HTMX já coletou os parâmetros nesse ponto. Usar sempre `htmx:configRequest`, que dispara após a coleta e permite sobrescrever `evt.detail.parameters`.

---

### Padrão: após criação, modal reabre com o objeto criado

Ao salvar um novo objeto via modal, o controller retorna o próprio modal em modo edição (com o objeto recém-criado), e um div oculto com `hx-trigger="load"` recarrega a tabela em segundo plano.

#### `modal.html` — form com `hx-target` dinâmico

```html
<form th:attr="hx-post=${obj == null ? '/entidade' : null},
              hx-put=${obj != null ? '/entidade/' + obj.id : null},
              hx-target=${obj == null ? '#modal-container' : '#table-container'}"
      hx-swap="innerHTML"
      class="flex flex-col flex-1 min-h-0">
```

- **Criação:** `hx-post=/entidade` + `hx-target=#modal-container` → resposta vai para o `#modal-container`
- **Edição:** `hx-put=/entidade/{id}` + `hx-target=#table-container` → resposta vai para a tabela

#### Controller — criação com sucesso

```java
@PostMapping
public String create(@ModelAttribute ObjForm form, Model model) {
    Obj created = service.create(form);
    model.addAttribute("obj", created);
    model.addAttribute("refreshTable", true);
    return "pages/entidade/modal :: modal";  // reabre em modo edição
}
```

#### Controller — erro na criação

```java
model.addAttribute("obj", null);  // mantém em modo criação
model.addAttribute("toastMsg", "Erro: ...");
model.addAttribute("toastType", "error");
return "pages/entidade/modal :: modal";
```

#### `modal.html` — trigger de reload (dentro do fragment)

```html
<!-- Toast OOB -->
<th:block th:if="${toastMsg}">
  <div th:replace="~{fragments/toast :: toast(toastMessage=${toastMsg}, toastType=${toastType})}"></div>
</th:block>

<!-- Recarrega tabela após criação — NUNCA usar OOB aninhado -->
<div th:if="${refreshTable}"
     hx-get="/entidade/table"
     hx-target="#table-container"
     hx-swap="innerHTML"
     hx-trigger="load"
     class="hidden"></div>
```

#### Armadilha: OOB aninhado quebra contador e paginação

**Nunca** usar um `hx-swap-oob` apontando para `#table-container` que contenha via `th:replace` o fragment de tabela. O fragment de tabela já possui OOBs internos (`#pagination-toolbar`, `#{obj}-counter`). O HTMX processa esses OOBs *e* os mantém no conteúdo do OOB externo, duplicando os elementos: o contador fica desposicionado com texto grande e a paginação aparece abaixo da tabela.

```html
<!-- ERRADO: OOB aninhado duplica contador e paginação -->
<div id="table-container" hx-swap-oob="innerHTML">
  <div th:replace="~{pages/entidade/table :: table}"></div>
</div>

<!-- CORRETO: GET limpo via hx-trigger="load", sem OOB aninhado -->
<div th:if="${refreshTable}"
     hx-get="/entidade/table"
     hx-target="#table-container"
     hx-swap="innerHTML"
     hx-trigger="load"
     class="hidden"></div>
```

O `hx-trigger="load"` garante que o HTMX dispare um GET independente assim que o modal for inserido no DOM, sem nenhum OOB aninhado.

---

## Detalhamento das Tarefas

---

### T-001
**Adicionar dependências Thymeleaf ao pom.xml**
- `spring-boot-starter-thymeleaf`
- `thymeleaf-extras-springsecurity6`
- `thymeleaf-layout-dialect`
- `thymeleaf-extras-java8time` (formatação de datas/moeda nos templates)

**Critérios de aceite:** Build Maven passa sem erros; Spring Boot sobe com Thymeleaf no classpath.

---

### T-002
**Configurar Spring Security com form login + session auth**
- Remover (ou paralelizar) o filtro JWT atual para rotas de UI
- Configurar `formLogin` com `/login` customizado e `defaultSuccessUrl("/")`
- Configurar `logout` em `POST /logout` redirecionando para `/login`
- Manter endpoints REST `/api/**` com JWT (se necessário para integração futura)
- Regras de autorização: `/users/**` exige `ADMIN` ou `SUPER_ADMIN`

**Critérios de aceite:** Login via form funciona; sessão é mantida; logout invalida sessão; acesso a `/users/**` sem role retorna 403/redirect.

---

### T-003
**Criar CurrentPathInterceptor**
- Implementar `HandlerInterceptor.preHandle` que adiciona `currentPath = request.getRequestURI()` ao `Model`
- Registrar o interceptor no `WebMvcConfigurer`

**Critérios de aceite:** `${currentPath}` disponível em todos os templates Thymeleaf; sidebar marca o item correto como ativo.

---

### T-004
**Configurar Tailwind CSS standalone + script de build**
- Baixar binário `tailwindcss-linux-x64` e commitar em `backend/tools/` (ou configurar download via Maven)
- Criar `src/main/resources/static/css/input.css` com `@tailwind base/components/utilities` + custom vars
- Criar `tailwind.config.js` apontando para `templates/**/*.html`
- Adicionar `exec-maven-plugin` ao `pom.xml` para rodar Tailwind no `generate-resources`
- Criar script de dev watch (`./tailwind-watch.sh`)

**Critérios de aceite:** `mvn package` gera `output.css` minificado; em dev, watch detecta mudanças nos templates.

---

### T-005
**Criar estrutura de diretórios em resources/templates e resources/static**
- `templates/layout/`
- `templates/fragments/`
- `templates/pages/{login,dashboard,circuits,customers,cdrs,dids,plans,trunks,users,reports}/`
- `static/css/`, `static/js/`, `static/images/`

**Critérios de aceite:** Estrutura criada e commitada; Spring Boot localiza templates sem erros de resolução.

---

### T-006
**Incluir htmx.min.js em resources/static/js**
- Baixar `htmx.min.js` v2.0.4 (ou versão estável atual) e salvar em `static/js/`
- Não usar CDN (garantir funcionamento offline e sem dependência externa)

**Critérios de aceite:** Arquivo disponível em `GET /js/htmx.min.js`; hash SRI documentado.

---

### T-007
**Criar layout base.html**
- Sidebar fixa com logo, navegação, menus colapsáveis e área de perfil no rodapé
- Active state via `th:classappend` usando `${currentPath}` (sem JavaScript)
- Menu "Administração" visível apenas para `ADMIN/SUPER_ADMIN` via `sec:authorize`
- `<main>` com slot `th:replace="${content}"` para conteúdo das páginas
- `<div id="modal-container">` e `<div id="toast-container">` fixos no body
- Inclusão de `output.css` e `htmx.min.js`
- JS global mínimo: `closeModal()`, sidebar submenu toggle (~10 linhas), toast auto-dismiss (~15 linhas)

**Critérios de aceite:** Página de dashboard renderiza com sidebar; active state correto em cada rota; menus colapsáveis funcionam.

---

### T-008
**Criar layout base-login.html**
- Layout sem sidebar, centrado, para página de login
- Mesmo header de CSS

**Critérios de aceite:** Página de login usa este layout sem exibir a sidebar.

---

### T-009
**Criar fragment toast.html com suporte a HTMX OOB swap**
- Fragment incluído no response de operações CRUD junto com o fragment principal
- Usa `hx-swap-oob="beforeend"` no `#toast-container`
- Auto-dismiss via CSS animation (`fadeOut 3s forwards`) sem JavaScript adicional
- Suporte a tipos: `success`, `error`, `warning`

**Critérios de aceite:** Toast aparece após salvar/deletar; desaparece automaticamente; não bloqueia a UI.

---

### T-010
**Criar fragment pagination.html reutilizável**
- Recebe `page` (objeto `Page<T>` do Spring Data) e `baseUrl`
- Botão anterior desabilitado quando `page.first`; botão próximo desabilitado quando `page.last`
- Exibe "Página X de Y"
- Passa parâmetros extras de filtro/sort via `hx-include` ou query string

**Critérios de aceite:** Paginação funciona em qualquer tabela que use o fragment; estado dos botões correto nos extremos.

---

### T-011
**Criar fragment confirm-delete.html**
- Botão de deletar com `hx-confirm` nativo do HTMX (dialog padrão do browser)
- Parâmetros: `url` (endpoint DELETE), `target` (onde recarregar após delete), `label` (nome do objeto)

**Critérios de aceite:** Confirmação exibida antes de disparar o DELETE; cancelar não executa a operação.

---

### T-012
**Criar fragment search-select.html + endpoint /fragments/search-options**
- Fragment: input de texto + hidden input para o valor + lista dropdown
- HTMX dispara `GET /fragments/search-options?field={name}&q={query}` com debounce 200ms
- Endpoint retorna `<li>` options filtradas
- Ao clicar em opção: preenche o hidden input e fecha o dropdown (~10 linhas JS no fragment)
- Parâmetros: `name`, `placeholder`, `selected` (valor inicial)

**Critérios de aceite:** Busca filtra em tempo real; valor correto submetido no form; funciona em Circuitos (tronco, plano, cliente) e Audit (circuito).

---

### T-013
**Migrar página de Login**
- Template `pages/login.html` usando `base-login.html`
- Form `th:action="@{/login}" method="post"` com campos `username` e `password`
- Mensagem de erro com `th:if="${param.error}"`
- Sem JavaScript (zero)

**Critérios de aceite:** Login funciona; credenciais erradas exibem mensagem de erro; redirect correto após sucesso.
**Redução:** 133 linhas → ~40 linhas. Zero JS.

---

### T-014
**Migrar Dashboard**
- `DashboardController.GET /` popula `DashboardDTO` e serializa JSON para Chart.js
- Template renderiza cards (Circuitos, Troncos, Clientes, Planos) server-side com `th:text`
- Dados dos charts injetados via `th:inline="javascript"` (único JS justificado: Chart.js)

**Critérios de aceite:** Cards exibem valores corretos do banco; charts renderizam com dados reais; nenhum fetch JS adicional.
**Redução:** 470 linhas → ~150 linhas template + ~80 linhas JS (só Chart.js).

---

### T-015
**Migrar Reports index**
- Template `pages/reports/index.html` com grid estático de cards linkando para os sub-relatórios
- Zero JavaScript

**Critérios de aceite:** Página renderiza; links navegam para as rotas corretas.

---

### T-016
**Criar modais de Perfil e Alteração de Senha no layout**
- Botões no rodapé da sidebar disparam `hx-get="/profile/modal"` e `hx-get="/profile/password-modal"`
- Fragments `profile/modal.html` e `profile/password-modal.html`
- `ProfileController` com endpoints `GET /profile/modal`, `GET /profile/password-modal`, `PUT /profile`, `PUT /profile/password`

**Critérios de aceite:** Modal de perfil exibe dados do usuário logado; salvar atualiza o nome/email; alterar senha valida senha atual e salva nova.

---

### T-017
**Migrar Troncos: list.html + table.html (fragment)**
- `list.html`: barra de busca, filtros de status, paginação, container da tabela com `hx-get="/trunks/table" hx-trigger="load"`
- `table.html`: fragment com `th:each` nas linhas, badges de status, paginação via fragment T-010
- Busca com debounce 300ms via `hx-trigger="keyup changed delay:300ms"`

**Critérios de aceite:** Tabela carrega ao acessar `/trunks`; busca filtra em tempo real; paginação navega corretamente.

---

### T-018
**Migrar Troncos: modal.html (criar/editar) + toggle de senha**
- Fragment carregado via `hx-get="/trunks/{id}/modal"` ou `hx-get="/trunks/modal/new"`
- Campos: nome, host, porta, usuário, senha (toggle visibilidade ~5 linhas JS)
- Botões Salvar (`hx-put`/`hx-post`) e Deletar (`hx-delete` com confirm)
- Response retorna tabela atualizada + toast de sucesso (OOB)

**Critérios de aceite:** Criar e editar tronco funcionam; toggle de senha funciona; deletar exibe confirmação; toast aparece após operação.

---

### T-019
**Criar TrunkViewController**
- `GET /trunks` → página completa `pages/trunks/list`
- `GET /trunks/table` → fragment tabela com parâmetros `page`, `size`, `sort`, `search`, `status`
- `GET /trunks/{id}/modal` → fragment modal com dados do tronco
- `GET /trunks/modal/new` → fragment modal vazio
- `POST /trunks` → cria + retorna tabela + toast
- `PUT /trunks/{id}` → atualiza + retorna tabela + toast
- `DELETE /trunks/{id}` → remove + retorna tabela + toast

**Critérios de aceite:** Todos os endpoints funcionam; respostas retornam fragments corretos; service existente reutilizado.

---

### T-020
**Migrar Planos: list.html + table.html (fragment)**
- Mesma estrutura de Troncos (T-017)
- Colunas adicionais: tipo de pacote, preço

**Critérios de aceite:** Idênticos a T-017 para Planos.

---

### T-021
**Migrar Planos: modal.html com campos condicionais por tipo de pacote**
- `<select name="packageType">` com `hx-get="/plans/package-fields" hx-target="#package-fields-container"`
- Fragment `pages/plans/package-fields.html` retorna campos específicos para `NONE`, `FIXED_MOBILE`, `FIXED_ONLY`
- Máscara de moeda no campo de preço (~15 linhas JS)

**Critérios de aceite:** Ao mudar tipo de pacote, campos corretos aparecem/somem sem reload; máscara formata o valor corretamente.

---

### T-022
**Criar endpoint /plans/package-fields**
- `GET /plans/package-fields?packageType={tipo}` retorna fragment HTML com campos condicionais
- Cada tipo retorna um conjunto diferente de campos

**Critérios de aceite:** Fragment correto retornado para cada tipo; campos ausentes quando não aplicável.

---

### T-023
**Criar PlanViewController**
- Mesmos endpoints de TrunkViewController (T-019), adaptados para Planos
- Endpoint adicional `GET /plans/package-fields`

**Critérios de aceite:** CRUD completo de Planos funcional; campos condicionais funcionam.

---

### T-024
**Migrar DIDs: list.html + table.html (fragment)**
- Mesma estrutura de Troncos (T-017)
- Colunas: número, circuito vinculado, status

**Critérios de aceite:** Tabela carrega e filtra corretamente.

---

### T-025
**Migrar DIDs: modal.html com máscara de telefone**
- Fields: número DID, circuito (SearchSelect fragment T-012), status
- Máscara de telefone no campo número (~10 linhas JS)

**Critérios de aceite:** Criar e editar DID funcionam; máscara formata número; SearchSelect filtra circuitos.

---

### T-026
**Criar DidViewController**
- Mesmos endpoints de TrunkViewController (T-019), adaptados para DIDs

**Critérios de aceite:** CRUD completo de DIDs funcional; máscara e SearchSelect funcionam.

---

### T-027
**Migrar Usuários: list.html + table.html com paginação server-side**
- **Atenção:** página atual faz busca/filtro client-side (traz tudo de uma vez). Migrar para paginação server-side como as demais páginas.
- Filtro por role no button group
- Colunas: nome, email, role, status

**Critérios de aceite:** Paginação server-side funciona; filtro por role funciona; busca por nome/email funciona.

---

### T-028
**Migrar Usuários: modal.html com filtro por role + reset de senha admin**
- Fields: nome, email, role (select), status (toggle)
- Botão "Resetar Senha" (visível apenas para ADMIN/SUPER_ADMIN) via `sec:authorize`
- Endpoint `POST /users/{id}/reset-password` retorna toast de confirmação

**Critérios de aceite:** CRUD funciona; reset de senha visível apenas para admins; operação envia email ou retorna senha temporária.

---

### T-029
**Criar UserViewController**
- Mesmos endpoints de TrunkViewController (T-019), adaptados para Usuários
- Endpoint adicional `POST /users/{id}/reset-password`

**Critérios de aceite:** CRUD completo de Usuários; autorização por role aplicada; reset de senha funcional.

---

### T-030
**Migrar Circuitos: list.html + table.html (fragment) com filtros status/online**
- Barra de filtros: busca (texto), button group status (Todos/Ativo/Inativo), button group online (Todos/Online/Offline)
- `hx-include` para combinar todos os filtros na requisição
- Colunas: número, tronco, cliente, plano, DIDs vinculados, status, online

**Critérios de aceite:** Todos os filtros combinados funcionam; tabela atualiza sem reload de página.

---

### T-031
**Migrar Circuitos: modal.html com estrutura de 3 tabs**
- Tabs: Detalhes, DIDs, Histórico
- Cada tab dispara `hx-get="/circuits/{id}/tab/{nome}"` ao ser clicada
- Tab Detalhes carregada por default ao abrir o modal
- Footer com botões Salvar e Deletar

**Critérios de aceite:** Modal abre com tab Detalhes ativa; navegar entre tabs carrega conteúdo correto; fechar modal limpa `#modal-container`.

---

### T-032
**Migrar Circuitos: tab-detalhes.html com SearchSelect para tronco/plano/cliente**
- Fields: número do circuito, status, tronco (SearchSelect), plano (SearchSelect), cliente (SearchSelect)
- SearchSelect usa fragment T-012 para cada campo
- Form submetido com `hx-put="/circuits/{id}" hx-include="#form-circuit"`

**Critérios de aceite:** SearchSelects filtram corretamente; valores pré-preenchidos ao editar; form salva todos os campos.

---

### T-033
**Migrar Circuitos: tab-dids.html com link/unlink de DIDs**
- Lista DIDs vinculados ao circuito com botão de desvincular cada um
- Lista DIDs livres com botão de vincular
- Cada operação dispara HTMX e recarrega a tab

**Critérios de aceite:** Vincular DID livre ao circuito funciona; desvincular move o DID para a lista de livres; sem reload de página.

---

### T-034
**Migrar Circuitos: tab-history.html (read-only)**
- Lista de eventos de histórico do circuito (data, tipo, descrição, usuário)
- Read-only, sem ações

**Critérios de aceite:** Histórico exibe eventos ordenados por data decrescente.

---

### T-035
**Criar CircuitViewController**
- `GET /circuits` → página completa
- `GET /circuits/table` → fragment tabela (parâmetros: page, sort, search, status, online)
- `GET /circuits/{id}/modal` e `GET /circuits/modal/new` → fragment modal
- `GET /circuits/{id}/tab/detalhes`, `/tab/dids`, `/tab/historico` → fragments de tabs
- `POST /circuits`, `PUT /circuits/{id}`, `DELETE /circuits/{id}` → CRUD + retorna tabela + toast
- `POST /circuits/{id}/dids/{didId}` e `DELETE /circuits/{id}/dids/{didId}` → link/unlink DID

**Critérios de aceite:** Todos os endpoints funcionam; 1250 linhas JS/HTML do Astro substituídas por ~300 linhas de templates + ~100 linhas de controller.

---

### T-036
**Migrar Clientes: list.html + table.html (fragment)**
- Estrutura idêntica a Circuitos (T-030)
- Colunas: nome, documento, email, circuitos vinculados, status

**Critérios de aceite:** Tabela e filtros funcionam.

---

### T-037
**Migrar Clientes: modal.html com estrutura de tabs**
- Tabs: Detalhes, Circuitos, Histórico
- Mesma estrutura de tabs de Circuitos (T-031)

**Critérios de aceite:** Modal com tabs funciona; navegação entre tabs carrega conteúdo correto.

---

### T-038
**Migrar Clientes: tab-circuits.html (listagem de circuitos do cliente)**
- Lista circuitos vinculados ao cliente (read-only nesta tab)
- Link para abrir modal de circuito específico se necessário

**Critérios de aceite:** Circuitos do cliente listados corretamente.

---

### T-039
**Migrar Clientes: tab-history.html (read-only)**
- Idêntico a T-034 para Clientes

**Critérios de aceite:** Histórico do cliente exibido.

---

### T-040
**Criar CustomerViewController**
- Mesma estrutura de CircuitViewController (T-035), adaptado para Clientes
- Sem endpoints de link/unlink (gerenciado pelo lado do Circuito)

**Critérios de aceite:** CRUD completo de Clientes; 1055 linhas do Astro substituídas.

---

### T-041
**Migrar CDRs: list.html + table.html com 6 filtros via HTMX**
- Filtros: origem (src), destino (dst), circuito, status (ANSWERED/NO ANSWER/BUSY/FAILED), data início, data fim
- Todos os filtros usam classe CSS comum (ex: `.cdr-filter`) para `hx-include`
- Colunas: data, origem, destino, duração, custo, tipo, status

**Critérios de aceite:** Combinação de qualquer filtro funciona; tabela atualiza sem reload; filtros de data usam `type="date"` nativo.

---

### T-042
**Migrar CDRs: detail-modal.html (read-only)**
- Modal com todos os campos da CDR formatados
- Botão de fechar apenas (sem edição)

**Critérios de aceite:** Modal exibe todos os dados da ligação selecionada; formatações corretas.

---

### T-043
**Criar CdrViewController**
- `GET /cdrs` → página completa
- `GET /cdrs/table` → fragment tabela (parâmetros: page, sort, src, dst, circuit, disposition, from, to)
- `GET /cdrs/{id}/modal` → fragment modal de detalhe

**Critérios de aceite:** Todos os endpoints funcionam; filtros combinados retornam resultados corretos.

---

### T-044
**Migrar formatações JS de CDR para o backend**
- `formatDateTime()` → `th:text="${#temporals.format(cdr.callDate, 'dd/MM/yyyy HH:mm')}"`
- `formatDuration(seconds)` → método `getFormattedDuration()` no DTO ou utilitário Thymeleaf
- `formatCost(value)` → `th:text="${#numbers.formatDecimal(cdr.cost, 1, 'COMMA', 2, 'POINT')}"`
- `badgeDisposition()` e `badgeCallType()` → Thymeleaf `th:class` com mapeamento de valores

**Critérios de aceite:** Todas as formatações exibidas corretamente nos templates; zero funções JS de formatação.

---

### T-045
**Migrar Report Cost per Circuit**
- Template `pages/reports/cost-per-circuit.html`: seletores mês/ano
- Fragment `table.html` carregado via HTMX ao alterar seletores
- Botão "Baixar PDF" usa o endpoint server-side existente (`cost-per-circuit-pdf`)

**Critérios de aceite:** Selecionar mês/ano carrega tabela; botão PDF faz download do arquivo gerado no backend.

---

### T-046
**Migrar Report Call Cost + migrar geração de PDF para o backend**
- Template `pages/reports/call-cost.html`: seletores mês/ano
- Fragment tabela carregado via HTMX
- **Migrar jsPDF client-side para backend:** implementar geração com iText ou JasperReports
- Controller retorna `ResponseEntity<byte[]>` com `Content-Type: application/pdf`

**Critérios de aceite:** Tabela carrega; PDF baixado diretamente do servidor; jsPDF removido do client.

---

### T-047
**Migrar Report Audit**
- Template `pages/reports/audit.html`: SearchSelect de circuito (fragment T-012) + seletores mês/ano
- Fragment de simulação carregado via HTMX ao submeter filtros
- Modal de seleção de DID via HTMX
- PDF gerado no backend (iText/JasperReports, reutilizando lógica de T-046)

**Critérios de aceite:** SearchSelect filtra circuitos; simulação carrega após selecionar circuito e período; PDF baixado do servidor.

---

### T-048
**Criar ReportViewController**
- `GET /reports` → página index
- `GET /reports/cost-per-circuit` → página + `GET /reports/cost-per-circuit/table`
- `GET /reports/cost-per-circuit/pdf` → download PDF (endpoint existente)
- `GET /reports/call-cost` → página + `GET /reports/call-cost/table` + `GET /reports/call-cost/pdf`
- `GET /reports/audit` → página + `GET /reports/audit/simulation` + `GET /reports/audit/pdf`

**Critérios de aceite:** Todos os relatórios funcionam; PDFs gerados no servidor.

---

### T-049
**Remover frontend/ do docker-compose e do repositório de submodules**
- Remover serviço `frontend` do `docker-compose.yml` (dev e prod)
- Remover submodule `frontend/` do repositório raiz
- Atualizar `dev.sh` e `prod.sh` para não referenciar mais o frontend

**Critérios de aceite:** `docker-compose up` sobe apenas backend, postgres e asterisk (+ nginx se prod); sem erros de submodule.

---

### T-050
**Atualizar Dockerfile do backend para incluir build do Tailwind CSS**
- Multi-stage build: stage de build com binário Tailwind, stage final só com o JAR
- Garantir que `output.css` gerado na build seja incluído no JAR em `static/css/`

**Critérios de aceite:** `docker build` gera imagem funcional com CSS correto; nenhuma dependência de Node.js na imagem final.

---

### T-051
**Atualizar/simplificar configuração Nginx**
- Remover regras de proxy para `frontend:4321`
- Manter apenas: SSL termination (se aplicável) e proxy para `backend:8090`
- Nginx torna-se opcional; Spring Boot pode servir diretamente na porta 80/443

**Critérios de aceite:** Nginx direciona todo tráfego para o backend; sem referências ao container frontend removido.

---

### T-052
**Atualizar README e documentação de infraestrutura**
- Remover referências ao Astro, Node.js e container frontend
- Atualizar seção de arquitetura (de 2 containers para 1)
- Documentar novo fluxo de desenvolvimento: Tailwind watch + Spring Boot
- Atualizar `CLAUDE.md` com nova estrutura de templates e convenções Thymeleaf

**Critérios de aceite:** README reflete a arquitetura atual; instruções de dev funcionam para um novo colaborador.

---

## Resumo

| Fase | Tarefas | Escopo |
|------|---------|--------|
| 0 — Infraestrutura | T-001 a T-012 | Dependências, Spring Security, Tailwind, HTMX, layouts, fragments base |
| 1 — Páginas simples | T-013 a T-016 | Login, Dashboard, Reports index, modais de perfil |
| 2 — CRUDs simples | T-017 a T-029 | Troncos, Planos, DIDs, Usuários |
| 3 — CRUDs complexos | T-030 a T-040 | Circuitos (1250 linhas), Clientes (1055 linhas) |
| 4 — CDRs e Relatórios | T-041 a T-048 | CDRs com 6 filtros, 3 relatórios + PDFs server-side |
| 5 — Cleanup | T-049 a T-052 | Remoção frontend, Docker, Nginx, docs |
| **Total** | **52 tarefas** | **~14-19 dias úteis** |
