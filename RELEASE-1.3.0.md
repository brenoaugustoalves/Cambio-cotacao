# Câmbio Claro 1.3.0 — 30/10/2026

Instale o APK sobre a versão anterior. O pacote e a assinatura foram preservados; não é preciso desinstalar. Android mínimo declarado: API 26. Compilado para API 37. Nada foi publicado na Google Play.

## Entregue

- Dez moedas frente ao real: cotação de mercado com data e horário da fonte, gráfico intradiário de pontos publicados, PTAX identificada separadamente, conversão e widget.
- USD comercial e USD turismo em telas e pares distintos. O turismo é uma referência de mercado sem IOF, spread ou tarifas adicionais.
- Painel de ações e Ibovespa. As ações gratuitas usam dados da brapi; o índice requer chave própria de API. O horário exibido é o da fonte, e a consulta pode sofrer atraso.
- Mercado futuro com WIN, DOL e WDO. A seleção mostra o código completo, mês de vencimento e último pregão do calendário B3 de 2026–2027. Após o último pregão, o vencimento padrão avança. O app não calcula códigos além do calendário verificado.
- O preço, a variação, o horário de cotação e o atraso dos futuros aparecem como indisponíveis. Nenhuma API de preços para futuros foi ativada, pois ainda falta confirmar o fornecedor e a licença de exibição. Não são mostrados valores inventados.
- Diagnóstico local ampliado e interface escura ou clara nas preferências.

## Fontes e licença dos futuros

Calendário e UDN (último dia de negociação): [B3](https://www.b3.com.br/pt_br/solucoes/plataformas/puma-trading-system/para-participantes-e-traders/calendario-de-negociacao/vencimentos/calendario-de-vencimentos-de-contratos-financeiros/). A brapi [documenta futuros de fim de dia, atualizados uma vez à noite](https://brapi.dev/docs/futuros) e [cotação do último pregão](https://brapi.dev/docs/futuros/cotacao). O plano Pro é anunciado com cobertura de futuros e custa, na consulta de 30/10/2026, R$ 1.399,90/ano na [página de preços](https://brapi.dev/pricing); esse preço pode mudar. O acesso de demonstração sem token é limitado a WIN e WDO, sem DOL. A cobertura autenticada de DOL não foi comprovada com chamada real.

A [brapi declara que não é distribuidora licenciada da B3](https://brapi.dev/faq). Para dados futuros em tempo real ou com 15 minutos de atraso e distribuição a terceiros, a [B3 descreve acordo de distribuição e contato com distribuidor autorizado](https://www.b3.com.br/en_us/market-data-and-indices/data-services/market-data/distributors/faq/). Preço final, frequência, cobertura WIN/DOL/WDO e direito de exibição em aplicativo dependem de proposta e contrato. Até essa confirmação, o módulo exibe somente calendário, nunca cotação.

## Verificação

`verify.py --emulator emulator-5554 --bundle` compila, assina e verifica APK/AAB, roda 21 testes Java e 176 checagens Android em emulador Android 17. O mesmo conjunto Android passou no emulador Android 14 antes do último ajuste visual; a mudança final de recorte da rolagem foi verificada no Android 17. Foram testados casos de troca de contratos, virada de ano e fim do calendário. Nenhum teste em Motorola físico ou auditoria independente de segurança foi realizado. Testes automatizados reduzem riscos conhecidos, sem garantir ausência de falhas.

O ZIP de código exclui a chave de assinatura. Para atualizar este mesmo aplicativo no futuro, preserve o backup privado da assinatura fornecido na entrega anterior.
