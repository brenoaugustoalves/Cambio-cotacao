import io
import json
import threading
import unittest
from http.client import HTTPConnection
from http.server import ThreadingHTTPServer
from unittest.mock import patch
from urllib.error import HTTPError

import brapi_proxy as app


class Response:
    def __init__(self, body):
        self.body = io.BytesIO(body)

    def __enter__(self):
        return self

    def __exit__(self, *_):
        pass

    def read(self, size):
        return self.body.read(size)


class GatewayTests(unittest.TestCase):
    def test_missing_token_fails_closed(self):
        with self.assertRaises(ValueError):
            app.Gateway(None)

    def test_batch_and_authorization_header(self):
        body = b'{"results":[{"symbol":"PETR4","requestedSymbol":"PETR4"},{"symbol":"VALE3","requestedSymbol":"VALE3"}]}'
        def send(request, timeout):
            self.assertEqual(request.get_header("Authorization"), "Bearer dummy-server-token")
            self.assertIn("symbols=PETR4,VALE3", request.full_url)
            return Response(body)
        gateway = app.Gateway("dummy-server-token")
        with patch.object(app, "urlopen", side_effect=send) as upstream:
            status, _, result = gateway.quote("PETR4,VALE3")
            self.assertEqual(status, 200)
            self.assertEqual(len(json.loads(result)["results"]), 2)
            self.assertEqual(gateway.quote("PETR4,VALE3")[0], 200)
            self.assertEqual(upstream.call_count, 1, "cache avoids repeated paid calls")

    def test_permuted_symbols_share_canonical_cache_entry(self):
        body = b'{"results":[{"symbol":"PETR4","requestedSymbol":"PETR4"},{"symbol":"VALE3","requestedSymbol":"VALE3"}]}'
        gateway = app.Gateway("dummy")
        with patch.object(app, "urlopen", return_value=Response(body)) as upstream:
            self.assertEqual(gateway.quote("VALE3,PETR4")[0], 200)
            self.assertEqual(gateway.quote("PETR4,VALE3")[0], 200)
        self.assertEqual(upstream.call_count, 1)

    def test_symbols_allowlist(self):
        gateway = app.Gateway("dummy")
        with patch.object(app, "urlopen") as upstream:
            for symbols in ("", "PETR4,PETR4", "BAD4", "PETR4,UNKNOWN"):
                self.assertEqual(gateway.quote(symbols)[0], 400)
            upstream.assert_not_called()

    def test_unauthorized_and_not_found(self):
        for code in (401, 403, 404):
            gateway = app.Gateway("dummy")
            error = HTTPError("https://brapi.dev", code, "failed token=secret", {}, None)
            with patch.object(app, "urlopen", side_effect=error):
                status, _, body = gateway.quote("PETR4")
                self.assertEqual(status, code)
                self.assertNotIn(b"secret", body)

    def test_retry_after_prevents_early_upstream_retry(self):
        gateway = app.Gateway("dummy")
        error = HTTPError("https://brapi.dev", 429, "rate limited", {"Retry-After": "120"}, None)
        with patch.object(app, "urlopen", side_effect=error) as upstream:
            status, headers, _ = gateway.quote("PETR4")
            self.assertEqual(status, 429)
            self.assertEqual(headers["Retry-After"], "120")
            self.assertEqual(gateway.quote("VALE3")[0], 429)
            self.assertEqual(upstream.call_count, 1)

    def test_invalid_json_is_not_cached(self):
        gateway = app.Gateway("dummy")
        with patch.object(app, "urlopen", return_value=Response(b"bad")):
            self.assertEqual(gateway.quote("PETR4")[0], 502)

    def test_response_must_match_requested_symbols(self):
        cases = (
            b'{"results":[{"symbol":"VALE3","requestedSymbol":"VALE3"}]}',
            b'{"results":[{"symbol":"PETR4","requestedSymbol":"VALE3"}]}',
            b'{"results":[{"symbol":"PETR4","requestedSymbol":"PETR4"},{"symbol":"PETR4","requestedSymbol":"PETR4"}]}',
            b'{"results":[null]}',
        )
        for body in cases:
            with self.subTest(body=body):
                gateway = app.Gateway("dummy")
                with patch.object(app, "urlopen", return_value=Response(body)):
                    self.assertEqual(gateway.quote("PETR4")[0], 502)
                    self.assertEqual(gateway.quote("PETR4")[0], 502, "invalid response must not enter cache")

    def test_daily_upstream_budget(self):
        gateway = app.Gateway("dummy", daily_limit=1)
        body = b'{"results":[{"symbol":"PETR4","requestedSymbol":"PETR4"}]}'
        with patch.object(app, "urlopen", return_value=Response(body)) as upstream:
            self.assertEqual(gateway.quote("PETR4")[0], 200)
            status, headers, _ = gateway.quote("VALE3")
        self.assertEqual(status, 429)
        self.assertGreater(int(headers["Retry-After"]), 0)
        self.assertEqual(upstream.call_count, 1)

    def test_daily_budget_configuration_is_bounded(self):
        for value in (0, 100_001):
            with self.subTest(value=value), self.assertRaises(ValueError):
                app.Gateway("dummy", daily_limit=value)

    def test_client_rate_limit_is_bounded_and_expires(self):
        gateway = app.Gateway("dummy")
        for index in range(app.CLIENT_REQUESTS_PER_MINUTE):
            self.assertTrue(gateway.allow_client("192.0.2.1", now=100 + index / 100)[0])
        allowed, retry = gateway.allow_client("192.0.2.1", now=100.5)
        self.assertFalse(allowed)
        self.assertGreater(retry, 0)
        self.assertTrue(gateway.allow_client("192.0.2.1", now=161)[0])
        for index in range(app.MAX_TRACKED_CLIENTS - 1):
            self.assertTrue(gateway.allow_client("192.0.2." + str(index + 2), now=200)[0])
        self.assertFalse(gateway.allow_client("192.0.3.1", now=200)[0])

    def test_request_parser_rejects_query_pollution_and_oversize(self):
        invalid = (
            "/v1/stocks/quote?symbols=PETR4&symbols=VALE3",
            "/v1/stocks/quote?symbols=PETR4&admin=true",
            "/v1/stocks/quote?symbols=PETR4;VALE3",
            "/v1/stocks/quote?symbols=PETR4,PETR4",
            "/v1/stocks/quote?symbols=PETR4%0d%0aHost:evil",
            "/v1/stocks/quote?symbols=" + "A" * app.MAX_REQUEST_TARGET,
            "https://example.test/v1/stocks/quote?symbols=PETR4",
            "/v1/stocks/quote/../quote?symbols=PETR4",
        )
        for target in invalid:
            with self.subTest(target=target[:80]):
                self.assertIsNone(app.symbols_from_target(target))

    def test_network_errors_are_sanitized(self):
        gateway = app.Gateway("never-return-secret")
        with patch.object(app, "urlopen", side_effect=OSError("failure token=secret")):
            status, _, body = gateway.quote("PETR4")
        self.assertEqual(status, 502)
        self.assertNotIn(b"secret", body)

    def test_oversized_upstream_body_is_rejected(self):
        gateway = app.Gateway("dummy")
        with patch.object(app, "urlopen", return_value=Response(b"x" * (app.MAX_BODY + 1))):
            self.assertEqual(gateway.quote("PETR4")[0], 502)


