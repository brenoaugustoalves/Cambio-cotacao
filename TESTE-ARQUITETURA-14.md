# Etapa 14 — apresentação de notícias

## Alterações

- Criado `NewsPresenter` e modelo `NewsDisplay` para exibir feeds do Banco Central, Fed e BCE.
- A apresentação preserva título, fonte, URL e horário de publicação; limita a quantidade visível sem alterar a ordem de origem.
- Estados de feed vazio, cache indisponível, falha parcial e horário de consulta futuro são explícitos.
- `EconomyActivity` mantém a construção dos botões Android e encaminha cada toque ao link publicado.

## Verificação

Comando: `python verify.py --emulator emulator-5556`.

- 9 testes de apresentação de notícias passaram.
- 308 verificações instrumentadas no Android API 34 passaram.
- APK compilado e instalado no emulador; release gate aprovado.
- 300 respostas locais de parser processadas em 1.957 ms.
