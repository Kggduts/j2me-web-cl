#!/usr/bin/env python3
"""Apply the small, pinned FreeJ2ME Web source overlay before its Ant build."""
from pathlib import Path
import shutil
import sys

root = Path(__file__).resolve().parents[1]
upstream = Path(sys.argv[1]).resolve()
expected = "c19416e75cbc15f9a27f7e967ee81cb108761e30"

import subprocess
actual = subprocess.check_output(
    ["git", "-C", str(upstream), "rev-parse", "HEAD"], text=True
).strip()
if actual != expected:
    raise SystemExit("Refusing to patch unexpected upstream revision: " + actual)

overlay = root / "patches" / "src"
for source in overlay.rglob("*.java"):
    relative = source.relative_to(overlay)
    target = upstream / "src" / relative
    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(source, target)

main_js = upstream / "web" / "src" / "main.js"
js = main_js.read_text(encoding="utf-8")
marker = "// J2ME Cloud Player: optional user-authenticated Tailscale networking."
if marker not in js:
    needle = "    await cheerpjInit({\n        enableDebug: false,"
    replacement = """    await cheerpjInit({
        // J2ME Cloud Player: optional user-authenticated Tailscale networking.
        // No reusable auth key is embedded in the public client.
        tailscaleLoginUrlCb(url) {
            const login = document.getElementById('tailscale-login');
            if (login) {
                login.href = url;
                login.hidden = false;
                login.textContent = 'Sign in to enable TCP/UDP networking';
            }
        },
        tailscaleIpCb(ip) {
            const status = document.getElementById('tailscale-status');
            if (status) status.textContent = 'Private network connected: ' + ip;
        },
        enableDebug: false,"""
    if needle not in js:
        raise SystemExit("Expected cheerpjInit anchor not found in upstream main.js")
    js = js.replace(needle, replacement, 1)
    main_js.write_text(js, encoding="utf-8")

# The upstream launcher links to an extensionless "run" path. Static hosting
# serves run.html, not /run, so installed games otherwise open a 404 page.
launcher_js = upstream / "web" / "src" / "launcher.js"
launcher = launcher_js.read_text(encoding="utf-8")
launcher_replacements = {
    'link.href = "run?app=" + game.appId;':
        'link.href = "run.html?app=" + encodeURIComponent(game.appId);',
    'link.href = "run?app=" + game.appId + "&mobile=1";':
        'link.href = "run.html?app=" + encodeURIComponent(game.appId) + "&mobile=1";',
}
for old, new in launcher_replacements.items():
    if old not in launcher:
        raise SystemExit("Expected installed-game route not found in upstream launcher.js: " + old)
    launcher = launcher.replace(old, new, 1)
launcher_js.write_text(launcher, encoding="utf-8")

run_html = upstream / "web" / "run.html"
html = run_html.read_text(encoding="utf-8")
html_marker = '<!-- J2ME Cloud Player: optional private-network sign-in -->'
if html_marker not in html:
    needle = '    <div id="screen-area">'
    replacement = """    <!-- J2ME Cloud Player: optional private-network sign-in -->
    <div id="network-tools" style="position:fixed;z-index:20;top:8px;left:8px;max-width:min(90vw,420px);font:13px sans-serif;background:rgba(255,255,255,.94);padding:7px 10px;border-radius:6px;box-shadow:0 1px 5px #0003">
        <a id="tailscale-login" href="#" target="_blank" rel="noopener noreferrer" hidden>Sign in to enable TCP/UDP networking</a>
        <span id="tailscale-status" aria-live="polite"></span>
    </div>
    <div id="screen-area">"""
    if needle not in html:
        raise SystemExit("Expected screen-area anchor not found in upstream run.html")
    html = html.replace(needle, replacement, 1)
    run_html.write_text(html, encoding="utf-8")

print("Applied GCF transport and optional Tailscale login overlays.")


