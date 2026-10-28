/* Single API client for every page: same-origin fetch, session cookie auth,
   CSRF header, field-error parsing, and one 401 redirect handler. */
(function (global) {
  "use strict";

  var csrfToken = null;
  var redirectingToLogin = false;

  function readCookie(name) {
    var row = document.cookie
      .split("; ")
      .find(function (c) { return c.indexOf(name + "=") === 0; });
    return row ? decodeURIComponent(row.substring(name.length + 1)) : null;
  }

  async function ensureCsrf(force) {
    if (csrfToken && !force) return csrfToken;
    var res = await fetch("/api/auth/csrf", { credentials: "same-origin" });
    if (!res.ok) throw new Error("Could not load CSRF token");
    var body = await res.json();
    csrfToken = body.token || (body.attributes && body.attributes.rawToken) || null;
    return csrfToken;
  }

  function buildQuery(params) {
    if (!params) return "";
    var usp = new URLSearchParams();
    Object.keys(params).forEach(function (key) {
      var value = params[key];
      if (value !== undefined && value !== null && value !== "") {
        usp.append(key, value);
      }
    });
    var s = usp.toString();
    return s ? "?" + s : "";
  }

  /**
   * request(method, path, { query, body, form, retryCsrf })
   * - JSON by default; `form` sends application/x-www-form-urlencoded.
   * - 401 clears the cached session and sends the browser to /login.html.
   * - 403 with a missing/rotated CSRF cookie retries once with a fresh token.
   */
  async function request(method, path, opts) {
    opts = opts || {};
    var headers = {};
    var body;

    if (opts.form) {
      headers["Content-Type"] = "application/x-www-form-urlencoded";
      body = new URLSearchParams(opts.form).toString();
    } else if (opts.body !== undefined) {
      headers["Content-Type"] = "application/json";
      body = JSON.stringify(opts.body);
    }

    if (method !== "GET" && method !== "HEAD") {
      var token = await ensureCsrf();
      if (token) headers["X-XSRF-TOKEN"] = token;
    }

    var res = await fetch(path + buildQuery(opts.query), {
      method: method,
      headers: headers,
      credentials: "same-origin",
      body: body
    });

    if (res.status === 401) {
      csrfToken = null;
      if (!redirectingToLogin && !opts.noRedirect) {
        redirectingToLogin = true;
        var here = location.pathname + location.search;
        if (location.pathname !== "/login.html") {
          location.replace("/login.html?next=" + encodeURIComponent(here));
        }
      }
      var err401 = new Error("Not signed in");
      err401.status = 401;
      throw err401;
    }

    if (res.status === 403) {
      var cookieNow = readCookie("XSRF-TOKEN");
      if (!opts.retryCsrf && cookieNow && cookieNow !== csrfToken) {
        csrfToken = cookieNow;
        return request(method, path, Object.assign({}, opts, { retryCsrf: true }));
      }
    }

    if (res.status === 204 || res.status === 205) return null;

    var text = await res.text();
    var data = null;
    if (text) {
      try {
        data = JSON.parse(text);
      } catch (ignored) {
        data = text;
      }
    }

    if (!res.ok) {
      var message = "Request failed (" + res.status + ")";
      var fieldErrors = null;
      if (data && typeof data === "object") {
        if (data.message) message = data.message;
        if (data.fieldErrors) fieldErrors = data.fieldErrors;
      }
      var err = new Error(message);
      err.status = res.status;
      err.fieldErrors = fieldErrors;
      throw err;
    }

    return data;
  }

  async function downloadCsv(path, params) {
    var res = await fetch(path + buildQuery(params), { credentials: "same-origin" });
    if (!res.ok) {
      var message = "Download failed (" + res.status + ")";
      try {
        var data = await res.json();
        if (data && data.message) message = data.message;
      } catch (ignored) { /* body was not JSON */ }
      throw new Error(message);
    }
    var blob = await res.blob();
    var disposition = res.headers.get("Content-Disposition") || "";
    var match = disposition.match(/filename\*?=(?:UTF-8''|")?([^";]+)/i);
    var name = match ? decodeURIComponent(match[1]) : "download.csv";
    var url = URL.createObjectURL(blob);
    var link = document.createElement("a");
    link.href = url;
    link.download = name;
    document.body.appendChild(link);
    link.click();
    link.remove();
    setTimeout(function () { URL.revokeObjectURL(url); }, 1000);
  }

  global.FSApi = {
    request: request,
    get: function (path, query) { return request("GET", path, { query: query }); },
    post: function (path, body) { return request("POST", path, { body: body }); },
    put: function (path, body) { return request("PUT", path, { body: body }); },
    patch: function (path, body) { return request("PATCH", path, { body: body }); },
    del: function (path, body) { return request("DELETE", path, { body: body }); },
    ensureCsrf: ensureCsrf,
    downloadCsv: downloadCsv
  };
})(window);
