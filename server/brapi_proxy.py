"""Small, read-only brapi gateway. Put it behind HTTPS and an edge rate limit.

BRAPI_TOKEN is read only on the server. Never ship this process or its environment
inside the Android APK. The public endpoint accepts a fixed allowlist of symbols.
"""
import email.utils
import datetime
import http.client
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
MAX_REQUEST_TARGET = 512
MAX_ACTIVE_CLIENTS = 32
CLIENT_SOCKET_TIMEOUT_SECONDS = 10
DEFAULT_DAILY_UPSTREAM_LIMIT = 400
CLIENT_REQUESTS_PER_MINUTE = 60
MAX_TRACKED_CLIENTS = 4096


def canonical_symbols(value):
    if not isinstance(value, str) or not value or len(value) > sum(map(len, ALLOWED)) + len(ALLOWED) - 1:
        return None
    requested = value.split(",")
    if not requested or len(requested) > len(ALLOWED) or len(set(requested)) != len(requested):
        return None
    if any(symbol not in ALLOWED for symbol in requested):
        return None
    return tuple(symbol for symbol in ALLOWED if symbol in requested)


def symbols_from_target(target):
    if not isinstance(target, str) or len(target) > MAX_REQUEST_TARGET or any(ord(char) < 32 for char in target):
        return None
    try:
        parsed = urlsplit(target)
        params = parse_qs(parsed.query, keep_blank_values=True, strict_parsing=True, max_num_fields=2)
    except (ValueError, UnicodeError):
        return None
    if parsed.scheme or parsed.netloc or parsed.fragment or parsed.path != "/v1/stocks/quote":
        return None
    if set(params) != {"symbols"} or len(params["symbols"]) != 1:
        return None
    return canonical_symbols(params["symbols"][0])


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
    def __init__(self, token, daily_limit=None):
        if not token or not token.strip():
            raise ValueError("Defina BRAPI_TOKEN no ambiente do servidor.")
        self.token = token.strip()
        configured_limit = daily_limit if daily_limit is not None else int(os.environ.get("BRAPI_MAX_UPSTREAM_CALLS_PER_DAY", DEFAULT_DAILY_UPSTREAM_LIMIT))
        if configured_limit < 1 or configured_limit > 100_000:
            raise ValueError("BRAPI_MAX_UPSTREAM_CALLS_PER_DAY deve ficar entre 1 e 100000.")
        self.daily_limit = configured_limit
        self.budget_day = datetime.datetime.now(datetime.timezone.utc).date()
        self.upstream_calls_today = 0
        self.lock = threading.Lock()
        self.cache = {}
        self.cooldown_until = 0
        self.client_windows = {}
        self.client_lock = threading.Lock()

    def allow_client(self, client, now=None):
        now = time.monotonic() if now is None else now
        with self.client_lock:
            expired = [address for address, (_, start) in self.client_windows.items() if now - start >= 60]
            for address in expired:
                del self.client_windows[address]
            window = self.client_windows.get(client)
            if window is None:
                if len(self.client_windows) >= MAX_TRACKED_CLIENTS:
                    return False, 60
                self.client_windows[client] = (1, now)
                return True, 0
            count, start = window
            if count >= CLIENT_REQUESTS_PER_MINUTE:
                return False, max(1, int(60 - (now - start)))
            self.client_windows[client] = (count + 1, start)
            return True, 0

    def quote(self, symbols):
        requested = canonical_symbols(symbols)
        if requested is None:
            return 400, {}, b'{"error":"symbols invalidos"}'
        now = time.monotonic()
        with self.lock:
            if now < self.cooldown_until:
                wait = max(1, int(self.cooldown_until - now))
                return 429, {"Retry-After": str(wait)}, b'{"error":"limite da fonte; aguarde"}'
            cached = self.cache.get(requested)
            if cached and now - cached[0] < CACHE_SECONDS:
                return 200, {}, cached[1]
            utc_day = datetime.datetime.now(datetime.timezone.utc).date()
            if utc_day != self.budget_day:
                self.budget_day = utc_day
                self.upstream_calls_today = 0
            if self.upstream_calls_today >= self.daily_limit:
                until_reset = datetime.datetime.combine(utc_day + datetime.timedelta(days=1), datetime.time(), datetime.timezone.utc)
                wait = max(1, int((until_reset - datetime.datetime.now(datetime.timezone.utc)).total_seconds()))
                return 429, {"Retry-After": str(wait)}, b'{"error":"limite diario local atingido; tente amanha"}'
            self.upstream_calls_today += 1
            url = UPSTREAM + quote(",".join(requested), safe=",")
            request = Request(url, headers={"Accept": "application/json", "Authorization": "Bearer " + self.token})
            try:
                with urlopen(request, timeout=20) as response:
                    body = response.read(MAX_BODY + 1)
                    if len(body) > MAX_BODY:
                        return 502, {}, b'{"error":"resposta excessiva da fonte"}'
                    parsed = json.loads(body)
                    rows = parsed.get("results") if isinstance(parsed, dict) else None
                    if not isinstance(rows, list) or len(rows) > len(requested):
                        return 502, {}, b'{"error":"resposta invalida da fonte"}'
                    seen = set()
                    for row in rows:
                        if not isinstance(row, dict):
                            return 502, {}, b'{"error":"resposta invalida da fonte"}'
                        symbol = row.get("symbol")
                        if symbol not in requested or row.get("requestedSymbol") != symbol or symbol in seen:
                            return 502, {}, b'{"error":"resposta invalida da fonte"}'
                        seen.add(symbol)
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
            except (URLError, TimeoutError, OSError, http.client.HTTPException, ValueError, json.JSONDecodeError):
                return 502, {}, b'{"error":"fonte indisponivel"}'


