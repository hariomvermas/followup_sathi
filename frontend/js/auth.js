/* Session cache and route guards. Session lives in a variable + sessionStorage
   (name and business flag only — never tokens or passwords). */
(function (global) {
  "use strict";

  var CACHE_KEY = "fs.user";
  var user = null;

  try {
    user = JSON.parse(sessionStorage.getItem(CACHE_KEY) || "null");
  } catch (ignored) {
    user = null;
  }

  function cache(u) {
    user = u;
    try {
      if (u) sessionStorage.setItem(CACHE_KEY, JSON.stringify(u));
      else sessionStorage.removeItem(CACHE_KEY);
    } catch (ignored) {
      /* storage unavailable: session still held in memory */
    }
  }

  function defaultNext() {
    return location.pathname === "/login.html" || location.pathname === "/register.html"
      ? "/dashboard.html"
      : location.pathname + location.search;
  }

  /** Fetch /api/auth/me; null when signed out (401). */
  async function load() {
    try {
      var me = await FSApi.get("/api/auth/me");
      cache(me);
      return me;
    } catch (err) {
      if (err.status === 401) {
        cache(null);
        return null;
      }
      throw err;
    }
  }

  /**
   * requireAuth({ business })
   *  - no session  -> redirect /login.html?next=… (unless opts.login)
   *  - session but no business (opts.business) -> /onboarding.html
   *  - logged-in visit to login/register (opts.guest) -> /dashboard.html
   */
  async function requireAuth(opts) {
    opts = opts || {};
    var me = user || (await load());
    if (FSApi.__justLoggedOut) { me = await load(); FSApi.__justLoggedOut = false; }
    if (!me) {
      if (opts.login) return null;
      location.replace("/login.html?next=" +
        encodeURIComponent(defaultNext()));
      return null;
    }
    if (opts.guest) {
      location.replace("/dashboard.html");
      return null;
    }
    if (opts.business && !me.hasBusiness) {
      location.replace("/onboarding.html");
      return null;
    }
    return me;
  }

  async function login(email, password) {
    var me = await FSApi.post("/api/auth/login", { email: email, password: password });
    cache(me);
    return me;
  }

  /** Register then login (the API does not create a session on register). */
  async function register(payload) {
    await FSApi.post("/api/auth/register", payload);
    return login(payload.email, payload.password);
  }

  async function logout() {
    try {
      await FSApi.post("/api/auth/logout");
    } finally {
      cache(null);
    }
  }

  global.FSAuth = {
    load: load,
    requireAuth: requireAuth,
    login: login,
    register: register,
    logout: logout,
    current: function () { return user; }
  };
})(window);
