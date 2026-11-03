# Câmbio Claro 1.8.0 — armazenamento local do histórico

- Gráfico `1D` usa observações intradiárias reais; `5D`, `1M` e `1A` consultam fechamentos diários reais da AwesomeAPI, com datas, horários, valores e períodos provenientes da fonte.
- Períodos usam tabelas SQLite separadas por moeda e frequência. A linha conecta apenas pontos reais; segmentos visuais não criam nem salvam cotações intermediárias.
- Variações de mercado são exibidas para cada moeda quando existem duas publicações reais para comparar. Sem base, a UI informa indisponibilidade.
- Menu da tela inicial separa Moedas, Bolsa, Economia e Fontes para reduzir rolagem e agrupar assuntos.
- A área Moedas tem submenus Cotação, Gráfico, PTAX, Converter e Outras; cada assunto abre sozinho para encurtar a página.
- O conversor permite selecionar a origem entre as moedas suportadas e identifica BRL como destino. Distingue USD comercial, USD turismo, cotação de mercado e fallback PTAX salvo.
- A navegação principal passou para uma barra inferior fixa; controles têm alvos de toque ampliados, e a cotação sinaliza estado recente, atrasado ou indisponível com texto e cor.
- No gráfico, a faixa de seleção reúne horário, direção, variação absoluta e percentual, e cotação do ponto observado.
- Checagem de Releases GitHub a cada aproximadamente 12 horas via JobScheduler, com aviso local, comparação de versão, limite de tamanho, SHA-256 quando fornecido pelo GitHub, conferência do pacote/certificado e instalação com confirmação do Android.
- Workflow de tag cria Releases assinadas; a chave é lida apenas dos segredos privados do GitHub Actions e nunca do repositório.
- Validação desta etapa: `python verify.py` passou (29 testes de domínio); `python verify.py --emulator emulator-5556` passou (308 verificações Android na API 34; carga local de parsers em 1.881 ms).
- Substitui o JSON único em `SharedPreferences` por SQLite (`MarketArchiveDb`) com tabela por observação e chave primária `(moeda, horário em ms)`.
- Gravação transacional, consultas por moeda e intervalo com índice e deduplicação pelo timestamp do fornecedor.
- Retenção de até 5.000 observações por moeda e 365 dias; pontos fora da janela e timestamps futuros são descartados.
- Migra automaticamente os arquivos de histórico já existentes em aparelhos 1.7.x e incorpora as observações PTAX de mercado de hoje sem recalcular preços.
- O armazenamento permanece privado no aparelho. O banco não consulta fonte, não gera cotação e não preenche lacunas.
- Versionamento: `versionCode=23`, `versionName=1.8.0`.

O mecanismo usa `SQLiteOpenHelper` nativo porque o projeto atual compila sem Gradle e sem bibliotecas externas. A camada `MarketArchive` isola acesso ao repositório para permitir trocar a implementação de persistência no futuro sem alterar o gráfico.

Antes de liberar uma atualização, o instalador do Android exige autorização para instalar apps desconhecidos e confirmação da pessoa. Android pode adiar verificações periódicas para reduzir consumo de bateria. Consulte [o fluxo de Releases e os segredos obrigatórios](README.md#atualizações-pelo-github-antes-da-play-store) e a [arquitetura aplicada](ARCHITECTURE.md).
