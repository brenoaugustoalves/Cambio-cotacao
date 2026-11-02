# Câmbio Claro

Aplicativo Android nativo para consultar cotações de moedas frente ao real, PTAX, bolsa e indicadores econômicos. O projeto mantém as fontes e horários visíveis e não cria cotações para preencher lacunas.

## Versão 1.7.9

Esta versão adiciona dois recursos:

1. **Períodos no gráfico de mercado:** `1D`, `5D`, `1M` e `1A`.
2. **Alertas configuráveis para cotação de mercado:** limite igual/acima ou igual/abaixo por moeda, separados dos alertas diários da PTAX.

Veja [notas da versão 1.7.9](RELEASE-1.7.9.md) e [resultados dos testes](TESTE-MERCADO-1.7.9.md).

## Gráfico de mercado — como funciona

- A fonte é a cotação de venda (`ask`) da AwesomeAPI, com o horário que a fonte publicou.
- Os períodos longos usam o **arquivo local de observações verdadeiras** recebido após a instalação desta versão. O app guarda até 5.000 pontos por moeda e remove pontos com mais de 365 dias.
- O histórico cresce quando o app recebe respostas de mercado ou histórico. Ele não baixa nem reconstrói preços antigos que não foram coletados pelo aparelho.
- A linha liga os pontos disponíveis para facilitar a leitura, sem interpolar ou salvar valores entre publicações. Nos períodos acima de 1 dia, o eixo termina na última observação real. No gráfico de 1 dia, o último valor pode ser mantido visualmente até o horário atual, sempre identificado como sem nova publicação.
- Se ainda não houver observações para o período, o app mostra essa condição. Para USD turismo, períodos maiores que 1 dia ficam indisponíveis até existir histórico específico; a cotação comercial não é usada como substituta.
- A atualização em primeiro plano segue os intervalos mostrados pelo app. O Android pode atrasar consultas em segundo plano por economia de bateria, ausência de rede ou políticas do fabricante.

## Alertas — como funcionam

### Alerta de mercado

Em **Preferências → Alertas de mercado**, escolha a moeda, o sentido do limite e o valor em reais por unidade. O alerta compara o limite com `ask` da AwesomeAPI. Dispara uma vez e é rearmado ao salvar novamente. O app só envia a notificação se a observação tiver até 10 minutos e as notificações estiverem habilitadas.

O sistema Agenda do Android pode postergar consultas de fundo. Portanto, o recurso é uma notificação eventual após uma consulta válida, não um feed em tempo real e não garante execução no instante em que o mercado toca o limite. A notificação identifica a fonte, o preço recebido e o horário publicado.

### Alerta PTAX

O alerta PTAX usa a taxa de venda publicada pelo Banco Central do Brasil. A PTAX é uma referência diária, não um preço de mercado contínuo. Esse alerta permanece separado do alerta de mercado e tem sua própria regra de atualização em segundo plano.

## Outras partes do aplicativo

- Moedas cotadas em reais, com cotação comercial e USD turismo separados.
- Histórico PTAX e gráfico com seleção de período.
- Variações entre publicações com preço, horário, valor absoluto e percentual.
- Widget de tela inicial para acompanhar uma moeda.
- Ações de bolsa PETR4, VALE3, ITUB4 e MGLU3; integração de Ibovespa via servidor HTTPS próprio.
- Seção econômica com indicadores, notícias brasileiras e internacionais, agenda e simulador.
- Diagnóstico local compartilhável pelo usuário; nenhum relatório é enviado automaticamente.
- Tema escuro e claro, configurações locais e acessibilidade com descrições do gráfico.

## Segurança e privacidade

- Requisições de fontes externas usam HTTPS e limites de tamanho/tempo; respostas inválidas, pares inesperados e campos ausentes não são convertidos em valores inventados.
- Preferências, alertas e histórico coletado ficam no armazenamento privado do aplicativo.
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

Os testes deste projeto cobrem parsing, cache, datas, séries reais, lacunas, seleção do gráfico, alertas, temas, componentes, widget e verificações de segurança. Consulte [TESTE-MERCADO-1.7.9.md](TESTE-MERCADO-1.7.9.md), [matriz Android 8–17](tests/ANDROID-MATRIX-8-17.md) e [testes de estresse](tests/ANDROID-STRESS-8-17.md).

## Organização do código

- `src/br/com/cambioclaro/app/`: atividades, clientes das fontes, modelos, gráficos, alertas e widget.
- `res/`: layouts, ícones, strings e estilos Android.
- `tests/`: testes instrumentados, testes de domínio e fixtures de respostas públicas.
- `server/`: proxy opcional para proteger a chave da brapi.
- `build.py`, `build_bundle.py`, `build_tests.py`, `verify.py`: compilação e verificação local.
- `RELEASE-*.md`, `TESTE-*.md`: descrição por versão e evidências de validação.

## Histórico de versões

As notas de cada versão estão em [`RELEASE-1.7.9.md`](RELEASE-1.7.9.md), [`RELEASE-1.7.8.md`](RELEASE-1.7.8.md), [`RELEASE-1.7.7.md`](RELEASE-1.7.7.md), [`RELEASE-1.7.6.md`](RELEASE-1.7.6.md), [`RELEASE-1.7.5.md`](RELEASE-1.7.5.md), [`RELEASE-1.7.4.md`](RELEASE-1.7.4.md), [`RELEASE-1.7.3.md`](RELEASE-1.7.3.md), [`RELEASE-1.7.2.md`](RELEASE-1.7.2.md), [`RELEASE-1.7.1.md`](RELEASE-1.7.1.md), [`RELEASE-1.7.0.md`](RELEASE-1.7.0.md), [`RELEASE-1.6.2.md`](RELEASE-1.6.2.md), [`RELEASE-1.6.1.md`](RELEASE-1.6.1.md), [`RELEASE-1.6.0.md`](RELEASE-1.6.0.md), [`RELEASE-1.5.0.md`](RELEASE-1.5.0.md), [`RELEASE-1.4.0.md`](RELEASE-1.4.0.md), [`RELEASE-1.3.1.md`](RELEASE-1.3.1.md) e [`RELEASE-1.3.0.md`](RELEASE-1.3.0.md).

Este projeto ainda não foi publicado na Google Play.
