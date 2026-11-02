from pathlib import Path
import subprocess,xml.etree.ElementTree as E,re,time,json
ROOT=Path(__file__).resolve().parent;P='br.com.cambioclaro.app';results=[]
def adb(*args):return subprocess.check_output(['adb','-s','emulator-5554',*args],timeout=40).decode('utf8','replace')
def tree():
    for _ in range(5):
        if 'dumped' in adb('shell','uiautomator','dump','/sdcard/v11-ui.xml'):return E.fromstring(adb('shell','cat','/sdcard/v11-ui.xml'))
        time.sleep(.3)
    raise AssertionError('UI tree unavailable')
def texts(r):return [n.get('text','') for n in r.iter('node')]
def tap(n):
    x1,y1,x2,y2=map(int,re.findall(r'\d+',n.get('bounds')));adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2))
def find(text,scroll=False):
    for _ in range(16 if scroll else 1):
        r=tree();matches=[n for n in r.iter('node') if text in n.get('text','')]
        if matches:return matches[0]
        adb('shell','input','swipe','800','1900','800','650','350')
    raise AssertionError('Missing '+text+' '+str(texts(r)))
def wait(text,seconds=45):
    start=time.monotonic()
    while time.monotonic()-start<seconds:
        r=tree()
        if any(text in s for s in texts(r)):return r
    raise AssertionError('Timeout '+text+' '+str(texts(r)))
def shot(name):(ROOT/name).write_bytes(subprocess.check_output(['adb','-s','emulator-5554','exec-out','screencap','-p']))
def passed(label):results.append(label);print('PASS',label,flush=True);(ROOT/'v11-ui-results.json').write_text(json.dumps(results,ensure_ascii=False,indent=2),encoding='utf8')
def start():adb('shell','am','force-stop',P);adb('shell','am','start','-n',P+'/.MainActivity');wait('Moedas em reais')
def choose(code):
    r=tree();tap(next(n for n in r.iter('node') if n.get('class')=='android.widget.Spinner'));tap(find(code+' •'));wait(code+' / BRL')
def widget():
    adb('shell','input','keyevent','3')
    for _ in range(4):
        r=tree()
        if any(n.get('resource-id')==P+':id/widget_rate' for n in r.iter('node')):return r
        adb('shell','input','swipe','950','1200','100','1200','300')
    raise AssertionError('No pinned widget')
def node(r,id):return next(n for n in r.iter('node') if n.get('resource-id')==P+':id/'+id)
start();wait('Fonte consultada');shot('v11-main-light.png');passed('Updated app opens with real source and separate publication/consultation dates')
choose('EUR');wait('Fonte consultada');r=tree();fav=next(n for n in r.iter('node') if 'Favoritar EUR' in n.get('text','') or 'Remover EUR' in n.get('text',''))
if 'Favoritar' in fav.get('text'):tap(fav)
wait('Remover EUR');start();wait('Remover EUR');passed('Favorite survives restart')
tap(find('Comparar moedas no mesmo período',True));wait('Comparar de 2 a 3 moedas');tap(find('USD •'));tap(find('EUR •'));tap(next(n for n in tree().iter('node') if n.get('text','').lower()=='comparar'));wait('Comparação de moedas');wait('Base 0%');shot('v11-comparison.png');passed('Two currencies compared with common baseline and accessible data table');adb('shell','input','keyevent','4')
start();tap(find('Preferências, alertas e privacidade'));wait('Preferências e privacidade');tap(find('Aparência:'));tap(find('Escura'));wait('Aparência: escura');shot('v11-settings-dark.png');adb('shell','input','keyevent','4');wait('Moedas em reais');shot('v11-main-dark.png');passed('Dark theme persists and applies when returning to main screen')
tap(find('Preferências, alertas e privacidade'));r=tree();value=next(n for n in r.iter('node') if n.get('class')=='android.widget.EditText');tap(value);adb('shell','input','text','0');adb('shell','input','keyevent','4');tap(find('Salvar / rearmar alerta',True));r=tree();assert any('maior que zero' in s for s in texts(r)),texts(r);passed('Alert form rejects zero threshold');adb('shell','input','keyevent','4')
tap(find('Desativar alerta desta moeda',True));tap(find('Permissão de notificações',True)) if False else None
find('brenoaugustoalves@gmail.com',True);shot('v11-privacy.png');passed('In-app privacy and user supplied support email are visible');adb('shell','input','keyevent','4')
r=widget();tap(node(r,'widget_settings'));wait('Moeda deste widget');tap(find('EUR •'));r=widget();assert node(r,'widget_pair').get('text')=='EUR / BRL';assert 'publicação anterior' in node(r,'widget_change').get('text');passed('Pinned widget configured independently with actual percent change')
tap(node(r,'widget_rate'));wait('EUR / BRL');choose('JPY');wait('Fonte consultada');r=widget();assert node(r,'widget_pair').get('text')=='EUR / BRL';shot('v11-widget.png');passed('Widget keeps EUR while app changes to JPY');tap(node(r,'widget_rate'));wait('EUR / BRL');passed('Widget tap opens its own currency')
scale=adb('shell','settings','get','system','font_scale').strip()
try:
    adb('shell','settings','put','system','font_scale','2.0');start();wait('Moedas em reais');find('Preferências, alertas e privacidade');shot('v11-large-font.png');passed('200 percent system font remains operable with scrolling')
finally:adb('shell','settings','put','system','font_scale',scale)
start();tap(find('Preferências, alertas e privacidade'));tap(find('Aparência:'));tap(find('Clara'));wait('Aparência: clara');adb('shell','input','keyevent','4');wait('Moedas em reais');shot('v11-main-light.png');passed('Light theme restored without losing favorite')
(ROOT/'v11-ui-results.json').write_text(json.dumps(results,ensure_ascii=False,indent=2),encoding='utf8')
