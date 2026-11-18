// J2ME Cloud Player: pointer-based multi-touch keypad.
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
