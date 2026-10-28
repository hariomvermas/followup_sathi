/* Dashboard: stat tiles, today/overdue list, pipeline bars. */
(function () {
  "use strict";

  function statNumber(id, value) {
    var node = document.getElementById(id);
    if (node) node.textContent = String(value);
  }

  function followUpRow(item) {
    var who = FSUi.el("div", { class: "who" },
      FSUi.el("div", { class: "name", text: item.customerName }),
      FSUi.el("div", {
        class: "meta",
        text: [item.reason, item.serviceName].filter(Boolean).join(" · ") ||
              item.customerPhone || ""
      }));

    var date = FSUi.dayParts(item.date);
    var badge = FSUi.el("span", {
      class: "badge " + (item.overdue ? "bucket-overdue" : "bucket-today"),
      text: item.overdue ? "Overdue" : "Today"
    });

    return FSUi.el("a", {
      class: "row-item",
      href: "/enquiry-details.html?id=" + item.enquiryId
    },
      FSUi.el("div", { class: "followup-date" + (item.overdue ? " overdue" : "") },
        FSUi.el("span", { class: "d", text: date.day }),
        FSUi.el("span", { class: "m", text: date.mon })),
      who,
      badge);
  }

  function renderPipeline(dash) {
    var body = document.getElementById("pipeline-body");
    body.textContent = "";
    var statuses = [
      ["NEW", dash.newEnquiries],
      ["CONTACTED", dash.contacted],
      ["INTERESTED", dash.interested],
      ["FOLLOW_UP_REQUIRED", dash.followUpRequired],
      ["CONVERTED", dash.converted]
    ];
    var total = dash.totalEnquiries || 1;
    statuses.forEach(function (pair) {
      var status = pair[0];
      var count = pair[1];
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
    if (!dash.totalEnquiries) {
      body.appendChild(FSUi.el("p", { class: "muted", style: "margin:var(--s-2) 0 0" },
        "No enquiries yet — add your first one."));
    }
  }

  async function init() {
    try {
      var me = await FSAuth.requireAuth({ business: true });
      if (!me) return;
      FSNav.build(me);
    } catch (err) {
      FSUi.toast(err.message, true);
      return;
    }

    try {
      var biz = await FSApi.get("/api/business");
      document.getElementById("biz-line").textContent =
        biz.businessName + " · " + biz.timeZone;
    } catch (ignored) {
      document.getElementById("biz-line").textContent = "";
    }

    try {
      var dash = await FSApi.get("/api/dashboard");
      statNumber("st-total", dash.totalEnquiries);
      statNumber("st-due", dash.dueToday);
      statNumber("st-overdue", dash.overdue);
      statNumber("st-converted", dash.converted);

      var rows = document.getElementById("today-rows");
      rows.textContent = "";
      if (!dash.todayItems || !dash.todayItems.length) {
        rows.appendChild(FSUi.emptyState("All clear for today",
          "No follow-ups are due. New enquiries scheduled for today appear here."));
      } else {
        dash.todayItems.forEach(function (item) {
          rows.appendChild(followUpRow(item));
        });
      }
      renderPipeline(dash);
    } catch (err) {
      var rows2 = document.getElementById("today-rows");
      rows2.textContent = "";
      rows2.appendChild(FSUi.errorState(err.message, init));
    }
  }

  init();
})();
