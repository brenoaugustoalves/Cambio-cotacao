# Câmbio Claro 1.6.0 — refinamento visual Android

Esta versão reorganiza a tela principal para facilitar a leitura em celulares: preferências e diagnóstico ocupam menos espaço, cartões têm espaçamento mais contido, o valor de mercado permanece em destaque e a lista de moedas usa linhas compactas com cotação alinhada à direita. A seção de moedas mostra quatro itens inicialmente, coloca favoritas primeiro e expande o restante por “Ver todas”; cada cotação fica em uma linha simples, sem cartões internos. A cotação principal usa precisão visual consistente, com a taxa de referência detalhada mantida junto à fonte. O gráfico intradiário foi reduzido um pouco para equilibrar a tela sem retirar escala, horários ou interação; lacunas entre publicações aparecem com traço interrompido para não parecer uma série contínua observada. Variações exibidas como 0,00% são classificadas como estáveis, evitando indicar alta ou baixa por arredondamento.

PTAX, mercado, dólar turismo, bolsa e futuros continuam em áreas claramente identificadas. Horários, avisos de atraso, indisponibilidade e origem dos dados permanecem visíveis. Não foram adicionadas cotações estimadas ou dados novos. O tema escuro/claro e o widget foram preservados.

- `versionCode=10`, `versionName=1.6.0`; assinatura local existente preservada para atualizar a instalação anterior.
- Compatibilidade declarada: Android 8.0/API 26 ou superior; `targetSdkVersion=37`.
- Distribuição local para teste. Nenhum envio à Google Play.
- Arquivo de instalação: `CambioClaro-1.6.0.apk`.

## Verificação

Executar `python verify.py --emulator emulator-5554 --bundle` compila o APK/AAB, executa os testes Java e de instrumentação e instala no emulador de teste. O comando não publica o app. A captura de tela da versão compilada acompanha os artefatos desta entrega.

