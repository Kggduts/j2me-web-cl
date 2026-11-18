"use strict";

const byId = (id) => document.getElementById(id);
const runButton = byId("run");

function showStatus(label, kind) {
  const node = byId("status");
  node.textContent = label;
  node.dataset.kind = kind || "neutral";
}

function formatError(error) {
  if (error && error.name === "AbortError") return "Request timed out or was cancelled.";
  if (error instanceof TypeError) {
    return "The browser did not expose a response. Possible causes include CORS, DNS, TLS, mixed-content policy, an invalid URL, or a network failure. The browser intentionally hides some details.";
  }
  return error && error.message ? error.message : String(error);
}

runButton.addEventListener("click", async () => {
  const rawUrl = byId("target").value.trim();
  const method = byId("method").value;
  let url;

  try {
    url = new URL(rawUrl);
    if (url.protocol !== "http:" && url.protocol !== "https:") {
      throw new Error("Only HTTP and HTTPS URLs are allowed.");
    }
    if (url.username || url.password) {
      throw new Error("URLs containing embedded credentials are not accepted.");
    }
  } catch (error) {
    showStatus("Invalid URL", "error");
    byId("details").textContent = error.message;
    return;
  }

  runButton.disabled = true;
  showStatus("Requesting…", "pending");
  byId("details").textContent = "Waiting for the browser response…";
  ["final-url", "http-status", "duration", "content-type", "bytes"].forEach((id) => {
    byId(id).textContent = "—";
  });

  const controller = new AbortController();
  const timeout = window.setTimeout(() => controller.abort(), 15000);
  const started = performance.now();

  try {
    const response = await fetch(url.href, {
      method,
      mode: "cors",
      credentials: "omit",
      cache: "no-store",
      redirect: "follow",
      signal: controller.signal
    });

    let byteLength = 0;
    const maxBytes = 5 * 1024 * 1024;
    if (method !== "HEAD" && response.body) {
      const reader = response.body.getReader();
      try {
        while (true) {
          const part = await reader.read();
          if (part.done) break;
          byteLength += part.value.byteLength;
          if (byteLength > maxBytes) {
            await reader.cancel("Response exceeds the 5 MiB diagnostic limit.");
            throw new Error("Response exceeded the 5 MiB diagnostic limit; the remaining body was cancelled.");
          }
        }
      } finally {
        reader.releaseLock();
      }
    }

    byId("final-url").textContent = response.url || url.href;
    byId("http-status").textContent = String(response.status) + " " + response.statusText;
    byId("duration").textContent = Math.round(performance.now() - started) + " ms";
    byId("content-type").textContent = response.headers.get("content-type") || "(not exposed)";
    byId("bytes").textContent = String(byteLength);
    byId("details").textContent = [
      "type: " + response.type,
      "redirected: " + response.redirected,
      "ok: " + response.ok,
      "Note: a readable response confirms browser fetch access only; it does not validate J2ME GCF behavior."
    ].join("\n");
    showStatus(response.ok ? "Response received" : "HTTP error response", response.ok ? "success" : "warning");
  } catch (error) {
    byId("duration").textContent = Math.round(performance.now() - started) + " ms";
    byId("details").textContent = formatError(error);
    showStatus(error && error.name === "AbortError" ? "Timed out" : "Request failed", "error");
  } finally {
    window.clearTimeout(timeout);
    runButton.disabled = false;
  }
});