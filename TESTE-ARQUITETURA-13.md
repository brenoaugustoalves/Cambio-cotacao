# Etapa 13 — simulador de custo de câmbio

## Alterações

- O domínio `EconomicSimulator` agora fica em `economy/domain`, separado do framework Android.
- `CalculateEconomicScenario` coordena a seleção da referência publicada e o cálculo; `EconomicQuoteProvider` é a porta para fontes.
- `AndroidEconomicQuoteProvider` adapta mercado/AwesomeAPI e PTAX/BCB, preservando horário, origem e aviso de dado antigo ou fallback.
- A `EconomyActivity` lê/valida os campos e apresenta o resultado; consulta de fonte e cálculo ficam fora da Activity.
- Sem cotação válida, não há resultado estimado nem fallback silencioso entre mercado e PTAX.

## Verificação

Comando: `python verify.py --emulator emulator-5556`.

- 10 testes novos de cenário: fonte selecionada, cálculo com taxa publicada, metadados, falhas, ausência de cotação e entrada inválida.
- 308 verificações instrumentadas no Android API 34 passaram.
- APK compilado/instalado no emulador; release gate aprovado.
- 300 respostas de parser processadas em 2.769 ms.
