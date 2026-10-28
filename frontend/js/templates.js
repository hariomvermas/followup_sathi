/* Message templates: CRUD list, create/edit modal with sample preview, delete. */
(function () {
  "use strict";

  function openModal(existing) {
    var backdrop = FSUi.el("div", { class: "modal-backdrop" });
    function close() {
      document.removeEventListener("keydown", onKey);
      backdrop.remove();
    }
    function onKey(e) { if (e.key === "Escape") close(); }

    var form = FSUi.el("form", { novalidate: "novalidate" });
    var name = FSUi.el("input", { name: "templateName", type: "text", maxlength: "80",
      value: existing ? existing.templateName : "", required: "required" });
    var body = FSUi.el("textarea", { name: "messageBody", maxlength: "2000", rows: "7",
      text: undefined });
    body.value = existing ? existing.messageBody : "";
    form.appendChild(FSUi.el("div", { class: "field" },
      FSUi.el("label", { text: "Template name *" }), name, FSUi.el("small")));
    form.appendChild(FSUi.el("div", { class: "field" },
      FSUi.el("label", { text: "Message *" }), body, FSUi.el("small")));

    var preview = FSUi.el("div", { class: "rendered hidden", style: "margin-bottom:var(--s-4)" });
    var previewBtn = FSUi.el("button", { class: "btn btn-secondary btn-sm", type: "button",
      text: "Preview with sample data" });
    previewBtn.addEventListener("click", async function () {
      if (!body.value.trim()) {
        FSValidate.fieldError(body, "Write the message first");
        return;
      }
      FSValidate.fieldError(body, null);
      preview.classList.remove("hidden");
      // For an existing template the server renders it; unsaved text renders
      // client-side with the same sample data so the button always works.
      if (existing) {
        preview.textContent = "Rendering…";
        try {
          var p = await FSApi.get("/api/templates/" + existing.id + "/preview");
          preview.textContent = p.rendered +
            (p.missingPlaceholders && p.missingPlaceholders.length
              ? "\n\nUnfilled placeholders: " + p.missingPlaceholders.join(", ") : "");
          return;
        } catch (ignored) { /* fall through to the local sample render */ }
      }
      preview.textContent = sampleRender(body.value);
    });
    form.appendChild(FSUi.el("div", { class: "row", style: "margin-bottom:var(--s-3)" }, previewBtn));
    form.appendChild(preview);
    form.appendChild(FSUi.el("div", { class: "row end" },
      FSUi.el("button", { class: "btn btn-secondary", type: "button", text: "Cancel",
        onclick: close }),
      FSUi.el("button", { class: "btn btn-primary", type: "submit",
        text: existing ? "Save changes" : "Create template" })));

    form.addEventListener("submit", async function (event) {
      event.preventDefault();
      FSValidate.clearErrors(form);
      if (!FSValidate.validateForm(form, {
        templateName: [FSValidate.rules.required, FSValidate.rules.maxLen(80)],
        messageBody: [FSValidate.rules.required, FSValidate.rules.maxLen(2000)]
      })) return;
      var payload = { templateName: form.templateName.value.trim(),
        messageBody: form.messageBody.value };
      try {
        await FSValidate.busyButton(form.querySelector("button[type=submit]"),
          existing ? FSApi.put("/api/templates/" + existing.id, payload)
                  : FSApi.post("/api/templates", payload));
        FSUi.toast(existing ? "Template updated" : "Template created");
        close();
        load();
      } catch (err) {
        FSValidate.applyServerErrors(form, err.fieldErrors);
        if (!err.fieldErrors) FSUi.toast(err.message, true);
      }
    });

    backdrop.appendChild(FSUi.el("div", { class: "modal", role: "dialog", "aria-modal": "true",
      "aria-label": existing ? "Edit template" : "New template" },
      FSUi.el("h2", { text: existing ? "Edit template" : "New template" }),
      form));
    document.addEventListener("keydown", onKey);
    document.body.appendChild(backdrop);
    name.focus();
  }

  /** Client-side sample render for unsaved text (honest: labelled as sample). */
  function sampleRender(text) {
    return text
      .replace(/\{\{customerName\}\}/g, "Ramesh Kumar")
      .replace(/\{\{businessName\}\}/g, "Your Business")
      .replace(/\{\{serviceName\}\}/g, "the enquiry")
      .replace(/\{\{nextFollowUpDate\}\}/g, FSUi.fmtDate(new Date().toISOString().slice(0, 10)))
      + "\n\n(sample data)";
  }

  function row(t) {
    return FSUi.el("div", { class: "row-item", style: "align-items:flex-start" },
      FSUi.el("div", { class: "who" },
        FSUi.el("div", { class: "name" },
          t.templateName,
          t.isDefault ? FSUi.el("span", { class: "badge st-converted", style: "margin-left:8px",
            text: "Default" }) : null),
        FSUi.el("div", { class: "meta", style: "white-space:normal", text: t.messageBody })),
      FSUi.el("div", { class: "row", style: "gap:6px;flex:none" },
        FSUi.el("button", { class: "btn btn-secondary btn-sm", type: "button", text: "Edit",
          onclick: function () { openModal(t); } }),
        FSUi.el("button", { class: "btn btn-danger btn-sm", type: "button", text: "Delete",
          onclick: async function () {
            var yes = await FSUi.confirmDialog({
              title: "Delete this template?",
              message: '"' + t.templateName + '" will no longer be offered when preparing messages.',
              confirmText: "Delete", danger: true });
            if (!yes) return;
            try {
              await FSApi.del("/api/templates/" + t.id);
              FSUi.toast("Template deleted");
              load();
            } catch (err) {
              FSUi.toast(err.message, true);
            }
          } })));
  }

  async function load() {
    var rows = document.getElementById("rows");
    rows.textContent = "";
    rows.appendChild(FSUi.loadingBlock());
    try {
      var templates = await FSApi.get("/api/templates");
      rows.textContent = "";
      if (!templates.length) {
        rows.appendChild(FSUi.emptyState("No templates yet",
          "Save the messages you send often — greeting, price follow-up, festival wish.",
          FSUi.el("button", { class: "btn btn-primary", type: "button", text: "+ New template",
            onclick: function () { openModal(null); } })));
        return;
      }
      templates.forEach(function (t) { rows.appendChild(row(t)); });
    } catch (err) {
      rows.textContent = "";
      rows.appendChild(FSUi.errorState(err.message, load));
    }
  }

  document.getElementById("new-btn").addEventListener("click", function () {
    openModal(null);
  });

  (async function init() {
    try {
      var me = await FSAuth.requireAuth({ business: true });
      if (!me) return;
      FSNav.build(me);
      load();
    } catch (err) {
      FSUi.toast(err.message, true);
    }
  })();
})();
