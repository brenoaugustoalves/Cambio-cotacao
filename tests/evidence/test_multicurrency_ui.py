from pathlib import Path
import subprocess,xml.etree.ElementTree as E,re,time,json,sys
sys.stdout.reconfigure(encoding='utf8');ROOT=Path(__file__).resolve().parent;ADB=str(Path.home()/'AppData/Local/Android/Sdk/platform-tools/adb.exe');P='br.com.cambioclaro.app'
def adb(*a):return subprocess.run([ADB,'-s','emulator-5554',*a],capture_output=True,check=True).stdout.decode('utf8','replace')
def tree():adb('shell','uiautomator','dump','/sdcard/multi-test.xml');return E.fromstring(adb('shell','cat','/sdcard/multi-test.xml'))
def texts(r):return [n.get('text','') for n in r.iter('node') if n.get('text')]
def tap(n):
    b=list(map(int,re.findall(r'\d+',n.get('bounds'))));adb('shell','input','tap',str((b[0]+b[2])//2),str((b[1]+b[3])//2))
def wait(test,limit=35):
    start=time.time()
    while time.time()-start<limit:
        r=tree()
        if test(texts(r)):return r
    raise AssertionError('Timeout: '+str(texts(r)))
def choose(code):
    r=tree();tap(next(n for n in r.iter('node') if n.get('class')=='android.widget.Spinner'));r=tree();tap(next(n for n in r.iter('node') if n.get('text','').startswith(code+' •') and '▾' not in n.get('text','')))
def shot(name):(ROOT/name).write_bytes(subprocess.run([ADB,'-s','emulator-5554','exec-out','screencap','-p'],capture_output=True,check=True).stdout)
results=[]
def passed(s):results.append(s);print('PASS',s,flush=True)
adb('shell','am','force-stop',P);adb('shell','am','start','-n',P+'/.MainActivity');wait(lambda t:any('Fonte consultada' in s for s in t))
expected={'USD':'5,3420','EUR':'5,9515','GBP':'7,1059','JPY':'0,03377','CAD':'3,8059','AUD':'3,7592','CHF':'6,4377','DKK':'0,7962','NOK':'0,5765','SEK':'0,5383'}
for code,value in expected.items():
    choose(code);r=wait(lambda t:'R$ '+value in t and any('Fonte consultada' in s for s in t));assert any(code+' / BRL' in s for s in texts(r));passed(code+' exact BCB quote, currency label and graph selection')
    if code=='JPY':shot('cambio-jpy.png')
choose('EUR');r=wait(lambda t:'R$ 5,9515' in t);shot('cambio-eur.png')
# Return to USD for the offline and conversion scenarios.
choose('USD');wait(lambda t:'R$ 5,3420' in t and any('Fonte consultada' in s for s in t));shot('cambio-final-app.png')
for _ in range(7):
    r=tree();ed=next((n for n in r.iter('node') if n.get('class')=='android.widget.EditText'),None)
    if ed is not None:break
    adb('shell','input','swipe','520','2100','520','850','350')
assert ed is not None;tap(ed);adb('shell','input','keyevent','123')
for _ in range(15):adb('shell','input','keyevent','67')
adb('shell','input','text','100');adb('shell','input','keyevent','4');r=tree();assert 'R$ 534,20' in texts(r);passed('Converter remains exact after repeated currency switches');shot('cambio-converter.png')
for _ in range(5):
    r=tree()
    if any(s.startswith('▲ Alta') or s.startswith('▼ Baixa') or s.startswith('● Estável') for s in texts(r)):break
    adb('shell','input','swipe','520','2000','520','900','350')
assert any(s.startswith('▲ Alta') or s.startswith('▼ Baixa') or s.startswith('● Estável') for s in texts(r));shot('cambio-moedas-painel.png');passed('Multi-currency panel displays actual publication-to-publication changes')
# Reset viewport without changing caches.
adb('shell','am','force-stop',P);adb('shell','am','start','-n',P+'/.MainActivity');wait(lambda t:any('Fonte consultada' in s for s in t))
try:
    adb('shell','svc','wifi','disable');adb('shell','svc','data','disable');choose('EUR')
    r=wait(lambda t:any('Dados salvos' in s for s in t),50);assert 'R$ 5,9515' in texts(r) and 'R$ 5,3420' not in texts(r);passed('Offline EUR uses EUR cache, never USD data');shot('cambio-offline-eur.png')
finally:adb('shell','svc','wifi','enable');adb('shell','svc','data','enable')
choose('USD');wait(lambda t:any('Fonte consultada' in s for s in t));passed('Currency switching recovers after reconnect')
(ROOT/'multicurrency-ui-results.json').write_text(json.dumps(results,indent=2),encoding='utf8')
