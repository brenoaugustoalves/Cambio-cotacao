# Iteração 3 — apresentação do conversor

## Objetivo e critérios de aceite

Separar a tradução de resultados de conversão em mensagens de apresentação, mantendo `MainActivity` como bind de dados para views.

- Cotações de mercado, turismo e PTAX aparecem com origem correta.
- Horários de mercado usam o fuso recebido (Brasília no app); PTAX mostra a data de publicação.
- Entrada inválida, modo inválido e cotação indisponível geram mensagem específica e não exibem valor calculado.
- Turismo indisponível não indica nem usa cotação comercial como substituto.
- Nenhum código do apresentador depende de Android ou consulta fontes.

## Implementação

- `ConversionPresenter` produz o estado de apresentação localizado.
- `ConversionDisplay` é um resultado imutável pronto para bind.
- `MainActivity` executa o caso de uso e aplica somente o valor formatado e a mensagem às views.
- `ConversionPresenterTest` fixa os textos, metadados de fonte e fuso com dados determinísticos.

## Evidência

Executado em 2026-11-02:

```text
29 domain checks passed
17 conversion checks passed
10 market refresh policy checks passed
13 conversion presenter checks passed
308 Android parser, cache, component, widget, load and security checks passed. (API 34)
Local parser load: 250 market + 25 PTAX + 25 SGS history responses in 1312 ms.
RELEASE_GATE_PASSED
```

Comando: `python verify.py --emulator emulator-5556`.

## Próxima fatia

O próximo passo é extrair a coordenação das consultas de mercado da `MainActivity`, com cancelamento, concorrência e callbacks de UI explicitamente testados.