# J2ME Cloud Player: mobile keypad input improvements.
# The upstream keypad omitted Backspace and listened only for legacy touch events.
kbd_file = upstream / "web" / "src" / "screenKbd.js"
kbd = kbd_file.read_text(encoding="utf-8")
kbd_marker = "// J2ME Cloud Player: pointer-based multi-touch keypad."
if kbd_marker not in kbd:
    kbd = """// J2ME Cloud Player: pointer-based multi-touch keypad.
export const kbdWidth = 220, kbdHeight = 220;

const activePointers = new Map();
const keyPointers = new Map();
let handleKey = null;

function getKeyAtPoint(x, y) {
    return document.elementFromPoint(x, y)?.closest('.key') || null;
}

function activateKey(key, pointerId) {
    if (!key) return;
    let pointers = keyPointers.get(key);
    if (!pointers) {
        pointers = new Set();
        keyPointers.set(key, pointers);
    }
    if (pointers.size === 0) {
        key.classList.add('active');
        if (handleKey) handleKey(true, key.dataset.key);
    }
    pointers.add(pointerId);
}

function deactivateKey(key, pointerId) {
    if (!key) return;
    const pointers = keyPointers.get(key);
    if (!pointers) return;
    pointers.delete(pointerId);
    if (pointers.size === 0) {
        keyPointers.delete(key);
        key.classList.remove('active');
        if (handleKey) handleKey(false, key.dataset.key);
    }
}

function onPointerDown(e) {
    if (e.pointerType === 'mouse' && e.button !== 0) return;
    const key = e.target.closest('.key');
    if (!key) return;
    e.preventDefault();
    activePointers.set(e.pointerId, key);
    try { e.currentTarget.setPointerCapture(e.pointerId); } catch (_) {}
    activateKey(key, e.pointerId);
}

function onPointerMove(e) {
    if (!activePointers.has(e.pointerId)) return;
    e.preventDefault();
    const previous = activePointers.get(e.pointerId);
    const current = getKeyAtPoint(e.clientX, e.clientY);
    if (current === previous) return;
    deactivateKey(previous, e.pointerId);
    if (current) {
        activePointers.set(e.pointerId, current);
        activateKey(current, e.pointerId);
    } else {
        activePointers.delete(e.pointerId);
    }
}

function onPointerEnd(e) {
    const key = activePointers.get(e.pointerId);
    if (key) deactivateKey(key, e.pointerId);
    activePointers.delete(e.pointerId);
}

export function initKbdListeners() {
    document.querySelectorAll('.keypad-part').forEach(keypad => {
        keypad.addEventListener('pointerdown', onPointerDown);
        keypad.addEventListener('pointermove', onPointerMove);
        keypad.addEventListener('pointerup', onPointerEnd);
        keypad.addEventListener('pointercancel', onPointerEnd);
        keypad.addEventListener('lostpointercapture', onPointerEnd);
    });
}

export function setKbdHandler(handler) {
    handleKey = handler;
}
"""
    kbd_file.write_text(kbd, encoding="utf-8")

run_html = upstream / "web" / "run.html"
html = run_html.read_text(encoding="utf-8")
backspace_marker = '<button data-key="Backspace"'
if backspace_marker not in html:
    needle = '            <button data-key="NumpadDivide" class="key">#</button>'
    replacement = needle + '\n            <button data-key="Backspace" class="key backspace-key" aria-label="Delete / Backspace">⌫<span>Xóa</span></button>'
    if needle not in html:
        raise SystemExit("Expected numeric keypad anchor not found in upstream run.html")
    html = html.replace(needle, replacement, 1)
if "grid-template-rows: repeat(5, 1fr);" not in html:
    needle = """    .left-grid {
        grid-template-columns: repeat(3, 1fr);
        grid-template-rows: repeat(4, 1fr);
    }"""
    replacement = """    .left-grid {
        grid-template-columns: repeat(3, 1fr);
        grid-template-rows: repeat(5, 1fr);
    }
    .keypad-part, .key {
        touch-action: none;
    }
    .backspace-key {
        font-size: 1.25em;
        background-color: #593b3b;
    }"""
    if needle not in html:
        raise SystemExit("Expected left keypad CSS anchor not found in upstream run.html")
    html = html.replace(needle, replacement, 1)

