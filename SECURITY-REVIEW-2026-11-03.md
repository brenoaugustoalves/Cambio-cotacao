# Revisão de segurança local — 03/11/2026

## Escopo

Revisão estática do app Android e do proxy opcional da brapi, com cenários de abuso executados apenas contra objetos mockados e um servidor HTTP local em `127.0.0.1`. Nenhum endpoint público foi sondado; não foram feitas chamadas reais/pagas à brapi nem tentativas contra Google, B3 ou outros terceiros.

## Achados e correções

1. **Cache permitia combinações equivalentes de símbolos em ordens diferentes.** A lista agora é canonicalizada antes do cache e da chamada ao fornecedor.
2. **Entrada HTTP tinha pouca validação e não limitava consumo por cliente.** Agora há limite de tamanho, rota e query estritas, allowlist sem duplicatas, limite local de 60 GETs/minuto por IP, 32 conexões simultâneas, timeout de socket e tabela de clientes limitada.
3. **Chamadas ao fornecedor não tinham teto diário por processo.** Foi adicionado o padrão de 400 chamadas/dia, configurável por `BRAPI_MAX_UPSTREAM_CALLS_PER_DAY`, além da cache de 60 segundos.
4. **Respostas do fornecedor eram validadas parcialmente.** Corpo limitado a 256 KiB; símbolos, duplicatas, tipo de linhas e JSON são verificados antes do cache. Erros internos são substituídos por respostas sanitizadas e 429 respeita `Retry-After`.
5. **Métodos diferentes de GET não tinham resposta de política explícita e o servidor anunciava versão da linguagem.** Métodos não suportados retornam 405 e `Allow: GET`; o cabeçalho de servidor não revela a versão do Python.

## Cenários verificados

- símbolo fora da allowlist, duplicado ou com query poluída;
- caminho errado, alvo excessivo e tentativa de incluir controle/CRLF;
- permutação de símbolos para contornar cache;
- payload upstream inválido, excedente, duplicado ou contendo ativo inesperado;
- falhas de rede e respostas 401/403/404/429 sem vazar detalhes;
- esgotamento de limite por IP e do orçamento diário;
- rota, cabeçalhos e método HTTP no servidor local.

No manifesto Android também foram confirmados `usesCleartextTraffic=false`, `allowBackup=false` e componentes internos não exportados (atividades de entrada e componentes do sistema continuam exportados quando necessário).

## Resultado

`python -m unittest -v` na pasta `server/`: 15 testes passaram. `python verify.py`: `RELEASE_GATE_PASSED`, APK compilado/verificado e testes de domínio/apresentação passaram. O verificador Java emite avisos de compatibilidade por compilar código legado com `--release 8`; não houve falha de build.

## Risco pendente antes de publicar o servidor

O proxy continua **sem autenticação de cliente**. Como o app é distribuído em dispositivos, uma chave estática no APK não seria segredo. Os limites implementados são por processo, reiniciam com o processo e não são compartilhados entre instâncias. Antes de disponibilizar uma URL pública, colocar atrás de HTTPS com autenticação apropriada para cliente móvel, rate limit compartilhado na borda, limite de instâncias, orçamento/alertas no fornecedor e logs/monitoramento sem tokens. Até lá, o servidor permanece configurado para escutar em `127.0.0.1`.
