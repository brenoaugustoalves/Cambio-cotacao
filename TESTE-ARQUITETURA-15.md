# Etapa 15 — apresentação do histórico macroeconômico

## Alterações

- Criados `EconomyHistoryPresenter` e `HistoryDisplay` para formatar as observações mensais reais de IPCA e Selic.
- A tela mantém a ordem mais recente primeiro, mas a regra e formatação agora podem ser testadas sem Android.
- Cada série conserva unidade e data própria; períodos sem observação permanecem explícitos e não são interpolados.
- `EconomyActivity` limita-se a adaptar o cache e criar os componentes de texto.

## Verificação

Comando: `python verify.py --emulator emulator-5556`.

- 9 testes de apresentação do histórico passaram.
- 308 verificações instrumentadas no Android API 34 passaram.
- APK compilado e instalado no emulador; release gate aprovado.
- 300 respostas locais de parser processadas em 2.028 ms.
