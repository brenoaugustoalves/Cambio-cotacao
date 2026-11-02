# Câmbio Claro 1.7.0 — painel econômico com fontes

## Parte 1: painel macroeconômico e simulador pessoal

- Nova tela **Economia brasileira**, acessível pela tela principal.
- Dados observados: meta Selic (BCB/SGS 432) e IPCA mensal (BCB/SGS 433), com competência e unidade.
- Expectativas Focus: medianas anuais para IPCA, Selic, Câmbio e PIB Total, com data da pesquisa, ano de referência e número de instituições respondentes.
- Valores observados ficam separados das expectativas. O Focus é identificado como pesquisa de mercado, não previsão do Banco Central.
- Respostas HTTPS são limitadas em tamanho e tempo, validadas quanto a formato, intervalo e data. Datas futuras, campos inválidos e respostas incompletas são rejeitados.
- Cache privado por até seis horas para reduzir tráfego; consulta manual disponível. Em falha de rede, a tela indica a última consulta válida. Não estima nem preenche lacunas.
- O diagnóstico local passa a incluir somente data/estado do cache econômico, sem valores macroeconômicos.
- Simulador de compra: calcula um cenário usando cotação de referência consultada no momento (mercado ask quando disponível ou PTAX venda), spread e encargos informados pelo usuário. Não aplica IOF nem tarifas presumidas; o usuário informa custos e a tela deixa explícito que é uma estimativa.
- Sem cotação atual válida, o simulador usa cache somente com aviso visível de dado antigo/fonte indisponível; não disfarça referência como preço final.
- `versionCode=13`, `versionName=1.7.0`, assinatura local preservada para atualização sobre versões anteriores.

## Teste desta parte

`verify.py --emulator emulator-5554` executado no emulador Android 14: 29 verificações de domínio, 208 verificações instrumentadas Android e gate de release aprovados. Consulte `TESTE-ECONOMIA-1.7.0.md` para escopo e limites.

## Próximas partes

Agenda econômica baseada em calendário oficial; notícias oficiais com links e data de publicação; comparação de séries econômicas; e expansão de ativos/fundos após validar fonte e permissão de distribuição. Cada parte terá testes e resultados registrados antes da próxima.
