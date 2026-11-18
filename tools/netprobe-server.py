#!/usr/bin/env python3
"""Local echo endpoints for J2ME Cloud Player network experiments.

Safe default: bind to loopback. To test through Tailscale, pass the host's
Tailscale IPv4 address explicitly and restrict access with tailnet ACLs.
Do not expose these unauthenticated endpoints to the public internet.
"""
from __future__ import annotations

import argparse
import json
import signal
import socket
import socketserver
import threading
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlparse

MAX_HTTP_BODY = 1024 * 1024
MAX_TCP_BYTES = 16 * 1024 * 1024
TCP_IDLE_TIMEOUT = 20.0


class ProbeHTTPHandler(BaseHTTPRequestHandler):
    server_version = "J2MEProbe/1.0"

    def _cors(self) -> None:
        allowed = self.server.cors_origin
        origin = self.headers.get("Origin")
        if allowed and origin == allowed:
            self.send_header("Access-Control-Allow-Origin", allowed)
            self.send_header("Vary", "Origin")
            self.send_header("Access-Control-Allow-Methods", "GET, HEAD, POST, OPTIONS")
            self.send_header("Access-Control-Allow-Headers", "Content-Type")
            self.send_header("Access-Control-Max-Age", "600")

    def _send(self, code: int, body: bytes = b"", content_type: str = "application/octet-stream") -> None:
        self.send_response(code)
        self._cors()
        self.send_header("Content-Type", content_type)
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        if self.command != "HEAD" and body:
            self.wfile.write(body)

    def do_OPTIONS(self) -> None:
        origin = self.headers.get("Origin")
        if not self.server.cors_origin or origin != self.server.cors_origin:
            self._send(403, b"CORS origin not allowed", "text/plain; charset=utf-8")
            return
        self._send(204)

    def do_HEAD(self) -> None:
        self.do_GET()

    def do_GET(self) -> None:
        path = urlparse(self.path).path
        if path == "/health":
            body = json.dumps({"ok": True, "service": "j2me-netprobe"}).encode()
            self._send(200, body, "application/json")
        elif path.startswith("/status/"):
            try:
                code = int(path.rsplit("/", 1)[1])
                if not 100 <= code <= 599:
                    raise ValueError
            except ValueError:
                self._send(400, b"Invalid status code", "text/plain; charset=utf-8")
                return
            self._send(code, ("HTTP status probe: " + str(code)).encode(), "text/plain; charset=utf-8")
        else:
            self._send(404, b"Not found", "text/plain; charset=utf-8")

    def do_POST(self) -> None:
        if urlparse(self.path).path != "/echo":
            self._send(404, b"Not found", "text/plain; charset=utf-8")
            return
        try:
            length = int(self.headers.get("Content-Length", "0"))
        except ValueError:
            self._send(400, b"Invalid Content-Length", "text/plain; charset=utf-8")
            return
        if length < 0 or length > MAX_HTTP_BODY:
            self._send(413, b"Request body too large", "text/plain; charset=utf-8")
            return
        body = self.rfile.read(length)
        if len(body) != length:
            self._send(400, b"Incomplete request body", "text/plain; charset=utf-8")
            return
        self._send(200, body, self.headers.get("Content-Type", "application/octet-stream"))

    def log_message(self, fmt: str, *args: object) -> None:
        # Avoid logging request bodies or full query strings.
        print("[HTTP] %s - %s" % (self.client_address[0], fmt % args))


class ThreadingTCPServer(socketserver.ThreadingTCPServer):
    allow_reuse_address = True
    daemon_threads = True


class ThreadingUDPServer(socketserver.ThreadingUDPServer):
    allow_reuse_address = True
    max_packet_size = 65507
    daemon_threads = True


class TCPEchoHandler(socketserver.BaseRequestHandler):
    def handle(self) -> None:
        sock = self.request
        sock.settimeout(TCP_IDLE_TIMEOUT)
        total = 0
        try:
            while total < MAX_TCP_BYTES:
                data = sock.recv(65536)
                if not data:
                    break
                total += len(data)
                sock.sendall(data)
        except (TimeoutError, socket.timeout, ConnectionError, OSError):
            pass
        finally:
            try:
                sock.shutdown(socket.SHUT_RDWR)
            except OSError:
                pass
            sock.close()


class UDPEchoHandler(socketserver.BaseRequestHandler):
    def handle(self) -> None:
        data, sock = self.request
        if len(data) <= 65507:
            sock.sendto(data, self.client_address)


def main() -> None:
    parser = argparse.ArgumentParser(description="Run controlled HTTP, TCP and UDP echo endpoints.")
    parser.add_argument("--host", default="127.0.0.1", help="Bind address (default: 127.0.0.1)")
    parser.add_argument("--http-port", type=int, default=8765)
    parser.add_argument("--tcp-port", type=int, default=8766)
    parser.add_argument("--udp-port", type=int, default=8767)
    parser.add_argument("--cors-origin", default="", help="One exact allowed browser origin, e.g. https://example.github.io")
    args = parser.parse_args()

    http = ThreadingHTTPServer((args.host, args.http_port), ProbeHTTPHandler)
    http.cors_origin = args.cors_origin
    tcp = ThreadingTCPServer((args.host, args.tcp_port), TCPEchoHandler)
    udp = ThreadingUDPServer((args.host, args.udp_port), UDPEchoHandler)

    servers = [
        ("HTTP", http, args.http_port),
        ("TCP", tcp, args.tcp_port),
        ("UDP", udp, args.udp_port),
    ]
    threads = []
    for name, server, port in servers:
        thread = threading.Thread(target=server.serve_forever, name=name, daemon=True)
        thread.start()
        threads.append(thread)
        print(f"{name} listening on {args.host}:{port}")

    print("Press Ctrl+C to stop. These endpoints have no authentication.")
    try:
        for thread in threads:
            thread.join()
    except KeyboardInterrupt:
        print("\nStopping probe servers...")
    finally:
        for _, server, _ in servers:
            server.shutdown()
            server.server_close()


if __name__ == "__main__":
    main()
