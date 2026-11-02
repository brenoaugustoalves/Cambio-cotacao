from pathlib import Path
import os,subprocess,zipfile,shutil
ROOT=Path(__file__).resolve().parent
SDK=Path(os.environ['ANDROID_HOME']);JDK=Path(os.environ.get('JAVA_HOME',r'C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot'))
BT=SDK/'build-tools/36.0.0';PLATFORM=SDK/'platforms/android-37.0/android.jar';B=ROOT/'build/android-tests'
for x in ['generated','classes','dex','assets']:(B/x).mkdir(parents=True,exist_ok=True)
env=os.environ.copy();env['JAVA_HOME']=str(JDK);env['PATH']=str(JDK/'bin')+os.pathsep+env['PATH']
def run(args):subprocess.run([str(x) for x in args],cwd=ROOT,env=env,check=True)
shutil.copy2(ROOT/'tests/fixtures/bcb-response-2026-10-29.json',B/'assets/bcb-response.json')
shutil.copy2(ROOT/'tests/fixtures/market-response-2026-10-30.json',B/'assets/market-response.json')
shutil.copy2(ROOT/'tests/fixtures/market-usd-intraday-2026-10-30.json',B/'assets/market-usd-intraday.json')
shutil.copy2(ROOT/'tests/fixtures/tourism-latest-2026-10-30.json',B/'assets/tourism-latest.json')
shutil.copy2(ROOT/'tests/fixtures/tourism-history-2026-10-30.json',B/'assets/tourism-history.json')
shutil.copy2(ROOT/'tests/fixtures/brapi-stocks-2026-10-30.json',B/'assets/brapi-stocks.json')
for fixture in (ROOT/'tests/fixtures').glob('*-history.json'):shutil.copy2(fixture,B/'assets'/fixture.name)
run([BT/'aapt2.exe','link','-o',B/'base.apk','--manifest',ROOT/'tests/AndroidManifest.xml','-I',PLATFORM,'-A',B/'assets'])
run([JDK/'bin/javac.exe','--release','8','-encoding','UTF-8','-cp',str(PLATFORM)+os.pathsep+str(ROOT/'build/classes.jar'),'-d',B/'classes',ROOT/'tests/Checks.java'])
run([JDK/'bin/jar.exe','cf',B/'test.jar','-C',B/'classes','.'])
run([BT/'d8.bat','--release','--min-api','26','--lib',PLATFORM,'--classpath',ROOT/'build/classes.jar','--output',B/'dex',B/'test.jar'])
shutil.copy2(B/'base.apk',B/'unsigned.apk')
with zipfile.ZipFile(B/'unsigned.apk','a',compression=zipfile.ZIP_DEFLATED) as z:z.write(B/'dex/classes.dex','classes.dex')
run([BT/'zipalign.exe','-f','4',B/'unsigned.apk',B/'aligned.apk'])
run([BT/'apksigner.bat','sign','--ks',ROOT/'.signing/cambio-claro.jks','--ks-key-alias','cambio-claro','--ks-pass','file:'+str(ROOT/'.signing/password.txt'),'--out',B/'tests.apk',B/'aligned.apk'])
print('TEST_APK_READY',flush=True)
