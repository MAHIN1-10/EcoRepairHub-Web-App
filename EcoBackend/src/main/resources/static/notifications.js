// Live notifications for the bell dropdown (all user pages) and the full list (user-notifications.html).
(function () {
  const API = "/api/notifications";
  const user = JSON.parse(localStorage.getItem("ecorepair_user") || "null");
  if (!user || !user.email) return;

  const ICONS = { pickup: "truck", certificate: "file-badge", reward: "star", campaign: "calendar", info: "bell" };
  const q = "?email=" + encodeURIComponent(user.email);

  const style = document.createElement("style");
  style.textContent = `
    .notif-dot.is-empty { display: none; }
    .notif-dropdown { max-height: 380px; overflow-y: auto; width: 320px; }
    .notif-item { cursor: pointer; }
    .notif-item.unread { background: var(--green-light, #ecfdf5); border-left: 3px solid var(--green, #10b981); }
    .notif-item .ni-title { font-weight: 600; }
    .notif-item .ni-time { font-size: 12px; color: #9ca3af; margin-top: 2px; }
    .notif-empty { padding: 16px; font-size: 14px; color: #6b7280; text-align: center; }
    .notif-page-actions { display: flex; justify-content: flex-end; margin-bottom: 12px; }
    .notif-mark-all { background: none; border: 1px solid #d1d5db; border-radius: 8px; padding: 6px 12px;
      font-size: 13px; font-weight: 600; cursor: pointer; color: inherit; }
    .notif-mark-all:hover { background: #f3f4f6; }
    .notif-card[data-id] { cursor: pointer; }
  `;
  document.head.appendChild(style);

  const esc = (s) => String(s).replace(/[&<>"']/g, (c) =>
    ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));

  function ago(iso) {
    const sec = Math.max(0, (Date.now() - new Date(iso).getTime()) / 1000);
    if (sec < 60) return "just now";
    if (sec < 3600) return Math.floor(sec / 60) + " min ago";
    if (sec < 86400) return Math.floor(sec / 3600) + " hr ago";
    if (sec < 2592000) return Math.floor(sec / 86400) + " day(s) ago";
    return new Date(iso).toLocaleDateString();
  }

  function fullDate(iso) {
    return new Date(iso).toLocaleDateString(undefined, { month: "short", day: "numeric", year: "numeric" });
  }

  async function markRead(id) {
    try { await fetch(`${API}/${id}/read${q}`, { method: "PUT" }); } catch (e) { /* ignore */ }
    load();
  }

  async function markAll() {
    try { await fetch(`${API}/read-all${q}`, { method: "PUT" }); } catch (e) { /* ignore */ }
    load();
  }

  function renderBadge(unread) {
    document.querySelectorAll(".notif-dot").forEach((dot) => {
      dot.textContent = unread > 9 ? "9+" : unread;
      dot.classList.toggle("is-empty", unread === 0);
    });
  }

  function renderDropdown(items) {
    const dd = document.getElementById("notifDropdown");
    if (!dd) return;
    const rows = items.slice(0, 5).map((n) => `
      <div class="notif-item ${n.read ? "" : "unread"}" data-id="${n.id}">
        <div class="ni-title">${esc(n.title)}</div>
        <div>${esc(n.message)}</div>
        <div class="ni-time">${ago(n.createdAt)}</div>
      </div>`).join("");
    dd.innerHTML = (rows || '<div class="notif-empty">No notifications yet</div>') +
      '<a href="' + (me.role === "COLLECTOR" ? "collector-notifications.html" : "user-notifications.html") + '" class="notif-all">See all notifications</a>';
    dd.querySelectorAll(".notif-item").forEach((el) =>
      el.addEventListener("click", () => markRead(el.dataset.id)));
  }

  function renderPage(items, unread) {
    const list = document.querySelector(".notif-list");
    if (!list) return;

    if (!document.querySelector(".notif-mark-all")) {
      const bar = document.createElement("div");
      bar.className = "notif-page-actions";
      bar.innerHTML = '<button class="notif-mark-all" type="button">Mark all as read</button>';
      list.parentNode.insertBefore(bar, list);
      bar.querySelector("button").addEventListener("click", markAll);
    }
    document.querySelector(".notif-mark-all").style.display = unread ? "" : "none";

    list.innerHTML = items.length ? items.map((n) => `
      <div class="notif-card ${n.read ? "" : "notif-unread"}" data-id="${n.id}">
        <div class="notif-icon"><i data-lucide="${ICONS[n.type] || "bell"}" style="width:20px;height:20px;"></i></div>
        <div class="notif-content">
          <div class="notif-title">${esc(n.title)}</div>
          <div class="notif-desc">${esc(n.message)}</div>
          <div class="notif-meta">${fullDate(n.createdAt)} • ${ago(n.createdAt)}</div>
        </div>
      </div>`).join("") : '<div class="notif-empty">You have no notifications yet.</div>';

    list.querySelectorAll(".notif-card[data-id]").forEach((el) =>
      el.addEventListener("click", () => { if (el.classList.contains("notif-unread")) markRead(el.dataset.id); }));
    if (window.lucide) window.lucide.createIcons();
  }

  async function load() {
    try {
      const res = await fetch(API + q);
      if (!res.ok) return;
      const data = await res.json();
      renderBadge(data.unread);
      renderDropdown(data.items);
      renderPage(data.items, data.unread);
    } catch (e) {
      console.error("Could not load notifications:", e);
    }
  }

  document.addEventListener("DOMContentLoaded", () => {
    load();
    setInterval(load, 10000);
  });
})();