class Handler(BaseHTTPRequestHandler):
    gateway = None
    server_version = "CambioClaro"
    sys_version = ""

    def do_GET(self):
        allowed, wait = self.gateway.allow_client(self.client_address[0])
        if not allowed:
            status, headers, body = 429, {"Retry-After": str(wait)}, b'{"error":"limite local de requisicoes; aguarde"}'
        elif len(self.path) > MAX_REQUEST_TARGET:
            status, headers, body = 414, {}, b'{"error":"requisicao muito longa"}'
        else:
            symbols = symbols_from_target(self.path)
            if symbols is None:
                status, headers, body = 400, {}, b'{"error":"requisicao invalida"}'
            else:
                status, headers, body = self.gateway.quote(",".join(symbols))
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Cache-Control", "no-store")
        self.send_header("X-Content-Type-Options", "nosniff")
        self.send_header("Referrer-Policy", "no-referrer")
        self.send_header("Content-Security-Policy", "default-src 'none'")
        for key, value in headers.items():
            self.send_header(key, value)
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, format, *args):
        # No request paths, credentials or responses in application logs.
        pass

    def do_POST(self):
        self._method_not_allowed()

    def do_PUT(self):
        self._method_not_allowed()

    def do_DELETE(self):
        self._method_not_allowed()

    def do_PATCH(self):
        self._method_not_allowed()

    def do_HEAD(self):
        self._method_not_allowed()

    def do_OPTIONS(self):
        self._method_not_allowed()

    def do_TRACE(self):
        self._method_not_allowed()

    def _method_not_allowed(self):
        body = b'{"error":"metodo nao permitido"}'
        self.send_response(405)
        self.send_header("Allow", "GET")
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Cache-Control", "no-store")
        self.send_header("X-Content-Type-Options", "nosniff")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)


class BoundedThreadingHTTPServer(ThreadingHTTPServer):
    request_queue_size = MAX_ACTIVE_CLIENTS
    daemon_threads = True

    def __init__(self, server_address, handler_class, max_clients=MAX_ACTIVE_CLIENTS):
        super().__init__(server_address, handler_class)
        self._client_slots = threading.BoundedSemaphore(max_clients)

    def process_request(self, request, client_address):
        request.settimeout(CLIENT_SOCKET_TIMEOUT_SECONDS)
        if not self._client_slots.acquire(blocking=False):
            self.shutdown_request(request)
            return
        try:
            super().process_request(request, client_address)
        except Exception:
            self._client_slots.release()
            raise

    def process_request_thread(self, request, client_address):
        try:
            super().process_request_thread(request, client_address)
        finally:
            self._client_slots.release()


def main():
    Handler.gateway = Gateway(os.environ.get("BRAPI_TOKEN"))
    host = os.environ.get("BRAPI_LISTEN_HOST", "127.0.0.1")
    port = int(os.environ.get("BRAPI_LISTEN_PORT", "8787"))
    BoundedThreadingHTTPServer((host, port), Handler).serve_forever()


if __name__ == "__main__":
    main()
