/* Reports: range segments (+custom from/to), summary tiles, pipeline bars, CSV export. */
(function () {
  "use strict";

  var range = "THIS_MONTH";

  var STAT_DEFS = [
    { key: "enquiriesCreated", label: "Enquiries created" },
    { key: "enquiriesConverted", label: "Converted" },
    { key: "followUpsCompleted", label: "Follow-ups completed" },
    { key: "followUpsScheduled", label: "Follow-ups scheduled" },
    { key: "pendingFollowUps", label: "Pending follow-ups" },
    { key: "conversionRatePercent", label: "Conversion rate" }
  ];

  function statCard(label, value) {
    return FSUi.el("div", { class: "card stat" },
      FSUi.el("span", { class: "num", text: String(value) }),
      FSUi.el("span", { class: "lbl", text: label }));
  }

  function fmtValue(key, value) {
    if (key === "conversionRatePercent") {
      return (value === null || value === undefined ? 0 : value) + "%";
    }
    return value;
  }

  function renderStats(view) {
    var stats = document.getElementById("stats");
    stats.textContent = "";
    STAT_DEFS.forEach(function (def) {
      stats.appendChild(statCard(def.label, fmtValue(def.key, view[def.key])));
    });
  }

  function renderPipeline(view) {
    var body = document.getElementById("pipeline");
    body.textContent = "";
    var counts = {};
    view.statusCounts.forEach(function (sc) { counts[sc.status] = sc.count; });
    var order = ["NEW", "CONTACTED", "INTERESTED", "FOLLOW_UP_REQUIRED", "CONVERTED"];
    var total = order.reduce(function (sum, s) { return sum + (counts[s] || 0); }, 0);
    if (!total) {
      body.appendChild(FSUi.el("p", { class: "muted", style: "margin:0" },
        "No active enquiries yet."));
      return;
    }
    order.forEach(function (status) {
      var count = counts[status] || 0;
      if (!count) return;
      var percent = Math.round((count / total) * 100);
      body.appendChild(FSUi.el("div", { class: "bar-row", style: "margin-bottom:var(--s-3)" },
        FSUi.el("div", { class: "bar-top" },
          FSUi.el("span", { text: FSUi.statusLabel(status) }),
          FSUi.el("b", { text: count })),
        FSUi.el("div", { class: "bar-track" },
          FSUi.el("div", {
            class: "bar-fill",
            style: "width:" + percent + "%;background:var(--st-" +
              status.toLowerCase().replace(/_/g, "-") + ")"
          }))));
    });
  }

  function rangeLine(view) {
    var label = {
      THIS_MONTH: "This month", THIS_WEEK: "This week", LAST_30_DAYS: "Last 30 days"
    }[view.range] || "Custom period";
    return label + " · " + FSUi.fmtDate(view.fromDate) + " – " + FSUi.fmtDate(view.toDate);
  }

  async function load() {
    var stats = document.getElementById("stats");
    stats.textContent = "";
    stats.appendChild(FSUi.loadingBlock());
    try {
      var view = await FSApi.get("/api/reports/summary", {
        range: range === "CUSTOM" ? undefined : range,
        from: range === "CUSTOM" ? document.getElementById("rep-from").value : undefined,
        to: range === "CUSTOM" ? document.getElementById("rep-to").value : undefined
      });
      document.getElementById("range-line").textContent = rangeLine(view);
      renderStats(view);
      renderPipeline(view);
    } catch (err) {
      stats.textContent = "";
      stats.appendChild(FSUi.errorState(err.message, load));
    }
  }

  function initSegments() {
    [].forEach.call(document.querySelectorAll(".segments button"), function (button) {
      button.addEventListener("click", function () {
        range = button.getAttribute("data-range");
        [].forEach.call(document.querySelectorAll(".segments button"), function (b) {
          b.setAttribute("aria-pressed", String(b === button));
        });
        document.getElementById("custom-form").classList
          .toggle("hidden", range !== "CUSTOM");
        if (range !== "CUSTOM") load();
      });
    });
  }

  document.getElementById("custom-form").addEventListener("submit", function (event) {
    event.preventDefault();
    var form = event.target;
    FSValidate.clearErrors(form);
    if (!FSValidate.validateForm(form, {
      from: [FSValidate.rules.required, FSValidate.rules.date],
      to: [FSValidate.rules.required, FSValidate.rules.date]
    })) return;
    if (form.to.value < form.from.value) {
      FSValidate.fieldError(form.to, "To date cannot be before the from date");
      return;
    }
    load();
  });

  document.getElementById("export-btn").addEventListener("click", async function () {
    var button = this;
    var params = { format: "csv" };
    if (range === "CUSTOM") {
      var from = document.getElementById("rep-from").value;
      var to = document.getElementById("rep-to").value;
      if (!from || !to) {
        FSUi.toast("Pick both custom dates first", true);
        return;
      }
      params.from = from;
      params.to = to;
    } else {
      params.range = range;
    }
    try {
      button.disabled = true;
      await FSApi.downloadCsv("/api/reports/summary", params);
      FSUi.toast("CSV downloaded");
    } catch (err) {
      if (err.status !== 401) FSUi.toast(err.message, true);
    } finally {
      button.disabled = false;
    }
  });

  (async function init() {
    try {
      var me = await FSAuth.requireAuth({ business: true });
      if (!me) return;
      FSNav.build(me);
      initSegments();

      var sub = document.querySelector(".page-head .sub");
      var line = FSUi.el("div", { class: "muted", id: "range-line",
        style: "font-size:var(--text-sm);margin-top:2px" });
      sub.parentNode.insertBefore(line, sub.nextSibling);

      load();
    } catch (err) {
      FSUi.toast(err.message, true);
    }
  })();
})();
