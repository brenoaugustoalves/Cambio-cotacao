"""Run build.py first. Set BUNDLETOOL_JAR to Google's bundletool-all JAR."""
from pathlib import Path
import os,subprocess,zipfile
ROOT=Path(__file__).resolve().parent;SDK=Path(os.environ['ANDROID_HOME']);JDK=Path(os.environ.get('JAVA_HOME',r'C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot'))
BT=SDK/'build-tools/36.0.0';PLATFORM=SDK/'platforms/android-37.0/android.jar';B=ROOT/'build'
TOOL=Path(os.environ.get('BUNDLETOOL_JAR',str(ROOT.parent/'bundletool.jar')))
env=os.environ.copy();env['JAVA_HOME']=str(JDK)
def run(a):subprocess.run([str(x) for x in a],cwd=ROOT,env=env,check=True)
run([BT/'aapt2.exe','link','--proto-format','-o',B/'proto.apk','--manifest',ROOT/'AndroidManifest.xml','-I',PLATFORM,'--auto-add-overlay',B/'resources.zip'])
with zipfile.ZipFile(B/'proto.apk') as src,zipfile.ZipFile(B/'base-module.zip','w',zipfile.ZIP_DEFLATED) as out:
    for info in src.infolist():out.writestr('manifest/AndroidManifest.xml' if info.filename=='AndroidManifest.xml' else info.filename,src.read(info.filename))
    for dex in (B/'dex').glob('*.dex'):out.write(dex,'dex/'+dex.name)
aab=B/'CambioClaro-1.8.0.aab'
run([JDK/'bin/java.exe','-jar',TOOL,'build-bundle','--modules='+str(B/'base-module.zip'),'--output='+str(aab),'--overwrite'])
run([JDK/'bin/jarsigner.exe','-keystore',ROOT/'.signing/cambio-claro.jks','-storepass:file',ROOT/'.signing/password.txt','-sigalg','SHA256withRSA','-digestalg','SHA-256',aab,'cambio-claro'])
run([JDK/'bin/jarsigner.exe','-verify',aab])
run([JDK/'bin/java.exe','-jar',TOOL,'validate','--bundle='+str(aab)])
run([JDK/'bin/java.exe','-jar',TOOL,'build-apks','--bundle='+str(aab),'--output='+str(B/'bundle-test.apks'),'--overwrite','--mode=universal','--ks='+str(ROOT/'.signing/cambio-claro.jks'),'--ks-pass=file:'+str(ROOT/'.signing/password.txt'),'--ks-key-alias=cambio-claro','--aapt2='+str(BT/'aapt2.exe')])
with zipfile.ZipFile(B/'bundle-test.apks') as z:(B/'CambioClaro-universal.apk').write_bytes(z.read('universal.apk'))
run([BT/'apksigner.bat','verify','--verbose',B/'CambioClaro-universal.apk'])
print('BUNDLE_AND_UNIVERSAL_APK_READY',flush=True)


