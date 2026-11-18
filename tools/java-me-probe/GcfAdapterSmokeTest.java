import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.Datagram;
import javax.microedition.io.DatagramConnection;
import javax.microedition.io.ServerSocketConnection;

/** Runs the project's GCF adapters on a normal JVM against the loopback probe server. */
public final class GcfAdapterSmokeTest {
    private static final byte[] DATA = new byte[] {
        0, 1, 2, 3, 10, 13, 31, 32, 65, 90, 127, (byte)128, (byte)200, (byte)254, (byte)255
    };

    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "127.0.0.1";
        int http = args.length > 1 ? Integer.parseInt(args[1]) : 18765;
        int tcp = args.length > 2 ? Integer.parseInt(args[2]) : 18766;
        int udp = args.length > 3 ? Integer.parseInt(args[3]) : 18767;
        testHttp(host, http);
        testTcp(host, tcp);
        testUdp(host, udp);
        testServerSocket();
        testServerSocketCloseUnblocks();
        testDatagramCloseUnblocks();
        System.out.println("PASS: GCF HTTP, TCP, UDP, server-socket and close-lifecycle smoke tests");
    }

    private static void testHttp(String host, int port) throws Exception {
        HttpConnection get = (HttpConnection) Connector.open(
            "http://" + host + ":" + port + "/health", Connector.READ, true);
        require(get.getResponseCode() == HttpConnection.HTTP_OK, "GCF HTTP GET status");
        drain(get.openInputStream());
        get.close();

        HttpConnection status = (HttpConnection) Connector.open(
            "http://" + host + ":" + port + "/status/418", Connector.READ, true);
        require(status.getResponseCode() == 418, "GCF HTTP non-2xx status");
        status.close();

        HttpConnection post = (HttpConnection) Connector.open(
            "http://" + host + ":" + port + "/echo", Connector.READ_WRITE, true);
        post.setRequestMethod(HttpConnection.POST);
        post.setRequestProperty("Content-Type", "application/octet-stream");
        OutputStream out = post.openOutputStream();
        out.write(DATA);
        out.close();
        require(post.getResponseCode() == HttpConnection.HTTP_OK, "GCF HTTP POST status");
        require(Arrays.equals(DATA, readAll(post.openInputStream())), "GCF HTTP binary echo");
        post.close();
        System.out.println("PASS: GCF HTTP status and binary body");
    }

    private static void testTcp(String host, int port) throws Exception {
        StreamConnection connection = (StreamConnection) Connector.open(
            "socket://" + host + ":" + port, Connector.READ_WRITE, true);
        OutputStream out = connection.openOutputStream();
        InputStream in = connection.openInputStream();
        out.write(DATA);
        out.flush();
        byte[] received = new byte[DATA.length];
        int offset = 0;
        while (offset < received.length) {
            int count = in.read(received, offset, received.length - offset);
            if (count < 0) throw new AssertionError("GCF TCP peer closed early");
            offset += count;
        }
        require(Arrays.equals(DATA, received), "GCF TCP binary echo");
        connection.close();
        System.out.println("PASS: GCF TCP binary stream");
    }

    private static void testUdp(String host, int port) throws Exception {
        DatagramConnection connection = (DatagramConnection) Connector.open(
            "datagram://" + host + ":" + port, Connector.READ_WRITE, true);
        Datagram request = connection.newDatagram(256);
        request.write(DATA);
        connection.send(request);
        Datagram response = connection.newDatagram(256);
        connection.receive(response);
        byte[] received = new byte[DATA.length];
        response.readFully(received);
        require(Arrays.equals(DATA, received), "GCF UDP DataInput/DataOutput binary datagram");
        connection.close();
        System.out.println("PASS: GCF UDP binary datagram");
    }

    private static void testServerSocket() throws Exception {
        final ServerSocketConnection listener = (ServerSocketConnection) Connector.open(
            "serversocket://127.0.0.1:18768", Connector.READ_WRITE, true);
        final Throwable[] serverFailure = new Throwable[1];
        Thread server = new Thread(new Runnable() {
            public void run() {
                StreamConnection accepted = null;
                try {
                    accepted = listener.acceptAndOpen();
                    InputStream in = accepted.openInputStream();
                    OutputStream out = accepted.openOutputStream();
                    byte[] received = new byte[DATA.length];
                    int offset = 0;
                    while (offset < received.length) {
                        int count = in.read(received, offset, received.length - offset);
                        if (count < 0) throw new AssertionError("Accepted socket closed early");
                        offset += count;
                    }
                    if (!Arrays.equals(DATA, received)) throw new AssertionError("Server socket received incorrect bytes");
                    out.write(received);
                    out.flush();
                } catch (Throwable error) {
                    serverFailure[0] = error;
                } finally {
                    if (accepted != null) accepted.close();
                }
            }
        });
        server.start();

        java.net.Socket client = new java.net.Socket("127.0.0.1", listener.getLocalPort());
        client.setSoTimeout(3000);
        client.getOutputStream().write(DATA);
        client.getOutputStream().flush();
        byte[] echoed = new byte[DATA.length];
        int offset = 0;
        while (offset < echoed.length) {
            int count = client.getInputStream().read(echoed, offset, echoed.length - offset);
            if (count < 0) throw new AssertionError("Server socket closed before echo completed");
            offset += count;
        }
        require(Arrays.equals(DATA, echoed), "GCF server-socket binary echo");
        client.close();
        server.join(3000);
        listener.close();
        if (server.isAlive()) throw new AssertionError("Server socket accept worker did not finish");
        if (serverFailure[0] != null) throw new AssertionError("GCF server socket failed: " + serverFailure[0]);
        System.out.println("PASS: GCF server-socket accept and binary stream");
    }

    private static void testServerSocketCloseUnblocks() throws Exception {
        final ServerSocketConnection listener = (ServerSocketConnection) Connector.open(
            "serversocket://127.0.0.1:18769", Connector.READ_WRITE, false);
        final Throwable[] failure = new Throwable[1];
        Thread waiter = new Thread(new Runnable() {
            public void run() {
                try { listener.acceptAndOpen(); }
                catch (Throwable error) { failure[0] = error; }
            }
        });
        waiter.start();
        Thread.sleep(100);
        listener.close();
        waiter.join(3000);
        if (waiter.isAlive()) throw new AssertionError("Closing server socket did not unblock accept");
        if (!(failure[0] instanceof IllegalStateException)) {
            throw new AssertionError("Expected closed accept to report IllegalStateException: " + failure[0]);
        }
        System.out.println("PASS: closing server socket unblocks accept");
    }

    private static void testDatagramCloseUnblocks() throws Exception {
        final DatagramConnection connection = (DatagramConnection) Connector.open(
            "datagram://:18770", Connector.READ_WRITE, false);
        final Throwable[] failure = new Throwable[1];
        Thread waiter = new Thread(new Runnable() {
            public void run() {
                try { connection.receive(connection.newDatagram(128)); }
                catch (Throwable error) { failure[0] = error; }
            }
        });
        waiter.start();
        Thread.sleep(100);
        connection.close();
        waiter.join(3000);
        if (waiter.isAlive()) throw new AssertionError("Closing datagram connection did not unblock receive");
        if (!(failure[0] instanceof IllegalStateException)) {
            throw new AssertionError("Expected closed receive to report IllegalStateException: " + failure[0]);
        }
        System.out.println("PASS: closing datagram socket unblocks receive");
    }

    private static void drain(InputStream in) throws Exception {
        try { byte[] buffer = new byte[256]; while (in.read(buffer) != -1) { } }
        finally { in.close(); }
    }

    private static byte[] readAll(InputStream in) throws Exception {
        try {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[256];
            int count;
            while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
            return out.toByteArray();
        } finally { in.close(); }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
