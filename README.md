# Câmbio Claro

Aplicativo Android nativo para consultar cotações de moedas frente ao real, PTAX, bolsa e indicadores econômicos. O projeto mantém as fontes e horários visíveis e não cria cotações para preencher lacunas.

## Versão 1.8.0

Esta versão separa os fechamentos diários dos pontos intradiários em SQLite, migra o histórico anterior sem recalcular preços e organiza a tela principal em seções curtas.

O histórico fica organizado por moeda e horário, com gravação transacional, deduplicação pela chave `(moeda, horário)`, consulta indexada por período e retenção limitada a 5.000 pontos e 365 dias. A atualização preserva os pontos JSON existentes na primeira abertura após a instalação.

No gráfico, `1D` consulta cotações intradiárias; `5D`, `1M` e `1A` consultam fechamentos diários da AwesomeAPI. Cada par tem sua própria série. Pontos intradiários e diários não se misturam. A tela comparativa mostra variação de cada moeda quando há duas publicações reais para comparar. O limite de 360 registros diários segue a [documentação da API de moedas AwesomeAPI](https://docs.awesomeapi.com.br/api-de-moedas). Veja [notas da versão 1.8.0](RELEASE-1.8.0.md) e [resultados dos testes](TESTE-ARMAZENAMENTO-1.8.0.md).

## Gráfico de mercado — como funciona

- A fonte é a cotação de venda (`ask`) da AwesomeAPI, com o horário que a fonte publicou.
- `1D` usa observações intradiárias da fonte e o arquivo local de hoje. `5D`, `1M` e `1A` buscam os fechamentos diários da fonte e mantêm uma cópia SQLite por moeda. A consulta anual respeita o limite documentado de até 360 dias da fonte.
- O histórico cresce quando o app recebe respostas de mercado ou histórico. Ele não baixa nem reconstrói preços antigos que não foram coletados pelo aparelho.
- A linha contínua conecta pontos consecutivos retornados pela fonte, em qualquer período. Cada vértice e seu horário são reais; o trecho reto é só um conector visual e não fornece cotações intermediárias. O app não salva nem mostra preço para horas sem publicação e não estende o último preço após a última observação.
- A faixa de seleção reúne horário, direção, variação absoluta, percentual e cotação observada. Os valores mudam ao tocar ou arrastar sobre o gráfico.
- Se ainda não houver observações para o período, o app mostra essa condição. USD turismo não usa o histórico comercial como substituto; períodos longos aparecem indisponíveis enquanto a fonte não fornecer uma série diária de turismo.
- A atualização em primeiro plano segue os intervalos mostrados pelo app. O Android pode atrasar consultas em segundo plano por economia de bateria, ausência de rede ou políticas do fabricante.
- Para moedas, a tela comparativa calcula cada variação a partir das publicações reais daquele par. Sem duas observações do dia, mostra “variação indisponível” em vez de estimar um movimento.

## Navegação da tela principal

O menu inferior separa **Moedas**, **Bolsa**, **Economia** e **Fontes** e permanece acessível durante a rolagem. Dentro de Moedas, os atalhos **Cotação**, **Gráfico**, **PTAX**, **Converter** e **Outras** mostram apenas o assunto escolhido. Ações/derivativos, conteúdo econômico e fontes ficam em seus próprios destinos. Preferências e Diagnóstico permanecem acessíveis no topo.

## Atualizações pelo GitHub antes da Play Store

O app consulta Releases estáveis do repositório, verifica se a versão é mais recente e, quando permitido, avisa no Android. A checagem automática usa rede disponível e o agendador do sistema em intervalos aproximados de 12 horas; Android e fabricantes podem atrasar a execução. A pessoa abre **Preferências → Atualizações pelo GitHub**, baixa o APK e confirma a instalação na tela do Android. O APK é recusado se pacote, versão, tamanho ou certificado de assinatura não corresponderem.

Uma tag `vX.Y.Z` inicia o workflow [release-apk.yml](.github/workflows/release-apk.yml), que valida a versão, compila, executa testes de domínio e publica `CambioClaro-X.Y.Z.apk` numa Release. Para habilitar a assinatura automática:

1. No GitHub, abra **Settings → Secrets and variables → Actions**.
2. Crie `CAMBIO_SIGNING_KEYSTORE_BASE64` com o Base64 do arquivo local `.signing/cambio-claro.jks` e `CAMBIO_SIGNING_PASSWORD` com o conteúdo de `.signing/password.txt`.
3. Em cada versão, incremente `versionCode` e `versionName` no manifesto, atualize as notas/testes da versão e envie uma tag correspondente, como `v1.8.1`.

No PowerShell local, gere o valor Base64 para colar diretamente no campo do segredo; não o coloque em arquivo do repositório nem nesta conversa:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes('.signing/cambio-claro.jks'))
```

Guarde cópias seguras do keystore e da senha. Sem a mesma chave, os aparelhos que já instalaram o app não aceitarão a atualização. O fluxo sideload do GitHub é para distribuição fora da Play Store; antes de publicar na loja será necessário rever/remover a permissão e a interface de instalação externa.

## Alertas — como funcionam

### Alerta de mercado

Em **Preferências → Alertas de mercado**, escolha a moeda, o sentido do limite e o valor em reais por unidade. O alerta compara o limite com `ask` da AwesomeAPI. Dispara uma vez e é rearmado ao salvar novamente. O app só envia a notificação se a observação tiver até 10 minutos e as notificações estiverem habilitadas.

O sistema Agenda do Android pode postergar consultas de fundo. Portanto, o recurso é uma notificação eventual após uma consulta válida, não um feed em tempo real e não garante execução no instante em que o mercado toca o limite. A notificação identifica a fonte, o preço recebido e o horário publicado.

### Alerta PTAX

O alerta PTAX usa a taxa de venda publicada pelo Banco Central do Brasil. A PTAX é uma referência diária, não um preço de mercado contínuo. Esse alerta permanece separado do alerta de mercado e tem sua própria regra de atualização em segundo plano.

## Outras partes do aplicativo

- Moedas cotadas em reais, com cotação comercial e USD turismo separados.
- Conversor com origem selecionável entre as moedas suportadas e destino BRL explícito; USD turismo permanece separado, e a tela identifica se usou mercado (ask) ou PTAX venda salva.
- Histórico PTAX e gráfico com seleção de período.
- Variações entre publicações com preço, horário, valor absoluto e percentual.
- Widget de tela inicial para acompanhar uma moeda.
- Ações de bolsa PETR4, VALE3, ITUB4 e MGLU3; integração de Ibovespa via servidor HTTPS próprio.
- Seção econômica com indicadores, notícias brasileiras e internacionais, agenda e simulador.
- Diagnóstico local compartilhável pelo usuário; nenhum relatório é enviado automaticamente.
- Tema escuro e claro, configurações locais e acessibilidade com descrições do gráfico.

## Segurança e privacidade

- Requisições de fontes externas usam HTTPS e limites de tamanho/tempo; respostas inválidas, pares inesperados e campos ausentes não são convertidos em valores inventados.
- Preferências e alertas ficam no armazenamento privado do aplicativo; o histórico é mantido no banco SQLite privado do aparelho.
- A chave brapi não deve ser colocada no app. Configure `BRAPI_TOKEN` somente no servidor descrito em [`server/`](server/README.md), se usar o Ibovespa.
- Os relatórios de teste, prints de tela e resultados locais servem para revisão. Não inclua o keystore `.signing/`, APKs ou tokens no Git.

## Compilar e testar

Requisitos: Python 3, JDK 21 e Android SDK (build-tools 36.0.0 e plataforma Android 37). O app define `minSdkVersion 26`.

```powershell
python build.py
python verify.py
python verify.py --emulator emulator-5556
```

`verify.py --emulator` instala o app e testes num emulador descartável e desativa/restaura rede durante os casos offline. Não use como destino um celular com dados importantes. Para executar um conjunto de testes em outra API, inicialize um AVD dessa API e informe o serial do emulador.

Os testes deste projeto cobrem parsing, cache, datas, séries reais, lacunas, seleção do gráfico, migração SQLite, retenção, alertas, temas, componentes, widget e verificações de segurança. Consulte [TESTE-ARMAZENAMENTO-1.8.0.md](TESTE-ARMAZENAMENTO-1.8.0.md), [matriz Android 8–17](tests/ANDROID-MATRIX-8-17.md) e [testes de estresse](tests/ANDROID-STRESS-8-17.md).

## Organização do código

- `src/br/com/cambioclaro/app/`: apresentação Android, integrações atuais, domínio, gráficos, alertas e widgets.
- `src/br/com/cambioclaro/app/conversion/`: caso de uso de conversão independente do Android, contrato de leitura de cotações e adaptador para as fontes/cache existentes.
- `src/br/com/cambioclaro/app/market/application/`: política de atualização do mercado testável sem Android.
- `src/br/com/cambioclaro/app/updates/`: metadados de Release, cliente GitHub, verificação/instalação de APK e notificação, divididos por responsabilidade.
- `MarketArchive.java` / `MarketArchiveDb.java`: fachada de repositório, migração assíncrona e tabelas SQLite separadas por série.
- `res/`: layouts, ícones, strings e estilos Android.
- `tests/`: testes instrumentados, testes de domínio e fixtures de respostas públicas.
- `server/`: proxy opcional para proteger a chave da brapi.
- `build.py`, `build_bundle.py`, `build_tests.py`, `verify.py`: compilação e verificação local.
- `RELEASE-*.md`, `TESTE-*.md`: descrição por versão e evidências de validação.
- `ARCHITECTURE.md`: limites de camadas aplicados e refatorações incrementais planejadas.
- `TESTE-ARQUITETURA-1.8.0.md`: escopo, critérios de aceite e evidência da primeira extração do conversor.
- `TESTE-ARQUITETURA-2.md`: critérios e evidências da política de atualização e economia de dados/bateria.

## Histórico de versões

As notas de cada versão estão em [`RELEASE-1.8.0.md`](RELEASE-1.8.0.md), [`RELEASE-1.7.9.md`](RELEASE-1.7.9.md), [`RELEASE-1.7.8.md`](RELEASE-1.7.8.md), [`RELEASE-1.7.7.md`](RELEASE-1.7.7.md), [`RELEASE-1.7.6.md`](RELEASE-1.7.6.md), [`RELEASE-1.7.5.md`](RELEASE-1.7.5.md), [`RELEASE-1.7.4.md`](RELEASE-1.7.4.md), [`RELEASE-1.7.3.md`](RELEASE-1.7.3.md), [`RELEASE-1.7.2.md`](RELEASE-1.7.2.md), [`RELEASE-1.7.1.md`](RELEASE-1.7.1.md), [`RELEASE-1.7.0.md`](RELEASE-1.7.0.md), [`RELEASE-1.6.2.md`](RELEASE-1.6.2.md), [`RELEASE-1.6.1.md`](RELEASE-1.6.1.md), [`RELEASE-1.6.0.md`](RELEASE-1.6.0.md), [`RELEASE-1.5.0.md`](RELEASE-1.5.0.md), [`RELEASE-1.4.0.md`](RELEASE-1.4.0.md), [`RELEASE-1.3.1.md`](RELEASE-1.3.1.md) e [`RELEASE-1.3.0.md`](RELEASE-1.3.0.md).

Este projeto ainda não foi publicado na Google Play.
