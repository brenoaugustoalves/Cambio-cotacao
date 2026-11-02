import io
import json
import unittest
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
        body = b'{"results":[{"symbol":"PETR4"},{"symbol":"VALE3"}]}'
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

    def test_symbols_allowlist(self):
        gateway = app.Gateway("dummy")
        with patch.object(app, "urlopen") as upstream:
            for symbols in ("", "PETR4,PETR4", "BAD4", "PETR4,UNKNOWN"):
                self.assertEqual(gateway.quote(symbols)[0], 400)
            upstream.assert_not_called()

    def test_unauthorized_and_not_found(self):
        for code in (401, 404):
            gateway = app.Gateway("dummy")
            error = HTTPError("https://brapi.dev", code, "failed", {}, None)
            with patch.object(app, "urlopen", side_effect=error):
                self.assertEqual(gateway.quote("PETR4")[0], code)

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


if __name__ == "__main__":
    unittest.main()
