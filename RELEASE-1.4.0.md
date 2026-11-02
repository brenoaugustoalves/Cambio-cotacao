# Câmbio Claro 1.4.0 — 30/10/2026

## O que mudou

- O gráfico do dia liga todas as observações reais. Trechos cinza mostram intervalos sem publicação; a linha entre as duas pontas é apenas uma ligação visual, não uma cotação intermediária. O toque continua selecionando somente pontos publicados.
- A tela principal mostra uma cotação da modalidade selecionada: dólar comercial ou dólar turismo. A PTAX é outra referência do Banco Central, não é o dólar turismo. Seu cartão e histórico ficam recolhidos por padrão no botão “Ver PTAX”.
- O valor principal de USD no app e no widget é arredondado a centavos, como nos conversores comuns; a taxa exata com quatro ou mais casas fica logo abaixo. O app indica quantos minutos se passaram desde a publicação e alerta quando não há nova publicação há dez minutos. O arredondamento não modifica a taxa usada na conversão.
- Regra de atualização: com o app aberto, consultar individualmente a moeda selecionada e seu gráfico a cada minuto; consultar o lote das demais moedas, PTAX e ações a cada cinco minutos. A consulta individual prevalece quando o lote retorna dado mais antigo. Consultas ocorrem também nos fins de semana, mas valores só mudam quando a fonte publica. Sem internet, mantém-se o último dado identificado com horário. O widget, com o app fechado, solicita atualização a cada trinta minutos; o Android pode adiá-la.
- A tela mostra alta, baixa ou estabilidade da moeda escolhida em relação à publicação de mercado imediatamente anterior do mesmo dia. Se faltarem duas observações, a variação fica indisponível.
- As quatro ações públicas (PETR4, VALE3, ITUB4 e MGLU3) são consultadas em uma chamada `GET /api/v2/stocks/quote?symbols=...`, sem chave. O Ibovespa depende de servidor HTTPS configurado. O app não pede nem armazena a chave brapi; uma chave que tenha sido salva por versão anterior é removida na abertura desta versão.
- O código em `server/brapi_proxy.py` lê `BRAPI_TOKEN` do ambiente **do servidor** e envia `Authorization: Bearer` somente à brapi. Aceita apenas símbolos autorizados, limita a resposta, faz cache breve, trata 401, 403, 404, 429 e respeita `Retry-After`. Não foi implantado, pois ainda não existe servidor HTTPS informado.

## Para ativar o Ibovespa

1. Hospede o gateway Python em um servidor próprio, atrás de HTTPS e limite de requisições na borda. Ele escuta em `127.0.0.1:8787` por padrão e exige `BRAPI_TOKEN` no ambiente antes de iniciar. Nunca inclua essa variável no APK, no código ou em um repositório.
2. Confirme que a chave e o plano dão acesso a `^BVSP` e que a exibição atende às condições da fonte e da B3.
3. No app, em Preferências, informe apenas a origem HTTPS do servidor, como `https://dados.seudominio.com`. O app consulta `/v1/stocks/quote?symbols=%5EBVSP`.

Sem o servidor, o índice aparece indisponível. Isso não impede as quatro ações públicas. O backend fornecido é uma base local; não equivale a uma implantação com domínio, TLS, monitoramento e proteção contra abuso.

## Testes e fontes

Leia [brapi llms.txt](https://brapi.dev/llms.txt), [OpenAPI](https://brapi.dev/openapi.json), [cotação de ações](https://brapi.dev/docs/acoes/cotacao) e [autenticação](https://brapi.dev/docs/authentication). A operação escolhida é `/api/v2/stocks/quote`. Uma consulta pública real com `symbols=PETR4,VALE3` foi executada em 30/10/2026; valores e horários devem ser consultados novamente, pois mudam.

Rode `python -m unittest discover -s server -p 'test_*.py'` para testar o gateway sem chave real: 6 testes passaram. `python verify.py --emulator emulator-5554 --bundle` compilou, assinou e validou APK/AAB, além de 21 testes Java e 183 verificações Android 17. As mesmas 183 verificações passaram no emulador Android 14. A consulta individual também foi conferida visualmente com dados atuais. A variável `BRAPI_TOKEN` não estava configurada neste ambiente; portanto, não houve teste autenticado nem teste de Ibovespa real. O Motorola físico também não estava conectado. Os testes automatizados não garantem ausência de todas as falhas.

O pacote e a assinatura do APK foram mantidos, com versionCode 8 para atualização sobre as versões anteriores. Nada foi publicado na Google Play. O ZIP de código exclui a chave de assinatura.
