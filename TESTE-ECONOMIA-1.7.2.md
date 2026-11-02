# Testes — agenda econômica 1.7.2

## Casos adicionados

- Valida que as categorias Copom e divulgações macroeconômicas apontam para o host e caminho oficial do BC via HTTPS.
- Rejeita URLs HTTP e links em host de terceiros.
- Testa a presença dos dois atalhos na tela econômica, em conjunto com as seções já entregues.
- Executa toda a suíte Android e verificações de domínio como regressão.

## Resultado

Resultado em 01/11/2026: `python verify.py --emulator emulator-5554` concluiu com `RELEASE_GATE_PASSED`; 29 verificações de domínio e 218 verificações Android instrumentadas passaram no emulador Android 14 / API 34. APK assinado versionCode 15, versionName 1.7.2, 107.431 bytes.

Links levam à agenda oficial do Banco Central; o teste automatizado valida segurança e presença dos atalhos, não faz scraping ou validação das datas publicadas pelo site. Android 8–13 e 15–17 e o aparelho físico não foram cobertos nesta execução.
