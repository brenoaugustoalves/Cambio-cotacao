# Etapa 10 — validade do cache econômico

## Alterações

- TTLs e regra de validade foram centralizados em `EconomyFreshnessPolicy`, uma política pura sem dependência de Android.
- SGS/Focus, histórico SGS, feed do BCB e feeds internacionais passaram a usar a mesma regra, mantendo suas janelas individuais: 6 horas para indicadores/histórico e 3 horas para notícias.
- Timestamp ausente, relógio do aparelho recuado, instante futuro, limite exato de expiração e atualização forçada têm comportamento testado.

## Verificação

Comando: `python verify.py --emulator emulator-5556`.

- 10 testes novos da política de frescor do cache.
- 308 verificações instrumentadas no emulador Android API 34 passaram.
- 300 respostas locais de parser foram processadas em 1.362 ms.
- APK compilado, instalado e release gate aprovado.

## Compatibilidade

Os intervalos e os dados salvos existentes foram preservados; a mudança centraliza a decisão sem alterar o formato do cache.
