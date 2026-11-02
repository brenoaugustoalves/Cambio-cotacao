# Câmbio Claro 1.6.1 — continuidade e eficiência

O gráfico intradiário preserva o último preço observado com um segmento tracejado em períodos sem publicação. O trecho é identificado como valor retido, sem inventar novas observações, e se estende até a consulta atual. Quando chega uma cotação real nova, o gráfico passa ao novo ponto sem ligar os dois por uma diagonal que sugira dados intermediários.

As atualizações em primeiro plano agora se adaptam à rede: intervalo-base de 1 minuto no Wi‑Fi, 5 minutos em rede móvel e pelo menos 15 minutos no modo de economia de bateria. O histórico do gráfico é consultado a cada 5 minutos no Wi‑Fi ou 15 minutos em rede móvel. Séries PTAX das moedas em segundo plano usam cache de até 24 horas; a moeda selecionada é consultada ao iniciar/atualizar manualmente e em ciclos espaçados. As consultas de mercado usam uma chamada em lote para os pares suportados; as tarefas do widget deixaram de repetir uma chamada por moeda. Em segundo plano, o Android ainda pode adiar o trabalho.

- `versionCode=11`, `versionName=1.6.1`, mesma assinatura local.
- Compatibilidade: Android 8.0/API 26 ou superior, incluindo Android 17/API 37.
- APK e AAB destinados a instalação/teste local. Nada foi enviado à Google Play.
- O app funciona por HTTPS em Wi‑Fi Starlink. Conexão celular direta ao satélite depende de serviço/operadora autorizados no país e não é controlada pelo aplicativo.
- Cotações, notícias e avisos da B3 exigem fornecedor e licença confirmados. Sem fonte conectada, o app não inventa conteúdo nem promete atualização 24/7.

## Verificação

Os testes de parsing, cache, widgets, rede, segurança e carga local são executados por `verify.py`. Os limites dos emuladores, do Android em segundo plano e do fornecedor estão registrados em `TESTE-RESILIENCIA-1.6.1.md`.
