# Etapa 11 — agendamento de consulta PTAX

## Alterações

- Criada a política pura `PtaxRefreshPolicy` para o intervalo de atualização da PTAX em telas de resumo e no widget.
- A consulta considera timestamp ausente, limite de seis/24 horas conforme o chamador e recuo do relógio. Assim, um timestamp futuro não provoca uma sequência de tentativas.
- O `WidgetUpdateService` continua sendo o adaptador de ciclo de vida do JobScheduler; consulta, gravação e alertas permanecem delegados aos casos de uso PTAX.

## Verificação

Comando: `python verify.py --emulator emulator-5556`.

- 7 testes novos de política PTAX.
- 10 testes de frescor econômico, 7 de atualização independente de fontes, 9 de turismo e 8 de PTAX passaram.
- 308 verificações Android API 34 passaram.
- 300 respostas de parser local processadas em 1.891 ms.
- APK compilado/instalado e release gate aprovado.

## Escopo arquitetural

Regras, políticas e coordenação de fontes ficam em classes Java testáveis sem Android; rede e preferências ficam nos adaptadores. Construção de views, estado visual e callbacks permanecem nas Activities, que são a camada de apresentação do app nativo.
