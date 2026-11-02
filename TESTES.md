# Verificação do Câmbio Claro 1.1.0

Data: 30/10/2026. Android 17/API 37, emulador Pixel 8a x86_64. APK de produção universal gerado a partir do AAB, versionCode 2, pacote br.com.cambioclaro.app. Sem publicação na Google Play.

## Resultados

- 21 verificações Java de cálculos, arredondamento, datas e entradas inválidas: passaram.
- 125 verificações de instrumentação Android: passaram no APK final. Cobrem as dez moedas, parser, cache, segurança, widget, favoritos, comparação, alertas e preferências.
- 11 cenários de navegação e aparência: passaram. Incluem favorito persistente, comparação com base comum, temas, rejeição de limite zero, privacidade, widget independente e fonte de 200%.
- 5 verificações adicionais no APK final: passaram, incluindo layout com fonte ampliada após o último ajuste, consulta online, widget offline, retomada automática após reconexão e ausência de registros de crash.
- Assinaturas verificadas, AAB validado pelo bundletool e instalação por atualização bem-sucedida. Certificado SHA-256 idêntico ao da versão 1.0.0: 8a2f61c2f21d60f45adc2144a4233deb7816e1a6b77e4d2386aed69ccd0c4db8.
- Backup DPAPI: descriptografia em memória e restauração dos arquivos em pasta temporária privada verificadas por comparação de hashes. Os arquivos temporários restaurados foram removidos.

Os 11 cenários de navegação foram executados antes do últimos ajustes de layout para fontes grandes, indicação de consulta em todos os dias e repetição após falhas transitórias; os 125 testes Android, 21 testes Java e 5 verificações finais ocorreram depois desse ajuste.

## Casos importantes

Notificação bloqueada não consome o alerta. Um limite satisfeito gera uma notificação com a data real da cotação. Consultas repetidas não duplicam o aviso. Desativar o último alerta cancela o agendamento periódico. São verificados limites inclusivos e rejeição de zero, negativos, notação exponencial, separador ambíguo e precisão excessiva.

Widgets distintos mantêm USD e EUR independentemente da moeda do app. A atualização de um widget para cache vazio limpa gráfico e variação antigos. A comparação utiliza a interseção de datas, com base percentual na mesma data. O cache offline validado permanece disponível após a virada do dia. Requisições de sábado e domingo mantêm a data solicitada, sem retroceder artificialmente a um dia útil.

A instrumentação usa fixtures reais preservados do BCB e rede temporariamente desligada para evitar que uma consulta de produção substitua os fixtures no meio do teste. Ela restaura os caches e preferências ao terminar. Os fixtures não são empacotados no APK. A interface foi exercitada também com consultas reais de rede; telas e imagens representam os dados recebidos na data de execução.

O automatizador de interface encontrou indisponibilidades transitórias da árvore de acessibilidade durante recriações de tela e repetiu as leituras. Isso não produziu registros de crash do aplicativo. Os resultados efetivos constam dos arquivos de evidência.

## Limites

Não houve celular físico conectado, auditoria independente de segurança, teste de todos os fabricantes/launchers, auditoria completa com TalkBack ou ensaio prolongado de bateria/Doze. O agendamento persistente e o intervalo foram inspecionados; não foi feito ensaio real de reboot com alerta ativo. O Android pode adiar consultas, e a PTAX é diária. Os testes não garantem ausência de todas as falhas.

As evidências e os scripts ficam em tests/evidence-v1.1 no ZIP. O comando verify.py executa o conjunto automatizado de compilação, cálculos, assinatura e instrumentação. Os testes de UI usam ADB e exigem um widget já adicionado; não são executados automaticamente pelo verify.py.
