/* Field validation helpers: mirror of the backend Bean Validation rules,
   so users get feedback before the request is sent. */
(function (global) {
  "use strict";

  var EMAIL = /^[^@\s]+@[^@\s]+\.[^@\s]+$/;
  var PHONE = /^[0-9+\-\s]*$/;
  var DATE = /^\d{4}-\d{2}-\d{2}$/;
  var TIME = /^([01]\d|2[0-3]):[0-5]\d$/;

  var rules = {
    required: function (value, msg) {
      if (value === undefined || value === null || String(value).trim() === "") {
        return msg || "This field is required";
      }
      return null;
    },
    email: function (value, msg) {
      if (!value) return null;
      return EMAIL.test(value.trim()) ? null : (msg || "Enter a valid email address");
    },
    phone: function (value, msg) {
      if (!value) return null;
      return PHONE.test(value) ? null : (msg || "Only digits, spaces, + and - allowed");
    },
    maxLen: function (max) {
      return function (value) {
        if (!value) return null;
        return value.length > max ? "Must be at most " + max + " characters" : null;
      };
    },
    minLen: function (min) {
      return function (value) {
        if (!value) return null;
        return value.length < min ? "Must be at least " + min + " characters" : null;
      };
    },
    date: function (value, msg) {
      if (!value) return null;
      return DATE.test(value) ? null : (msg || "Use YYYY-MM-DD");
    },
    time: function (value, msg) {
      if (!value) return null;
      return TIME.test(value) ? null : (msg || "Use 24-hour HH:mm format");
    },
    password: function (value) {
      if (!value) return null;
      if (value.length < 8) return "Must be at least 8 characters";
      if (value.length > 72) return "Must be at most 72 characters";
      return null;
    }
  };

  function fieldError(input, message) {
    var field = input.closest(".field");
    if (!field) return;
    var small = field.querySelector("small");
    if (message) {
      field.classList.add("invalid");
      if (!small) {
        small = document.createElement("small");
        field.appendChild(small);
      }
      small.textContent = message;
    } else {
      field.classList.remove("invalid");
      if (small) small.textContent = "";
    }
  }

  /**
   * validateForm(form, schema)
   * schema: { inputName: [ruleFn, ruleFn, …] }
   * Marks .field.invalid, focuses the first bad input, returns ok:boolean.
   */
  function validateForm(form, schema) {
    var ok = true;
    var firstBad = null;
    Object.keys(schema).forEach(function (name) {
      var input = form.elements[name];
      if (!input) return;
      var value = input.type === "checkbox"
        ? (input.checked ? "on" : "")
        : input.value;
      var message = null;
      schema[name].some(function (rule) {
        message = rule(value);
        return !!message;
      });
      fieldError(input, message);
      if (message) {
        ok = false;
        if (!firstBad) firstBad = input;
      }
    });
    if (firstBad) firstBad.focus();
    return ok;
  }

  /** Apply server-side fieldErrors ({name: [messages]}) to a form. */
  function applyServerErrors(form, fieldErrors) {
    if (!fieldErrors) return;
    Object.keys(fieldErrors).forEach(function (name) {
      var input = form.elements[name];
      if (input) fieldError(input, [].concat(fieldErrors[name]).join(" "));
    });
  }

  function clearErrors(form) {
    [].forEach.call(form.querySelectorAll(".field"), function (field) {
      field.classList.remove("invalid");
      var small = field.querySelector("small");
      if (small) small.textContent = "";
    });
  }

  /** Button state guard: disables during async work, restores after. */
  function busyButton(button, promise, opts) {
    opts = opts || {};
    var original = button.textContent;
    button.disabled = true;
    button.setAttribute("aria-busy", "true");
    if (opts.label) button.textContent = opts.label;
    return promise.finally(function () {
      button.disabled = false;
      button.removeAttribute("aria-busy");
      button.textContent = original;
    });
  }

  global.FSValidate = {
    rules: rules,
    fieldError: fieldError,
    validateForm: validateForm,
    applyServerErrors: applyServerErrors,
    clearErrors: clearErrors,
    busyButton: busyButton
  };
})(window);
