# Etapa 8 — atualização do dólar turismo

## Alterações

- Extraído `RefreshTourismQuotes`, com política pura de atualização independente para cotação e histórico.
- `AndroidTourismGateway` concentra leitura dos horários de consulta, acesso à AwesomeAPI e persistência pelo cache já validado.
- Tela principal e widget compartilham o mesmo caso de uso. A falha da cotação não cancela a tentativa de histórico e vice-versa.
- Atualização explícita do widget força uma consulta de cada tipo; a tela respeita os intervalos conforme rede e estado de energia.
- Falhas específicas continuam registradas pelo Diagnóstico e a interface mantém cache válido.

## Verificação

Comando: `python verify.py --emulator emulator-5556`.

- 9 testes do caso de uso turismo.
- Todos os testes anteriores passaram: domínio, conversão, agenda, apresentador, coordenação de requisição, mercado, ações, histórico e PTAX.
- 308 verificações instrumentadas no Android API 34 passaram, incluindo parsers, cache, componentes, widget, carga local e segurança.
- Teste de parsing local: 250 respostas de mercado, 25 PTAX e 25 SGS em 1.421 ms.
- APK compilado, instalado no emulador e release gate concluído.

## Limite

Os pontos continuam dependendo da publicação da fonte. Uma linha do gráfico não deve ser interpretada como cotação nos períodos em que a API não publicou dados.
