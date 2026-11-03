# Etapa 22 — calendário de contratos futuros no domínio

## Alterações

- Movido `FuturesContracts` do pacote raiz da interface para `market/domain`.
- Seleção de vencimento, código do contrato (WIN/DOL/WDO + letra do mês + ano), ordenação e ausência de calendário projetado ficaram independentes do Android.
- A tela mantém a seleção salva e apresenta o vencimento, o último dia de negociação e a indisponibilidade de preço enquanto não existir provedor confirmado.
- Fonte do calendário: [calendário de vencimentos de contratos financeiros da B3](https://www.b3.com.br/pt_br/solucoes/plataformas/puma-trading-system/para-participantes-e-traders/calendario-de-negociacao/vencimentos/calendario-de-vencimentos-de-contratos-financeiros/).
- A extração não integra cotações futuras nem cria números, horário ou atraso.

## Verificação

Comando executado: `python verify.py --emulator emulator-5556`.

- 9 testes Java puros de seleção e validação de contratos passaram.
- 308 verificações instrumentadas no emulador Android API 34 passaram.
- Build assinado e instalação no emulador concluídos; release gate aprovado.
- 300 respostas locais de mercado, PTAX e histórico SGS processadas em 1.405 ms.
- `git diff --check` sem erros.
