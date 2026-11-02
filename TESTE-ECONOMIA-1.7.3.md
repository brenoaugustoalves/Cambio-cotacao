# Testes — histórico econômico 1.7.3

## Casos adicionados

- SGS ordena os dados por data e mantém valores IPCA negativos permitidos.
- Alinhamento mensal seleciona o último registro Selic do mês, mantém IPCA e Selic em colunas/unidades diferentes e preserva meses sem observação de uma fonte.
- Rejeita datas futuras, formatos numéricos inválidos, valor Selic não positivo e séries não autorizadas.
- Testa endpoints fixos e limites, cache independente e tempo de atualização.
- Testa presença da seção histórica na tela e contraste/regressão geral.

## Resultado

Em 01/11/2026: `python verify.py --emulator emulator-5554` concluiu com `RELEASE_GATE_PASSED`; 29 verificações de domínio e 230 verificações Android instrumentadas passaram no emulador Android 14 / API 34. APK assinado versionCode 16, versionName 1.7.3, 111.527 bytes.

Smoke test real do SGS: o endpoint IPCA retornou HTTP 200 e 13 observações (01/09/2025 a 01/09/2026, 483 bytes); a consulta Selic por intervalo retornou HTTP 200 e 366 observações (01/11/2025 a 01/11/2026, 13.909 bytes). O endpoint `ultimos/400` respondeu 400 e informou o limite máximo de 99; foi substituído pelo intervalo explícito de datas e a falha foi documentada/corrigida antes do gate final.

Teste local de carga dos parsers: 250 respostas de mercado, 25 PTAX e 25 históricos SGS simulados de 366 pontos em 2.504 ms no emulador.

Limite: teste de rede valida a resposta observada nessa execução, não SLA futuro. Em falha de uma fonte, o app mantém o cache válido independente e marca a falta. Android 8–13/15–17 e aparelho Motorola físico não foram cobertos.
