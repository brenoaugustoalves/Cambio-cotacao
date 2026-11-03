# Etapa 18 — comparação entre moedas

## Alterações

- Movida a comparação para `market/domain`, sem dependência de views ou Android.
- As moedas são alinhadas exclusivamente por datas com publicações reais em comum; variações partem da primeira data comum.
- Datas, mapa e valores de cada série são imutáveis.
- Entrada ausente/inválida é rejeitada; sem sobreposição, o modelo retorna vazio em vez de estimar valores.

## Verificação

Comando: `python verify.py --emulator emulator-5556`.

- 10 testes de domínio para interseção, baseline, variações positiva/negativa, imutabilidade e ausência de sobreposição.
- 308 verificações instrumentadas no Android API 34 passaram.
- APK compilado/instalado e release gate aprovado.
- 300 respostas de parser locais processadas em 2.093 ms.
