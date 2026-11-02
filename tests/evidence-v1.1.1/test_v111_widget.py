from pathlib import Path
source=(Path(__file__).parent/'test_v11_ui.py').read_text(encoding='utf8')
exec(compile(source.split("start();wait('Fonte consultada');")[0],str(Path(__file__).parent/'test_v11_ui.py'),'exec'))
results=[]
def passed(label):
    results.append(label);print('PASS',label,flush=True)
    (ROOT/'v111-widget-results.json').write_text(json.dumps(results,ensure_ascii=False,indent=2),encoding='utf8')
start();wait('Moedas em reais');r=widget()
for cold in [False,True,True]:
    r=widget()
    if cold:adb('shell','am','kill',P)
    tap(node(r,'widget_rate'));wait('Moedas em reais');passed(('Cold' if cold else 'Warm')+' widget quote opens app')
    r=widget()
    if cold:adb('shell','am','kill',P)
    tap(node(r,'widget_settings'));wait('Moeda deste widget');tap(find('EUR •'));r=widget();assert node(r,'widget_pair').get('text')=='EUR / BRL';passed(('Cold' if cold else 'Warm')+' widget gear configures EUR')
start();tap(find('Diagnóstico do widget / falhas'));wait('Diagnóstico do Câmbio Claro');wait('Nenhuma falha registrada');shot('v111-diagnostic.png');passed('Private diagnostic screen opens and shows no invented crash')
assert not adb('logcat','-d','-b','crash').strip();passed('No crash in widget cold/warm navigation on emulator')
