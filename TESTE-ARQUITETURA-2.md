# Iteração 2 — política de atualização

## Objetivo e critérios de aceite

Retirar da `MainActivity` as decisões sobre frequência de consultas em primeiro plano e mantê-las em uma política pura, previsível e testável sem Android.

- Rede ilimitada: atualização de mercado a cada 1 minuto; histórico a cada 5 minutos.
- Rede medida: atualização de mercado a cada 5 minutos; histórico a cada 15 minutos.
- Economia de bateria: atualização de mercado respeita intervalo mínimo de 15 minutos.
- Consulta completa vence após 12 horas.
- Regressão do relógio não dispara atualização completa antecipada.
- A política não altera a execução em segundo plano: o Android pode adiar tarefas.

## Implementação

- `MarketRefreshPolicy` contém os intervalos e a regra para decidir quando vence uma atualização completa.
- `MainActivity` mantém a leitura de conectividade/economia de energia e agenda o `Handler`, mas delega as decisões para a política.
- `AUTO_REFRESH_MS` permanece como alias de compatibilidade para o teste de integração existente.
- `MarketRefreshPolicyTest` cobre intervalos, limites e relógio retrocedido sem criar dependência Android.

## Evidência

Executado em 2026-11-02:

```text
29 domain checks passed
17 conversion checks passed
10 market refresh policy checks passed
308 Android parser, cache, component, widget, load and security checks passed. (API 34)
Local parser load: 250 market + 25 PTAX + 25 SGS history responses in 1356 ms.
RELEASE_GATE_PASSED
```

Comando: `python verify.py --emulator emulator-5556`.

## Próxima fatia

`MainActivity` ainda decide quando iniciar cada tipo de consulta e também contém construção de UI. Próximo passo: separar um coordenador de atualização com dependências explícitas, mantendo o agendamento Android fora do domínio e cobrindo cancelamento, concorrência e estado em segundo plano.
