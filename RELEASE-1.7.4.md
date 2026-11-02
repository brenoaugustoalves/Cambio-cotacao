# Câmbio Claro 1.7.4 — notícias para acompanhar mercados

## Parte 5: Brasil e exterior

- **Brasil:** notas oficiais do Banco Central com data e link para a publicação original; botão para abrir a página de notícias da B3.
- **Exterior:** manchetes de política monetária dos feeds oficiais do Federal Reserve e do Banco Central Europeu, mostrando entidade, data/hora e link de origem. Os títulos permanecem no idioma publicado.
- **Inflação e emprego dos EUA:** links diretos para as páginas do BLS. O app não consulta nem contorna o bloqueio a automação observado nos feeds RSS desse órgão.
- As notícias são publicações e comunicados, sem resumo gerado ou afirmação de causalidade/previsão de preço. Uma publicação pode ser relevante para acompanhar juros, inflação ou atividade, mas o app não presume o efeito na bolsa.
- Cache separado por feed por até três horas. Fontes falham independentemente e a tela identifica cópia salva/indisponibilidade. Até oito manchetes internacionais são mostradas ordenadas pelo horário publicado.
- RSS internacional: HTTPS, hosts fixos, redirecionamentos desativados, timeout e limite de tamanho, parser XML com DTD desativado, URLs de matérias limitadas ao host oficial e datas futuras filtradas.
- A B3 tem página pública de notícias; as matérias abrem direto no site da B3, sem redistribuir cópia no app. A B3 informa que a distribuição de Market Data/notícias é uma atividade regulada por sua política comercial.
- `versionCode=17`, `versionName=1.7.4`; assinatura local mantida.
