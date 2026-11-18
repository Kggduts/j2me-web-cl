from __future__ import annotations

import importlib.util
import socket
import threading
import unittest
import urllib.error
import urllib.request
from pathlib import Path

MODULE_PATH = Path(__file__).with_name("netprobe-server.py")
SPEC = importlib.util.spec_from_file_location("netprobe_server", MODULE_PATH)
assert SPEC and SPEC.loader
probe = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(probe)


class NetworkProbeServerTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.http = probe.ThreadingHTTPServer(("127.0.0.1", 0), probe.ProbeHTTPHandler)
        cls.http.cors_origin = "https://test.example"
        cls.tcp = probe.ThreadingTCPServer(("127.0.0.1", 0), probe.TCPEchoHandler)
        cls.udp = probe.ThreadingUDPServer(("127.0.0.1", 0), probe.UDPEchoHandler)
        cls.servers = [cls.http, cls.tcp, cls.udp]
        cls.threads = [
            threading.Thread(target=server.serve_forever, daemon=True)
            for server in cls.servers
        ]
        for thread in cls.threads:
            thread.start()

    @classmethod
    def tearDownClass(cls) -> None:
        for server in cls.servers:
            server.shutdown()
            server.server_close()
        for thread in cls.threads:
            thread.join(timeout=2)

    def test_http_health(self) -> None:
        url = f"http://127.0.0.1:{self.http.server_address[1]}/health"
        with urllib.request.urlopen(url, timeout=3) as response:
            self.assertEqual(response.status, 200)
            self.assertIn(b'"ok": true', response.read())

    def test_http_post_echo_and_cors(self) -> None:
        url = f"http://127.0.0.1:{self.http.server_address[1]}/echo"
        request = urllib.request.Request(
            url,
            data=b"\x00binary\xff",
            headers={
                "Content-Type": "application/octet-stream",
                "Origin": "https://test.example",
            },
            method="POST",
        )
        with urllib.request.urlopen(request, timeout=3) as response:
            self.assertEqual(response.read(), b"\x00binary\xff")
            self.assertEqual(
                response.headers.get("Access-Control-Allow-Origin"),
                "https://test.example",
            )

    def test_http_status_endpoint(self) -> None:
        url = f"http://127.0.0.1:{self.http.server_address[1]}/status/418"
        with self.assertRaises(urllib.error.HTTPError) as error:
            urllib.request.urlopen(url, timeout=3)
        self.assertEqual(error.exception.code, 418)

    def test_tcp_binary_echo(self) -> None:
        payload = bytes(range(256)) * 4
        with socket.create_connection(self.tcp.server_address, timeout=3) as client:
            client.settimeout(3)
            client.sendall(payload)
            received = bytearray()
            while len(received) < len(payload):
                chunk = client.recv(len(payload) - len(received))
                if not chunk:
                    break
                received.extend(chunk)
        self.assertEqual(bytes(received), payload)

    def test_udp_binary_echo(self) -> None:
        payload = bytes(range(256)) * 4
        with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as client:
            client.settimeout(3)
            client.sendto(payload, self.udp.server_address)
            received, _ = client.recvfrom(65507)
        self.assertEqual(received, payload)


if __name__ == "__main__":
    unittest.main()
