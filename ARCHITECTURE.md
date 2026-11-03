# Arquitetura do Câmbio Claro

## Limites aplicados nesta etapa

O projeto continua Android nativo e sem dependências externas de runtime. A compilação usa o SDK diretamente; a organização por responsabilidade não depende de Gradle.

### Atualizações do aplicativo

```text
SettingsActivity ──> GithubUpdates (coordena casos de uso)
                         ├──> GithubReleaseClient (HTTP, JSON, limites e resumo)
                         ├──> GithubRelease (metadados imutáveis e comparação de versão)
                         ├──> ApkInstaller (pacote, versionCode e certificado)
                         └──> UpdateNotifier (notificação local)
```

`UpdateCheckJob` é o adaptador do JobScheduler. `UpdateInstallReceiver` encaminha o estado da sessão à camada que usa o instalador do Android. A atividade não faz parsing HTTP nem verifica certificado diretamente.

### Histórico de mercado

`MarketArchive` é a fachada de acesso. `MarketArchiveDb` é a implementação SQLite. A tabela intradiária e a tabela diária são isoladas, cada uma com chave `(moeda, horário)` e consultas indexadas. O histórico antigo de `SharedPreferences` é migrado em uma thread de fundo; enquanto a migração não termina, a leitura retorna vazia e a tela informa a ausência de pontos.

`MarketHistory` valida pares, timestamps e valores fornecidos pela AwesomeAPI. Para períodos maiores, a UI consulta a operação de fechamento diário documentada pela fonte e salva esses pontos no repositório diário. Não existe geração de cotações entre timestamps.

### Conversão de moedas

```text
MainActivity ──> ConvertCurrency ──> ConversionQuoteSource (porta)
                       │                       ▲
                       └──> Rate               │
                                 AndroidConversionQuoteSource
                                   ├── MarketStore
                                   ├── Tourism
                                   └── RateStore (PTAX)
```

`ConvertCurrency` recebe uma intenção (`ConversionRequest`), aplica as regras de modo/moeda e calcula usando a regra de domínio `Rate.convert`. A porta permite testar essa política sem Android, rede ou armazenamento real; o adaptador Android lê somente observações persistidas. `ConversionResult` comunica estados explícitos à apresentação, que formata valor, origem e horário.

`ConversionPresenter` traduz os estados em texto e metadados de exibição usando o fuso informado pelo chamador; `ConversionDisplay` é o modelo imutável para bind. Ambos são Java puro, então idioma, indicação de fonte e mensagens de indisponibilidade podem ser testados sem instanciar views.

Para conversão comercial, uma cotação de mercado ausente permite usar a última PTAX salva, com essa origem indicada ao usuário. Para turismo, a falta de uma cotação própria resulta em indisponibilidade; não há substituição por comercial/PTAX. A conversão não consulta rede durante digitação e não estima taxas nem tarifas.

### Política de atualização

`MarketRefreshPolicy` define os intervalos sem depender do Android: atualização principal a cada 1 min em rede ilimitada ou 5 min em rede medida; economia de bateria eleva o intervalo mínimo a 15 min; histórico atualiza a cada 5 min ou 15 min, respectivamente; atualização completa vence em 12 h. `MainActivity` lê as condições do dispositivo e o `Handler` apenas agenda a próxima execução. O Android ainda pode adiar tarefas quando o app está em segundo plano.

`LatestRequestCoordinator` controla o token e a tarefa cancelável da consulta PTAX selecionada. Ao trocar a moeda ou destruir a atividade, o token é invalidado e o `Future` é cancelado; uma resposta só é aplicada se ainda pertencer ao pedido corrente e à moeda visível. As atividades ainda mantêm flags locais para impedir consultas duplicadas de histórico, ações e turismo.

`RefreshMarketQuotes` coordena o lote usado pela tela principal e pelo widget: fonte → repositório → observações por moeda → arquivo de histórico e alertas. `StoreMarketBatch` compartilha somente busca e persistência com o simulador, sem acionar alertas. `RefreshStockQuotes` mantém erros independentes para ações e Ibovespa. `RefreshMarketHistory` separa carregamentos intradiários e diários e limita a janela pedida ao provedor; `AndroidMarketHistoryGateway` valida e grava cada série no seu repositório.

### Apresentação

A tela principal agrupa componentes em destinos Moedas, Bolsa e Fontes; Economia abre sua própria atividade. Dentro de Moedas, navegação adicional isola Cotação, Gráfico, PTAX, Converter e Outras. Assim, o cliente abre apenas o assunto escolhido e não precisa percorrer uma única página longa.

## Regras de dependência

- A apresentação chama serviços/repositorios e formata resultados para exibição; não guarda credenciais de API.
- Clientes de fonte validam HTTPS, formato, par, limites e timestamp antes de repassar observações.
- Domínio de variação opera em observações tipadas e é reutilizado por USD, EUR e demais moedas.
- Persistência local não corrige resposta da fonte nem inventa dados ausentes.
- Integrações de atualização só aceitam Release estável do repositório fixo e APK assinado pelo mesmo certificado instalado.
- Qualquer permissão especial permanece sujeita a ação explícita da pessoa no Android.

## Refatoração incremental restante

`MainActivity` ainda constrói várias telas e conserva a coordenação de PTAX, turismo, carregamentos e callbacks. `EconomyActivity` ainda coordena downloads SGS, Focus, notícias e histórico; essas fontes não foram migradas para casos de uso nesta etapa. A camada de aplicação dos fluxos de câmbio, mercado, ações e histórico foi separada e testada, mas a aplicação inteira ainda não está migrada para Clean Architecture. Próximos passos são extrair PTAX/turismo e dados macro/notícias, depois separar componentes de apresentação sem mudar os contratos públicos.

## Distribuição externa

O workflow de Release usa dois segredos de Actions para acessar a mesma chave mantida fora do Git. Nenhuma chave, senha ou APK de build é versionado. Depois do build, o Android verifica novamente o certificado antes de permitir a instalação com confirmação explícita.
