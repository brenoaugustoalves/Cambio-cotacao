# Etapa 17 — apresentação de movimentos do gráfico

## Alterações

- Extraída a regra de movimento para `MarketMovementPresenter`, que recebe observações tipadas e fuso explicitamente.
- Cálculo de variação absoluta/percentual, sinal, ponto base e texto do ponto selecionado não depende de Android.
- `IntradayMovement` converte os pontos do cache para o modelo de domínio e encaminha à apresentação compartilhada pela Activity e pelo gráfico.
- Variação só existe quando há duas publicações reais; a primeira permanece ponto base.

## Verificação

Comando: `python verify.py --emulator emulator-5556`.

- 10 testes novos do apresentador do gráfico passaram.
- 308 verificações instrumentadas no Android API 34 passaram.
- APK compilado/instalado e release gate aprovado.
- 300 respostas locais de parser processadas em 1.703 ms.
