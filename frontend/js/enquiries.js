/* Enquiries list: search, status/due/archived filters, pagination. */
(function () {
  "use strict";

  var state = { page: 0, size: 10, q: "", status: "", due: "", archived: "" };
  var debouncer = null;

  function enquiryRow(item) {
    var badges = FSUi.el("div", {
      class: "row",
      style: "gap:6px;justify-content:flex-end;flex:none"
    });
    if (item.hasPendingFollowUp) {
      badges.appendChild(FSUi.el("span", { class: "badge bucket-today", text: "Due" }));
    }
    badges.appendChild(FSUi.badge(item.status));
    badges.appendChild(FSUi.priorityBadge(item.priority));

    var metaParts = [];
    if (item.phoneNumber) metaParts.push(item.phoneNumber);
    if (item.serviceName) metaParts.push(item.serviceName);
    if (item.nextFollowUpDate) {
      metaParts.push("Next: " + FSUi.fmtDate(item.nextFollowUpDate));
    }

    return FSUi.el("a", { class: "row-item", href: "/enquiry-details.html?id=" + item.id },
      FSUi.el("div", { class: "who" },
        FSUi.el("div", { class: "name", text: item.customerName }),
        FSUi.el("div", { class: "meta", text: metaParts.join(" · ") || "—" })),
      badges);
  }

  async function load() {
    var rows = document.getElementById("rows");
    var pagination = document.getElementById("pagination");
    rows.textContent = "";
    rows.appendChild(FSUi.loadingBlock());

    var query = {
      page: state.page, size: state.size,
      q: state.q || undefined,
      status: state.status || undefined,
      due: state.due || undefined,
      archived: state.archived === "" ? undefined : state.archived
    };

    try {
      var data = await FSApi.get("/api/enquiries", query);
      rows.textContent = "";
      document.getElementById("count-line").textContent =
        data.totalElements + " enquiry" + (data.totalElements === 1 ? "" : "s");

      if (!data.items.length) {
        var filterActive = state.q || state.status || state.due ||
          state.archived === "true";
        rows.appendChild(filterActive
          ? FSUi.emptyState("No matching enquiries",
              "Try clearing the search or choosing a different filter.")
          : FSUi.emptyState("No enquiries yet",
              "Add your first enquiry — it takes about ten seconds.",
              FSUi.el("a", { class: "btn btn-primary", href: "/enquiry-details.html?new=1",
                text: "+ Add enquiry" })));
        pagination.classList.add("hidden");
        return;
      }

      data.items.forEach(function (item) { rows.appendChild(enquiryRow(item)); });

      pagination.classList.remove("hidden");
      document.getElementById("page-info").textContent =
        "Page " + (data.page + 1) + " of " + Math.max(data.totalPages, 1);
      document.getElementById("prev").disabled = data.page === 0;
      document.getElementById("next").disabled = data.page >= data.totalPages - 1;
    } catch (err) {
      rows.textContent = "";
      rows.appendChild(FSUi.errorState(err.message, load));
    }
  }

  function readFilters() {
    state.q = document.getElementById("q").value.trim();
    state.status = document.getElementById("status").value;
    state.due = document.getElementById("due").value;
    state.archived = document.getElementById("archived").value;
    state.page = 0;
  }

  document.getElementById("filter-form").addEventListener("submit", function (e) {
    e.preventDefault();
    readFilters();
    load();
  });
  document.getElementById("q").addEventListener("input", function () {
    clearTimeout(debouncer);
    debouncer = setTimeout(function () {
      readFilters();
      load();
    }, 300);
  });
  ["status", "due", "archived"].forEach(function (id) {
    document.getElementById(id).addEventListener("change", function () {
      readFilters();
      load();
    });
  });
  document.getElementById("prev").addEventListener("click", function () {
    state.page = Math.max(state.page - 1, 0);
    load();
  });
  document.getElementById("next").addEventListener("click", function () {
    state.page += 1;
    load();
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
