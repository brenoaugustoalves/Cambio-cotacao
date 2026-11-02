# Testes — notícias Brasil e exterior 1.7.4

## Casos adicionados

- Parser Fed e BCE: título XML, datas GMT/offset, normalização do horário, ordenação e limite de tamanho/itens.
- Segurança: recusa feed desconhecido, rejeita DTD, filtra links de matérias fora do host oficial e publicações futuras.
- Resiliência: caches Fed e BCE independentes, atualização por idade e preservação de uma fonte se outra falhar.
- Componente: verifica seções Brasil/Exterior, links B3/BLS e preservação do painel econômico, histórico e agenda.
- Regressão geral: rede, widget, gráficos, dados nulos, diagnóstico, segurança e carga dos parsers.

## Resultado automatizado

- 240 verificações Android passaram, incluindo parsers, cache, componentes, widget, carga e segurança.
- 29 verificações de domínio passaram.
- APK instalado e inicializado no emulador Android API 34.
- Carga local: 250 respostas de mercado, 25 PTAX e 25 séries SGS processadas em 1.937 ms.
- APK assinado com v2 e v3; tamanho: 115.623 bytes.
- Avisos do compilador: `source/target value 8 is obsolete`; não impediram build nem testes.

## Verificação de fontes

- O feed oficial do Banco Central respondeu HTTP 200 quando consultado no ambiente de build.
- Os feeds do Fed e do BCE responderam HTTP 200 em validação anterior no ambiente de build. Os feeds RSS do BLS retornaram HTTP 403; o app não tenta contornar o bloqueio e abre as páginas oficiais por links.
- No teste visual do emulador, as notícias brasileiras do BC ficaram indisponíveis. O app manteve a indicação de falha e o link da página oficial de notícias da B3.
- O diagnóstico do emulador registrou `SSLHandshakeException` / `Trust anchor for certification path not found` para o feed do BCE. A verificação de certificados HTTPS permanece habilitada; nenhum fallback inseguro foi adicionado. A tela exibiu os dados em cache e informou que uma fonte falhou.

A disponibilidade ao vivo depende das fontes e da rede do aparelho. A interface identifica cache ou indisponibilidade em vez de apresentar conteúdo como atual quando não conseguiu consultar a fonte.
