/* Shared UI helpers: DOM building, text/date formatting, status styling,
   toasts, confirm modal, empty states, loading blocks. */
(function (global) {
  "use strict";

  var STATUS_LABELS = {
    NEW: "New",
    CONTACTED: "Contacted",
    INTERESTED: "Interested",
    FOLLOW_UP_REQUIRED: "Follow-up",
    CONVERTED: "Converted",
    NOT_INTERESTED: "Not interested"
  };

  var STATUS_CLASS = {
    NEW: "st-new",
    CONTACTED: "st-contacted",
    INTERESTED: "st-interested",
    FOLLOW_UP_REQUIRED: "st-follow",
    CONVERTED: "st-converted",
    NOT_INTERESTED: "st-lost"
  };

  var PRIORITY_LABELS = { LOW: "Low", MEDIUM: "Medium", HIGH: "High" };
  var PRIORITY_CLASS = { LOW: "p-low", MEDIUM: "p-medium", HIGH: "p-high" };

  var SOURCE_LABELS = {
    PHONE_CALL: "Phone call",
    WALK_IN: "Walk-in",
    WHATSAPP: "WhatsApp",
    INSTAGRAM: "Instagram",
    REFERRAL: "Referral",
    OTHER: "Other"
  };

  var OUTCOME_LABELS = {
    INTERESTED: "Interested",
    NOT_INTERESTED: "Not interested",
    NO_RESPONSE: "No response",
    POSTPONED: "Postponed"
  };

  function el(tag, attrs) {
    var node = document.createElement(tag);
    if (attrs) {
      Object.keys(attrs).forEach(function (key) {
        var value = attrs[key];
        if (value === undefined || value === null) return;
        if (key === "class") node.className = value;
        else if (key === "text") node.textContent = value;
        else if (key === "html") node.innerHTML = value; // caller-supplied markup only
        else if (key.slice(0, 2) === "on") node.addEventListener(key.slice(2), value);
        else node.setAttribute(key, value);
      });
    }
    for (var i = 2; i < arguments.length; i++) {
      appendChildFlattened(node, arguments[i]);
    }
    return node;
  }

  /** appendChild that accepts strings, nodes, null/undefined and (nested) arrays. */
  function appendChildFlattened(node, child) {
    if (child === undefined || child === null) return;
    if (Array.isArray(child)) {
      child.forEach(function (one) { appendChildFlattened(node, one); });
      return;
    }
    node.appendChild(typeof child === "string" ? document.createTextNode(child) : child);
  }

  function fmtDate(iso) {
    if (!iso) return "—";
    var d = new Date(iso.length === 10 ? iso + "T00:00:00" : iso);
    if (isNaN(d)) return iso;
    return d.toLocaleDateString("en-IN", { day: "numeric", month: "short", year: "numeric" });
  }

  function fmtDateTime(iso) {
    if (!iso) return "—";
    var d = new Date(iso);
    if (isNaN(d)) return iso;
    return d.toLocaleString("en-IN", {
      day: "numeric", month: "short", hour: "numeric", minute: "2-digit"
    });
  }

  /** "2026-10-28" -> { day: "28", mon: "Oct" } in the business's timezone. */
  function dayParts(iso) {
    var d = new Date(iso.length === 10 ? iso + "T00:00:00" : iso);
    if (isNaN(d)) return { day: "--", mon: "" };
    return {
      day: d.toLocaleDateString("en-IN", { day: "2-digit" }),
      mon: d.toLocaleDateString("en-IN", { month: "short" })
    };
  }

  function badge(status) {
    return el("span", {
      class: "badge " + (STATUS_CLASS[status] || "st-archived"),
      text: STATUS_LABELS[status] || status
    });
  }

  function priorityBadge(priority) {
    if (!priority) return null;
    return el("span", {
      class: "badge " + (PRIORITY_CLASS[priority] || "p-low"),
      text: PRIORITY_LABELS[priority] || priority
    });
  }

  function toast(message, isError) {
    var region =
      document.querySelector(".toast-region") ||
      document.body.appendChild(el("div", { class: "toast-region", "aria-live": "polite" }));
    var node = el("div", { class: "toast" + (isError ? " error" : ""), text: message });
    region.appendChild(node);
    setTimeout(function () {
      node.style.opacity = "0";
      node.style.transition = "opacity 200ms";
      setTimeout(function () { node.remove(); }, 220);
    }, 3200);
  }

  /** Confirm dialog. Returns a promise: true = confirmed. */
  function confirmDialog(opts) {
    return new Promise(function (resolve) {
      var backdrop = el("div", { class: "modal-backdrop" });
      var cancelled = function () { cleanup(false); };
      var accepted = function () { cleanup(true); };
      function cleanup(result) {
        document.removeEventListener("keydown", onKey);
        backdrop.remove();
        resolve(result);
      }
      function onKey(e) {
        if (e.key === "Escape") cancelled();
      }
      var cancelBtn = el("button", { class: "btn btn-secondary", type: "button", text: opts.cancelText || "Cancel", onclick: cancelled });
      var okBtn = el("button", {
        class: "btn " + (opts.danger ? "btn-danger" : "btn-primary"),
        type: "button",
        text: opts.confirmText || "Confirm",
        onclick: accepted
      });
      var dialog = el(
        "div",
        { class: "modal", role: "dialog", "aria-modal": "true", "aria-label": opts.title },
        el("h2", { text: opts.title }),
        el("p", { class: "muted", text: opts.message || "" }),
        el("div", { class: "row end mt-4" }, cancelBtn, okBtn)
      );
      backdrop.appendChild(dialog);
      document.addEventListener("keydown", onKey);
      backdrop.addEventListener("mousedown", function (e) {
        if (e.target === backdrop) cancelled();
      });
      document.body.appendChild(backdrop);
      okBtn.focus();
    });
  }

  function emptyState(title, message, action) {
    return el(
      "div",
      { class: "empty" },
      el("div", { class: "glyph", "aria-hidden": "true" }, el("div", { text: "◎" })),
      el("h3", { text: title }),
      el("p", { text: message }),
      action || null
    );
  }

  function loadingBlock(label) {
    return el(
      "div",
      { class: "loading", role: "status" },
      el("div", { class: "spinner", "aria-hidden": "true" }),
      label || "Loading…"
    );
  }

  function errorState(message, retry) {
    var box = el(
      "div",
      { class: "empty" },
      el("h3", { text: "Something went wrong" }),
      el("p", { text: message })
    );
    if (retry) {
      box.appendChild(el("div", { class: "mt-4" },
        el("button", { class: "btn btn-secondary", type: "button", text: "Try again", onclick: retry })));
    }
    return box;
  }

  global.FSUi = {
    el: el,
    fmtDate: fmtDate,
    fmtDateTime: fmtDateTime,
    dayParts: dayParts,
    badge: badge,
    priorityBadge: priorityBadge,
    statusLabel: function (s) { return STATUS_LABELS[s] || s; },
    sourceLabel: function (s) { return SOURCE_LABELS[s] || s; },
    outcomeLabel: function (s) { return OUTCOME_LABELS[s] || s; },
    priorityLabel: function (p) { return PRIORITY_LABELS[p] || p; },
    toast: toast,
    confirmDialog: confirmDialog,
    emptyState: emptyState,
    loadingBlock: loadingBlock,
    errorState: errorState
  };
})(window);
