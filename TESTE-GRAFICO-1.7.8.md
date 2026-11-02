# Testes — linha contínua 1.7.8

## Casos verificados

- Linha sólida liga pontos publicados separados por intervalo superior a cinco minutos.
- Não há observações intermediárias criadas; preços e horários da lista continuam sendo apenas os recebidos da fonte.
- O trecho posterior à última cotação é mostrado como último valor retido.
- Alta, queda, estabilidade, seleção do ponto, preço/hora e deltas continuam corretos.
- Regressão geral de parser, cache, componentes, widget, segurança e carga.

## Resultado final

- 246 verificações Android passaram no emulador Android 14 / API 34.
- 29 verificações de domínio passaram.
- Build e assinatura APK passaram; assinaturas v2 e v3 válidas.
- Carga local: 250 respostas de mercado, 25 PTAX e 25 séries SGS processadas em 2.157 ms.
- APK assinado: 119.719 bytes.
- Instalação incremental e conferência visual concluídas no emulador Pixel 8a API 34.
- Avisos do compilador sobre Java 8 obsoleto/depreciação não impediram build ou testes.
