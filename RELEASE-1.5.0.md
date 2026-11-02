# Câmbio Claro 1.5.0 — 30/10/2026

## Atualização e variação

- No cartão do câmbio, a variação de mercado agora explicita a base: mudança desde a publicação imediatamente anterior e mudança desde a primeira observação real recebida hoje. A segunda comparação não é chamada de abertura oficial, porque a fonte não fornece essa referência como base deste gráfico.
- O widget mantém o valor arredondado e a taxa exata em linhas separadas. A variação da PTAX identifica que compara publicações do Banco Central; a do mercado identifica publicações da fonte de mercado.
- Mercado com última publicação há dez minutos ou mais aparece como desatualizado. Ações e Ibovespa marcados como antigos quando a última observação tem mais de 45 minutos. O limite é um aviso visual, não uma garantia sobre a periodicidade da fonte.
- Atualizações continuam sendo tentadas em todos os dias. O texto explica que buscar novamente não cria uma publicação que a fonte não forneceu. Fim de semana, feriado, fechamento do mercado, rede e limites do provedor podem manter o último preço. O Android também pode adiar tarefas em segundo plano para poupar bateria.

## Widget e diagnóstico

- O toque de atualização do widget substitui um agendamento pendente por um novo pedido, sujeito a conexão e execução do Android, com intervalo mínimo de 30 segundos para evitar pedidos repetidos.
- O diagnóstico local passa a registrar o horário e tipo do último toque na cotação, engrenagem ou atualização. Esse dado fica privado no aplicativo, é incluído somente quando o usuário abre/copiar o diagnóstico e não é enviado automaticamente.
- Se um toque ainda falhar em um aparelho físico, o relatório ajuda a relacionar a ação ao erro gravado. A interação do widget foi exercitada em emuladores; a conexão ADB deste computador mostrou apenas dois emuladores, sem o Motorola enumerado.

## Bolsa e fontes

- Cotação de ações mantém o horário publicado pela brapi e recebe aviso de dado antigo após 45 minutos. Esse marcador serve para chamar atenção; o plano gratuito pode atrasar os valores.
- Ações continuam usando consulta pública. Ibovespa e futuros seguem sem preço até existir servidor HTTPS e confirmação de cobertura e autorização de exibição. Nenhum valor é estimado ou preenchido artificialmente.
- Nenhuma chave de fornecedor é incluída no APK. `BRAPI_TOKEN` permanece apenas no ambiente do servidor se/quando o gateway for implantado.

## Testes e instalação

- `versionCode=9`, `versionName=1.5.0`, mesma assinatura das versões anteriores; instalação por cima deve preservar os dados.
- Testes automatizados de widget, atraso de cotação, base do percentual, diagnóstico, rede e segurança rodaram em emuladores Android 17 e Android 14. A validação no Motorola físico ainda requer que o dispositivo apareça em `adb devices` com estado `device` e depuração USB autorizada.
- Esta versão é local. Nada foi publicado na Google Play.
