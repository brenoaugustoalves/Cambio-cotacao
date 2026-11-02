# Proxy opcional da brapi

Este serviço é opcional. Ele mantém `BRAPI_TOKEN` no servidor e oferece ao app uma rota limitada para consultar ações/índice. A chave nunca deve entrar no APK, arquivo versionado, log ou resposta ao cliente.

## Rota

`GET /v1/stocks/quote?symbols=PETR4,VALE3`

Símbolos aceitos: `PETR4`, `VALE3`, `ITUB4`, `MGLU3` e `^BVSP`. A consulta é enviada à operação `/api/v2/stocks/quote` com o header `Authorization: Bearer …`. O gateway limita tamanho de resposta, timeout, lista de símbolos e frequência por cache curto; respeita `Retry-After` da brapi quando há HTTP 429.

## Configuração local

PowerShell:

```powershell
$env:BRAPI_TOKEN = "<token fornecido pela brapi>"
$env:BRAPI_LISTEN_HOST = "127.0.0.1"
$env:BRAPI_LISTEN_PORT = "8787"
python server/brapi_proxy.py
```

O padrão é escutar somente em `127.0.0.1`. Para um app Android fora do computador, publique atrás de um proxy reverso HTTPS e controle autenticação, limite de requisições e acesso de rede no servidor. Não exponha o servidor de desenvolvimento diretamente à internet. Definir `BRAPI_LISTEN_HOST=0.0.0.0` exige configurar essas proteções antes.

## Testes

Na pasta `server/`:

```powershell
python -m unittest -v
```

Os testes simulam a resposta upstream e verificam autenticação, allowlist, cache, falhas, tamanhos e respeito a `Retry-After`; não fazem chamadas reais pagas à brapi.
