/* Enquiry detail: profile, follow-ups, message log, history, edit/archive.
   Also renders the "Add enquiry" form when opened with ?new=1. */
(function () {
  "use strict";

  var params = new URLSearchParams(location.search);
  var enquiryId = params.get("id");
  var isNew = params.get("new") === "1";
  var enquiry = null;
  var templates = [];
  var followUps = [];
  var messages = [];
  var history = [];
  var root = document.getElementById("detail-root");

  /* ---------- shared form fields (create + edit) ---------- */

  function enquiryFields(values) {
    values = values || {};
    function field(labelText, input) {
      var wrap = FSUi.el("div", { class: "field" }, FSUi.el("label", { text: labelText }));
      wrap.appendChild(input);
      wrap.appendChild(FSUi.el("small"));
      return wrap;
    }
    function option(list, selected) {
      return list.map(function (pair) {
        var opt = FSUi.el("option", { value: pair[0], text: pair[1] });
        if (pair[0] === selected) opt.selected = true;
        return opt;
      });
    }
    var name = FSUi.el("input", { name: "customerName", type: "text", maxlength: "120", value: values.customerName || "" });
    var phone = FSUi.el("input", { name: "phoneNumber", type: "tel", maxlength: "20", inputmode: "tel", value: values.phoneNumber || "" });
    var email = FSUi.el("input", { name: "email", type: "email", maxlength: "180", value: values.email || "" });
    var service = FSUi.el("input", { name: "serviceName", type: "text", maxlength: "140", value: values.serviceName || "" });
    var source = FSUi.el("select", { name: "source" },
      option([["PHONE_CALL", "Phone call"], ["WALK_IN", "Walk-in"], ["WHATSAPP", "WhatsApp"],
        ["INSTAGRAM", "Instagram"], ["REFERRAL", "Referral"], ["OTHER", "Other"]], values.source || "WALK_IN"));
    var status = FSUi.el("select", { name: "status" },
      option([["NEW", "New"], ["CONTACTED", "Contacted"], ["INTERESTED", "Interested"],
        ["FOLLOW_UP_REQUIRED", "Follow-up required"], ["CONVERTED", "Converted"],
        ["NOT_INTERESTED", "Not interested"]], values.status || "NEW"));
    var priority = FSUi.el("select", { name: "priority" },
      option([["LOW", "Low"], ["MEDIUM", "Medium"], ["HIGH", "High"]], values.priority || "MEDIUM"));
    var notes = FSUi.el("textarea", { name: "notes", maxlength: "2000" });
    notes.value = values.notes || "";

    return [
      field("Customer name *", name),
      FSUi.el("div", { class: "row", style: "gap:var(--s-3);align-items:flex-start" },
        field("Phone", phone), field("Email", email)),
      field("What did they ask about? (service)", service),
      FSUi.el("div", { class: "row", style: "gap:var(--s-3);align-items:flex-start" },
        field("Source", source), field("Status", status), field("Priority", priority)),
      field("Notes", notes)
    ];
  }

  /** Empty string -> null: DTO @Pattern fields reject "" but accept null. */
  function blankToNull(value) {
    var trimmed = String(value === undefined || value === null ? "" : value).trim();
    return trimmed === "" ? null : trimmed;
  }

  function collectForm(form) {
    return {
      customerName: form.customerName.value.trim(),
      phoneNumber: blankToNull(form.phoneNumber.value),
      email: blankToNull(form.email.value),
      serviceName: blankToNull(form.serviceName.value),
      source: form.source.value,
      status: form.status.value,
      priority: form.priority.value,
      notes: blankToNull(form.notes.value)
    };
  }

  function validateEnquiry(form) {
    FSValidate.clearErrors(form);
    var ok = FSValidate.validateForm(form, {
      customerName: [FSValidate.rules.required, FSValidate.rules.maxLen(120)],
      phoneNumber: [FSValidate.rules.phone, FSValidate.rules.maxLen(20)],
      email: [FSValidate.rules.email, FSValidate.rules.maxLen(180)],
      serviceName: [FSValidate.rules.maxLen(140)],
      notes: [FSValidate.rules.maxLen(2000)]
    });
    return ok;
  }

  function submitButton(form, label) {
    return form.querySelector("button[type=submit]");
  }

  /* ---------- modal plumbing ---------- */

  function openModal(build) {
    var backdrop = FSUi.el("div", { class: "modal-backdrop" });
    function close() {
      document.removeEventListener("keydown", onKey);
      backdrop.remove();
    }
    function onKey(e) {
      if (e.key === "Escape") close();
    }
    var content = build(close);
    var modal = FSUi.el("div", {
      class: "modal", role: "dialog", "aria-modal": "true",
      "aria-label": content.label
    }, content.node);
    backdrop.appendChild(modal);
    backdrop.addEventListener("mousedown", function (e) {
      if (e.target === backdrop) close();
    });
    document.addEventListener("keydown", onKey);
    document.body.appendChild(backdrop);
    var first = modal.querySelector("input, select, textarea, button");
    if (first) first.focus();
    return close;
  }

  /* ---------- follow-up scheduling ---------- */

  function openFollowUpModal(existing) {
    openModal(function (close) {
      var form = FSUi.el("form", { novalidate: "novalidate" });
      var dateField = FSUi.el("div", { class: "field" },
        FSUi.el("label", { text: "Date *" }),
        FSUi.el("input", { name: "date", type: "date", required: "required",
          value: existing ? existing.date : "" }),
        FSUi.el("small"));
      var reasonField = FSUi.el("div", { class: "field" },
        FSUi.el("label", { text: "Why this follow-up? (reason)" }),
        FSUi.el("textarea", { name: "reason", maxlength: "300",
          text: undefined }),
        FSUi.el("small"));
      reasonField.querySelector("textarea").value = existing ? (existing.reason || "") : "";
      form.appendChild(dateField);
      form.appendChild(reasonField);
      form.appendChild(FSUi.el("div", { class: "row end" },
        FSUi.el("button", { class: "btn btn-secondary", type: "button", text: "Cancel",
          onclick: function () { close(); } }),
        FSUi.el("button", { class: "btn btn-primary", type: "submit",
          text: existing ? "Save new date" : "Schedule follow-up" })));

      form.addEventListener("submit", async function (event) {
        event.preventDefault();
        FSValidate.clearErrors(form);
        var ok = FSValidate.validateForm(form, {
          date: [FSValidate.rules.required, FSValidate.rules.date],
          reason: [FSValidate.rules.maxLen(300)]
        });
        if (!ok) return;
        var payload = { date: form.date.value, reason: form.reason.value.trim() };
        try {
          await FSValidate.busyButton(submitButton(form),
            existing
              ? FSApi.patch("/api/enquiries/" + enquiryId + "/follow-ups/" + existing.id + "/reschedule", payload)
              : FSApi.post("/api/enquiries/" + enquiryId + "/follow-ups", payload));
          FSUi.toast(existing ? "Follow-up rescheduled" : "Follow-up scheduled");
          close();
          loadDetail();
        } catch (err) {
          FSValidate.applyServerErrors(form, err.fieldErrors);
          if (!err.fieldErrors) FSUi.toast(err.message, true);
        }
      });

      return {
        label: existing ? "Reschedule follow-up" : "Schedule follow-up",
        node: FSUi.el("div", null,
          FSUi.el("h2", { text: existing ? "Reschedule follow-up" : "Schedule follow-up" }),
          FSUi.el("p", { class: "muted", text: "Pick a day — the daily list is organised by date, not clock time." }),
          form)
      };
    });
  }

  function openCompleteModal(fu) {
    openModal(function (close) {
      var form = FSUi.el("form", { novalidate: "novalidate" });
      var outcome = FSUi.el("select", { name: "outcomeType" });
      [["INTERESTED", "Interested — likely to buy"],
       ["NOT_INTERESTED", "Not interested"],
       ["NO_RESPONSE", "No response"],
       ["POSTPONED", "Asked to call later"]].forEach(function (pair) {
        outcome.appendChild(FSUi.el("option", { value: pair[0], text: pair[1] }));
      });
      var notes = FSUi.el("textarea", { name: "notes", maxlength: "2000" });
      form.appendChild(FSUi.el("div", { class: "field" },
        FSUi.el("label", { text: "Outcome *" }), outcome, FSUi.el("small")));
      form.appendChild(FSUi.el("div", { class: "field" },
        FSUi.el("label", { text: "Notes (what happened?)" }), notes, FSUi.el("small")));
      form.appendChild(FSUi.el("div", { class: "row end" },
        FSUi.el("button", { class: "btn btn-secondary", type: "button", text: "Cancel",
          onclick: function () { close(); } }),
        FSUi.el("button", { class: "btn btn-primary", type: "submit", text: "Mark done" })));

      form.addEventListener("submit", async function (event) {
        event.preventDefault();
        var payload = { outcomeType: form.outcomeType.value, notes: form.notes.value };
        try {
          await FSValidate.busyButton(submitButton(form),
            FSApi.patch("/api/enquiries/" + enquiryId + "/follow-ups/" + fu.id + "/complete", payload));
          FSUi.toast("Follow-up completed");
          close();
          loadDetail();
        } catch (err) {
          FSUi.toast(err.message, true);
        }
      });

      return {
        label: "Complete follow-up",
        node: FSUi.el("div", null,
          FSUi.el("h2", { text: "Complete follow-up" }),
          FSUi.el("p", { class: "muted", text: fu.customerName + " · " + FSUi.fmtDate(fu.date) }),
          form)
      };
    });
  }

  /* ---------- messages ---------- */

  function waLink(phone, text) {
    return "https://wa.me/" + String(phone || "").replace(/[^0-9]/g, "") +
      "?text=" + encodeURIComponent(text || "");
  }

  function openMessageModal() {
    openModal(function (close) {
      var form = FSUi.el("form", { novalidate: "novalidate" });
      var tpl = FSUi.el("select", { name: "template" },
        FSUi.el("option", { value: "", text: templates.length
          ? "Choose a template…" : "No templates yet — write below" }));
      templates.forEach(function (t) {
        tpl.appendChild(FSUi.el("option", { value: t.id, text: t.templateName }));
      });
      var channel = FSUi.el("select", { name: "channel" },
        FSUi.el("option", { value: "WHATSAPP", text: "WhatsApp" }),
        FSUi.el("option", { value: "SMS", text: "SMS" }));
      var body = FSUi.el("textarea", { name: "messageBody", maxlength: "2000", rows: "6" });

      form.appendChild(FSUi.el("div", { class: "row", style: "gap:var(--s-3);align-items:flex-start" },
        FSUi.el("div", { class: "field", style: "flex:1" },
          FSUi.el("label", { text: "Template" }), tpl, FSUi.el("small")),
        FSUi.el("div", { class: "field", style: "width:120px" },
          FSUi.el("label", { text: "Channel" }), channel, FSUi.el("small"))));
      form.appendChild(FSUi.el("div", { class: "field" },
        FSUi.el("label", { text: "Message *" }), body, FSUi.el("small")));
      form.appendChild(FSUi.el("div", { class: "row end" },
        FSUi.el("button", { class: "btn btn-secondary", type: "button", text: "Cancel",
          onclick: function () { close(); } }),
        FSUi.el("button", { class: "btn btn-primary", type: "submit", text: "Save message" })));

      tpl.addEventListener("change", async function () {
        if (!tpl.value) return;
        try {
          var preview = await FSApi.get("/api/templates/" + tpl.value + "/preview",
            { enquiryId: enquiryId });
          body.value = preview.rendered || "";
          if (preview.missingPlaceholders && preview.missingPlaceholders.length) {
            FSUi.toast("Unfilled placeholders: " + preview.missingPlaceholders.join(", "), true);
          }
        } catch (err) {
          FSUi.toast(err.message, true);
        }
      });

      form.addEventListener("submit", async function (event) {
        event.preventDefault();
        FSValidate.clearErrors(form);
        if (!FSValidate.validateForm(form, {
          messageBody: [FSValidate.rules.required, FSValidate.rules.maxLen(2000)]
        })) return;
        try {
          await FSValidate.busyButton(submitButton(form),
            FSApi.post("/api/enquiries/" + enquiryId + "/messages", {
              enquiryId: Number(enquiryId),
              messageBody: body.value,
              channel: channel.value
            }));
          FSUi.toast("Message saved to history");
          close();
          loadDetail();
        } catch (err) {
          FSValidate.applyServerErrors(form, err.fieldErrors);
          if (!err.fieldErrors) FSUi.toast(err.message, true);
        }
      });

      return {
        label: "Log a message",
        node: FSUi.el("div", null,
          FSUi.el("h2", { text: "Prepare a message" }),
          FSUi.el("p", { class: "muted", text: "Nothing is sent automatically. Save the wording, then open WhatsApp yourself." }),
          form)
      };
    });
  }

  /* ---------- edit / archive / delete ---------- */

  function openEditModal() {
    openModal(function (close) {
      var form = FSUi.el("form", { novalidate: "novalidate" });
      enquiryFields(enquiry).forEach(function (node) { form.appendChild(node); });
      form.appendChild(FSUi.el("div", { class: "row end" },
        FSUi.el("button", { class: "btn btn-secondary", type: "button", text: "Cancel",
          onclick: function () { close(); } }),
        FSUi.el("button", { class: "btn btn-primary", type: "submit", text: "Save changes" })));

      form.addEventListener("submit", async function (event) {
        event.preventDefault();
        if (!validateEnquiry(form)) return;
        try {
          await FSValidate.busyButton(submitButton(form),
            FSApi.patch("/api/enquiries/" + enquiryId, collectForm(form)));
          FSUi.toast("Enquiry updated");
          close();
          loadDetail();
        } catch (err) {
          FSValidate.applyServerErrors(form, err.fieldErrors);
          if (!err.fieldErrors) FSUi.toast(err.message, true);
        }
      });

      return {
        label: "Edit enquiry",
        node: FSUi.el("div", null,
          FSUi.el("h2", { text: "Edit enquiry" }), form)
      };
    });
  }

  async function toggleArchive() {
    var target = !enquiry.archived;
    var yes = await FSUi.confirmDialog({
      title: target ? "Archive this enquiry?" : "Restore this enquiry?",
      message: target
        ? "Archived enquiries leave the active lists. You can restore them any time."
        : "This enquiry will appear in the active lists again.",
      confirmText: target ? "Archive" : "Restore",
      danger: target
    });
    if (!yes) return;
    try {
      await FSApi.patch("/api/enquiries/" + enquiryId + "/archive", { archived: target });
      FSUi.toast(target ? "Enquiry archived" : "Enquiry restored");
      loadDetail();
    } catch (err) {
      FSUi.toast(err.message, true);
    }
  }

  async function deleteEnquiry() {
    var yes = await FSUi.confirmDialog({
      title: "Delete this enquiry?",
      message: "This permanently removes the enquiry, its follow-ups and message history. This cannot be undone.",
      confirmText: "Delete",
      danger: true
    });
    if (!yes) return;
    try {
      await FSApi.del("/api/enquiries/" + enquiryId);
      FSUi.toast("Enquiry deleted");
      location.replace("/enquiries.html");
    } catch (err) {
      FSUi.toast(err.message, true);
    }
  }

  /* ---------- rendering ---------- */

  function fact(label, value) {
    return FSUi.el("div", null,
      FSUi.el("div", { class: "faint", style: "font-size:var(--text-xs);font-weight:650", text: label }),
      FSUi.el("div", { text: value || "—" }));
  }

  function renderDetail() {
    root.textContent = "";

    var avatar = FSUi.el("div", { class: "avatar-lg", "aria-hidden": "true",
      text: enquiry.customerName.trim().charAt(0).toUpperCase() });
    var facts = FSUi.el("div", { class: "facts" },
      fact("Phone", enquiry.phoneNumber),
      fact("Email", enquiry.email),
      fact("Service", enquiry.serviceName),
      fact("Source", FSUi.sourceLabel(enquiry.source)),
      fact("Added", FSUi.fmtDate(enquiry.createdAt)));

    var statusSelect = FSUi.el("select", { "aria-label": "Change status",
      style: "width:auto" });
    [["NEW", "New"], ["CONTACTED", "Contacted"], ["INTERESTED", "Interested"],
     ["FOLLOW_UP_REQUIRED", "Follow-up required"], ["CONVERTED", "Converted"],
     ["NOT_INTERESTED", "Not interested"]].forEach(function (pair) {
      var opt = FSUi.el("option", { value: pair[0], text: pair[1] });
      if (enquiry.status === pair[0]) opt.selected = true;
      statusSelect.appendChild(opt);
    });
    statusSelect.addEventListener("change", async function () {
      try {
        enquiry = await FSApi.patch("/api/enquiries/" + enquiryId, { status: statusSelect.value });
        FSUi.toast("Status updated");
        loadDetail();
      } catch (err) {
        FSUi.toast(err.message, true);
        loadDetail();
      }
    });

    var actions = FSUi.el("div", { class: "row" },
      statusSelect,
      FSUi.el("button", { class: "btn btn-primary", type: "button", text: "Schedule follow-up",
        onclick: function () { openFollowUpModal(null); } }),
      FSUi.el("button", { class: "btn btn-secondary", type: "button", text: "Edit",
        onclick: openEditModal }),
      FSUi.el("button", { class: "btn btn-secondary", type: "button",
        text: enquiry.archived ? "Restore" : "Archive", onclick: toggleArchive }),
      FSUi.el("button", { class: "btn btn-danger", type: "button", text: "Delete",
        onclick: deleteEnquiry }));

    var head = FSUi.el("section", { class: "card detail-head" },
      FSUi.el("div", { class: "identity" },
        avatar,
        FSUi.el("div", { style: "min-width:0" },
          FSUi.el("h1", { style: "font-size:var(--text-2xl)", text: enquiry.customerName }),
          FSUi.el("div", { class: "row", style: "gap:6px;margin-top:6px" },
            FSUi.badge(enquiry.status),
            FSUi.priorityBadge(enquiry.priority),
            enquiry.archived ? FSUi.el("span", { class: "badge st-archived", text: "Archived" }) : null,
            enquiry.convertedAt
              ? FSUi.el("span", { class: "badge st-converted", text: "Converted " + FSUi.fmtDate(enquiry.convertedAt) })
              : null)),
        actions),
      facts);

    if (enquiry.notes) {
      head.appendChild(FSUi.el("div", { class: "rendered", text: enquiry.notes }));
    }

    /* follow-ups column */
    var fuRows = FSUi.el("div", { class: "rows" });
    if (!followUps.length) {
      fuRows.appendChild(FSUi.emptyState("No follow-ups yet",
        "Schedule the first one — the customer appears in your daily list on that date."));
    }
    followUps.forEach(function (fu) {
      var done = fu.status === "COMPLETED";
      var parts = FSUi.dayParts(fu.date);
      var right;
      if (done) {
        right = FSUi.el("span", { class: "badge bucket-done",
          text: "Done · " + FSUi.outcomeLabel(fu.outcomeType) });
      } else {
        var overdue = fu.date < todayIso();
        right = FSUi.el("span", { class: "badge " + (overdue ? "bucket-overdue" : "bucket-upcoming"),
          text: overdue ? "Overdue" : "Pending" });
      }
      fuRows.appendChild(FSUi.el("div", { class: "row-item" },
        FSUi.el("div", { class: "followup-date" + (overdue && !done ? " overdue" : "") },
          FSUi.el("span", { class: "d", text: parts.day }),
          FSUi.el("span", { class: "m", text: parts.mon })),
        FSUi.el("div", { class: "who" },
          FSUi.el("div", { class: "name", text: FSUi.fmtDate(fu.date) }),
          FSUi.el("div", { class: "meta", text: fu.reason || "—" })),
        right,
        done ? null : FSUi.el("button", { class: "btn btn-secondary btn-sm", type: "button",
          text: "Reschedule", onclick: function () { openFollowUpModal(fu); } }),
        done ? null : FSUi.el("button", { class: "btn btn-primary btn-sm", type: "button",
          text: "Complete", onclick: function () { openCompleteModal(fu); } })));
    });

    var fuCard = FSUi.el("section", { class: "card" },
      FSUi.el("div", { class: "card-head" },
        FSUi.el("h2", { text: "Follow-ups" }),
        FSUi.el("button", { class: "btn btn-ghost btn-sm", type: "button",
          text: "+ Schedule", onclick: function () { openFollowUpModal(null); } })),
      fuRows);

    /* messages column */
    var msgRows = FSUi.el("div", { class: "rows" });
    if (!messages.length) {
      msgRows.appendChild(FSUi.emptyState("No messages logged",
        "Save the wording you send so the whole team knows what was promised."));
    }
    messages.forEach(function (msg) {
      var copyBtn = FSUi.el("button", { class: "btn btn-secondary btn-sm", type: "button",
        text: "Copy", onclick: async function () {
          try {
            await navigator.clipboard.writeText(msg.messageBody);
            FSUi.toast("Copied");
          } catch (err) {
            FSUi.toast("Copy failed — select the text manually", true);
          }
        } });
      var openBtn = enquiry.phoneNumber
        ? FSUi.el("a", { class: "btn btn-ghost btn-sm", href: waLink(enquiry.phoneNumber, msg.messageBody),
            target: "_blank", rel: "noopener", text: "Open " + (msg.channel === "SMS" ? "SMS" : "WhatsApp") })
        : null;
      msgRows.appendChild(FSUi.el("div", { class: "row-item", style: "align-items:flex-start" },
        FSUi.el("div", { class: "who" },
          FSUi.el("div", { class: "name", text: FSUi.fmtDateTime(msg.sentAt) + " · " + msg.channel }),
          FSUi.el("div", { class: "meta", style: "white-space:normal", text: msg.messageBody })),
        FSUi.el("div", { class: "row", style: "gap:6px" }, copyBtn, openBtn)));
    });

    var msgCard = FSUi.el("section", { class: "card" },
      FSUi.el("div", { class: "card-head" },
        FSUi.el("h2", { text: "Messages" }),
        FSUi.el("button", { class: "btn btn-ghost btn-sm", type: "button", text: "+ New message",
          onclick: openMessageModal })),
      msgRows);

    /* history column */
    var timeline = FSUi.el("ul", { class: "timeline" });
    history.forEach(function (item) {
      timeline.appendChild(FSUi.el("li", null,
        FSUi.el("div", { text: item.description || FSUi.statusLabel(item.eventType) }),
        FSUi.el("div", { class: "when",
          text: FSUi.fmtDateTime(item.createdAt) + (item.actorName ? " · " + item.actorName : "") })));
    });
    var historyCard = FSUi.el("section", { class: "card" },
      FSUi.el("div", { class: "card-head" }, FSUi.el("h2", { text: "History" })),
      FSUi.el("div", { class: "card-body" }, history.length
        ? timeline
        : FSUi.el("p", { class: "muted", style: "margin:0", text: "Activity will appear here as you work this enquiry." })));

    root.appendChild(head);
    root.appendChild(FSUi.el("div", { class: "detail-grid mt-5" },
      FSUi.el("div", { class: "stack-4" }, fuCard, msgCard),
      historyCard));
  }

  function todayIso() {
    var now = new Date();
    return now.getFullYear() + "-" +
      String(now.getMonth() + 1).padStart(2, "0") + "-" +
      String(now.getDate()).padStart(2, "0");
  }

  /* ---------- new-enquiry mode ---------- */

  function renderNewForm() {
    document.title = "Add enquiry — FollowUp Saathi";
    root.textContent = "";
    var form = FSUi.el("form", { novalidate: "novalidate" });
    enquiryFields({}).forEach(function (node) { form.appendChild(node); });
    form.appendChild(FSUi.el("div", { class: "row end" },
      FSUi.el("a", { class: "btn btn-secondary", href: "/enquiries.html", text: "Cancel" }),
      FSUi.el("button", { class: "btn btn-primary", type: "submit", text: "Save enquiry" })));

    var card = FSUi.el("section", { class: "card card-pad", style: "max-width:640px" },
      FSUi.el("h1", { style: "font-size:var(--text-2xl)", text: "Add enquiry" }),
      FSUi.el("p", { class: "muted", text: "Name and contact are enough to start. You can schedule the first follow-up right after saving." }),
      form);
    root.appendChild(card);

    form.addEventListener("submit", async function (event) {
      event.preventDefault();
      if (!validateEnquiry(form)) return;
      var payload = collectForm(form);
      delete payload.status; // Create DTO has no status field
      try {
        var created = await FSValidate.busyButton(submitButton(form),
          FSApi.post("/api/enquiries", payload), { label: "Saving…" });
        FSUi.toast("Enquiry saved");
        location.replace("/enquiry-details.html?id=" + created.id);
      } catch (err) {
        FSValidate.applyServerErrors(form, err.fieldErrors);
        if (!err.fieldErrors) FSUi.toast(err.message, true);
      }
    });
  }

  /* ---------- data ---------- */

  async function loadDetail() {
    if (!enquiryId) {
      location.replace("/enquiries.html");
      return;
    }
    try {
      var results = await Promise.all([
        FSApi.get("/api/enquiries/" + enquiryId),
        FSApi.get("/api/enquiries/" + enquiryId + "/follow-ups"),
        FSApi.get("/api/enquiries/" + enquiryId + "/messages"),
        FSApi.get("/api/enquiries/" + enquiryId + "/history"),
        FSApi.get("/api/templates").catch(function () { return []; })
      ]);
      enquiry = results[0];
      followUps = results[1] || [];
      /* MessageLogController returns a Spring Page: { content: [...] }. */
      var rawMessages = results[2] || {};
      messages = Array.isArray(rawMessages) ? rawMessages : (rawMessages.content || []);
      history = results[3] || [];
      templates = results[4] || [];
      renderDetail();
    } catch (err) {
      root.textContent = "";
      root.appendChild(FSUi.errorState(err.status === 404
        ? "This enquiry does not exist (or belongs to another business)."
        : err.message, function () { location.replace("/enquiries.html"); }));
    }
  }

  (async function init() {
    try {
      var me = await FSAuth.requireAuth({ business: true });
      if (!me) return;
      FSNav.build(me);
      if (isNew) renderNewForm();
      else loadDetail();
    } catch (err) {
      FSUi.toast(err.message, true);
    }
  })();
})();
