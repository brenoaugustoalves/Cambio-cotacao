from pathlib import Path
source=(Path(__file__).parent/'test_v11_ui.py').read_text(encoding='utf8')
exec(compile(source.split("start();wait('Fonte consultada');")[0],str(Path(__file__).parent/'test_v11_ui.py'),'exec'))
results=[]
def passed(label):
    results.append(label);print('PASS',label,flush=True);(ROOT/'v112-visual-results.json').write_text(json.dumps(results,indent=2),encoding='utf8')
start();tap(find('Preferências, alertas e privacidade'));wait('Aparência: escura');passed('Update starts in dark mode')
for mode,option,appearance in [('dark','Escuro','escura'),('light','Claro','clara')]:
    tap(find('Aparência:'));tap(find(option));wait('Aparência: '+appearance);shot('v112-settings-'+mode+'.png');passed(mode+' settings visible')
    r=tree();tap(next(n for n in r.iter('node') if n.get('class')=='android.widget.Spinner'));wait('USD •');shot('v112-dropdown-'+mode+'.png');adb('shell','input','keyevent','4');passed(mode+' currency menu visible')
    r=tree();edit=next(n for n in r.iter('node') if n.get('class')=='android.widget.EditText');tap(edit);adb('shell','input','keyevent','123');adb('shell','input','keyevent','67');adb('shell','input','text','0');adb('shell','input','keyevent','4');tap(find('Salvar / rearmar alerta',True));wait('Limite inválido');shot('v112-dialog-'+mode+'.png');adb('shell','input','keyevent','4');passed(mode+' validation dialog visible')
    start();shot('v112-main-'+mode+'.png');tap(find('Diagnóstico do widget / falhas'));wait('Diagnóstico do Câmbio Claro');shot('v112-diagnostic-'+mode+'.png');passed(mode+' diagnostic visible')
    start();tap(find('Ver tabela',True));wait('Publicações do Banco Central');shot('v112-table-'+mode+'.png');adb('shell','input','keyevent','4');passed(mode+' table visible')
    r=widget();shot('v112-widget-'+mode+'.png');tap(node(r,'widget_settings'));wait('Moeda deste widget');shot('v112-widget-config-'+mode+'.png');adb('shell','input','keyevent','4');passed(mode+' widget and configuration visible')
    start();tap(find('Preferências, alertas e privacidade'));wait('Aparência: '+appearance);passed(mode+' selection persists after restart')
tap(find('Aparência:'));tap(find('Escuro'));wait('Aparência: escura');shot('v112-settings-dark.png');start();shot('v112-main-dark.png');passed('Dark mode restored for delivery')
assert not adb('logcat','-d','-b','crash').strip();passed('No crash during theme navigation')
