# Testes — recursos de mercado 1.7.9

## Escopo

- Parsing de histórico com timestamps de segundos/milissegundos, ordenação e limite da fonte.
- Persistência local, deduplicação por timestamp, janela máxima do arquivo e filtro dos períodos.
- Gráfico: seletor entre modo intradiário com valor retido e períodos históricos que terminam na última observação real.
- Alertas: limite atingido nos dois sentidos, igualdade, alerta one-shot e rejeição de cotações antigas/indisponíveis.
- Preferências: controles distintos para alerta PTAX e de mercado, rótulo da fonte e contraste em tema escuro/claro.
- Regressão de componentes, dados offline, segurança, widget e carga de parsers.

## Resultado

Executado com `python verify.py --emulator emulator-5556`, em emulador Android 14 / API 34:

- 29 testes de domínio passaram.
- 259 testes instrumentados Android passaram.
- Instalação incremental e validação criptográfica do APK aprovadas.
- Carga local de 250 respostas de mercado, 25 PTAX e 25 séries SGS em 2.109 ms.
- Conferência visual feita em Pixel 8a virtual, incluindo tela principal e Preferências.

O teste não afirma monitoramento contínuo do fornecedor: dados de produção variam conforme as publicações da AwesomeAPI, conexão e agendamento do sistema. Nenhum teste gerou preço sintético para o gráfico.
