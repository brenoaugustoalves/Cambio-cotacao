from pathlib import Path
source=(Path(__file__).parent/'test_v11_ui.py').read_text(encoding='utf8')
exec(compile(source.split("start();wait('Fonte consultada');")[0],str(Path(__file__).parent/'test_v11_ui.py'),'exec'))
if not any('CONTROLLED_WIDGET_DIAGNOSTIC_TEST' in s for s in texts(tree())):
    start();wait('Moedas em reais');tap(find('Diagnóstico do widget / falhas'))
wait('CONTROLLED_WIDGET_DIAGNOSTIC_TEST');shot('v111-captured-crash.png')
report='Actual controlled uncaught exception was captured locally and displayed after process restart.\n'
tap(find('COPIAR DIAGNÓSTICO'));wait('Diagnóstico do Câmbio Claro');report+='Copy button is operable. No automatic upload.\n'
tap(find('APAGAR DIAGNÓSTICO LOCAL'));wait('Nenhuma falha registrada');report+='User clear removes the local report.\n'
crash=adb('logcat','-d','-b','crash');assert 'CONTROLLED_WIDGET_DIAGNOSTIC_TEST' in crash
(ROOT/'controlled-crash-v111.log').write_text(crash,encoding='utf8')
adb('logcat','-b','crash','-c')
(ROOT/'diagnostic-probe-result.txt').write_text(report,encoding='utf8');print(report,flush=True)
