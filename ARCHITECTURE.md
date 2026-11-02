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

O próximo limite de maior valor é reduzir `MainActivity`, que ainda constrói muitas telas e coordena consultas de moedas, ações, derivativos e PTAX. Essa extração deve ser feita por assunto, mantendo testes instrumentados de navegação, cache e origem dos dados a cada passo. Uma migração geral de pacote/módulo agora aumentaria o risco sem melhorar a experiência imediata.

## Distribuição externa

O workflow de Release usa dois segredos de Actions para acessar a mesma chave mantida fora do Git. Nenhuma chave, senha ou APK de build é versionado. Depois do build, o Android verifica novamente o certificado antes de permitir a instalação com confirmação explícita.
