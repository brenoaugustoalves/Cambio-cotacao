# Testes — comunicados oficiais 1.7.1

## Testes automatizados

- Atom: decodifica título XML, ordena por horário, preserva timezone e URL HTTPS oficial.
- Resiliência de conteúdo: descarta link de host externo, filtra publicação futura e rejeita ano de feed fora do intervalo.
- Segurança XML: rejeita DTD/entidade externa; URL de consulta tem host, caminho e parâmetro de ano fixos. Parser limita tamanho e quantidade de registros.
- Componente Android: seção de comunicados aparece no painel com título, publicação oficial e simulador; atividade continua privada.
- Suíte de regressão existente: conversão, fontes, cache independente, gráfico, widget, ações, entradas nulas, rede, diagnóstico e segurança.

Os dados de teste embutidos exercitam o parser e não alimentam a interface. A interface usa o feed oficial HTTPS e a matéria oficial como fonte.

## Resultado em 01/11/2026

- `python verify.py --emulator emulator-5554`: concluído com `RELEASE_GATE_PASSED`.
- Smoke test da fonte em 01/11/2026: HTTP 200, `application/atom+xml`, dez comunicados no feed oficial de 2026; título e data da primeira publicação foram lidos do XML.
- 29 verificações de domínio passaram.
- 215 verificações Android instrumentadas passaram no emulador Android 14 / API 34.
- APK assinado: versionCode 14, versionName 1.7.1, 107.431 bytes.
- Desempenho medido nas fixtures antigas: 250 respostas de mercado e 25 respostas PTAX em 1.372 ms no emulador.

Limites: não representa teste de aparelho físico, nem matriz Android 8–13 ou 15–17. Testes com fixtures não garantem disponibilidade da fonte. B3 não está incluída nesta parte.
