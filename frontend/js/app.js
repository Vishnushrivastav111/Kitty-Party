/**
 * MicroVault — shared app core (localStorage, auth, CRUD helpers)
 */
(function (global) {
  "use strict";

  const KEYS = {
    users: "mv_users",
    currentUser: "mv_currentUser",
    isLoggedIn: "mv_isLoggedIn",
    finance: "mv_finance",
    transactions: "mv_transactions",
    goals: "mv_goals",
    savings: "mv_savings",
    budgets: "mv_budgets",
    notifications: "mv_notifications",
    reports: "mv_reports",
    affordChecks: "mv_affordChecks",
    feedback: "mv_feedback",
    news: "mv_news",
    registerDraft: "mv_registerDraft",
    termsAccepted: "mv_termsAccepted",
  };

  const ROLES = { USER: "user", ADMIN: "admin", SUPERADMIN: "superadmin" };

  const API_DEFAULT = "http://localhost:8080/api";

  function apiBase() {
    return (global.MV_API_BASE || API_DEFAULT).replace(/\/$/, "");
  }

  function authToken() {
    return localStorage.getItem("mv_token") || "";
  }

  async function apiRequest(path, options) {
    options = options || {};
    const headers = Object.assign({ "Content-Type": "application/json" }, options.headers || {});
    const t = authToken();
    if (t) headers.Authorization = "Bearer " + t;
    const response = await fetch(apiBase() + path, Object.assign({}, options, { headers }));
    const text = await response.text();
    let payload = null;
    try { payload = text ? JSON.parse(text) : null; } catch { payload = { ok: false, message: text }; }
    if (!response.ok) {
      const message = (payload && payload.message) || "Request failed";
      const err = new Error(message);
      err.status = response.status;
      err.payload = payload;
      throw err;
    }
    return payload;
  }

  function apiData(payload) {
    if (payload && Object.prototype.hasOwnProperty.call(payload, "data")) return payload.data;
    return payload;
  }

  const cache = {
    users: [],
    members: [],
    admins: [],
    finance: null,
    transactions: [],
    goals: [],
    savings: [],
    budgets: [],
    notifications: [],
    reports: [],
    affordChecks: [],
    feedback: [],
    allFeedback: [],
    news: [],
    allNews: [],
    insights: null,
    setupSkipped: false,
  };

  const COLLECTION_PATH = {
    [KEYS.transactions]: "/transactions",
    [KEYS.goals]: "/goals",
    [KEYS.savings]: "/savings",
    [KEYS.budgets]: "/budgets",
    [KEYS.notifications]: "/notifications",
    [KEYS.reports]: "/reports",
    [KEYS.affordChecks]: "/affordability",
  };

  function cacheKey(base) {
    if (base === KEYS.transactions) return "transactions";
    if (base === KEYS.goals) return "goals";
    if (base === KEYS.savings) return "savings";
    if (base === KEYS.budgets) return "budgets";
    if (base === KEYS.notifications) return "notifications";
    if (base === KEYS.reports) return "reports";
    if (base === KEYS.affordChecks) return "affordChecks";
    return base;
  }

  const SUPERADMIN = {
    id: "sa-001",
    fullName: "Super Admin",
    email: "supermicrovault@microvault.com",
    phone: "9999999999",
    password: "SuperAdmin@123",
    role: ROLES.SUPERADMIN,
    createdAt: "2024-01-01",
    status: "active",
  };

  /* ---------- storage ---------- */
  function get(key, fallback) {
    try {
      const raw = localStorage.getItem(key);
      if (raw === null || raw === undefined) return fallback;
      return JSON.parse(raw);
    } catch {
      return fallback;
    }
  }

  function set(key, value) {
    localStorage.setItem(key, JSON.stringify(value));
  }

  function uid(prefix) {
    return prefix + "-" + Date.now().toString(36) + Math.random().toString(36).slice(2, 7);
  }

  function todayISO() {
    const d = new Date();
    const m = String(d.getMonth() + 1).padStart(2, "0");
    const day = String(d.getDate()).padStart(2, "0");
    return d.getFullYear() + "-" + m + "-" + day;
  }

  function formatINR(n) {
    const num = Number(n) || 0;
    return "₹" + num.toLocaleString("en-IN", { maximumFractionDigits: 0 });
  }

  function formatDate(iso) {
    if (!iso) return "—";
    const d = new Date(iso + "T00:00:00");
    if (isNaN(d)) return iso;
    return d.toLocaleDateString("en-IN", { day: "2-digit", month: "short", year: "numeric" });
  }

  /* ---------- seed ---------- */
  function ensureSeed() {
    let users = get(KEYS.users, []);
    const hasSA = users.some((u) => u.email === SUPERADMIN.email);
    if (!hasSA) {
      users.push({ ...SUPERADMIN });
      set(KEYS.users, users);
    } else {
      users = users.map((u) =>
        u.email === SUPERADMIN.email ? { ...u, role: ROLES.SUPERADMIN, password: u.password || SUPERADMIN.password } : u
      );
      set(KEYS.users, users);
    }

    // Static demo members + admin so tables always have data
    const demoPeople = [
      {
        id: "usr-demo-1",
        fullName: "Aarav Sharma",
        email: "aarav.sharma@example.com",
        phone: "9876543210",
        password: "User@1234",
        role: ROLES.USER,
        createdAt: "2025-11-12",
        status: "active",
      },
      {
        id: "usr-demo-2",
        fullName: "Priya Patel",
        email: "priya.patel@example.com",
        phone: "9123456780",
        password: "User@1234",
        role: ROLES.USER,
        createdAt: "2025-12-03",
        status: "active",
      },
      {
        id: "usr-demo-3",
        fullName: "Rohan Mehta",
        email: "rohan.mehta@example.com",
        phone: "9988776655",
        password: "User@1234",
        role: ROLES.USER,
        createdAt: "2026-01-18",
        status: "inactive",
      },
      {
        id: "adm-demo-1",
        fullName: "Neha Admin",
        email: "admin@microvault.com",
        phone: "9001122334",
        password: "Admin@1234",
        role: ROLES.ADMIN,
        createdAt: "2025-10-01",
        status: "active",
      },
    ];
    users = get(KEYS.users, []);
    let changed = false;
    demoPeople.forEach((d) => {
      if (!users.some((u) => u.email === d.email)) {
        users.push(d);
        changed = true;
      }
    });
    if (changed) set(KEYS.users, users);
  }

  function daysAgo(n) {
    const d = new Date();
    d.setDate(d.getDate() - n);
    const m = String(d.getMonth() + 1).padStart(2, "0");
    const day = String(d.getDate()).padStart(2, "0");
    return d.getFullYear() + "-" + m + "-" + day;
  }

  /** Seed colorful static demo datasets only after financial setup is complete. */
  function ensureDemoData(userId) {
    return;
  }
  function getReportById(reportId) {
    return listFor(KEYS.reports).find((r) => r.id === reportId) || null;
  }

  function buildReportDownload(report) {
    if (!report) return null;
    const txs = listFor(KEYS.transactions).filter(
      (t) => t.date >= report.fromDate && t.date <= report.toDate
    );
    const lines = [
      "MicroVault Financial Report",
      "===========================",
      "Type: " + report.type,
      "Generated: " + formatDate(report.createdAt || todayISO()),
      "Period: " + formatDate(report.fromDate) + " to " + formatDate(report.toDate),
      "Income: " + formatINR(report.income),
      "Expense: " + formatINR(report.expense),
      "Net: " + formatINR(report.net),
      "Transactions counted: " + (report.txCount || txs.length),
      "",
      "Detailed transactions",
      "--------------------",
      "Date,Name,Category,Type,Amount",
    ];
    txs.forEach((t) => {
      lines.push(
        [t.date, '"' + (t.name || "").replace(/"/g, "'") + '"', t.category, t.type, t.amount].join(",")
      );
    });
    if (!txs.length) lines.push("(No transactions in this period)");
    lines.push("");
    lines.push("Prepared by MicroVault — business finance workspace");
    return lines.join("\r\n");
  }

  function downloadReport(reportId, format) {
    const report = typeof reportId === "object" ? reportId : getReportById(reportId);
    if (!report) return false;
    format = (format || "txt").toLowerCase();
    const body = buildReportDownload(report);
    const isCsv = format === "csv";
    const content = isCsv
      ? body
      : body;
    const mime = isCsv ? "text/csv;charset=utf-8" : "text/plain;charset=utf-8";
    const ext = isCsv ? "csv" : "txt";
    const blob = new Blob([content], { type: mime });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download =
      "MicroVault_" +
      (report.type || "Report").replace(/\s+/g, "_") +
      "_" +
      (report.fromDate || todayISO()) +
      "_to_" +
      (report.toDate || todayISO()) +
      "." +
      ext;
    document.body.appendChild(a);
    a.click();
    a.remove();
    URL.revokeObjectURL(url);
    return true;
  }

  function bindPasswordToggles(root) {
    const scope = root || document;
    scope.querySelectorAll("input[type='password']").forEach((input) => {
      if (input.dataset.eyeBound === "1") return;
      input.dataset.eyeBound = "1";
      const wrap = input.parentElement;
      if (!wrap) return;
      if (getComputedStyle(wrap).position === "static") wrap.style.position = "relative";
      let btn = wrap.querySelector(".toggle-pass[data-for='" + input.id + "']");
      if (!btn) {
        btn = document.createElement("button");
        btn.type = "button";
        btn.className = "toggle-pass";
        btn.dataset.for = input.id;
        btn.setAttribute("aria-label", "Show password");
        btn.innerHTML = '<i class="fas fa-eye"></i>';
        wrap.appendChild(btn);
      }
      btn.addEventListener("click", function () {
        const show = input.type === "password";
        input.type = show ? "text" : "password";
        this.innerHTML = show ? '<i class="fas fa-eye-slash"></i>' : '<i class="fas fa-eye"></i>';
      });
    });
  }

  const CHART_COLORS = ["#0d9488", "#0284c7", "#d97706", "#dc2626", "#7c3aed", "#059669", "#ea580c", "#0b3a5c", "#db2777", "#4f46e5"];

  /* ---------- users ---------- */
  function getUsers() {
    return cache.users.length ? cache.users : cache.members.concat(cache.admins);
  }

  function saveUsers(list) {
    cache.users = list;
  }

  function findUserByEmail(email) {
    return getUsers().find((u) => (u.email || "").toLowerCase() === String(email).toLowerCase());
  }

  function getNormalUsers() {
    return cache.members.length ? cache.members : getUsers().filter((u) => u.role === ROLES.USER || !u.role);
  }

  function getAdmins() {
    return cache.admins.length ? cache.admins : getUsers().filter((u) => u.role === ROLES.ADMIN);
  }

  /* ---------- auth ---------- */
  function getCurrentUser() {
    return get(KEYS.currentUser, null);
  }

  function isLoggedIn() {
    return !!authToken() && !!getCurrentUser();
  }

  async function login(email, password) {
    try {
      const res = await apiRequest("/auth/login", {
        method: "POST",
        body: JSON.stringify({ email, password }),
      });
      if (!res.ok) return { ok: false, message: res.message || "Invalid email or password" };
      localStorage.setItem("mv_token", res.token);
      localStorage.setItem(KEYS.isLoggedIn, "true");
      set(KEYS.currentUser, res.user);
      return { ok: true, user: res.user };
    } catch (e) {
      return { ok: false, message: e.message || "Invalid email or password" };
    }
  }

  async function logout() {
    try { await apiRequest("/auth/logout", { method: "POST" }); } catch { /* ignore */ }
    localStorage.removeItem("mv_token");
    localStorage.setItem(KEYS.isLoggedIn, "false");
    localStorage.removeItem(KEYS.currentUser);
  }

  async function registerUser(data) {
    try {
      const payload = await apiRequest("/auth/register", {
        method: "POST",
        body: JSON.stringify({
          fullName: data.fullName,
          email: data.email,
          phone: data.phone,
          password: data.password,
          confirmPassword: data.confirmPassword || data.password,
          termsAccepted: true,
        }),
      });
      return { ok: true, user: apiData(payload) };
    } catch (e) {
      return { ok: false, message: e.message || "Email already registered" };
    }
  }

  function getHomePage(role) {
    if (role === ROLES.SUPERADMIN || role === ROLES.ADMIN) return "admin-overview.html";
    return "dashboard.html";
  }

  function requireAuth(options) {
    options = options || {};
    ensureSeed();
    if (!isLoggedIn()) {
      window.location.href = options.loginUrl || "../html/login.html";
      return null;
    }
    const user = getCurrentUser();
    if (options.roles && options.roles.length && !options.roles.includes(user.role)) {
      window.location.href = options.denyUrl || getHomePage(user.role);
      return null;
    }
    return user;
  }

  function refreshCurrentUser() {
    const cur = getCurrentUser();
    if (!cur) return null;
    const fresh = getUsers().find((u) => u.id === cur.id || u.email === cur.email);
    if (fresh) set(KEYS.currentUser, fresh);
    return fresh || cur;
  }

  /* ---------- per-user data bags ---------- */
  function userKey(base, userId) {
    return base + "_" + (userId || (getCurrentUser() || {}).id || "guest");
  }

  function getUserData(base, fallback) {
    const cur = getCurrentUser();
    if (!cur) return fallback || [];
    return get(userKey(base, cur.id), fallback || []);
  }

  function setUserData(base, value) {
    const cur = getCurrentUser();
    if (!cur) return;
    set(userKey(base, cur.id), value);
  }

  function getFinance(userId) {
    return cache.finance;
  }

  async function setFinance(data, userId) {
    const saved = apiData(await apiRequest("/finance", { method: "PUT", body: JSON.stringify(data) }));
    cache.finance = saved;
    return saved;
  }

  /* ---------- CRUD factories ---------- */
  function listFor(base) {
    return cache[cacheKey(base)] || [];
  }

  async function saveFor(base, list) {
    cache[cacheKey(base)] = list || [];
    if (Array.isArray(list) && list.length === 0 && COLLECTION_PATH[base]) {
      await apiRequest(COLLECTION_PATH[base], { method: "DELETE" });
    }
  }

  async function addItem(base, item) {
    if (base === KEYS.reports) {
      const row = apiData(await apiRequest("/reports/generate", { method: "POST", body: JSON.stringify({
        fromDate: item.fromDate, toDate: item.toDate, type: item.type
      }) }));
      cache.reports.unshift(row);
      return row;
    }
    if (base === KEYS.affordChecks) {
      const row = apiData(await apiRequest("/affordability/check", { method: "POST", body: JSON.stringify({
        itemName: item.itemName, amount: String(item.amount), checkDate: item.date, priority: item.priority
      }) }));
      cache.affordChecks.unshift(row);
      return row;
    }
    const path = COLLECTION_PATH[base];
    const row = apiData(await apiRequest(path, { method: "POST", body: JSON.stringify(item) }));
    const key = cacheKey(base);
    cache[key] = cache[key] || [];
    cache[key].unshift(row);
    return row;
  }

  async function updateItem(base, id, patch) {
    if (base === KEYS.notifications && patch && patch.read) {
      await apiRequest("/notifications/" + id + "/read", { method: "PUT" });
      cache.notifications = (cache.notifications || []).map((n) => n.id === id || String(n.id) === String(id) ? Object.assign({}, n, { read: true }) : n);
      return cache.notifications.find((n) => String(n.id) === String(id));
    }
    const path = COLLECTION_PATH[base];
    const row = apiData(await apiRequest(path + "/" + id, { method: "PUT", body: JSON.stringify(patch) }));
    const key = cacheKey(base);
    cache[key] = (cache[key] || []).map((x) => String(x.id) === String(id) ? row : x);
    return row;
  }

  async function deleteItem(base, id) {
    const path = COLLECTION_PATH[base];
    await apiRequest(path + "/" + id, { method: "DELETE" });
    const key = cacheKey(base);
    cache[key] = (cache[key] || []).filter((x) => String(x.id) !== String(id));
    return true;
  }

  async function markAllNotificationsRead() {
    await apiRequest("/notifications/read-all", { method: "PUT" });
    cache.notifications = (cache.notifications || []).map((n) => Object.assign({}, n, { read: true }));
  }

  /* ---------- notifications helper ---------- */
  async function pushNotification(title, message, type) {
    /* server emits notifications for setup/news/reports; keep local cache hint */
    cache.notifications = cache.notifications || [];
    cache.notifications.unshift({
      id: uid("not"),
      title,
      message,
      type: type || "info",
      read: false,
      date: todayISO(),
      createdAt: todayISO(),
    });
  }

  function listAllFeedback() {
    return cache.allFeedback.length ? cache.allFeedback : cache.feedback;
  }

  function getMyFeedback() {
    return cache.feedback || [];
  }

  async function addFeedback(data) {
    try {
      const row = apiData(await apiRequest("/feedback", { method: "POST", body: JSON.stringify(data) }));
      cache.feedback.unshift(row);
      return { ok: true, feedback: row };
    } catch (e) {
      return { ok: false, message: e.message };
    }
  }

  async function updateFeedback(id, patch) {
    try {
      const row = apiData(await apiRequest("/feedback/" + id, { method: "PUT", body: JSON.stringify(patch) }));
      cache.feedback = (cache.feedback || []).map((x) => String(x.id) === String(id) ? row : x);
      return { ok: true, feedback: row };
    } catch (e) {
      return { ok: false, message: e.message };
    }
  }

  async function deleteFeedback(id) {
    try {
      await apiRequest("/feedback/" + id, { method: "DELETE" });
      cache.feedback = (cache.feedback || []).filter((x) => String(x.id) !== String(id));
      return { ok: true };
    } catch (e) {
      return { ok: false, message: e.message };
    }
  }

  async function getFeedbackHistory(id) {
    try {
      return apiData(await apiRequest("/feedback/" + id + "/history")) || [];
    } catch {
      return [];
    }
  }

  function listAllNews() {
    return cache.allNews.length ? cache.allNews : cache.news;
  }

  function getPublishedNews() {
    return cache.news || [];
  }

  async function addNews(data) {
    try {
      const row = apiData(await apiRequest("/admin/news", { method: "POST", body: JSON.stringify(data) }));
      cache.allNews.unshift(row);
      if (row.status === "published") cache.news.unshift(row);
      return { ok: true, news: row };
    } catch (e) {
      return { ok: false, message: e.message };
    }
  }

  async function updateNews(id, patch) {
    try {
      const row = apiData(await apiRequest("/admin/news/" + id, { method: "PUT", body: JSON.stringify(patch) }));
      cache.allNews = (cache.allNews || []).map((x) => String(x.id) === String(id) ? row : x);
      return { ok: true, news: row };
    } catch (e) {
      return { ok: false, message: e.message };
    }
  }

  async function deleteNews(id) {
    try {
      await apiRequest("/admin/news/" + id, { method: "DELETE" });
      cache.allNews = (cache.allNews || []).filter((x) => String(x.id) !== String(id));
      cache.news = (cache.news || []).filter((x) => String(x.id) !== String(id));
      return { ok: true };
    } catch (e) {
      return { ok: false, message: e.message };
    }
  }

  function getAdminInsights() {
    return cache.insights || {
      totalMembers: 0, activeMembers: 0, inactiveMembers: 0, adminCount: 0,
      setupDone: 0, setupPending: 0, feedbackTotal: 0, feedbackOpen: 0,
      newsTotal: 0, publishedNews: 0, joinLabels: [], joinValues: [],
      statusLabels: ["Active", "Inactive"], statusValues: [0, 0],
      setupLabels: ["Setup done", "Pending"], setupValues: [0, 0], recentMembers: [],
    };
  }


  /* ---------- validation ---------- */
  const Validators = {
    required(v, label) {
      if (v === undefined || v === null || String(v).trim() === "") return label + " is required";
      return "";
    },
    email(v) {
      if (!v) return "Email is required";
      if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v)) return "Enter a valid email address";
      return "";
    },
    phone(v) {
      if (!v) return "Phone is required";
      if (!/^[6-9]\d{9}$/.test(v)) return "Enter a valid 10-digit Indian mobile number";
      return "";
    },
    password(v) {
      if (!v) return "Password is required";
      if (v.length < 8) return "Password must be at least 8 characters";
      if (!/[A-Z]/.test(v)) return "Include at least one uppercase letter";
      if (!/[a-z]/.test(v)) return "Include at least one lowercase letter";
      if (!/[0-9]/.test(v)) return "Include at least one number";
      if (!/[^A-Za-z0-9]/.test(v)) return "Include at least one special character";
      return "";
    },
    name(v) {
      if (!v || v.trim().length < 3) return "Full name must be at least 3 characters";
      if (!/^[A-Za-z\s.]+$/.test(v.trim())) return "Name can only contain letters and spaces";
      return "";
    },
    amount(v, label) {
      label = label || "Amount";
      if (v === "" || v === null || v === undefined) return label + " is required";
      const n = Number(v);
      if (isNaN(n) || n < 0) return label + " must be a valid non-negative number";
      return "";
    },
    dateNotFuture(v, label) {
      label = label || "Date";
      if (!v) return label + " is required";
      if (v > todayISO()) return label + " cannot be in the future";
      return "";
    },
  };

  function showFieldError(inputOrId, message) {
    const el = typeof inputOrId === "string" ? document.getElementById(inputOrId) : inputOrId;
    if (!el) return;
    const errId = el.id + "Error";
    let err = document.getElementById(errId);
    if (!err) {
      err = document.createElement("span");
      err.id = errId;
      err.className = "field-error";
      el.parentNode.appendChild(err);
    }
    err.textContent = message || "";
    err.style.display = message ? "block" : "none";
    el.classList.toggle("input-error", !!message);
    el.classList.toggle("input-success", !message && el.value);
  }

  function clearFieldError(inputOrId) {
    showFieldError(inputOrId, "");
  }

  function bindMaxDate(selector) {
    const max = todayISO();
    document.querySelectorAll(selector || 'input[type="date"]').forEach((inp) => {
      inp.setAttribute("max", max);
      inp.addEventListener("change", function () {
        if (this.value > max) {
          this.value = max;
          showFieldError(this, "Future dates are not allowed");
        }
      });
    });
  }

  /* ---------- table engine ---------- */
  function createTableController(opts) {
    const state = {
      page: 1,
      perPage: opts.perPage || 5,
      search: "",
      filters: {},
      sortKey: opts.sortKey || null,
      sortDir: opts.sortDir || "desc",
    };

    function getFiltered() {
      let rows = typeof opts.getData === "function" ? opts.getData() : opts.getData || [];
      const q = state.search.trim().toLowerCase();
      if (q) {
        rows = rows.filter((r) =>
          (opts.searchKeys || Object.keys(r)).some((k) => String(r[k] ?? "").toLowerCase().includes(q))
        );
      }
      Object.keys(state.filters).forEach((key) => {
        const val = state.filters[key];
        if (val !== "" && val !== null && val !== undefined) {
          rows = rows.filter((r) => String(r[key]) === String(val));
        }
      });
      if (state.sortKey) {
        const k = state.sortKey;
        const dir = state.sortDir === "asc" ? 1 : -1;
        rows = rows.slice().sort((a, b) => {
          if (a[k] < b[k]) return -1 * dir;
          if (a[k] > b[k]) return 1 * dir;
          return 0;
        });
      }
      return rows;
    }

    function render() {
      const all = getFiltered();
      const totalPages = Math.max(1, Math.ceil(all.length / state.perPage));
      if (state.page > totalPages) state.page = totalPages;
      const start = (state.page - 1) * state.perPage;
      const pageRows = all.slice(start, start + state.perPage);
      opts.renderRows(pageRows, all);
      if (opts.paginationEl) {
        const el = typeof opts.paginationEl === "string" ? document.querySelector(opts.paginationEl) : opts.paginationEl;
        if (el) {
          el.innerHTML =
            '<button type="button" class="page-btn" data-act="prev"' +
            (state.page <= 1 ? " disabled" : "") +
            ">Prev</button>" +
            '<span class="page-info">Page ' +
            state.page +
            " of " +
            totalPages +
            " (" +
            all.length +
            " records)</span>" +
            '<button type="button" class="page-btn" data-act="next"' +
            (state.page >= totalPages ? " disabled" : "") +
            ">Next</button>";
          el.querySelectorAll(".page-btn").forEach((btn) => {
            btn.addEventListener("click", () => {
              if (btn.dataset.act === "prev") state.page--;
              if (btn.dataset.act === "next") state.page++;
              render();
            });
          });
        }
      }
    }

    function bindSearch(sel) {
      const el = document.querySelector(sel);
      if (!el) return;
      el.addEventListener("input", () => {
        state.search = el.value;
        state.page = 1;
        render();
      });
    }

    function bindFilter(sel, key) {
      const el = document.querySelector(sel);
      if (!el) return;
      el.addEventListener("change", () => {
        state.filters[key] = el.value;
        state.page = 1;
        render();
      });
    }

    return { state, render, bindSearch, bindFilter, getFiltered, refresh: render };
  }

  /* ---------- admin CRUD on users ---------- */
  async function adminUpdateUser(id, patch) {
    try {
      const path = (patch && patch.role === ROLES.ADMIN) ? "/admin/admins/" : "/admin/users/";
      const row = apiData(await apiRequest(path + id, { method: "PUT", body: JSON.stringify(patch) }));
      cache.members = (cache.members || []).map((u) => String(u.id) === String(id) ? row : u);
      cache.admins = (cache.admins || []).map((u) => String(u.id) === String(id) ? row : u);
      return { ok: true, user: row };
    } catch (e) {
      return { ok: false, message: e.message };
    }
  }

  async function adminDeleteUser(id) {
    try {
      const target = getNormalUsers().concat(getAdmins()).find((u) => String(u.id) === String(id));
      const path = target && target.role === ROLES.ADMIN ? "/admin/admins/" : "/admin/users/";
      await apiRequest(path + id, { method: "DELETE" });
      cache.members = (cache.members || []).filter((u) => String(u.id) !== String(id));
      cache.admins = (cache.admins || []).filter((u) => String(u.id) !== String(id));
      return { ok: true };
    } catch (e) {
      return { ok: false, message: e.message };
    }
  }

  async function createAdmin(data) {
    try {
      const row = apiData(await apiRequest("/admin/admins", { method: "POST", body: JSON.stringify(data) }));
      cache.admins.unshift(row);
      return { ok: true, user: row };
    } catch (e) {
      return { ok: false, message: e.message };
    }
  }

  /* ---------- affordability ---------- */
  function canAfford(amount) {
    const finance = getFinance() || {};
    const income = Number(finance.monthlyIncome) || 0;
    const expenses = Number(finance.monthlyExpenses) || 0;
    const savingsList = listFor(KEYS.savings);
    const totalSavings = savingsList.reduce((s, x) => s + (Number(x.amount) || 0), 0);
    const available = Math.max(0, income - expenses) + totalSavings * 0.3;
    const amt = Number(amount) || 0;
    const ratio = available > 0 ? amt / available : 1;
    let verdict = "Not recommended";
    let level = "danger";
    if (ratio <= 0.3) {
      verdict = "Comfortably affordable";
      level = "success";
    } else if (ratio <= 0.6) {
      verdict = "Affordable with caution";
      level = "warning";
    } else if (ratio <= 1) {
      verdict = "Tight — consider delaying";
      level = "warning";
    }
    return { available, amount: amt, ratio, verdict, level, income, expenses, totalSavings };
  }

  /* ---------- dashboard stats ---------- */
  function getDashboardStats() {
    const finance = getFinance() || {};
    const txs = listFor(KEYS.transactions);
    const goals = listFor(KEYS.goals);
    const savings = listFor(KEYS.savings);
    const budgets = listFor(KEYS.budgets);
    const totalSavings = savings.reduce((s, x) => s + (Number(x.amount) || 0), 0);
    const monthBudget = Number(finance.monthlyBudget) || budgets.reduce((s, b) => s + (Number(b.limit) || 0), 0) || 30000;
    const spent = txs.filter((t) => t.type === "expense").reduce((s, t) => s + (Number(t.amount) || 0), 0);
    const income = Number(finance.monthlyIncome) || 0;
    const expenses = Number(finance.monthlyExpenses) || spent;
    let health = 50;
    if (income > 0) {
      const saveRate = Math.max(0, (income - expenses) / income);
      health = Math.min(100, Math.round(40 + saveRate * 60 + (totalSavings > 0 ? 10 : 0)));
    }
    const activeGoals = goals.filter((g) => g.status !== "completed").length;
    return { totalSavings, monthBudget, spent, health, activeGoals, income, expenses, txs, goals, savings, budgets };
  }

  function confirmDelete(message, options) {
    options = options || {};
    return new Promise((resolve) => {
      let backdrop = document.getElementById("mvConfirmBackdrop");
      if (!backdrop) {
        backdrop = document.createElement("div");
        backdrop.id = "mvConfirmBackdrop";
        backdrop.className = "mv-confirm-backdrop";
        backdrop.innerHTML =
          '<div class="mv-confirm-dialog" role="dialog" aria-modal="true" aria-labelledby="mvConfirmTitle">' +
          '<div class="mv-confirm-icon" id="mvConfirmIcon"><i class="fas fa-trash"></i></div>' +
          '<h3 id="mvConfirmTitle">Confirm delete</h3>' +
          '<p id="mvConfirmMessage"></p>' +
          '<div class="mv-confirm-actions">' +
          '<button type="button" class="btn btn-outline" id="mvConfirmCancel">Cancel</button>' +
          '<button type="button" class="btn btn-danger" id="mvConfirmOk">Delete</button>' +
          "</div></div>";
        document.body.appendChild(backdrop);
      }

      const mode = options.mode || "delete";
      const icon = document.getElementById("mvConfirmIcon");
      icon.className = "mv-confirm-icon" + (mode === "info" ? " info" : "");
      icon.innerHTML = mode === "info" ? '<i class="fas fa-circle-info"></i>' : '<i class="fas fa-trash"></i>';

      document.getElementById("mvConfirmTitle").textContent = options.title || (mode === "info" ? "Notice" : "Confirm delete");
      document.getElementById("mvConfirmMessage").textContent =
        message || "Are you sure you want to delete this item? This action cannot be undone.";

      const okBtn = document.getElementById("mvConfirmOk");
      const cancelBtn = document.getElementById("mvConfirmCancel");
      okBtn.textContent = options.okText || (mode === "info" ? "OK" : "Delete");
      okBtn.className = "btn " + (mode === "info" ? "btn-primary" : "btn-danger");
      cancelBtn.textContent = options.cancelText || "Cancel";
      cancelBtn.style.display = mode === "info" ? "none" : "";

      const finish = (value) => {
        backdrop.classList.remove("open");
        document.removeEventListener("keydown", onKey);
        resolve(value);
      };
      const onKey = (e) => {
        if (e.key === "Escape") finish(false);
      };

      cancelBtn.onclick = () => finish(false);
      okBtn.onclick = () => finish(true);
      backdrop.onclick = (e) => {
        if (e.target === backdrop) finish(mode === "info");
      };
      document.addEventListener("keydown", onKey);
      backdrop.classList.add("open");
      setTimeout(() => okBtn.focus(), 50);
    });
  }

  function setupSkippedKey(userId) {
    return "mv_setup_skipped_" + (userId || (getCurrentUser() || {}).id || "guest");
  }

  function isFinanceSetupComplete(userId) {
    if (userId && getCurrentUser() && String(userId) !== String(getCurrentUser().id)) {
      const member = (cache.members || []).find((u) => String(u.id) === String(userId));
      return !!(member && member.setupComplete);
    }
    return !!cache.finance;
  }

  function needsFinanceSetup(user) {
    const u = user || getCurrentUser();
    if (!u) return false;
    if (u.role === ROLES.ADMIN || u.role === ROLES.SUPERADMIN) return false;
    return !isFinanceSetupComplete(u.id);
  }

  function clearMemberData(userId) {
    cache.finance = null;
    cache.transactions = [];
    cache.goals = [];
    cache.savings = [];
    cache.budgets = [];
    cache.notifications = [];
    cache.reports = [];
    cache.affordChecks = [];
  }

  async function markSetupSkipped(userId) {
    await apiRequest("/finance/skip", { method: "POST" });
    cache.setupSkipped = true;
    clearMemberData(userId);
  }

  function clearSetupSkipped(userId) {
    cache.setupSkipped = false;
  }

  function isSetupSkipped(userId) {
    return !!cache.setupSkipped;
  }

  function promptFinanceSetup(options) {
    options = options || {};
    const setupUrl = options.setupUrl || "personal-financial-setup.html";
    return new Promise((resolve) => {
      let backdrop = document.getElementById("mvSetupBackdrop");
      if (!backdrop) {
        backdrop = document.createElement("div");
        backdrop.id = "mvSetupBackdrop";
        backdrop.className = "mv-confirm-backdrop";
        backdrop.innerHTML =
          '<div class="mv-confirm-dialog" role="dialog" aria-modal="true">' +
          '<div class="mv-confirm-icon info"><i class="fas fa-sliders"></i></div>' +
          "<h3>Complete financial setup</h3>" +
          '<p id="mvSetupMessage">Please complete Personal Financial Setup to view your data, charts, and insights.</p>' +
          '<div class="mv-confirm-actions">' +
          '<button type="button" class="btn btn-outline" id="mvSetupLater">Stay on dashboard</button>' +
          '<button type="button" class="btn btn-primary" id="mvSetupGo">Set up now</button>' +
          "</div></div>";
        document.body.appendChild(backdrop);
      }
      if (options.message) {
        document.getElementById("mvSetupMessage").textContent = options.message;
      }
      const finish = (go) => {
        backdrop.classList.remove("open");
        if (go) window.location.href = setupUrl;
        resolve(go);
      };
      document.getElementById("mvSetupLater").onclick = () => finish(false);
      document.getElementById("mvSetupGo").onclick = () => finish(true);
      backdrop.onclick = (e) => {
        if (e.target === backdrop) finish(false);
      };
      backdrop.classList.add("open");
      setTimeout(() => document.getElementById("mvSetupGo").focus(), 40);
    });
  }

  function showNotice(message, title) {
    return confirmDelete(message, { mode: "info", title: title || "Notice", okText: "OK" });
  }

  ensureSeed();

  async function bootstrap() {
    const user = getCurrentUser();
    if (!user || !user.email) return null;

    const params = new URLSearchParams();
    params.set("email", user.email);
    const id = String(user.id || "");
    if (/^[0-9a-fA-F-]{36}$/.test(id)) params.set("userId", id);

    const response = await fetch(apiBase() + "/bootstrap?" + params.toString(), {
      headers: { Accept: "application/json" },
    });
    const text = await response.text();
    let payload = null;
    try { payload = text ? JSON.parse(text) : null; } catch { payload = { message: text }; }
    if (!response.ok) {
      const err = new Error((payload && payload.message) || "Could not load dashboard");
      err.status = response.status;
      throw err;
    }

    const data = apiData(payload) || {};
    if (data.user) {
      set(KEYS.currentUser, Object.assign({}, user, {
        fullName: data.user.fullName || user.fullName,
        email: data.user.email || user.email,
        phone: data.user.phone || user.phone,
        role: data.user.role || user.role,
        status: data.user.status || user.status,
      }));
    }
    cache.finance = data.finance || null;
    cache.transactions = data.transactions || [];
    cache.goals = data.goals || [];
    cache.savings = data.savings || [];
    cache.budgets = data.budgets || [];
    cache.notifications = data.notifications || [];
    cache.reports = data.reports || [];
    cache.affordChecks = data.affordChecks || [];
    cache.feedback = data.feedback || [];
    cache.allFeedback = data.allFeedback || [];
    cache.news = data.news || [];
    cache.allNews = data.allNews || [];
    cache.members = data.members || [];
    cache.admins = data.admins || [];
    cache.insights = data.adminInsights || null;
    cache.setupSkipped = !!data.setupSkipped;
    return data;
  }

  async function updateMyProfile(data) {
    const row = apiData(await apiRequest("/users/me", { method: "PUT", body: JSON.stringify(data) }));
    set(KEYS.currentUser, row);
    return row;
  }

  async function changeMyPassword(data) {
    await apiRequest("/users/me/password", { method: "PUT", body: JSON.stringify(data) });
  }

  async function sendResetOtp(email) {
    const payload = await apiRequest("/auth/forgot/send-otp", { method: "POST", body: JSON.stringify({ email }) });
    return apiData(payload) || payload;
  }

  async function resetPassword(data) {
    await apiRequest("/auth/forgot/reset", { method: "POST", body: JSON.stringify(data) });
  }

  global.MV = {
    KEYS,
    ROLES,
    SUPERADMIN_EMAIL: SUPERADMIN.email,
    SUPERADMIN_DEMO_PASSWORD: SUPERADMIN.password,
    CHART_COLORS,
    get,
    set,
    uid,
    todayISO,
    formatINR,
    formatDate,
    daysAgo,
    ensureSeed,
    ensureDemoData,
    bootstrap,
    isFinanceSetupComplete,
    needsFinanceSetup,
    markSetupSkipped,
    clearSetupSkipped,
    clearMemberData,
    isSetupSkipped,
    promptFinanceSetup,
    bindPasswordToggles,
    confirmDelete,
    showNotice,
    getReportById,
    buildReportDownload,
    downloadReport,
    getUsers,
    saveUsers,
    findUserByEmail,
    getNormalUsers,
    getAdmins,
    getCurrentUser,
    isLoggedIn,
    login,
    logout,
    registerUser,
    requireAuth,
    getHomePage,
    listAllFeedback,
    getMyFeedback,
    addFeedback,
    updateFeedback,
    deleteFeedback,
    getFeedbackHistory,
    listAllNews,
    getPublishedNews,
    addNews,
    updateNews,
    deleteNews,
    getAdminInsights,
    refreshCurrentUser,
    getFinance,
    setFinance,
    listFor,
    saveFor,
    addItem,
    updateItem,
    deleteItem,
    pushNotification,
    Validators,
    showFieldError,
    clearFieldError,
    bindMaxDate,
    createTableController,
    adminUpdateUser,
    adminDeleteUser,
    createAdmin,
    markAllNotificationsRead,
    canAfford,
    getDashboardStats,
    updateMyProfile,
    changeMyPassword,
    sendResetOtp,
    resetPassword,
  };
})(window);
