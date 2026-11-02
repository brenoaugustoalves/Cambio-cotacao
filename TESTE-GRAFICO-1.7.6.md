# Testes — gráfico de cotação e variações 1.7.6

## Casos verificados

- Alta, queda e estabilidade exibem direção e variação absoluta/percentual exatas.
- Primeira cotação é identificada como base, sem comparação inventada.
- Gráfico de barras divergentes é desenhado e expõe descrição acessível.
- Ponto selecionado no gráfico de preço anuncia a mudança relativa à publicação anterior.
- Histórico mostra base e publicações reais; acima de seis mudanças, permite expandir para as anteriores.
- Regressão geral de parser, cache, componentes, widget, segurança e carga.

## Resultado final

- **246 verificações Android passaram** no emulador Android 14 / API 34.
- **29 verificações de domínio passaram.**
- Build e assinatura do APK passaram; assinatura v2 e v3 válidas.
- Carga local: 250 respostas de mercado, 25 PTAX e 25 séries SGS processadas em 2.264 ms.
- APK: 119.719 bytes.
- Conferência visual manual feita no emulador Pixel 8a API 34; instalação incremental concluída.
- Uma asserção inicial dependia de coordenada fixa do pixel e falhou em outra densidade. Foi substituída por verificação de desenho independente da densidade; a suíte final passou.
- Avisos do compilador sobre nível Java 8 obsoleto/depreciação não impediram build ou testes.
