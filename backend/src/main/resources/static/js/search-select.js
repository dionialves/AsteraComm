/**
 * SearchSelect — campo de busca + seleção com chip.
 *
 * Uso no template Thymeleaf:
 *   <div th:replace="~{fragments/search-select :: search-select(
 *       fieldName='planId',
 *       placeholder='Buscar plano...',
 *       selectedId=${obj?.plan?.id},
 *       selectedLabel=${obj?.plan?.name})}"></div>
 *
 * O fragment cria um [data-search-select] com os data-* necessários
 * e um <input type="hidden"> com o nome e valor do campo.
 *
 * O backend deve ter um SearchOptionsProvider registrado com o bean
 * name igual ao fieldName (ex: @Service("planId")).
 * O endpoint GET /fragments/search-options-json?field=X&q=Y retorna
 * List<{id, label}>.
 */
(function () {
  'use strict';

  class SearchSelect {
    constructor(host) {
      this._host  = host;
      this._field = host.dataset.fieldName;
      this._ph    = host.dataset.placeholder || 'Selecionar...';
      this._input = host.querySelector('input[type="hidden"]');
      this._value = (this._input && this._input.value) || '';
      this._label = host.dataset.selectedLabel || '';
      this._timer = null;
      this._isOpen = false;
      this._boundClose = this._onOutside.bind(this);
      this._build();
      if (this._value && this._label) this._lock();
    }

    // ── Construção do DOM ──────────────────────────────────────────────────

    _build() {
      // Trigger (estado inicial — nada selecionado)
      this._trigger = document.createElement('button');
      this._trigger.type = 'button';
      this._trigger.className = 'ss-trigger';
      this._trigger.addEventListener('click', () => this._toggle());

      this._triggerText = document.createElement('span');
      this._triggerText.className = 'ss-trigger-text ss-placeholder';
      this._triggerText.textContent = this._ph;

      this._arrow = document.createElement('span');
      this._arrow.className = 'ss-arrow';
      this._arrow.innerHTML = '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="6 9 12 15 18 9"/></svg>';

      this._trigger.appendChild(this._triggerText);
      this._trigger.appendChild(this._arrow);

      // Dropdown com campo de pesquisa
      this._dropdown = document.createElement('div');
      this._dropdown.className = 'ss-dropdown ss-hidden';

      this._searchInput = document.createElement('input');
      this._searchInput.type = 'text';
      this._searchInput.placeholder = 'Pesquisar...';
      this._searchInput.className = 'ss-search-input';
      this._searchInput.addEventListener('input', () => {
        clearTimeout(this._timer);
        this._timer = setTimeout(() => this._fetch(this._searchInput.value), 200);
      });

      this._list = document.createElement('ul');
      this._list.className = 'ss-options';
      this._list.addEventListener('click', function (e) {
        var li = e.target.closest('li[data-value]');
        if (li) this._select(li.dataset.value, li.dataset.label || li.textContent.trim());
      }.bind(this));

      this._dropdown.appendChild(this._searchInput);
      this._dropdown.appendChild(this._list);

      // Chip (estado selecionado)
      this._chip = document.createElement('div');
      this._chip.className = 'ss-chip ss-hidden';

      this._chipText = document.createElement('span');
      this._chipText.className = 'ss-chip-text';

      this._btnClear = document.createElement('button');
      this._btnClear.type = 'button';
      this._btnClear.className = 'ss-chip-clear';
      this._btnClear.title = 'Limpar seleção';
      this._btnClear.innerHTML = '<svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>';
      this._btnClear.addEventListener('click', () => this._clear());

      this._chip.appendChild(this._chipText);
      this._chip.appendChild(this._btnClear);

      // Inserir no host (que já contém o hidden input)
      this._host.appendChild(this._trigger);
      this._host.appendChild(this._dropdown);
      this._host.appendChild(this._chip);
    }

    // ── Abrir / fechar dropdown ────────────────────────────────────────────

    _toggle() {
      this._isOpen ? this._close() : this._open();
    }

    _open() {
      this._isOpen = true;
      this._dropdown.classList.remove('ss-hidden');
      this._searchInput.value = '';
      this._fetch('');
      requestAnimationFrame(() => this._searchInput.focus());
      document.addEventListener('mousedown', this._boundClose);
    }

    _close() {
      this._isOpen = false;
      this._dropdown.classList.add('ss-hidden');
      document.removeEventListener('mousedown', this._boundClose);
    }

    // ── Seleção / limpeza ──────────────────────────────────────────────────

    _select(value, label) {
      this._value = value;
      this._label = label;
      if (this._input) this._input.value = value;
      this._close();
      this._lock();
    }

    _clear() {
      this._value = '';
      this._label = '';
      if (this._input) this._input.value = '';
      this._unlock();
    }

    // ── Estados visuais ────────────────────────────────────────────────────

    _lock() {
      this._chipText.textContent = this._label;
      this._chip.classList.remove('ss-hidden');
      this._trigger.classList.add('ss-hidden');
    }

    _unlock() {
      this._chip.classList.add('ss-hidden');
      this._trigger.classList.remove('ss-hidden');
    }

    // ── Busca de opções via API ────────────────────────────────────────────

    _fetch(q) {
      var url = '/fragments/search-options-json'
              + '?field=' + encodeURIComponent(this._field)
              + '&q='     + encodeURIComponent(q || '');
      fetch(url, { credentials: 'same-origin' })
        .then(function (r) { return r.json(); })
        .then(function (opts) { this._renderOpts(opts); }.bind(this))
        .catch(function () { this._renderOpts([]); }.bind(this));
    }

    _renderOpts(opts) {
      this._list.innerHTML = '';
      if (!opts || !opts.length) {
        var empty = document.createElement('li');
        empty.className = 'ss-empty';
        empty.textContent = 'Nenhum resultado encontrado';
        this._list.appendChild(empty);
        return;
      }
      opts.forEach(function (opt) {
        var li = document.createElement('li');
        li.dataset.value = opt.id;
        li.dataset.label = opt.label;
        li.textContent   = opt.label;
        li.className     = 'ss-option';
        this._list.appendChild(li);
      }.bind(this));
    }

    _onOutside(e) {
      if (!this._host.contains(e.target)) this._close();
    }
  }

  // ── Auto-inicialização ─────────────────────────────────────────────────────

  function initAll(root) {
    (root || document).querySelectorAll('[data-search-select]').forEach(function (host) {
      if (!host._ss) host._ss = new SearchSelect(host);
    });
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', function () { initAll(document); });
  } else {
    initAll(document);
  }

  // Re-inicializa após qualquer swap do HTMX (modais, tabs etc.)
  document.body.addEventListener('htmx:afterSwap', function (e) {
    initAll(e.detail.target);
  });

})();
