# Etapa 23 — apresentação de contratos futuros

## Alterações

- Extraídos código e descrição do contrato selecionado para `FuturesContractPresenter` e o modelo imutável `FuturesContractDisplay`.
- A tela informa o vencimento B3 e último dia de negociação e identifica cotação, variação, horário e atraso como indisponíveis enquanto não houver provedor de futuros integrado.
- A `MainActivity` agora conecta seleção e texto apresentado, sem montar esses dados diretamente.

## Verificação

Executar `python verify.py --emulator emulator-5556`.

- 4 testes puros de apresentação dos contratos passaram.
- 308 verificações instrumentadas no Android API 34 passaram.
- Build assinado e instalação no emulador concluídos; release gate aprovado.
- `git diff --check` sem erros.
