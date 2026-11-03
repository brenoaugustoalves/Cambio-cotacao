# Iteração 4 — concorrência da consulta PTAX selecionada

## Objetivo e critérios de aceite

Evitar que uma consulta PTAX de moeda anterior atualize a interface depois de a pessoa selecionar outra moeda ou a atividade ser destruída.

- Uma requisição mais nova invalida a anterior e solicita seu cancelamento.
- Callback só pode atualizar a tela quando o token ainda for atual e a moeda continuar selecionada.
- Uma tarefa anexada depois de já ter sido substituída é cancelada de imediato.
- Invalidação cancela o trabalho ativo.
- Erro provocado por interrupção não é registrado como falha de fonte.
- Tokens continuam únicos mesmo quando várias requisições começam concorrentemente.

## Implementação e escopo

- `LatestRequestCoordinator` centraliza tokens, cancelamento de `Future` e consulta de atualidade.
- `MainActivity.load()` usa o coordenador para a consulta PTAX selecionada; troca de moeda e encerramento invalidam o token.
- O executor, Android UI thread e execução HTTP continuam nos adaptadores existentes.
- `LatestRequestCoordinatorTest` cobre cancelamento, anexação atrasada, invalidação e início concorrente.
- Consultas de mercado, ações e históricos ainda possuem flags locais e não foram migradas nesta etapa.

## Evidência

Executado em 2026-11-02:

```text
29 domain checks passed
17 conversion checks passed
10 market refresh policy checks passed
13 conversion presenter checks passed
8 latest request coordinator checks passed
308 Android parser, cache, component, widget, load and security checks passed. (API 34)
Local parser load: 250 market + 25 PTAX + 25 SGS history responses in 1284 ms.
RELEASE_GATE_PASSED
```

Comando: `python verify.py --emulator emulator-5556`.

## Próxima fatia

Migrar gradualmente os outros carregamentos e separar seus contratos de dados, preservando cache, erros por fonte e cancelamento por tela/moeda.
