/* Shared topbar for every app page, injected by navigation.js.
   Server data reaches the page after requireAuth, so the bar is filled here. */
(function (global) {
  "use strict";

  var LINKS = [
    { href: "/dashboard.html", label: "Dashboard" },
    { href: "/enquiries.html", label: "Enquiries" },
    { href: "/follow-ups.html", label: "Follow-ups" },
    { href: "/templates.html", label: "Templates" },
    { href: "/reports.html", label: "Reports" },
    { href: "/settings.html", label: "Settings" }
  ];

  function initials(name) {
    return (name || "?")
      .trim()
      .split(/\s+/)
      .slice(0, 2)
      .map(function (part) { return part.charAt(0).toUpperCase(); })
      .join("");
  }

  function build(me) {
    var nav = document.createElement("nav");
    nav.setAttribute("aria-label", "Main");
    LINKS.forEach(function (link) {
      nav.appendChild(FSUi.el("a", {
        href: link.href,
        text: link.label,
        "aria-current": location.pathname === link.href ? "page" : null
      }));
    });

    var chip = FSUi.el("div", { class: "user-chip" },
      FSUi.el("span", { class: "avatar", "aria-hidden": "true", text: initials(me.name) }),
      FSUi.el("b", { text: me.name }));

    var logoutBtn = FSUi.el("button", {
      class: "btn btn-secondary btn-sm",
      type: "button",
      text: "Log out",
      onclick: async function () {
        logoutBtn.disabled = true;
        await FSAuth.logout();
        location.replace("/login.html");
      }
    });

    var actions = FSUi.el("div", { class: "topbar-actions" }, chip, logoutBtn);

    var inner = FSUi.el("div", { class: "topbar-inner" },
      FSUi.el("a", { class: "brand", href: "/dashboard.html" },
        FSUi.el("span", { class: "brand-mark", "aria-hidden": "true", text: "FS" }),
        "FollowUp Saathi"),
      nav,
      actions);

    var header = FSUi.el("header", { class: "topbar" }, inner);
    var mount = document.querySelector("[data-topbar]");
    if (mount) {
      mount.replaceWith(header);
    } else {
      document.body.prepend(header);
    }
  }

  global.FSNav = { build: build };
})(window);
