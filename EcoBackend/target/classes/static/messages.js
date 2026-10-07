// Live chat between a customer and their collector. Used by user-messages.html and collector-messages.html.
(function () {
  const API = "/api/messages";
  const me = JSON.parse(localStorage.getItem("ecorepair_user") || "null");
  if (!me || !me.email) return;

  const $ = (sel) => document.querySelector(sel);
  const esc = (s) => String(s).replace(/[&<>"']/g, (c) =>
    ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));

  let conversations = [];
  let activeEmail = new URLSearchParams(location.search).get("with");
  let lastThreadKey = "";

  function fmtTime(iso) {
    if (!iso) return "";
    const d = new Date(iso), now = new Date();
    if (d.toDateString() === now.toDateString()) {
      return d.toLocaleTimeString([], { hour: "numeric", minute: "2-digit" });
    }
    const y = new Date(now); y.setDate(now.getDate() - 1);
    if (d.toDateString() === y.toDateString()) return "Yesterday";
    return d.toLocaleDateString([], { month: "short", day: "numeric" });
  }

  const roleLabel = (r) => (r === "COLLECTOR" ? "Collector" : r === "ADMIN" ? "Support" : "Customer");
  // the other side of a customer chat is a collector for users, a customer for collectors
  const avatarClass = me.role === "COLLECTOR" ? "user" : "orange";

  async function loadConversations() {
    try {
      const res = await fetch(`${API}/conversations?email=${encodeURIComponent(me.email)}` +
        (activeEmail ? `&with=${encodeURIComponent(activeEmail)}` : ""));
      if (!res.ok) return;
      conversations = await res.json();
    } catch (e) { console.error(e); return; }

    if (!activeEmail && conversations.length) activeEmail = conversations[0].email;
    renderSidebar();
    renderHeader();
  }

  function renderSidebar() {
    const list = $(".conv-list");
    if (!list) return;

    if (!conversations.length) {
      list.innerHTML = `<div style="padding:20px;font-size:14px;color:#6b7280;">
        No conversations yet. You can chat with the ${me.role === "COLLECTOR" ? "customer" : "collector"} once a pickup request is accepted.</div>`;
      return;
    }

    list.innerHTML = conversations.map((c) => `
      <div class="conv-item ${c.email.toLowerCase() === (activeEmail || "").toLowerCase() ? "active" : ""}" data-email="${esc(c.email)}">
        <div class="conv-avatar ${avatarClass}"><i data-lucide="user"></i></div>
        <div class="conv-body">
          <div class="conv-top">
            <span class="conv-name">${esc(c.name)}</span>
            <span class="conv-time">${fmtTime(c.lastTime)}</span>
          </div>
          <div class="conv-preview">${c.lastMessage ? esc(c.lastMessage) : "Say hello 👋"}</div>
        </div>
        ${c.unread > 0 ? `<div class="unread-badge">${c.unread}</div>` : ""}
      </div>`).join("");

    list.querySelectorAll(".conv-item").forEach((el) =>
      el.addEventListener("click", () => selectConversation(el.dataset.email)));
    if (window.lucide) window.lucide.createIcons();
  }

  function renderHeader() {
    const c = conversations.find((x) => x.email.toLowerCase() === (activeEmail || "").toLowerCase());
    const nameEl = $(".chat-header-name"), roleEl = $(".chat-header-role");
    if (nameEl) nameEl.textContent = c ? c.name : "No conversation selected";
    if (roleEl) roleEl.textContent = c ? roleLabel(c.role) : "";
    const input = $(".msg-input"), btn = $(".btn-send");
    if (input) input.disabled = !c;
    if (btn) btn.disabled = !c;
  }

  async function loadThread(force) {
    const area = $(".messages-area");
    if (!area) return;
    if (!activeEmail) {
      area.innerHTML = '<div style="padding:24px;color:#6b7280;font-size:14px;">Select a conversation to start chatting.</div>';
      return;
    }
    let msgs = [];
    try {
      const res = await fetch(`${API}/thread?email=${encodeURIComponent(me.email)}&with=${encodeURIComponent(activeEmail)}`);
      if (!res.ok) return;
      msgs = await res.json();
    } catch (e) { console.error(e); return; }

    const key = activeEmail + "|" + msgs.length + "|" + (msgs.length ? msgs[msgs.length - 1].id : 0);
    if (!force && key === lastThreadKey) return; // nothing new
    lastThreadKey = key;

    const nearBottom = area.scrollHeight - area.scrollTop - area.clientHeight < 80;
    area.innerHTML = msgs.length ? msgs.map((m) => `
      <div class="msg-group ${m.mine ? "msg-sent" : "msg-received"}">
        <div class="bubble">${esc(m.content)}</div>
        <div class="msg-time">${new Date(m.createdAt).toLocaleTimeString([], { hour: "numeric", minute: "2-digit" })}</div>
      </div>`).join("") :
      '<div style="padding:24px;color:#6b7280;font-size:14px;">No messages yet. Say hello!</div>';
    if (force || nearBottom) area.scrollTop = area.scrollHeight;
  }

  async function selectConversation(email) {
    activeEmail = email;
    renderSidebar();
    renderHeader();
    await loadThread(true);
    loadConversations(); // clears the unread badge
  }

  async function send() {
    const input = $(".msg-input");
    const content = input.value.trim();
    if (!content || !activeEmail) return;
    input.value = "";
    try {
      const res = await fetch(API, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ from: me.email, to: activeEmail, content })
      });
      if (!res.ok) {
        const body = await res.json().catch(() => ({}));
        alert(body.message || "Could not send your message.");
        input.value = content;
        return;
      }
    } catch (e) {
      console.error(e);
      alert("Could not connect to the server.");
      input.value = content;
      return;
    }
    await loadThread(true);
    loadConversations();
  }

  document.addEventListener("DOMContentLoaded", async () => {
    const input = $(".msg-input"), btn = $(".btn-send");
    btn?.addEventListener("click", send);
    input?.addEventListener("keydown", (e) => { if (e.key === "Enter") { e.preventDefault(); send(); } });

    await loadConversations();
    await loadThread(true);

    setInterval(async () => {
      await loadThread(false);
      loadConversations();
    }, 4000);
  });
})();
