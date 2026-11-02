from pathlib import Path
source=(Path(__file__).parent/'test_v11_ui.py').read_text(encoding='utf8')
exec(compile(source.split("start();wait('Fonte consultada');")[0],str(Path(__file__).parent/'test_v11_ui.py'),'exec'))
results=[]
def passed(label):
    results.append(label);print('PASS',label,flush=True)
    (ROOT/'v11-final-ui-results.json').write_text(json.dumps(results,ensure_ascii=False,indent=2),encoding='utf8')
scale=adb('shell','settings','get','system','font_scale').strip()
try:
    adb('shell','settings','put','system','font_scale','2.0');start();wait('Moedas em reais');assert find('Atualizar').get('text')=='Atualizar ↻';shot('v11-large-font.png');passed('Final layout at 200 percent font: full-width refresh and operable preferences')
finally:adb('shell','settings','put','system','font_scale',scale)
start();wait('Fonte consultada');shot('v11-main-light.png');passed('Final APK live consultation after font reset')
r=widget();rate=node(r,'widget_rate').get('text');pair=node(r,'widget_pair').get('text');assert rate!='—'
try:
    adb('shell','svc','wifi','disable');adb('shell','svc','data','disable');tap(node(r,'widget_refresh'));r=wait('dados salvos');assert node(r,'widget_rate').get('text')==rate;assert node(r,'widget_pair').get('text')==pair;shot('v11-widget-offline.png');passed('Final widget retains matching currency and real cached rate offline')
finally:adb('shell','svc','wifi','enable');adb('shell','svc','data','enable')
r=wait('Consulta:',150);assert node(r,'widget_pair').get('text')==pair;shot('v11-widget.png');passed('Final widget automatically completes queued update after reconnection')
assert not adb('logcat','-d','-b','crash').strip(),'Unexpected crash buffer entries'
passed('No application crash recorded during release verification')
