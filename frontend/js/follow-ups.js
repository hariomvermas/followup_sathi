/* Follow-up list: bucket segments, complete with outcome, reschedule. */
(function () {
  "use strict";

  var bucket = "ALL";
  var counts = { todayCount: 0, upcomingCount: 0, overdueCount: 0 };

  function todayIso() {
    var now = new Date();
    return now.getFullYear() + "-" +
      String(now.getMonth() + 1).padStart(2, "0") + "-" +
      String(now.getDate()).padStart(2, "0");
  }

  function bucketFor(item) {
    if (item.date === todayIso()) return "TODAY";
    if (item.date < todayIso()) return "OVERDUE";
    return "UPCOMING";
  }

  function row(item) {
    var parts = FSUi.dayParts(item.date);
    var itemBucket = bucketFor(item);
    return FSUi.el("div", { class: "row-item" },
      FSUi.el("div", { class: "followup-date" + (item.overdue ? " overdue" : "") },
        FSUi.el("span", { class: "d", text: parts.day }),
        FSUi.el("span", { class: "m", text: parts.mon })),
      FSUi.el("a", { class: "who", href: "/enquiry-details.html?id=" + item.enquiryId,
        style: "text-decoration:none" },
        FSUi.el("div", { class: "name", text: item.customerName }),
        FSUi.el("div", { class: "meta",
          text: [item.reason, item.serviceName].filter(Boolean).join(" · ") || item.customerPhone || "" })),
      FSUi.el("span", { class: "badge bucket-" + itemBucket.toLowerCase(),
        text: itemBucket === "TODAY" ? "Today" : itemBucket === "OVERDUE" ? "Overdue" : "Upcoming" }),
      FSUi.el("button", { class: "btn btn-primary btn-sm", type: "button", text: "Complete",
        onclick: function () { openComplete(item); } }),
      FSUi.el("button", { class: "btn btn-secondary btn-sm", type: "button", text: "Reschedule",
        onclick: function () { openReschedule(item); } }));
  }

  function openComplete(item) {
    var backdrop = FSUi.el("div", { class: "modal-backdrop" });
    function close() {
      document.removeEventListener("keydown", onKey);
      backdrop.remove();
    }
    function onKey(e) { if (e.key === "Escape") close(); }

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
        onclick: close }),
      FSUi.el("button", { class: "btn btn-primary", type: "submit", text: "Mark done" })));

    form.addEventListener("submit", async function (event) {
      event.preventDefault();
      try {
        await FSValidate.busyButton(form.querySelector("button[type=submit]"),
          FSApi.patch("/api/enquiries/" + item.enquiryId + "/follow-ups/" +
            item.followUpId + "/complete",
            { outcomeType: form.outcomeType.value, notes: form.notes.value }));
        FSUi.toast("Follow-up completed");
        close();
        load();
      } catch (err) {
        FSUi.toast(err.message, true);
      }
    });

    backdrop.appendChild(FSUi.el("div", { class: "modal", role: "dialog", "aria-modal": "true",
      "aria-label": "Complete follow-up" },
      FSUi.el("h2", { text: "Complete follow-up" }),
      FSUi.el("p", { class: "muted", text: item.customerName + " · " + FSUi.fmtDate(item.date) }),
      form));
    document.addEventListener("keydown", onKey);
    document.body.appendChild(backdrop);
    outcome.focus();
  }

  function openReschedule(item) {
    var backdrop = FSUi.el("div", { class: "modal-backdrop" });
    function close() {
      document.removeEventListener("keydown", onKey);
      backdrop.remove();
    }
    function onKey(e) { if (e.key === "Escape") close(); }

    var form = FSUi.el("form", { novalidate: "novalidate" });
    form.appendChild(FSUi.el("div", { class: "field" },
      FSUi.el("label", { text: "New date *" }),
      FSUi.el("input", { name: "date", type: "date", value: item.date, required: "required" }),
      FSUi.el("small")));
    form.appendChild(FSUi.el("div", { class: "field" },
      FSUi.el("label", { text: "Reason" }),
      FSUi.el("textarea", { name: "reason", maxlength: "300" }),
      FSUi.el("small")));
    form.reason.value = item.reason || "";
    form.appendChild(FSUi.el("div", { class: "row end" },
      FSUi.el("button", { class: "btn btn-secondary", type: "button", text: "Cancel",
        onclick: close }),
      FSUi.el("button", { class: "btn btn-primary", type: "submit", text: "Save new date" })));

    form.addEventListener("submit", async function (event) {
      event.preventDefault();
      FSValidate.clearErrors(form);
      if (!FSValidate.validateForm(form, {
        date: [FSValidate.rules.required, FSValidate.rules.date]
      })) return;
      try {
        await FSValidate.busyButton(form.querySelector("button[type=submit]"),
          FSApi.patch("/api/enquiries/" + item.enquiryId + "/follow-ups/" +
            item.followUpId + "/reschedule",
            { date: form.date.value, reason: form.reason.value.trim() }));
        FSUi.toast("Follow-up rescheduled");
        close();
        load();
      } catch (err) {
        FSValidate.applyServerErrors(form, err.fieldErrors);
        if (!err.fieldErrors) FSUi.toast(err.message, true);
      }
    });

    backdrop.appendChild(FSUi.el("div", { class: "modal", role: "dialog", "aria-modal": "true",
      "aria-label": "Reschedule follow-up" },
      FSUi.el("h2", { text: "Reschedule follow-up" }),
      FSUi.el("p", { class: "muted", text: item.customerName + " · currently " + FSUi.fmtDate(item.date) }),
      form));
    document.addEventListener("keydown", onKey);
    document.body.appendChild(backdrop);
    form.date.focus();
  }

  async function load() {
    var rows = document.getElementById("rows");
    rows.textContent = "";
    rows.appendChild(FSUi.loadingBlock());
    try {
      var data = await FSApi.get("/api/follow-ups", { bucket: bucket === "ALL" ? undefined : bucket });
      counts = data;
      document.getElementById("cnt-today").textContent = "(" + data.todayCount + ")";
      document.getElementById("cnt-upcoming").textContent = "(" + data.upcomingCount + ")";
      document.getElementById("cnt-overdue").textContent = "(" + data.overdueCount + ")";

      rows.textContent = "";
      document.getElementById("bucket-line").textContent =
        bucket === "ALL"
          ? (data.items.length
            ? data.items.length + " pending follow-up" + (data.items.length === 1 ? "" : "s")
            : "")
          : "";

      if (!data.items.length) {
        var messages = {
          ALL: ["Nothing pending", "Every scheduled follow-up is completed. New ones appear here when you schedule them."],
          TODAY: ["Today is clear", "No follow-ups scheduled for today. Overdue ones still show in their own tab."],
          OVERDUE: ["No overdue follow-ups", "You are all caught up."],
          UPCOMING: ["Nothing upcoming", "Schedule follow-ups from an enquiry to fill this list."]
        };
        rows.appendChild(FSUi.emptyState(messages[bucket][0], messages[bucket][1]));
        return;
      }
      data.items.forEach(function (item) { rows.appendChild(row(item)); });
    } catch (err) {
      rows.textContent = "";
      rows.appendChild(FSUi.errorState(err.message, load));
    }
  }

  [].forEach.call(document.querySelectorAll(".segments button"), function (button) {
    button.addEventListener("click", function () {
      bucket = button.getAttribute("data-bucket");
      [].forEach.call(document.querySelectorAll(".segments button"), function (b) {
        b.setAttribute("aria-pressed", String(b === button));
      });
      load();
    });
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
