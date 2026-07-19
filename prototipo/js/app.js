/* ================================================================
   CompraCerta — comportamentos compartilhados do protótipo
   Os dados são simulados e salvos no localStorage do navegador.
   ================================================================ */

const App = (() => {
  const STORAGE_KEY = "compracerta-prototype";
  const defaultState = {
    lists: [
      { id: 1, name: "Compras da semana", total: 12, done: 4, shared: true, status: "active", updated: "Hoje, 09:42" },
      { id: 2, name: "Churrasco de sábado", total: 9, done: 1, shared: true, status: "active", updated: "Ontem, 18:20" },
      { id: 3, name: "Farmácia e higiene", total: 6, done: 6, shared: false, status: "completed", updated: "12 jul, 16:08" }
    ],
    categories: [
      { id: 1, name: "Hortifruti", icon: "🥬", products: 8 },
      { id: 2, name: "Mercearia", icon: "🛍️", products: 14 },
      { id: 3, name: "Bebidas", icon: "🧃", products: 7 },
      { id: 4, name: "Limpeza", icon: "🧴", products: 5 }
    ],
    products: [
      { id: 1, name: "Arroz integral", category: "Mercearia", unit: "pacote", icon: "🛍️" },
      { id: 2, name: "Banana prata", category: "Hortifruti", unit: "quilograma", icon: "🥬" },
      { id: 3, name: "Leite sem lactose", category: "Bebidas", unit: "litro", icon: "🧃" },
      { id: 4, name: "Detergente neutro", category: "Limpeza", unit: "frasco", icon: "🧴" }
    ],
    participants: [
      { id: 1, name: "Marcos Silva", email: "marcos@email.com", initials: "MS", status: "active" },
      { id: 2, name: "Ana Rodrigues", email: "ana@email.com", initials: "AR", status: "active" },
      { id: 3, name: "Paula Nunes", email: "paula@email.com", initials: "PN", status: "pending" }
    ]
  };

  let state = loadState();

  function loadState() {
    try { return { ...defaultState, ...JSON.parse(localStorage.getItem(STORAGE_KEY)) }; }
    catch { return structuredClone(defaultState); }
  }

  function saveState() { localStorage.setItem(STORAGE_KEY, JSON.stringify(state)); }
  function escapeHTML(value = "") {
    return String(value).replace(/[&<>'"]/g, char => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", "'": "&#39;", '"': "&quot;" }[char]));
  }

  function toast(message, type = "success") {
    let element = document.querySelector(".toast");
    if (!element) {
      element = document.createElement("div");
      element.className = "toast";
      element.setAttribute("role", "status");
      document.body.append(element);
    }
    element.className = `toast ${type === "error" ? "error" : ""}`.trim();
    element.textContent = `${type === "error" ? "⚠" : "✓"} ${message}`;
    requestAnimationFrame(() => element.classList.add("show"));
    clearTimeout(element.timer);
    element.timer = setTimeout(() => element.classList.remove("show"), 3000);
  }

  function initShell() {
    const sidebar = document.querySelector(".sidebar");
    const overlay = document.querySelector(".mobile-overlay");
    const toggle = document.querySelector(".menu-toggle");
    const closeMenu = () => { sidebar?.classList.remove("open"); overlay?.classList.remove("show"); };
    toggle?.addEventListener("click", () => { sidebar?.classList.toggle("open"); overlay?.classList.toggle("show"); });
    overlay?.addEventListener("click", closeMenu);

    document.querySelectorAll("[data-dropdown]").forEach(dropdown => {
      const button = dropdown.querySelector("button");
      button?.addEventListener("click", event => { event.stopPropagation(); dropdown.classList.toggle("open"); });
    });
    document.addEventListener("click", () => document.querySelectorAll(".dropdown.open").forEach(el => el.classList.remove("open")));
  }

  function initPasswordToggles() {
    document.querySelectorAll("[data-password-toggle]").forEach(button => {
      button.addEventListener("click", () => {
        const input = document.getElementById(button.dataset.passwordToggle);
        if (!input) return;
        input.type = input.type === "password" ? "text" : "password";
        button.textContent = input.type === "password" ? "Mostrar" : "Ocultar";
      });
    });
  }

  function setError(input, message = "") {
    input.setAttribute("aria-invalid", String(Boolean(message)));
    const error = input.closest(".field")?.querySelector(".field-error");
    if (error) error.textContent = message;
  }

  function initLogin() {
    const form = document.querySelector("#login-form");
    if (!form) return;
    const email = form.elements.email;
    const password = form.elements.password;
    const submit = form.querySelector("[type=submit]");
    const update = () => { submit.disabled = !(email.value.trim() && password.value); };
    form.addEventListener("input", update);
    form.addEventListener("submit", event => {
      event.preventDefault();
      const validEmail = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.value);
      setError(email, validEmail ? "" : "Informe um e-mail válido.");
      setError(password, password.value ? "" : "Informe sua senha.");
      if (!validEmail || !password.value) return;
      submit.textContent = "Entrando…";
      setTimeout(() => { window.location.href = "listas.html"; }, 450);
    });
    update();
  }

  function initSignup() {
    const form = document.querySelector("#signup-form");
    if (!form) return;
    form.addEventListener("submit", event => {
      event.preventDefault();
      const data = new FormData(form);
      let valid = true;
      [...form.querySelectorAll("input[required]")].forEach(input => {
        const message = input.value.trim() ? "" : "Campo obrigatório.";
        setError(input, message); valid = valid && !message;
      });
      const email = form.elements.email;
      if (email.value && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.value)) { setError(email, "Informe um e-mail válido."); valid = false; }
      const password = form.elements.password;
      const confirm = form.elements.confirm;
      if (password.value.length < 6) { setError(password, "Use pelo menos 6 caracteres."); valid = false; }
      if (password.value !== confirm.value) { setError(confirm, "As senhas não coincidem."); valid = false; }
      if (!valid) return;
      localStorage.setItem("compracerta-user", data.get("name"));
      toast("Conta criada! Preparando suas categorias iniciais…");
      setTimeout(() => { window.location.href = "listas.html"; }, 700);
    });
  }

  function initFilters() {
    document.querySelectorAll("[data-filter-group]").forEach(group => {
      group.addEventListener("click", event => {
        const button = event.target.closest("[data-filter]");
        if (!button) return;
        group.querySelectorAll("[data-filter]").forEach(el => el.classList.remove("active"));
        button.classList.add("active");
        const target = button.dataset.filter;
        document.querySelectorAll("[data-status]").forEach(card => card.classList.toggle("hide", target !== "all" && card.dataset.status !== target));
      });
    });
    document.querySelectorAll("[data-search]").forEach(input => {
      input.addEventListener("input", () => {
        const query = input.value.trim().toLocaleLowerCase("pt-BR");
        document.querySelectorAll(input.dataset.search).forEach(row => row.classList.toggle("hide", !row.textContent.toLocaleLowerCase("pt-BR").includes(query)));
      });
    });
  }

  function initListForm() {
    const form = document.querySelector("#list-form");
    if (!form) return;
    form.addEventListener("submit", event => {
      event.preventDefault();
      const name = form.elements.name;
      if (!name.value.trim()) return setError(name, "Dê um nome à lista.");
      setError(name);
      const duplicate = state.lists.some(item => item.name.toLowerCase() === name.value.trim().toLowerCase());
      if (duplicate) return setError(name, "Já existe uma lista com esse nome.");
      state.lists.unshift({ id: Date.now(), name: name.value.trim(), total: 0, done: 0, shared: false, status: "active", updated: "Agora" });
      saveState();
      toast("Lista criada com sucesso.");
      setTimeout(() => { window.location.href = "lista-detalhes.html"; }, 500);
    });
  }

  function updateProgress() {
    const items = [...document.querySelectorAll(".shopping-item")];
    if (!items.length) return;
    const done = items.filter(item => item.querySelector("input[type=checkbox]")?.checked).length;
    document.querySelector("[data-total]").textContent = items.length;
    document.querySelector("[data-done]").textContent = done;
    document.querySelector("[data-pending]").textContent = items.length - done;
    document.querySelector("[data-progress]").style.width = `${Math.round(done / items.length * 100)}%`;
  }

  function initShoppingList() {
    document.querySelectorAll(".shopping-item input[type=checkbox]").forEach(box => {
      box.addEventListener("change", () => { box.closest(".shopping-item").classList.toggle("is-done", box.checked); updateProgress(); });
    });
    document.querySelectorAll("[data-collapse]").forEach(button => button.addEventListener("click", () => {
      const section = button.closest(".category-section");
      section.querySelector(".item-list").classList.toggle("hide");
      button.querySelector("[data-chevron]").textContent = section.querySelector(".item-list").classList.contains("hide") ? "⌄" : "⌃";
    }));
    updateProgress();
  }

  function initItemForm() {
    const form = document.querySelector("#item-form");
    if (!form) return;
    const product = form.elements.product;
    const suggestions = document.querySelector("#product-suggestions");
    product.addEventListener("input", () => {
      const query = product.value.toLowerCase();
      suggestions.innerHTML = state.products.filter(item => item.name.toLowerCase().includes(query)).slice(0, 4)
        .map(item => `<button type="button" class="data-row" data-product="${escapeHTML(item.name)}" data-category="${escapeHTML(item.category)}" data-unit="${escapeHTML(item.unit)}"><span class="item-icon">${item.icon}</span><span class="data-main"><strong>${escapeHTML(item.name)}</strong><small>${escapeHTML(item.category)} · ${escapeHTML(item.unit)}</small></span></button>`).join("");
      suggestions.classList.toggle("hide", !query || !suggestions.innerHTML);
    });
    suggestions.addEventListener("click", event => {
      const button = event.target.closest("[data-product]");
      if (!button) return;
      product.value = button.dataset.product;
      form.elements.category.value = button.dataset.category;
      form.elements.unit.value = button.dataset.unit;
      suggestions.classList.add("hide");
    });
    form.addEventListener("submit", event => {
      event.preventDefault();
      if (!product.value.trim()) return setError(product, "Escolha ou cadastre um produto.");
      if (product.value.toLowerCase().includes("arroz")) document.querySelector("#duplicate-dialog")?.showModal();
      else { toast("Item adicionado à lista."); setTimeout(() => window.location.href = "lista-detalhes.html", 500); }
    });
  }

  function renderCategories() {
    const list = document.querySelector("#category-list");
    if (!list) return;
    list.innerHTML = state.categories.map(category => `<li class="data-row category-row" data-id="${category.id}"><span class="category-icon">${category.icon}</span><span class="data-main"><strong>${escapeHTML(category.name)}</strong><small>${category.products} produto${category.products === 1 ? "" : "s"} associado${category.products === 1 ? "" : "s"}</small></span><span class="data-actions"><button class="icon-button" data-edit aria-label="Editar ${escapeHTML(category.name)}">✎</button><button class="icon-button btn-danger" data-delete aria-label="Excluir ${escapeHTML(category.name)}">×</button></span></li>`).join("");
  }

  function initCategories() {
    const list = document.querySelector("#category-list");
    const dialog = document.querySelector("#category-dialog");
    const form = document.querySelector("#category-form");
    if (!list || !dialog || !form) return;
    renderCategories();
    document.querySelector("[data-new-category]").addEventListener("click", () => { form.reset(); form.dataset.id = ""; dialog.querySelector("h2").textContent = "Nova categoria"; dialog.showModal(); });
    list.addEventListener("click", event => {
      const row = event.target.closest("[data-id]"); if (!row) return;
      const category = state.categories.find(item => item.id === Number(row.dataset.id));
      if (event.target.closest("[data-edit]")) { form.elements.name.value = category.name; form.elements.icon.value = category.icon; form.dataset.id = category.id; dialog.querySelector("h2").textContent = "Editar categoria"; dialog.showModal(); }
      if (event.target.closest("[data-delete]")) {
        if (category.products) return toast("Mova os produtos antes de excluir esta categoria.", "error");
        state.categories = state.categories.filter(item => item.id !== category.id); saveState(); renderCategories(); toast("Categoria excluída.");
      }
    });
    form.addEventListener("submit", event => {
      event.preventDefault();
      const name = form.elements.name.value.trim();
      if (!name) return;
      const id = Number(form.dataset.id);
      const duplicate = state.categories.some(item => item.name.toLowerCase() === name.toLowerCase() && item.id !== id);
      if (duplicate) return setError(form.elements.name, "Essa categoria já existe.");
      if (id) Object.assign(state.categories.find(item => item.id === id), { name, icon: form.elements.icon.value });
      else state.categories.push({ id: Date.now(), name, icon: form.elements.icon.value, products: 0 });
      saveState(); renderCategories(); dialog.close(); toast(id ? "Categoria atualizada." : "Categoria criada.");
    });
  }

  function renderProducts() {
    const list = document.querySelector("#product-list");
    if (!list) return;
    const category = document.querySelector("#category-filter")?.value || "all";
    list.innerHTML = state.products.filter(item => category === "all" || item.category === category).map(product => `<li class="data-row product-row" data-id="${product.id}"><span class="item-icon">${product.icon}</span><span class="data-main"><strong>${escapeHTML(product.name)}</strong><small>${escapeHTML(product.category)} · unidade padrão: ${escapeHTML(product.unit)}</small></span><span class="data-actions"><button class="icon-button" data-edit-product aria-label="Editar ${escapeHTML(product.name)}">✎</button><button class="icon-button btn-danger" data-disable-product aria-label="Desativar ${escapeHTML(product.name)}">×</button></span></li>`).join("");
  }

  function initProducts() {
    const list = document.querySelector("#product-list");
    const dialog = document.querySelector("#product-dialog");
    const form = document.querySelector("#product-form");
    if (!list || !dialog || !form) return;
    renderProducts();
    document.querySelector("#category-filter")?.addEventListener("change", renderProducts);
    document.querySelector("[data-new-product]").addEventListener("click", () => { form.reset(); form.dataset.id = ""; dialog.querySelector("h2").textContent = "Novo produto"; dialog.showModal(); });
    list.addEventListener("click", event => {
      const row = event.target.closest("[data-id]"); if (!row) return;
      const product = state.products.find(item => item.id === Number(row.dataset.id));
      if (event.target.closest("[data-edit-product]")) { form.elements.name.value = product.name; form.elements.category.value = product.category; form.elements.unit.value = product.unit; form.dataset.id = product.id; dialog.querySelector("h2").textContent = "Editar produto"; dialog.showModal(); }
      if (event.target.closest("[data-disable-product]")) { state.products = state.products.filter(item => item.id !== product.id); saveState(); renderProducts(); toast("Produto desativado para novos usos."); }
    });
    form.addEventListener("submit", event => {
      event.preventDefault();
      const name = form.elements.name.value.trim(); if (!name) return;
      const id = Number(form.dataset.id); const category = form.elements.category.value; const unit = form.elements.unit.value;
      const icon = state.categories.find(item => item.name === category)?.icon || "🛒";
      if (id) Object.assign(state.products.find(item => item.id === id), { name, category, unit, icon });
      else state.products.push({ id: Date.now(), name, category, unit, icon });
      saveState(); renderProducts(); dialog.close(); toast(id ? "Produto atualizado." : "Produto criado.");
    });
  }

  function renderParticipants() {
    const list = document.querySelector("#participant-list");
    if (!list) return;
    list.innerHTML = state.participants.map(person => `<li class="data-row" data-id="${person.id}"><span class="avatar">${escapeHTML(person.initials)}</span><span class="data-main"><strong>${escapeHTML(person.name)}</strong><small>${escapeHTML(person.email)}</small></span><span class="badge ${person.status === "pending" ? "warning" : ""}">${person.status === "pending" ? "Convite pendente" : "Pode editar"}</span><button class="icon-button btn-danger" data-remove-person aria-label="Remover ${escapeHTML(person.name)}">×</button></li>`).join("");
  }

  function initSharing() {
    const form = document.querySelector("#invite-form"); const list = document.querySelector("#participant-list");
    if (!form || !list) return;
    renderParticipants();
    form.addEventListener("submit", event => {
      event.preventDefault(); const email = form.elements.email.value.trim().toLowerCase();
      if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) return toast("Digite um e-mail válido.", "error");
      if (state.participants.some(item => item.email === email)) return toast("Esse e-mail já participa da lista.", "error");
      state.participants.push({ id: Date.now(), name: email.split("@")[0], email, initials: email.slice(0,2).toUpperCase(), status: "pending" });
      saveState(); renderParticipants(); form.reset(); toast("Convite enviado.");
    });
    list.addEventListener("click", event => {
      const button = event.target.closest("[data-remove-person]"); if (!button) return;
      const row = button.closest("[data-id]"); const person = state.participants.find(item => item.id === Number(row.dataset.id));
      document.querySelector("#remove-person-name").textContent = person.name;
      const dialog = document.querySelector("#remove-dialog"); dialog.dataset.id = person.id; dialog.showModal();
    });
    document.querySelector("#confirm-remove")?.addEventListener("click", () => {
      const dialog = document.querySelector("#remove-dialog"); state.participants = state.participants.filter(item => item.id !== Number(dialog.dataset.id));
      saveState(); renderParticipants(); dialog.close(); toast("Participante removido.");
    });
  }

  function initDialogs() {
    document.querySelectorAll("[data-close-dialog]").forEach(button => button.addEventListener("click", () => button.closest("dialog")?.close()));
    document.querySelectorAll("[data-open-dialog]").forEach(button => button.addEventListener("click", () => document.querySelector(button.dataset.openDialog)?.showModal()));
  }

  function init() {
    initShell(); initPasswordToggles(); initDialogs(); initLogin(); initSignup(); initFilters(); initListForm();
    initShoppingList(); initItemForm(); initCategories(); initProducts(); initSharing();
  }

  return { init, toast };
})();

// Disponibiliza apenas a API de feedback usada por alguns botões declarativos.
window.App = App;
document.addEventListener("DOMContentLoaded", App.init);
