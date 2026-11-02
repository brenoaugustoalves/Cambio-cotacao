"""Small, read-only brapi gateway. Put it behind HTTPS and an edge rate limit.

BRAPI_TOKEN is read only on the server. Never ship this process or its environment
inside the Android APK. The public endpoint accepts a fixed allowlist of symbols.
"""
import email.utils
import json
import os
import threading
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.error import HTTPError, URLError
from urllib.parse import parse_qs, quote, urlsplit
from urllib.request import Request, urlopen

ALLOWED = ("PETR4", "VALE3", "ITUB4", "MGLU3", "^BVSP")
UPSTREAM = "https://brapi.dev/api/v2/stocks/quote?symbols="
MAX_BODY = 262_144
CACHE_SECONDS = 60


def retry_seconds(value):
    if not value:
        return 60
    try:
        return max(1, min(86_400, int(value)))
    except ValueError:
        try:
            target = email.utils.parsedate_to_datetime(value).timestamp()
            return max(1, min(86_400, int(target - time.time())))
        except (TypeError, ValueError, OverflowError):
            return 60


class Gateway:
    def __init__(self, token):
        if not token or not token.strip():
            raise ValueError("Defina BRAPI_TOKEN no ambiente do servidor.")
        self.token = token.strip()
        self.lock = threading.Lock()
        self.cache = {}
        self.cooldown_until = 0

    def quote(self, symbols):
        requested = tuple(s.strip() for s in symbols.split(","))
        if not requested or len(requested) > len(ALLOWED) or len(set(requested)) != len(requested) or any(s not in ALLOWED for s in requested):
            return 400, {}, b'{"error":"symbols invalidos"}'
        now = time.monotonic()
        with self.lock:
            if now < self.cooldown_until:
                wait = max(1, int(self.cooldown_until - now))
                return 429, {"Retry-After": str(wait)}, b'{"error":"limite da fonte; aguarde"}'
            cached = self.cache.get(requested)
            if cached and now - cached[0] < CACHE_SECONDS:
                return 200, {}, cached[1]
            url = UPSTREAM + quote(",".join(requested), safe=",")
            request = Request(url, headers={"Accept": "application/json", "Authorization": "Bearer " + self.token})
            try:
                with urlopen(request, timeout=20) as response:
                    body = response.read(MAX_BODY + 1)
                    if len(body) > MAX_BODY:
                        return 502, {}, b'{"error":"resposta excessiva da fonte"}'
                    parsed = json.loads(body)
                    if not isinstance(parsed, dict) or not isinstance(parsed.get("results"), list):
                        return 502, {}, b'{"error":"resposta invalida da fonte"}'
                    self.cache[requested] = (now, body)
                    return 200, {}, body
            except HTTPError as error:
                if error.code == 429:
                    wait = retry_seconds(error.headers.get("Retry-After"))
                    self.cooldown_until = time.monotonic() + wait
                    return 429, {"Retry-After": str(wait)}, b'{"error":"limite da fonte; aguarde"}'
                if error.code in (401, 403, 404):
                    return error.code, {}, json.dumps({"error": {401: "chave inválida", 403: "plano sem acesso", 404: "ativo não encontrado"}[error.code]}).encode()
                return 502, {}, b'{"error":"fonte indisponivel"}'
            except (URLError, TimeoutError, ValueError, json.JSONDecodeError):
                return 502, {}, b'{"error":"fonte indisponivel"}'


class Handler(BaseHTTPRequestHandler):
    gateway = None

    def do_GET(self):
        parsed = urlsplit(self.path)
        params = parse_qs(parsed.query, keep_blank_values=True)
        if parsed.path != "/v1/stocks/quote" or set(params) != {"symbols"} or len(params["symbols"]) != 1:
            status, headers, body = 404, {}, b'{"error":"rota nao encontrada"}'
        else:
            status, headers, body = self.gateway.quote(params["symbols"][0])
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Cache-Control", "no-store")
        self.send_header("X-Content-Type-Options", "nosniff")
        for key, value in headers.items():
            self.send_header(key, value)
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, format, *args):
        # No request paths, credentials or responses in application logs.
        pass


def main():
    Handler.gateway = Gateway(os.environ.get("BRAPI_TOKEN"))
    host = os.environ.get("BRAPI_LISTEN_HOST", "127.0.0.1")
    port = int(os.environ.get("BRAPI_LISTEN_PORT", "8787"))
    ThreadingHTTPServer((host, port), Handler).serve_forever()


if __name__ == "__main__":
    main()
