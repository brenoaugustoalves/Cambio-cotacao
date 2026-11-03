# Iteração 6 — atualização de mercado, ações e históricos

## Objetivo

Completar os limites de aplicação pendentes para consultas do widget, simulador, ações e históricos sem alterar fornecedores, regras de cache ou formato dos dados exibidos.

## Alterações e aceite

- `StoreMarketBatch` compartilha busca e persistência no simulador econômico sem acionar alertas nem atualizar o arquivo de gráfico.
- `RefreshMarketQuotes` continua coordenando arquivo e alertas no app principal; o widget passou a usar o mesmo fluxo quando o lote está vencido.
- O widget continua avaliando alertas com a cotação salva quando não precisa buscar outro lote; respeita o intervalo mínimo já existente.
- `RefreshStockQuotes` busca ações e Ibovespa independentemente: uma fonte indisponível não impede a atualização da outra, e o índice não é consultado sem servidor configurado.
- `RefreshMarketHistory` separa histórico intradiário e fechamentos diários, limita a janela de consulta à faixa da AwesomeAPI e não completa lacunas.
- Adaptadores Android mantêm HTTP, SharedPreferences, SQLite e persistência fora dos casos de uso.

## Testes e evidência

Executado em 2026-11-02 com `python verify.py --emulator emulator-5556`:

```text
29 domain checks passed
17 conversion checks passed
13 market refresh policy checks passed
13 conversion presenter checks passed
8 latest request coordinator checks passed
8 market refresh use case checks passed
5 stock refresh use case checks passed
6 market history refresh checks passed
308 Android parser, cache, component, widget, load and security checks passed. (API 34)
Local parser load: 250 market + 25 PTAX + 25 SGS history responses in 1279 ms.
RELEASE_GATE_PASSED
```

Total: 99 checks JVM e 308 instrumentados no API 34. O APK foi recompilado e instalado no emulador.

## Limites

PTAX/turismo, o carregamento de dados SGS/Focus/notícias e os componentes de tela ainda são coordenados pelas activities/serviços existentes. Esta etapa não afirma que toda a aplicação já foi migrada para Clean Architecture; esses fluxos ficam para etapas próprias com seus testes.
