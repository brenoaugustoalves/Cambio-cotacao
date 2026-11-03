# Etapa 9 — fontes econômicas e notícias

## Alterações

- Criado o caso de uso Java puro `RefreshSources`, que tenta fontes independentes, preserva os sucessos e devolve cada falha com seu identificador e causa.
- Criado `AndroidEconomySources` para downloads, validação, cache e persistência de SGS/Focus, histórico SGS, comunicados do Banco Central e feeds do Fed/BCE.
- `EconomyActivity` agora coordena a apresentação e delega as fontes. Uma falha não interrompe atualização das outras fontes; o cache válido continua disponível e falhas seguem para Diagnóstico.
- Os dados permanecem separados por origem, observação/expectativa, competência e horário publicados. Nenhuma série ou notícia é sintetizada.

## Verificação

Comando: `python verify.py --emulator emulator-5556`.

- 7 testes novos do caso de uso de fontes independentes.
- 9 testes de turismo e 8 de PTAX, além das suítes existentes, passaram.
- 308 verificações Android API 34 passaram (parser, cache, componentes, widget, carga e segurança).
- Teste local: 300 respostas de parsers em 1.260 ms; APK compilado e instalado no emulador.
- Release gate concluído.

## Limites

Feeds podem estar temporariamente fora do ar ou publicar em intervalos próprios. A tela identifica falha e horário do cache, e não promete cobertura contínua nem reação de mercado.
