# Etapa 19 — apresentação do cartão de mercado

## Alterações

- Extraídos `MarketQuotePresenter` e `MarketQuoteDisplay` para derivar texto do cartão usando moeda, cotação, observações do dia, instante e fuso.
- A tela mantém o USD comercial e turismo separados; demais moedas conservam quatro casas na cotação principal.
- Movimento é mostrado apenas com observações reais suficientes. O cartão explicita hora publicada, tempo desde a publicação e atraso.
- `MainActivity` adapta o cache Android para observações de domínio e aplica os textos/cores ao cartão.

## Verificação

Comando: `python verify.py --emulator emulator-5556`.

- 11 testes novos do cartão de cotação passaram.
- 308 verificações instrumentadas no Android API 34 passaram.
- APK compilado/instalado e release gate aprovado.
- 300 respostas locais de parser processadas em 1.292 ms.
