# Iteração 7 — consulta e persistência da PTAX

## Objetivo e critérios

Remover busca, proteção contra resposta antiga e persistência PTAX das activities/JobService.

- Fonte BCB retorna resposta parseada e payload original para armazenamento.
- Repositório compara com a última observação e rejeita regressão antes de gravar.
- Horário de consulta é atribuído depois da resposta da fonte.
- Alertas só são avaliados após persistência bem-sucedida.
- Simulador, resumo e widget podem armazenar PTAX sem executar alertas; quando configurados, os alertas usam o caso de atualização completo.
- A pessoa usuária continua vendo erro da fonte e os dados salvos, se disponíveis.

## Implementação

- `PtaxBatch` carrega payload e observações imutáveis.
- `StorePtaxRates` coordena fonte, proteção temporal e repositório com relógio substituível em testes.
- `RefreshPtaxRates` acrescenta avaliação de alertas somente após a gravação.
- `AndroidPtaxGateway` adapta `BcbClient`, `RateStore` e `Alerts`.
- `MainActivity`, `WidgetUpdateService`, `refreshOverview` e o simulador usam esses casos de uso.
- `RefreshPtaxRatesTest` cobre sequência, lote antigo, falha de fonte/gravação, lote vazio e validação.

## Evidência

Executado em 2026-11-02 com `python verify.py --emulator emulator-5556`:

```text
29 domain checks passed
17 conversion checks passed
13 market refresh policy checks passed
13 conversion presenter checks passed
8 latest request coordinator checks passed
8 market refresh use case checks passed
5 stock refresh use case checks passed
6 market history refresh checks passed
8 PTAX refresh use case checks passed
308 Android parser, cache, component, widget, load and security checks passed. (API 34)
Local parser load: 250 market + 25 PTAX + 25 SGS history responses in 1389 ms.
RELEASE_GATE_PASSED
```

Total: 107 testes JVM e 308 instrumentados no API 34. APK recompilado e instalado no emulador.

## Ainda pendente

O provedor e a atividade de turismo, além de SGS/Focus/notícias, ainda mantêm fluxos próprios; não foram alterados nesta etapa.