class LocalHttpTests(unittest.TestCase):
    def test_http_route_and_method_policy(self):
        body = b'{"results":[{"symbol":"PETR4","requestedSymbol":"PETR4"}]}'
        app.Handler.gateway = app.Gateway("dummy")
        server = ThreadingHTTPServer(("127.0.0.1", 0), app.Handler)
        thread = threading.Thread(target=server.serve_forever, daemon=True)
        thread.start()
        try:
            with patch.object(app, "urlopen", return_value=Response(body)) as upstream:
                connection = HTTPConnection("127.0.0.1", server.server_port, timeout=2)
                connection.request("GET", "/v1/stocks/quote?symbols=PETR4")
                response = connection.getresponse()
                self.assertEqual(response.status, 200)
                self.assertEqual(response.getheader("X-Content-Type-Options"), "nosniff")
                self.assertEqual(response.getheader("Server"), "CambioClaro ")
                self.assertEqual(json.loads(response.read())["results"][0]["symbol"], "PETR4")
                connection.close()

                connection = HTTPConnection("127.0.0.1", server.server_port, timeout=2)
                connection.request("GET", "/v1/stocks/quote?symbols=BAD4")
                response = connection.getresponse()
                self.assertEqual(response.status, 400)
                response.read()
                connection.close()

                connection = HTTPConnection("127.0.0.1", server.server_port, timeout=2)
                connection.request("POST", "/v1/stocks/quote?symbols=PETR4")
                response = connection.getresponse()
                self.assertEqual(response.status, 405)
                self.assertEqual(response.getheader("Allow"), "GET")
                response.read()
                connection.close()

                connection = HTTPConnection("127.0.0.1", server.server_port, timeout=2)
                connection.request("OPTIONS", "/v1/stocks/quote?symbols=PETR4")
                response = connection.getresponse()
                self.assertEqual(response.status, 405)
                self.assertEqual(response.getheader("Allow"), "GET")
                response.read()
                connection.close()
                self.assertEqual(upstream.call_count, 1)
        finally:
            server.shutdown()
            server.server_close()
            thread.join(timeout=2)


if __name__ == "__main__":
    unittest.main()
