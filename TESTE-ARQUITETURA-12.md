# Etapa 12 — apresentação de indicadores econômicos

## Alterações

- Extraídos `EconomyPresenter` e `EconomyDisplay`, Java puro, para formatar dados SGS e expectativas Focus antes do bind Android.
- A Activity converte o cache para o modelo de entrada e apenas aplica os textos às views.
- Precisão, pt-BR, unidade, competência, número de respondentes, horário de Brasília, falha parcial e campos ausentes estão cobertos por testes.
- Valores sem observação válida continuam indisponíveis; nenhum valor ou horário é preenchido por estimativa.

## Verificação

Comando: `python verify.py --emulator emulator-5556`.

- 10 testes novos do apresentador econômico.
- 308 verificações instrumentadas no Android API 34 passaram.
- APK compilado e instalado no emulador; release gate passou.
- 300 respostas locais de parser processadas em 2.290 ms.

## Escopo

A composição de views e callbacks continua em `EconomyActivity`. A formatação e as regras de apresentação dos indicadores estão separadas e não dependem do Android.
