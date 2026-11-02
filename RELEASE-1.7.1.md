# Câmbio Claro 1.7.1 — comunicados oficiais

## Parte 2: notícias econômicas do Banco Central

- Nova seção de comunicados no painel econômico.
- Consulta o feed Atom oficial “Notas à imprensa”, com até cinco títulos, hora de publicação e link para a matéria original. A página do Banco Central informa que seus feeds RSS publicam atualizações oficiais ao longo do dia: https://www.bcb.gov.br/en/about/rssen
- Atualiza ao abrir e pelo botão de atualização; cache local por até três horas para economizar rede. Se a fonte falha, identifica claramente que exibe uma cópia salva e sua hora de consulta. Sem cópia, informa indisponibilidade.
- Parser limitado a 512 KiB/100 itens, HTTPS para host do Banco Central, redirecionamento desativado, DTD desativado, links de matéria aceitos somente em host BCB, campos e datas validados; publicações futuras são filtradas.
- Sem geração ou resumo automático: o usuário abre a publicação oficial para ler o texto completo.
- `versionCode=14`, `versionName=1.7.1`; a assinatura local existente é mantida para atualização do aplicativo instalado.

## Partes anteriores

A 1.7.0 adicionou indicadores SGS, expectativas Focus separadas das observações e um simulador com custos informados pelo usuário. Consulte `RELEASE-1.7.0.md` e `TESTE-ECONOMIA-1.7.0.md`.

## Próximas partes planejadas

Agenda oficial de divulgações; comparação histórica de indicadores; notícias da B3 com origem/horário verificados e direitos de distribuição; e dados de fundos via conjuntos públicos da CVM. Cada item será implementado e testado antes de passar ao seguinte.
