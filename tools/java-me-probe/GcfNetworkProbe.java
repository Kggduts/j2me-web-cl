import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import javax.microedition.io.Connector;
import javax.microedition.io.Connection;
import javax.microedition.io.HttpConnection;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.Datagram;
import javax.microedition.io.DatagramConnection;
import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.StringItem;
import javax.microedition.lcdui.TextField;
import javax.microedition.midlet.MIDlet;

/**
 * Interactive Java ME GCF diagnostic. Results reflect the emulator's actual
 * javax.microedition.io implementation, not Java SE networking.
 */
public final class GcfNetworkProbe extends MIDlet implements CommandListener {
    private static final String VPS_HOST = "100.81.186.89";
    private final Form form = new Form("J2ME GCF Network Probe");
    private final StringItem hostDisplay = new StringItem("Host / IP", VPS_HOST);
    private final ChoiceGroup hostChar = new ChoiceGroup("Character to insert",
        Choice.POPUP, new String[] {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "."}, null);
    private final TextField httpPort = new TextField("HTTP port", "8765", 5, TextField.NUMERIC);
    private final TextField tcpPort = new TextField("TCP port", "8766", 5, TextField.NUMERIC);
    private final TextField udpPort = new TextField("UDP port", "8767", 5, TextField.NUMERIC);
    private final StringItem result = new StringItem("Results", "Ready. Start the probe server on the VPS.");
    private final Command addChar = new Command("Insert", Command.ITEM, 1);
    private final Command deleteChar = new Command("Delete", Command.ITEM, 2);
    private final Command useVps = new Command("Use VPS IP", Command.ITEM, 3);
    private final Command run = new Command("Run tests", Command.OK, 4);
    private final Command exit = new Command("Exit", Command.EXIT, 5);
    private String hostValue = VPS_HOST;
    private volatile boolean running;

    public GcfNetworkProbe() {
        form.append(hostDisplay);
        form.append(hostChar);
        form.append(httpPort);
        form.append(tcpPort);
        form.append(udpPort);
        form.append(result);
        form.addCommand(addChar);
        form.addCommand(deleteChar);
        form.addCommand(useVps);
        form.addCommand(run);
        form.addCommand(exit);
        form.setCommandListener(this);
    }

    protected void startApp() {
        Display.getDisplay(this).setCurrent(form);
    }

    protected void pauseApp() { }

    protected void destroyApp(boolean unconditional) { }

    public void commandAction(Command command, Displayable displayable) {
        if (command == exit) {
            notifyDestroyed();
        } else if (command == addChar) {
            if (hostValue.length() < 128) {
                hostValue += hostChar.getString(hostChar.getSelectedIndex());
                hostDisplay.setText(hostValue);
            }
        } else if (command == deleteChar) {
            if (hostValue.length() > 0) {
                hostValue = hostValue.substring(0, hostValue.length() - 1);
                hostDisplay.setText(hostValue);
            }
        } else if (command == useVps) {
            hostValue = VPS_HOST;
            hostDisplay.setText(hostValue);
            result.setText("VPS target selected: " + VPS_HOST);
        } else if (command == run && !running) {
            final String target = hostValue.trim();
            final int hp;
            final int tp;
            final int up;
            try {
                hp = Integer.parseInt(httpPort.getString());
                tp = Integer.parseInt(tcpPort.getString());
                up = Integer.parseInt(udpPort.getString());
                if (target.length() == 0 || hp < 1 || hp > 65535 ||
                    tp < 1 || tp > 65535 || up < 1 || up > 65535) {
                    throw new IllegalArgumentException();
                }
            } catch (Exception e) {
                result.setText("Invalid host or port.");
                return;
            }
            running = true;
            result.setText("Starting diagnostics for " + target + "...");
            new Thread(new Runnable() {
                public void run() {
                    try {
                        runTests(target, hp, tp, up);
                    } finally {
                        running = false;
                    }
                }
            }).start();
        }
    }

    private void runTests(String target, int hp, int tp, int up) {
        StringBuffer log = new StringBuffer();
        append(log, "INFO", "Target " + target);
        publish(log, "Testing HTTP " + target + ":" + hp + "...");
        testHttp(target, hp, log);
        publish(log, "Testing TCP " + target + ":" + tp + "...");
        testTcp(target, tp, log);
        publish(log, "Testing UDP " + target + ":" + up + "...");
        testUdp(target, up, log);
        append(log, "INFO", "All tests finished.");
        publish(log, "Diagnostics complete.");
    }

