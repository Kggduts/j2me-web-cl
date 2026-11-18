import java.io.IOException;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Graphics;
import javax.microedition.midlet.MIDlet;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;

/**
 * Offline Java ME compatibility probe: MIDlet lifecycle, LCDUI rendering/input,
 * and RMS create/write/read/update/close/reopen persistence.
 */
public final class CoreCompatibilityProbe extends MIDlet implements CommandListener {
    private final ProbeCanvas canvas = new ProbeCanvas();
    private final Command runRms = new Command("Test RMS", Command.ITEM, 1);
    private final Command exit = new Command("Exit", Command.EXIT, 2);
    private boolean started;

    protected void startApp() {
        started = true;
        canvas.setStatus("MIDlet startApp() called");
        Display.getDisplay(this).setCurrent(canvas);
    }

    protected void pauseApp() {
        canvas.setStatus("MIDlet pauseApp() called");
    }

    protected void destroyApp(boolean unconditional) {
        started = false;
        canvas.setStatus("MIDlet destroyApp(" + unconditional + ")");
    }

    public void commandAction(Command command, Displayable displayable) {
        if (command == exit) {
            notifyDestroyed();
        } else if (command == runRms) {
            runRmsTest();
        }
    }

    private void runRmsTest() {
        RecordStore store = null;
        String name = "j2me_compat_probe";
        String result;
        try {
            store = RecordStore.openRecordStore(name, true);
            byte[] first = "J2ME-RMS-PERSISTENCE-1".getBytes("UTF-8");
            int id = store.addRecord(first, 0, first.length);
            byte[] read = store.getRecord(id);
            if (!equals(first, read)) throw new IOException("Initial read mismatch");

            byte[] updated = "J2ME-RMS-PERSISTENCE-2".getBytes("UTF-8");
            store.setRecord(id, updated, 0, updated.length);
            store.closeRecordStore();
            store = null;

            store = RecordStore.openRecordStore(name, false);
            byte[] reopened = store.getRecord(id);
            if (!equals(updated, reopened)) throw new IOException("Reopen/read mismatch");

            result = "PASS: RMS create/add/read/update/close/reopen/read";
        } catch (Throwable error) {
            result = "FAIL: RMS " + error.getClass().getName() + ": " + error.getMessage();
        } finally {
            if (store != null) {
                try { store.closeRecordStore(); } catch (Throwable ignored) { }
            }
        }
        canvas.setStatus(result);
    }

    private static boolean equals(byte[] a, byte[] b) {
        if (a == null || b == null || a.length != b.length) return false;
        for (int i = 0; i < a.length; i++) if (a[i] != b[i]) return false;
        return true;
    }

    private final class ProbeCanvas extends Canvas {
        private String status = "Tap Test RMS. Press any key to test input.";
        private int keyCount;
        private int pointerCount;

        ProbeCanvas() {
            addCommand(runRms);
            addCommand(exit);
            setCommandListener(CoreCompatibilityProbe.this);
        }

        void setStatus(String value) {
            status = value;
            repaint();
        }

        protected void paint(Graphics g) {
            int w = getWidth();
            int h = getHeight();
            g.setColor(12, 22, 36);
            g.fillRect(0, 0, w, h);
            g.setColor(255, 255, 255);
            g.drawString("J2ME CORE COMPATIBILITY", 4, 4, Graphics.TOP | Graphics.LEFT);
            g.setColor(105, 220, 160);
            g.drawRect(4, 28, w - 9, h - 34);
            g.setColor(255, 255, 255);
            g.drawString("Lifecycle: " + (started ? "STARTED" : "STOPPED"), 10, 38, Graphics.TOP | Graphics.LEFT);
            g.drawString("Key events: " + keyCount, 10, 58, Graphics.TOP | Graphics.LEFT);
            g.drawString("Pointer events: " + pointerCount, 10, 78, Graphics.TOP | Graphics.LEFT);
            g.drawString("Tap screen / press keypad", 10, 108, Graphics.TOP | Graphics.LEFT);
            g.setColor(255, 220, 120);
            drawWrapped(g, status, 10, 140, w - 20);
        }

        public void keyPressed(int keyCode) {
            keyCount++;
            status = "Input received: keyCode=" + keyCode;
            repaint();
        }

        public void pointerPressed(int x, int y) {
            pointerCount++;
            status = "Pointer received at " + x + "," + y;
            repaint();
        }

        private void drawWrapped(Graphics g, String text, int x, int y, int maxWidth) {
            String remaining = text;
            while (remaining.length() > 0 && y < getHeight() - 12) {
                int end = remaining.length();
                while (end > 1 && g.getFont().stringWidth(remaining.substring(0, end)) > maxWidth) end--;
                g.drawString(remaining.substring(0, end), x, y, Graphics.TOP | Graphics.LEFT);
                remaining = remaining.substring(end);
                y += g.getFont().getHeight() + 2;
            }
        }
    }
}
