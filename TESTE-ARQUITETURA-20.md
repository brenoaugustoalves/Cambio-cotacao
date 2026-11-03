# Etapa 20 — apresentação da PTAX

## Alterações

- Extraídos `PtaxQuotePresenter` e `PtaxQuoteDisplay` para a cotação oficial exibida na tela principal.
- Preço, horário/data publicada, ausência de publicação no dia e variação frente à observação PTAX anterior ficam numa apresentação testável.
- A PTAX permanece separada do preço de mercado e usa a regra oficial de publicação diária.
- A Activity faz bind dos textos e cores ao cartão.

## Verificação

Comando: `python verify.py --emulator emulator-5556`.

- 8 testes do apresentador PTAX passaram.
- 308 verificações instrumentadas no Android API 34 passaram.
- APK compilado/instalado e release gate aprovado.
- 300 respostas de parser locais processadas em 1.398 ms.
