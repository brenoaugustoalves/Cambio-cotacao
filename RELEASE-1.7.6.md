# Câmbio Claro 1.7.6 — cotação e movimento claramente separados

## Parte 6: leitura do gráfico intradiário

- O gráfico de cotação mantém os preços publicados ao longo do dia, com alta em verde, queda em vermelho e estabilidade em cinza.
- Um novo gráfico de barras divergentes mostra cada variação entre duas publicações reais: altas acima da linha zero, quedas abaixo, barras proporcionais ao maior movimento do período.
- O ponto selecionado exibe horário, cotação, diferença absoluta em reais e diferença percentual contra a publicação real anterior.
- A lista de publicações mostra a base do dia e cada mudança com preço, horário, direção, delta e percentual. Mostra as seis mais recentes; quando há mais, permite expandir para o histórico completo.
- As lacunas não são interpoladas. O tracejado no gráfico de cotação continua indicando o último valor conhecido, sem afirmar que houve negociações naquele intervalo.
- `versionCode=19`, `versionName=1.7.6`.
