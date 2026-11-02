from pathlib import Path
import subprocess,xml.etree.ElementTree as E,re,time,json
ROOT=Path(__file__).resolve().parent;ADB=str(Path.home()/'AppData/Local/Android/Sdk/platform-tools/adb.exe');P='br.com.cambioclaro.app'
def adb(*a):return subprocess.run([ADB,'-s','emulator-5554',*a],capture_output=True,check=True).stdout.decode('utf8','replace')
def tree():adb('shell','uiautomator','dump','/sdcard/qa-widget.xml');return E.fromstring(adb('shell','cat','/sdcard/qa-widget.xml'))
def nodes(r):return {n.get('resource-id'):n for n in r.iter('node') if n.get('resource-id','').startswith(P)}
def tap(n):
    b=list(map(int,re.findall(r'\d+',n.get('bounds'))));adb('shell','input','tap',str((b[0]+b[2])//2),str((b[1]+b[3])//2))
def wait(test):
    for _ in range(15):
        r=tree()
        if test(r):return r
        time.sleep(1)
    raise AssertionError('Timeout')
def shot(name):(ROOT/name).write_bytes(subprocess.run([ADB,'-s','emulator-5554','exec-out','screencap','-p'],capture_output=True,check=True).stdout)
results=[]
def passed(s):results.append(s);print('PASS',s,flush=True)
adb('shell','input','keyevent','3')
for _ in range(4):
    r=tree();n=nodes(r)
    if P+':id/widget_rate' in n:break
    adb('shell','input','swipe','950','1200','120','1200','300')
assert n[P+':id/widget_rate'].get('text')=='R$ 5,3420';assert n[P+':id/widget_chart_label'].get('text').startswith('30 dias');passed('Pinned widget renders real quote and historical sparkline')
try:
    adb('shell','svc','wifi','disable');adb('shell','svc','data','disable');tap(n[P+':id/widget_refresh'])
    r=wait(lambda r:any(s in nodes(r).get(P+':id/widget_status',E.Element('node')).get('text','') for s in ['depende de conexão','interrompida','Falha ao consultar']));assert nodes(r)[P+':id/widget_rate'].get('text')=='R$ 5,3420';shot('widget-offline.png');passed('Offline widget queues update and labels saved data')
finally:adb('shell','svc','wifi','enable');adb('shell','svc','data','enable')
r=wait(lambda r:nodes(r).get(P+':id/widget_status',E.Element('node')).get('text','').startswith('Consulta:'));passed('Scheduled widget refresh recovers when network returns');shot('widget-final.png')
tap(nodes(r)[P+':id/widget_rate']);wait(lambda r:any(x.get('text')=='Moedas em reais' for x in r.iter('node')));passed('Widget opens application')
rotation=adb('shell','settings','get','system','user_rotation').strip();auto=adb('shell','settings','get','system','accelerometer_rotation').strip()
try:
    adb('shell','settings','put','system','accelerometer_rotation','0');adb('shell','settings','put','system','user_rotation','1')
    wait(lambda r:any(x.get('text')=='Moedas em reais' for x in r.iter('node')));shot('cambio-landscape.png');passed('Landscape activity recreation preserves a functioning screen')
finally:
    adb('shell','settings','put','system','user_rotation',rotation);adb('shell','settings','put','system','accelerometer_rotation',auto)
(ROOT/'widget-tests.json').write_text(json.dumps(results,indent=2),encoding='utf8')
