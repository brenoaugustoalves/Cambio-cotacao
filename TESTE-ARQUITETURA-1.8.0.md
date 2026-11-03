# Iteração de arquitetura 1.8.0 — conversão

## Objetivo e aceite

Mover a política de conversão para uma camada que não dependa de Android, mantendo os dados e as fontes reais já usados pelo aplicativo.

- A regra de conversão aceita valor não negativo no formato já definido pelo domínio e arredonda BRL para duas casas.
- Origem e horário da observação acompanham o resultado.
- Conversão comercial usa mercado e pode recorrer à última PTAX salva, deixando a origem explícita.
- Conversão turismo usa exclusivamente a cotação turismo e mostra indisponibilidade se ela não existir.
- Entrada ausente/inválida e moeda/modo inválido produzem estados explícitos; nenhuma taxa é estimada.
- Tela permanece responsável pela formatação visual, sem escolher fontes ou aplicar fallback.

## Implementação

- `ConversionRequest`, `ConversionQuote` e `ConversionResult` representam entrada, referência e estados de saída.
- `ConvertCurrency` contém a política sem imports Android e recebe `ConversionQuoteSource` por construtor.
- `AndroidConversionQuoteSource` adapta `MarketStore`, `Tourism` e `RateStore` para essa porta.
- `MainActivity` agora apresenta o estado e o metadado recebidos do caso de uso.
- `ConversionTest` usa fonte falsa determinística; não acessa rede nem dados do usuário.

## Evidência local

Executado em 2026-11-02:

```text
29 domain checks passed
17 conversion checks passed
308 Android parser, cache, component, widget, load and security checks passed. (API 34)
Local parser load: 250 market + 25 PTAX + 25 SGS history responses in 2268 ms.
RELEASE_GATE_PASSED
```

Comando: `python verify.py --emulator emulator-5556`.

## Limites e próxima iteração

Esta entrega extraiu um fluxo, não toda a arquitetura. A tela principal ainda coordena várias áreas e constrói a UI programaticamente. Próximos cortes recomendados: componentes de apresentação do conversor e coordenação das atualizações/mercado, cada um com testes de comportamento e dos limites offline antes de remover lógica antiga.
