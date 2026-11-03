# Etapa 16 — apresentação do simulador

## Alterações

- Criados `EconomicScenarioPresenter` e `EconomicScenarioDisplay` para formatar o resultado financeiro em pt-BR.
- A apresentação mostra separadamente base, spread informado, demais encargos, tarifa fixa e total.
- A mesma saída mantém fonte, horário publicado e aviso de cotação antiga/fallback.
- A Activity passa a vincular os textos apresentados, sem recompor parcelas ou ocultar a origem da taxa.

## Verificação

Comando: `python verify.py --emulator emulator-5556`.

- 7 testes novos para o resultado do simulador.
- 308 verificações instrumentadas no Android API 34 passaram.
- APK compilado/instalado e release gate aprovado.
- 300 respostas de parser processadas em 2.246 ms.
