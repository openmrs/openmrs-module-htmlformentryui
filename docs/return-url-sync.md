# `returnUrl` client/server sync on form submission

## Problem

When the `enterHtmlForm` fragment renders, `returnUrl` is written to the page in two
independent places, both seeded from the same server-side value
(`command.returnUrl`):

- `omod/src/main/webapp/fragments/htmlform/enterHtmlForm.gsp:53-55` calls
  `htmlForm.setReturnUrl(...)`, which stores the value in a JS closure variable
  in `htmlForm.js`.
- `omod/src/main/webapp/fragments/htmlform/enterHtmlForm.gsp:138-140` renders a
  hidden `<input type="hidden" name="returnUrl">` with the same value.

`htmlForm.setReturnUrl(url)` (`omod/src/main/webapp/resources/scripts/htmlForm.js:309-311`)
is a public function on the global `htmlForm` object, and forms are expected to call it
after page load to change the navigation target dynamically (this is how the O3
`"post-message:..."` iframe-integration workflow works). That call only updates the
JS closure variable — nothing updates the hidden input to match.

This left two copies of `returnUrl` that could diverge after the initial render:

- The **hidden input** is what gets serialized into the AJAX submit body
  (`doSubmitHtmlForm` in `htmlForm.js`), and is what the server reads in
  `EnterHtmlFormFragmentController.submit()`
  (`omod/src/main/java/.../EnterHtmlFormFragmentController.java:227`). The server
  uses that value for one thing: deciding whether to suppress the "form saved" toast
  when it starts with `"post-message:"` (line 313).
- The **JS closure variable** is what actually drives client-side navigation in
  `goToReturnUrl()` (`htmlForm.js:147-176`) after a successful submission —
  either `location.href = returnUrl` or a `window.parent.postMessage(...)` call.

If a form called `htmlForm.setReturnUrl(newUrl)` after page load, the server would
still see the *original* value (from the untouched hidden input) when deciding
whether to show the toast, while the user would actually be redirected — or
receive a `postMessage` — based on the *new* value. The two stores had no
mechanism keeping them in sync.

## Solution

Rather than trying to keep the hidden input's DOM value in sync with every call to
`setReturnUrl` (which is awkward, since the initial `setReturnUrl` call happens
before the `<form>` element even exists in the DOM), the fix makes the JS closure
variable the single source of truth **at the moment the form is actually
submitted**.

In `doSubmitHtmlForm` (`htmlForm.js`), immediately after the outgoing `formData` is
built (and following the existing pattern used for `binaryDataInputs`, which
already patches `formData` post-construction), the current live `returnUrl` value
is force-written into the submission:

```js
if (returnUrl) {
    formData.set('returnUrl', returnUrl);
} else {
    formData.delete('returnUrl');
}
```

This guarantees the value the server receives (and therefore the value used for the
toast-suppression check and stored on the `FormEntrySession`) always matches the
value that will actually be used for client-side navigation after submission —
regardless of whether, or how many times, `setReturnUrl` was called after the
initial page render.

## Also removed: the `form.serialize()` fallback

While making this change, `doSubmitHtmlForm` still had an old feature-detection
branch for browsers without the [`FormData`](https://developer.mozilla.org/en-US/docs/Web/API/FormData)
API:

```js
var formData = false;
if (window.FormData) {
    formData = new FormData(form[0]);
}
if (!formData) {
    formData = form.serialize();
}
```

`FormData` has been supported in every browser this module targets (including
IE10+) for well over a decade, so this fallback was dead code. It also would have
needed its own string-manipulation branch (regex strip + re-append) for the
`returnUrl` sync fix above, since a plain serialized query string doesn't support
`.set()`/`.delete()` the way a `FormData` object does — carrying that fallback
forward would have doubled the surface area of this fix for no real benefit.

The fallback was deleted; `doSubmitHtmlForm` now unconditionally does
`var formData = new FormData(form[0]);`, and the `returnUrl` sync logic no longer
needs to branch on `formData.set` support.