# Testes — gráfico único 1.7.7

## Casos

- Alta, queda e estabilidade indicam sentido e valores absolutos/percentuais.
- A primeira observação é apresentada como base, sem comparação inventada.
- O gráfico selecionado anuncia no conteúdo acessível o horário, a cotação e a variação do ponto.
- A lista de publicações mantém a associação horário/preço/delta.
- A tela usa um único gráfico de linha; não inclui gráfico de barras adicional.
- Regressão geral de parsers, cache, componentes, widgets, segurança e carga.

## Resultado final

- 245 verificações Android passaram no emulador Android 14 / API 34.
- 29 verificações de domínio passaram.
- Build e assinatura APK passaram; assinaturas v2 e v3 válidas.
- Carga local: 250 respostas de mercado, 25 PTAX e 25 séries SGS processadas em 2.156 ms.
- APK assinado: 119.719 bytes.
- Avisos do compilador Java 8 obsoleto/depreciação não impediram build ou testes.
