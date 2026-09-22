/**
 * MicroVault — shared role-based sidebar (no fetch; works on file://)
 */
(function () {
  "use strict";

  const LINK_COLORS = {
    "dashboard.html": "nav-teal",
    "goal.html": "nav-amber",
    "savings.html": "nav-green",
    "transactions.html": "nav-sky",
    "budget.html": "nav-orange",
    "affordability.html": "nav-cyan",
    "notification.html": "nav-rose",
    "report.html": "nav-indigo",
    "help-support.html": "nav-lime",
    "feedback.html": "nav-pink",
    "news.html": "nav-cyan",
    "settings.html": "nav-slate",
    "admin-users.html": "nav-violet",
    "admin-overview.html": "nav-blue",
    "admin-feedback.html": "nav-fuchsia",
    "admin-news.html": "nav-amber",
    "superadmin-admins.html": "nav-gold",
    "logout.html": "nav-red",
  };

  function icon(name) {
    return '<i class="fas fa-' + name + '" aria-hidden="true"></i>';
  }

  function link(href, label, fa, page, index) {
    const active = page === href ? " active" : "";
    const color = LINK_COLORS[href] || "nav-teal";
    const delay = Math.min(index || 0, 14) * 0.04;
    return (
      '<li class="nav-item ' +
      color +
      active +
      '" style="--nav-i:' +
      delay +
      's">' +
      '<a href="' +
      href +
      '">' +
      '<span class="nav-ico">' +
      icon(fa) +
      "</span>" +
      "<span>" +
      label +
      "</span>" +
      "</a></li>"
    );
  }

  function section(title) {
    if (!title) return '<li class="nav-divider" aria-hidden="true"></li>';
    return '<li class="nav-section">' + title + "</li>";
  }

  function buildMemberNav(currentPage) {
    let html = "";
    let i = 0;

    html += section("Main");
    html += link("dashboard.html", "Dashboard", "gauge-high", currentPage, i++);
    html += link("goal.html", "Goals", "bullseye", currentPage, i++);
    html += link("savings.html", "Savings", "piggy-bank", currentPage, i++);
    html += link("transactions.html", "Transactions", "arrow-right-arrow-left", currentPage, i++);
    html += link("budget.html", "Budget", "chart-pie", currentPage, i++);
    html += link("affordability.html", "Affordability", "scale-balanced", currentPage, i++);
    html += link("notification.html", "Notifications", "bell", currentPage, i++);
    html += link("news.html", "News", "newspaper", currentPage, i++);
    html += link("report.html", "Reports", "file-lines", currentPage, i++);

    html += section("Support");
    html += link("feedback.html", "Feedback", "comment-dots", currentPage, i++);
    html += link("help-support.html", "Help & Support", "headset", currentPage, i++);
    html += link("settings.html", "Settings", "gear", currentPage, i++);

    html += section("");
    html += link("logout.html", "Logout", "right-from-bracket", currentPage, i++);

    return html;
  }

  function buildAdminNav(currentPage) {
    let html = "";
    let i = 0;

    html += section("Admin");
    html += link("admin-overview.html", "Dashboard", "gauge-high", currentPage, i++);
    html += link("admin-users.html", "Manage Users", "users", currentPage, i++);
    html += link("admin-feedback.html", "Feedback", "comments", currentPage, i++);
    html += link("admin-news.html", "News", "newspaper", currentPage, i++);

    html += section("Account");
    html += link("settings.html", "Settings", "gear", currentPage, i++);
    html += link("help-support.html", "Help & Support", "headset", currentPage, i++);

    html += section("");
    html += link("logout.html", "Logout", "right-from-bracket", currentPage, i++);

    return html;
  }

  function buildSuperAdminNav(currentPage) {
    let html = "";
    let i = 0;

    html += section("Admin");
    html += link("admin-overview.html", "Dashboard", "gauge-high", currentPage, i++);
    html += link("admin-users.html", "Manage Users", "users", currentPage, i++);
    html += link("admin-feedback.html", "Feedback", "comments", currentPage, i++);
    html += link("admin-news.html", "News", "newspaper", currentPage, i++);

    html += section("Super Admin");
    html += link("superadmin-admins.html", "Manage Admins", "user-shield", currentPage, i++);

    html += section("Account");
    html += link("settings.html", "Settings", "gear", currentPage, i++);
    html += link("help-support.html", "Help & Support", "headset", currentPage, i++);

    html += section("");
    html += link("logout.html", "Logout", "right-from-bracket", currentPage, i++);

    return html;
  }

  function buildNav(user, currentPage) {
    const role = (user && user.role) || MV.ROLES.USER;
    if (role === MV.ROLES.SUPERADMIN) return buildSuperAdminNav(currentPage);
    if (role === MV.ROLES.ADMIN) return buildAdminNav(currentPage);
    return buildMemberNav(currentPage);
  }

  function renderSidebar(currentPage) {
    const user = MV.getCurrentUser();
    if (!user) return;

    let host = document.getElementById("app-sidebar");
    if (!host) {
      host = document.createElement("aside");
      host.id = "app-sidebar";
      host.className = "sidebar";
      const dash = document.querySelector(".dashboard") || document.body;
      dash.insertBefore(host, dash.firstChild);
    }

    const roleLabel =
      user.role === MV.ROLES.SUPERADMIN
        ? "Super Admin"
        : user.role === MV.ROLES.ADMIN
          ? "Admin"
          : "Member";

    const roleClass =
      user.role === MV.ROLES.SUPERADMIN
        ? "role-super"
        : user.role === MV.ROLES.ADMIN
          ? "role-admin"
          : "role-member";

    host.className = "sidebar " + roleClass;
    host.innerHTML =
      '<div class="sidebar-brand">' +
      '<div class="brand-mark"><i class="fas fa-vault"></i></div>' +
      "<div><strong>MicroVault</strong><small class=\"role-chip\">" +
      roleLabel +
      "</small></div>" +
      '<button type="button" class="sidebar-close" id="sidebarClose" aria-label="Close menu"><i class="fas fa-times"></i></button>' +
      "</div>" +
      '<div class="sidebar-user">' +
      '<div class="avatar">' +
      (user.fullName || "U").charAt(0).toUpperCase() +
      "</div>" +
      "<div><strong>" +
      (user.fullName || "User") +
      "</strong><small>" +
      (user.email || "") +
      "</small></div></div>" +
      '<nav><ul class="sidebar-nav">' +
      buildNav(user, currentPage) +
      "</ul></nav>";

    const closeBtn = document.getElementById("sidebarClose");
    if (closeBtn) {
      closeBtn.addEventListener("click", () => document.body.classList.remove("sidebar-open"));
    }
  }

  function ensureTopbar(title) {
    const main = document.querySelector(".main-content");
    if (!main) return;
    if (document.querySelector(".topbar")) return;

    const bar = document.createElement("div");
    bar.className = "topbar";
    bar.innerHTML =
      '<button type="button" class="menu-toggle" id="menuToggle" aria-label="Open menu"><i class="fas fa-bars"></i></button>' +
      '<div class="topbar-title">' +
      (title || document.title.replace("MicroVault", "").replace("|", "").trim() || "Dashboard") +
      "</div>" +
      '<div class="topbar-date"><i class="fas fa-calendar-day"></i> <span id="currentDate"></span></div>';
    main.insertBefore(bar, main.firstChild);

    const toggle = document.getElementById("menuToggle");
    if (toggle) {
      toggle.addEventListener("click", () => document.body.classList.toggle("sidebar-open"));
    }
    const dateEl = document.getElementById("currentDate");
    if (dateEl) {
      dateEl.textContent = new Date().toLocaleDateString("en-IN", {
        weekday: "short",
        day: "numeric",
        month: "short",
        year: "numeric",
      });
    }
  }

  async function initLayout(options) {
    options = options || {};
    const roles = options.roles;
    const loginUrl = options.loginUrl || "../html/login.html";
    const user = MV.requireAuth({ roles: roles, loginUrl: loginUrl });
    if (!user) return null;

    try {
      await MV.bootstrap();
    } catch (e) {
      console.error(e);
      if (e.status === 401) {
        window.location.href = loginUrl;
        return null;
      }
    }

    if (MV.isFinanceSetupComplete(user.id)) {
      MV.clearSetupSkipped(user.id);
      MV.ensureDemoData(user.id);
    }

    MV.bindPasswordToggles();

    const page = options.page || (location.pathname.split("/").pop() || "dashboard.html");
    renderSidebar(page);
    ensureTopbar(options.title);

    if (!document.querySelector(".sidebar-overlay")) {
      const ov = document.createElement("div");
      ov.className = "sidebar-overlay";
      ov.addEventListener("click", () => document.body.classList.remove("sidebar-open"));
      document.body.appendChild(ov);
    }

    MV.bindMaxDate('input[type="date"]');
    document.body.classList.add("page-ready");

    if (MV.needsFinanceSetup(user) && page === "dashboard.html") {
      setTimeout(() => {
        MV.promptFinanceSetup({
          setupUrl: "personal-financial-setup.html",
          message:
            "You skipped Personal Financial Setup. Complete it to unlock dashboard data, charts, savings, and reports.",
        });
      }, 250);
    }

    return user;
  }

  window.MVSidebar = { initLayout, renderSidebar };
})();
