/**
 * MicroVault API client — talks to the Java HTTP layer, never to PostgreSQL.
 */
(function (global) {
  "use strict";

  function apiBase() {
    if (global.MV_API_BASE) return global.MV_API_BASE.replace(/\/$/, "");
    if (location.protocol === "http:" || location.protocol === "https:") {
      return location.origin + "/api";
    }
    return "http://localhost:8080/api";
  }

  function token() {
    return localStorage.getItem("mv_token") || "";
  }

  async function apiRequest(path, options) {
    options = options || {};
    const headers = Object.assign(
      { "Content-Type": "application/json" },
      options.headers || {}
    );
    const t = token();
    if (t) headers.Authorization = "Bearer " + t;
    const response = await fetch(apiBase() + path, Object.assign({}, options, { headers }));
    const text = await response.text();
    let payload = null;
    try {
      payload = text ? JSON.parse(text) : null;
    } catch {
      payload = { ok: false, message: text || "Request failed", raw: text };
    }
    if (!response.ok) {
      const err = new Error((payload && payload.message) || "Request failed");
      err.status = response.status;
      err.payload = payload;
      throw err;
    }
    return payload;
  }

  function unwrap(payload) {
    if (payload && Object.prototype.hasOwnProperty.call(payload, "data")) return payload.data;
    return payload;
  }

  global.MVApi = { apiBase, token, apiRequest, unwrap };
})(window);
