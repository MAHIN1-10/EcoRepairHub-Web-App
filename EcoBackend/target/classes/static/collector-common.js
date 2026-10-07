// Shared bits for the collector pages: login guard, profile name/avatar, logout.
(function () {
  const user = JSON.parse(localStorage.getItem("ecorepair_user") || "null");
  if (!user || !user.email) {
    window.location.href = "Login.html";
    return;
  }

  document.addEventListener("DOMContentLoaded", () => {
    const name = user.fullName || user.email;
    const initials = name.split(/\s+/).filter(Boolean).slice(0, 2).map(w => w[0].toUpperCase()).join("") || "U";

    const set = (sel, text) => document.querySelectorAll(sel).forEach(el => { el.textContent = text; });
    set("#profileBtn", initials);
    set(".profile-avatar", initials);
    set(".profile-name", name);
    set(".profile-email", user.email);

    document.querySelectorAll(".profile-menu-item.logout").forEach(el =>
      el.addEventListener("click", () => localStorage.removeItem("ecorepair_user")));
  });
})();
