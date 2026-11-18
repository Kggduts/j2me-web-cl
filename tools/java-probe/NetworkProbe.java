import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.util.Arrays;

/** Java SE smoke test only; this is not a MIDlet or a GCF test. */
public final class NetworkProbe {
    private static final byte[] DATA = new byte[] {
        0, 1, 2, 3, 10, 13, 31, 32, 65, 90, 127, (byte) 128,
        (byte) 200, (byte) 254, (byte) 255
    };

    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "127.0.0.1";
        int http = args.length > 1 ? Integer.parseInt(args[1]) : 8765;
        int tcp = args.length > 2 ? Integer.parseInt(args[2]) : 8766;
        int udp = args.length > 3 ? Integer.parseInt(args[3]) : 8767;
        testHttp(host, http);
        testTcp(host, tcp);
        testUdp(host, udp);
        System.out.println("PASS: Java SE HTTP, TCP and UDP smoke tests");
    }

    private static void testHttp(String host, int port) throws Exception {
        HttpURLConnection get = (HttpURLConnection)
            new URL("http", host, port, "/health").openConnection();
        get.setConnectTimeout(3000);
        get.setReadTimeout(3000);
        require(get.getResponseCode() == 200, "HTTP GET /health");
        drain(get.getInputStream());
        get.disconnect();

        HttpURLConnection post = (HttpURLConnection)
            new URL("http", host, port, "/echo").openConnection();
        post.setConnectTimeout(3000);
        post.setReadTimeout(3000);
        post.setRequestMethod("POST");
        post.setDoOutput(true);
        post.setRequestProperty("Content-Type", "application/octet-stream");
        post.setFixedLengthStreamingMode(DATA.length);
        OutputStream out = post.getOutputStream();
        out.write(DATA);
        out.close();
        require(post.getResponseCode() == 200, "HTTP POST /echo status");
        require(Arrays.equals(DATA, readAll(post.getInputStream())), "HTTP binary echo");
        post.disconnect();
        System.out.println("PASS: HTTP status and binary body");
    }

    private static void testTcp(String host, int port) throws Exception {
        Socket socket = new Socket();
        socket.connect(new java.net.InetSocketAddress(host, port), 3000);
        socket.setSoTimeout(3000);
        OutputStream out = socket.getOutputStream();
        out.write(DATA);
        out.flush();
        byte[] got = new byte[DATA.length];
        InputStream in = socket.getInputStream();
        int offset = 0;
        while (offset < got.length) {
            int n = in.read(got, offset, got.length - offset);
            if (n < 0) throw new AssertionError("TCP closed before echo completed");
            offset += n;
        }
        require(Arrays.equals(DATA, got), "TCP binary echo");
        socket.close();
        System.out.println("PASS: TCP binary stream");
    }

    private static void testUdp(String host, int port) throws Exception {
        DatagramSocket socket = new DatagramSocket();
        socket.setSoTimeout(3000);
        InetAddress address = InetAddress.getByName(host);
        socket.send(new DatagramPacket(DATA, DATA.length, address, port));
        byte[] buffer = new byte[256];
        DatagramPacket response = new DatagramPacket(buffer, buffer.length);
        socket.receive(response);
        byte[] got = Arrays.copyOf(response.getData(), response.getLength());
        socket.close();
        require(Arrays.equals(DATA, got), "UDP datagram boundary and payload");
        System.out.println("PASS: UDP binary datagram");
    }

    private static void drain(InputStream in) throws Exception {
        try { byte[] b = new byte[256]; while (in.read(b) != -1) { } }
        finally { in.close(); }
    }

    private static byte[] readAll(InputStream in) throws Exception {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] b = new byte[256];
            int n;
            while ((n = in.read(b)) != -1) out.write(b, 0, n);
            return out.toByteArray();
        } finally { in.close(); }
    }

    private static void require(boolean ok, String label) {
        if (!ok) throw new AssertionError(label);
    }
}