# J2ME Cloud Player: capture safe network diagnostics in-page for mobile users.
mobile_log_marker = '<!-- J2ME Cloud Player: on-screen network diagnostics -->'
if mobile_log_marker not in html:
    mobile_log = r"""
    <!-- J2ME Cloud Player: on-screen network diagnostics -->
    <style>
      #j2me-netlog-toggle{position:fixed;z-index:10020;right:10px;bottom:10px;border:0;border-radius:22px;padding:10px 14px;background:#173b67;color:#fff;font:600 14px sans-serif;box-shadow:0 2px 8px #0005}
      #j2me-netlog-panel{position:fixed;z-index:10021;inset:auto 8px 58px 8px;max-height:55vh;background:#101820;color:#e8f0f7;border:1px solid #526273;border-radius:10px;padding:10px;font:13px sans-serif;box-shadow:0 3px 16px #0008}
      #j2me-netlog-panel[hidden]{display:none}
      #j2me-netlog-output{box-sizing:border-box;width:100%;height:30vh;resize:vertical;background:#080d12;color:#b9f6ca;border:1px solid #526273;border-radius:6px;padding:8px;font:11px monospace}
      #j2me-netlog-panel button{border:0;border-radius:6px;padding:9px 12px;margin:6px 5px 0 0;background:#285b87;color:white;font-weight:600}
      #j2me-netlog-panel .muted{color:#b7c4d0;font-size:12px}
    </style>
    <button id="j2me-netlog-toggle" type="button" aria-expanded="false">Network Log <span id="j2me-netlog-count">0</span></button>
    <section id="j2me-netlog-panel" hidden aria-label="Network debug log">
      <strong>Network Debug Log</strong>
      <p class="muted">Chỉ lưu các dòng [J2ME-NET]. Không ghi nội dung gói tin hoặc mật khẩu.</p>
      <textarea id="j2me-netlog-output" readonly spellcheck="false" placeholder="Chưa có log. Hãy mở game và thử kết nối lại."></textarea>
      <div>
        <button id="j2me-netlog-copy" type="button">Sao chép log</button>
        <button id="j2me-netlog-clear" type="button">Xóa log</button>
        <button id="j2me-netlog-close" type="button">Đóng</button>
        <span id="j2me-netlog-status" class="muted" aria-live="polite"></span>
      </div>
    </section>
    <script>
    (() => {
      const lines = [];
      const limit = 400;
      const output = document.getElementById('j2me-netlog-output');
      const count = document.getElementById('j2me-netlog-count');
      const panel = document.getElementById('j2me-netlog-panel');
      const toggle = document.getElementById('j2me-netlog-toggle');
      const status = document.getElementById('j2me-netlog-status');
      const original = {
        log: console.log.bind(console),
        warn: console.warn.bind(console),
        error: console.error.bind(console)
      };
      function stringify(value) {
        if (typeof value === 'string') return value;
        try { return JSON.stringify(value); } catch (_) { return String(value); }
      }
      function capture(args) {
        const line = Array.from(args, stringify).join(' ');
        if (!line.includes('[J2ME-NET]')) return;
        lines.push(line.slice(0, 1200));
        if (lines.length > limit) lines.shift();
        output.value = lines.join('\n');
        count.textContent = String(lines.length);
        if (!panel.hidden) output.scrollTop = output.scrollHeight;
      }
      ['log', 'warn', 'error'].forEach(level => {
        console[level] = (...args) => {
          capture(args);
          original[level](...args);
        };
      });
      function setOpen(open) {
        panel.hidden = !open;
        toggle.setAttribute('aria-expanded', String(open));
        if (open) output.scrollTop = output.scrollHeight;
      }
      toggle.addEventListener('click', () => setOpen(panel.hidden));
      document.getElementById('j2me-netlog-close').addEventListener('click', () => setOpen(false));
      document.getElementById('j2me-netlog-clear').addEventListener('click', () => {
        lines.length = 0; output.value = ''; count.textContent = '0'; status.textContent = 'Đã xóa.';
      });
      document.getElementById('j2me-netlog-copy').addEventListener('click', async () => {
        const value = lines.join('\n');
        if (!value) { status.textContent = 'Chưa có log để sao chép.'; return; }
        try {
          if (navigator.clipboard && navigator.clipboard.writeText) {
            await navigator.clipboard.writeText(value);
          } else {
            output.focus(); output.select();
            if (!document.execCommand('copy')) throw new Error('copy unavailable');
          }
          status.textContent = 'Đã sao chép ' + lines.length + ' dòng.';
        } catch (_) {
          output.focus(); output.select();
          status.textContent = 'Đã chọn log. Nhấn giữ và chọn Sao chép.';
        }
      });
    })();
    </script>
    """
    if "</body>" not in html:
        raise SystemExit("Expected closing body tag not found in upstream run.html")
    html = html.replace("</body>", mobile_log + "\n</body>", 1)
run_html.write_text(html, encoding="utf-8")