    private void testHttp(String target, int port, StringBuffer log) {
        HttpConnection connection = null;
        InputStream in = null;
        long started = System.currentTimeMillis();
        try {
            connection = (HttpConnection) Connector.open(
                "http://" + target + ":" + port + "/health", Connector.READ, true);
            int status = connection.getResponseCode();
            in = connection.openInputStream();
            while (in.read() != -1) { }
            append(log, status == HttpConnection.HTTP_OK ? "PASS" : "FAIL",
                "HTTP " + target + ":" + port + "/health (" + status + "), " + elapsed(started));
        } catch (Throwable e) {
            append(log, "FAIL", "HTTP " + target + ":" + port + ": " + describe(e) + ", " + elapsed(started));
        } finally {
            close(in);
            close(connection);
        }
    }

    private void testTcp(String target, int port, StringBuffer log) {
        StreamConnection connection = null;
        InputStream in = null;
        OutputStream out = null;
        long started = System.currentTimeMillis();
        try {
            connection = (StreamConnection) Connector.open(
                "socket://" + target + ":" + port, Connector.READ_WRITE, true);
            in = connection.openInputStream();
            out = connection.openOutputStream();
            byte[] data = payload();
            out.write(data);
            out.flush();
            byte[] received = new byte[data.length];
            int offset = 0;
            while (offset < received.length) {
                int count = in.read(received, offset, received.length - offset);
                if (count < 0) throw new IOException("peer closed early");
                offset += count;
            }
            append(log, equal(data, received) ? "PASS" : "FAIL",
                "TCP " + target + ":" + port + " binary echo, " + elapsed(started));
        } catch (Throwable e) {
            append(log, "FAIL", "TCP " + target + ":" + port + ": " + describe(e) + ", " + elapsed(started));
        } finally {
            close(in);
            close(out);
            close(connection);
        }
    }

    private void testUdp(String target, int port, StringBuffer log) {
        DatagramConnection connection = null;
        long started = System.currentTimeMillis();
        try {
            connection = (DatagramConnection) Connector.open(
                "datagram://" + target + ":" + port, Connector.READ_WRITE, true);
            byte[] data = payload();
            Datagram request = connection.newDatagram(data, data.length);
            connection.send(request);
            Datagram response = connection.newDatagram(256);
            connection.receive(response);
            byte[] received = new byte[response.getLength()];
            System.arraycopy(response.getData(), response.getOffset(),
                received, 0, response.getLength());
            append(log, equal(data, received) ? "PASS" : "FAIL",
                "UDP " + target + ":" + port + " binary echo, " + elapsed(started));
        } catch (Throwable e) {
            append(log, "FAIL", "UDP " + target + ":" + port + ": " + describe(e) + ", " + elapsed(started));
        } finally {
            close(connection);
        }
    }

    private void publish(final StringBuffer log, final String stage) {
        final String snapshot = stage + "\n\n" + log.toString();
        Display.getDisplay(this).callSerially(new Runnable() {
            public void run() { result.setText(snapshot); }
        });
    }

    private static String elapsed(long started) {
        return (System.currentTimeMillis() - started) + " ms";
    }

    private static byte[] payload() {
        return new byte[] { 0, 1, 2, 3, 10, 13, 31, 32, 65, 90, 127,
            (byte) 128, (byte) 200, (byte) 254, (byte) 255 };
    }

    private static boolean equal(byte[] a, byte[] b) {
        if (a.length != b.length) return false;
        for (int i = 0; i < a.length; i++) if (a[i] != b[i]) return false;
        return true;
    }

    private static String describe(Throwable error) {
        String message = error.getMessage();
        return error.getClass().getName() + (message == null ? "" : ": " + message);
    }

    private static void append(StringBuffer log, String status, String message) {
        log.append(status).append(": ").append(message).append("\n");
    }

    private static void close(InputStream stream) {
        if (stream != null) try { stream.close(); } catch (IOException ignored) { }
    }

    private static void close(OutputStream stream) {
        if (stream != null) try { stream.close(); } catch (IOException ignored) { }
    }

    private static void close(Connection connection) {
        if (connection != null) try { connection.close(); } catch (Throwable ignored) { }
    }
}
