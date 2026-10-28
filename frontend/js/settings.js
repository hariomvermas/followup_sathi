/* Settings: business profile, reminders, password change, account deletion. */
(function () {
  "use strict";

  var DAYS = [
    ["1", "Mon"], ["2", "Tue"], ["3", "Wed"], ["4", "Thu"],
    ["5", "Fri"], ["6", "Sat"], ["0", "Sun"]
  ];

  function buildDayChips(workingDays) {
    var wrap = document.getElementById("day-chips");
    wrap.textContent = "";
    DAYS.forEach(function (day) {
      var box = FSUi.el("input", { type: "checkbox", name: "workingDays",
        value: day[0] });
      box.checked = !workingDays || workingDays.indexOf(Number(day[0])) !== -1;
      wrap.appendChild(FSUi.el("label", null, box, day[1]));
    });
  }

  function fill(biz) {
    var bizForm = document.getElementById("biz-form");
    bizForm.businessName.value = biz.businessName || "";
    bizForm.businessCategory.value = biz.businessCategory || "";
    bizForm.timeZone.value = biz.timeZone || "Asia/Kolkata";

    var remForm = document.getElementById("rem-form");
    remForm.reminderEnabled.checked = biz.reminderEnabled !== false;
    remForm.reminderTime.value = biz.reminderTime || "10:00";
    buildDayChips(biz.workingDays);
  }

  function bizPayload(bizForm, remForm) {
    var days = [].filter.call(remForm.workingDays, function (box) { return box.checked; })
      .map(function (box) { return Number(box.value); });
    return {
      businessName: bizForm.businessName.value.trim(),
      businessCategory: bizForm.businessCategory.value.trim(),
      timeZone: bizForm.timeZone.value.trim() || "Asia/Kolkata",
      // Reminders share one endpoint; both forms send the full settings body.
      preferredLanguage: "en",
      workingDays: days,
      reminderEnabled: remForm.reminderEnabled.checked,
      reminderTime: remForm.reminderTime.value
    };
  }

  function saveBusiness(forms) {
    var button = document.getElementById("biz-save");
    var payload = bizPayload(forms.bizForm, forms.remForm);
    try {
      new Intl.DateTimeFormat("en", { timeZone: payload.timeZone });
    } catch (ignored) {
      FSValidate.fieldError(forms.bizForm.timeZone, "Unknown time zone (e.g. Asia/Kolkata)");
      forms.bizForm.timeZone.focus();
      return;
    }
    FSValidate.busyButton(button, FSApi.put("/api/business", payload))
      .then(function (biz) {
        FSUi.toast("Profile saved");
        fill(biz);
      })
      .catch(function (err) {
        FSValidate.applyServerErrors(forms.bizForm, err.fieldErrors);
        if (!err.fieldErrors) FSUi.toast(err.message, true);
      });
  }

  function saveReminders(forms) {
    var button = document.getElementById("rem-save");
    var bizForm = forms.bizForm;
    FSValidate.clearErrors(forms.remForm);
    if (!FSValidate.validateForm(forms.remForm, {
      reminderTime: [FSValidate.rules.required, FSValidate.rules.time]
    })) return;
    // The upsert endpoint needs the profile fields too; reject client-side
    // when they are empty rather than silently overwriting them.
    if (!bizForm.businessName.value.trim() || !bizForm.businessCategory.value.trim()) {
      FSUi.toast("Save the business profile first (name and type are required)", true);
      return;
    }
    FSValidate.busyButton(button, FSApi.put("/api/business", bizPayload(bizForm, forms.remForm)))
      .then(function (biz) {
        FSUi.toast("Reminders saved");
        fill(biz);
      })
      .catch(function (err) {
        FSValidate.applyServerErrors(forms.remForm, err.fieldErrors);
        if (!err.fieldErrors) FSUi.toast(err.message, true);
      });
  }

  function changePassword() {
    var form = document.getElementById("pw-form");
    FSValidate.clearErrors(form);
    if (!FSValidate.validateForm(form, {
      currentPassword: [FSValidate.rules.required],
      newPassword: [FSValidate.rules.required, FSValidate.rules.password],
      confirmPassword: [FSValidate.rules.required]
    })) return;
    if (form.newPassword.value !== form.confirmPassword.value) {
      FSValidate.fieldError(form.confirmPassword, "The two passwords do not match");
      return;
    }
    if (form.newPassword.value === form.currentPassword.value) {
      FSValidate.fieldError(form.newPassword, "New password must be different from the current one");
      return;
    }
    var button = document.getElementById("pw-save");
    FSValidate.busyButton(button,
      FSApi.post("/api/account/password", {
        currentPassword: form.currentPassword.value,
        newPassword: form.newPassword.value
      }))
      .then(function () {
        FSUi.toast("Password changed");
        form.currentPassword.value = "";
        form.newPassword.value = "";
        form.confirmPassword.value = "";
      })
      .catch(function (err) {
        FSValidate.applyServerErrors(form, err.fieldErrors);
        if (!err.fieldErrors) FSUi.toast(err.message, true);
      });
  }

  function deleteAccount() {
    var form = document.getElementById("del-form");
    FSValidate.clearErrors(form);
    if (!FSValidate.validateForm(form, {
      password: [FSValidate.rules.required],
      confirmText: [FSValidate.rules.required]
    })) return;
    if (form.confirmText.value.trim() !== "DELETE") {
      FSValidate.fieldError(form.confirmText, "Type DELETE (in capitals) to confirm");
      return;
    }
    FSUi.confirmDialog({
      title: "Delete your account?",
      message: "Every enquiry, follow-up, template and message log will be removed " +
        "permanently. This cannot be undone.",
      confirmText: "Delete everything", danger: true
    }).then(function (yes) {
      if (!yes) return;
      var button = document.getElementById("del-btn");
      FSValidate.busyButton(button,
        FSApi.del("/api/account/me", {
          password: form.password.value,
          confirmText: form.confirmText.value.trim()
        }))
        .then(function () {
          try { sessionStorage.clear(); } catch (ignored) { /* optional */ }
          location.replace("/login.html");
        })
        .catch(function (err) {
          FSValidate.applyServerErrors(form, err.fieldErrors);
          if (!err.fieldErrors) FSUi.toast(err.message, true);
        });
    });
  }

  (async function init() {
    try {
      var me = await FSAuth.requireAuth({ business: true });
      if (!me) return;
      FSNav.build(me);
    } catch (err) {
      FSUi.toast(err.message, true);
      return;
    }

    var forms = {
      bizForm: document.getElementById("biz-form"),
      remForm: document.getElementById("rem-form")
    };

    try {
      var biz = await FSApi.get("/api/business");
      fill(biz);
    } catch (err) {
      FSUi.toast(err.message, true);
    }

    document.getElementById("biz-form").addEventListener("submit", function (event) {
      event.preventDefault();
      FSValidate.clearErrors(forms.bizForm);
      if (!FSValidate.validateForm(forms.bizForm, {
        businessName: [FSValidate.rules.required, FSValidate.rules.maxLen(140)],
        businessCategory: [FSValidate.rules.required, FSValidate.rules.maxLen(80)],
        timeZone: [FSValidate.rules.required, FSValidate.rules.maxLen(40)]
      })) return;
      saveBusiness(forms);
    });

    document.getElementById("rem-form").addEventListener("submit", function (event) {
      event.preventDefault();
      saveReminders(forms);
    });

    document.getElementById("pw-form").addEventListener("submit", function (event) {
      event.preventDefault();
      changePassword();
    });

    document.getElementById("del-form").addEventListener("submit", function (event) {
      event.preventDefault();
      deleteAccount();
    });
  })();
})();
