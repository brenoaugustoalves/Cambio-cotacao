# Proxy opcional da brapi

Este serviço é opcional. Ele mantém `BRAPI_TOKEN` no servidor e oferece ao app uma rota limitada para consultar ações/índice. A chave nunca deve entrar no APK, arquivo versionado, log ou resposta ao cliente.

## Rota

`GET /v1/stocks/quote?symbols=PETR4,VALE3`

Símbolos aceitos: `PETR4`, `VALE3`, `ITUB4`, `MGLU3` e `^BVSP`. A consulta é enviada à operação `/api/v2/stocks/quote` com o header `Authorization: Bearer …`. O gateway rejeita parâmetros extras, símbolos repetidos e caminhos inesperados; canonicaliza a ordem dos símbolos; limita alvo HTTP, corpo recebido, conexões simultâneas, clientes por minuto e chamadas upstream por dia; e respeita `Retry-After` da brapi quando há HTTP 429. Respostas incompletas ou incompatíveis são rejeitadas e erros upstream não revelam detalhes internos.

Os limites locais são uma proteção adicional por processo. O padrão permite até 60 GETs por IP por minuto, 32 conexões simultâneas, 10 segundos sem atividade por socket e 400 chamadas upstream por dia. O teto diário pode ser alterado por `BRAPI_MAX_UPSTREAM_CALLS_PER_DAY` (1–100000). Reiniciar o processo reinicia esse contador e múltiplas instâncias têm contadores independentes; por isso, esses limites **não substituem** autenticação e limitação compartilhada na borda, nem um orçamento configurado no fornecedor.

## Configuração local

PowerShell:

```powershell
$env:BRAPI_TOKEN = "<token fornecido pela brapi>"
$env:BRAPI_LISTEN_HOST = "127.0.0.1"
$env:BRAPI_LISTEN_PORT = "8787"
python server/brapi_proxy.py
```

O padrão é escutar somente em `127.0.0.1`. Para um app Android fora do computador, publique atrás de um proxy reverso HTTPS e configure autenticação adequada a clientes móveis, limitação compartilhada por IP/usuário, orçamento/alertas de custo e acesso de rede no servidor. Uma chave embutida no APK não é segredo. Não exponha o servidor de desenvolvimento diretamente à internet. Definir `BRAPI_LISTEN_HOST=0.0.0.0` não o torna seguro para publicação.

## Testes

Na pasta `server/`:

```powershell
python -m unittest -v
```

Os testes simulam a resposta upstream e verificam a chave de servidor enviada no header, allowlist, normalização do cache, limites locais, entradas malformadas, respostas inesperadas, falhas, tamanhos e respeito a `Retry-After`; não fazem chamadas reais pagas à brapi. Execute o conjunto junto da validação Android com `python verify.py` na raiz do projeto.
