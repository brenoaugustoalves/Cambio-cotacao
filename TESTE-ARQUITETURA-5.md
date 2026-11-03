# Iteração 5 — atualização em lote do mercado

## Objetivo e critérios de aceite

Extrair a sequência de atualização de mercado da tela principal sem alterar a fonte AwesomeAPI, validação, cache, arquivo ou regra de alertas.

- Uma resposta de lote é buscada uma vez e entregue ao repositório sem transformação.
- O repositório valida e persiste o lote antes de consultar observações efetivas por moeda.
- Cada observação salva é arquivada e avaliada para alertas na mesma moeda.
- Moedas sem observação são ignoradas; o fluxo não cria pontos nem dispara alertas para elas.
- Falha na fonte impede persistência e efeitos posteriores.
- `MainActivity` mantém a execução assíncrona e a atualização das views; a política do fluxo é Java puro.

## Implementação e escopo

- `RefreshMarketQuotes` coordena a fonte, o repositório, o writer do histórico e o destino de alertas por interfaces.
- `MarketObservation` transporta preço ask e timestamp sem dependência Android.
- `AndroidMarketRefreshGateway` adapta `MarketClient`, `MarketStore`, `MarketArchive` e `MarketAlerts`.
- `MainActivity.loadMarket()` executa o caso de uso no executor existente e mantém os estados visuais de carregamento/erro.
- O widget e o simulador econômico continuam usando seus fluxos próprios nesta iteração.
- `RefreshMarketQuotesTest` usa um gateway falso determinístico, sem rede ou dados privados.

## Evidência

Executado em 2026-11-02:

```text
29 domain checks passed
17 conversion checks passed
10 market refresh policy checks passed
13 conversion presenter checks passed
8 latest request coordinator checks passed
8 market refresh use case checks passed
308 Android parser, cache, component, widget, load and security checks passed. (API 34)
Local parser load: 250 market + 25 PTAX + 25 SGS history responses in 1443 ms.
RELEASE_GATE_PASSED
```

Comando: `python verify.py --emulator emulator-5556`.

## Próxima fatia

Reutilizar o fluxo de atualização no widget e no simulador econômico; depois extrair ações e histórico, com as regras de atraso e dados salvos preservadas.
