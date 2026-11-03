# Etapa 21 — resumo de moedas

## Alterações

- Criados `CurrencyOverviewPresenter` e `CurrencyOverviewDisplay` para as linhas de resumo por moeda.
- Quando há cotação de mercado, apresenta variação baseada em observações do dia; sem publicação atual, marca explicitamente a última observação.
- Se não há mercado válido, usa PTAX identificada; após falha, sinaliza que o valor é salvo.
- Sem valor de mercado ou PTAX, informa indisponibilidade/espera e não inventa números.
- `MainActivity` adapta o cache e aplica cores/textos às linhas do resumo.

## Verificação

Comando: `python verify.py --emulator emulator-5556`.

- 8 testes do resumo de moedas passaram.
- 308 verificações instrumentadas no Android API 34 passaram.
- APK compilado/instalado e release gate aprovado.
- 300 respostas locais de parser processadas em 1.802 ms.
