# Testes — painel econômico 1.7.0

## Verificações automatizadas

- Parser SGS: unidade, precisão, IPCA mensal negativo válido, valor malformado e data futura.
- Parser Focus: quatro indicadores e respondentes, data futura, resposta de datas inconsistentes, campos selecionados e URL limitada ao ano de referência.
- Componentes Android: tela econômica aberta em tema escuro e claro, conteúdo observado/expectativas, contraste e activity não exportada.
- Simulador: cálculo da base, spread percentual, encargos percentuais e tarifa fixa, arredondamento em centavos e rejeição de entradas inválidas/negativas/não finitas.
- Suítes existentes: conversão, cache, widget, rede e segurança.
- Teste de integração manual no emulador: navegação a partir da tela inicial; fontes BCB consultadas independentemente; falha HTTP em SGS 432 e Focus sem ocultar o IPCA (SGS 433) bem-sucedido; UI mostra estado parcial e não cria valores de reposição.

Os fixtures embutidos na instrumentação são amostras sintéticas para exercitar o parser; não alimentam a interface do aplicativo. A tela consulta somente as fontes HTTPS do Banco Central. O teste externo reflete disponibilidade da rede no instante da execução e não é garantia de SLA.

## Resultado

Resultado em 01/11/2026, no emulador `emulator-5554` (Android 14 / API 34): `29 domain checks passed`; `208 Android parser, cache, component, widget, load and security checks passed`; `RELEASE_GATE_PASSED`. O APK assinado foi gerado com versionCode 13 e versionName 1.7.0.

Limite: esta execução não valida Android 8–13 nem 15–17, aparelhos físicos, autonomia de bateria em campo ou indisponibilidade prolongada. A cobertura de compatibilidade nesses ambientes continua pendente e não deve ser apresentada como testada.
