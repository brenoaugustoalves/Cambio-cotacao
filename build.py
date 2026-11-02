"""Build a dependency-free native Android APK with the official SDK tools.
Set ANDROID_HOME and JAVA_HOME (JDK 21). Signing material stays in .signing/.
"""
from pathlib import Path
import os, subprocess, shutil, zipfile, secrets, sys, xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parent
SDK=Path(os.environ.get('ANDROID_HOME',str(Path.home()/'AppData/Local/Android/Sdk')))
JDK=Path(os.environ.get('JAVA_HOME',r'C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot'))
BT=SDK/'build-tools/36.0.0'
PLATFORM=SDK/'platforms/android-37.0/android.jar'
if not PLATFORM.exists():PLATFORM=SDK/'platforms/android-37/android.jar'
WINDOWS=os.name=='nt';ANDROID_NS='{http://schemas.android.com/apk/res/android}'
def jdk_tool(name):return JDK/'bin'/(name+('.exe' if WINDOWS else ''))
def sdk_tool(name):return BT/(name+('.exe' if WINDOWS and name in {'aapt2','zipalign'} else '.bat' if WINDOWS else ''))
manifest=ET.parse(ROOT/'AndroidManifest.xml').getroot();VERSION=manifest.attrib[ANDROID_NS+'versionName']
BUILD=ROOT/'build';BUILD.mkdir(exist_ok=True)
for folder in ['generated','classes','dex']:(BUILD/folder).mkdir(exist_ok=True)
env=os.environ.copy();env['JAVA_HOME']=str(JDK);env['PATH']=str(JDK/'bin')+os.pathsep+env['PATH']
def run(args):
    subprocess.run([str(x) for x in args],env=env,cwd=ROOT,check=True)
run([sdk_tool('aapt2'),'compile','--dir',ROOT/'res','-o',BUILD/'resources.zip'])
run([sdk_tool('aapt2'),'link','-o',BUILD/'base.apk','--manifest',ROOT/'AndroidManifest.xml','-I',PLATFORM,'--java',BUILD/'generated','--auto-add-overlay',BUILD/'resources.zip'])
sources=list((ROOT/'src').rglob('*.java'))+list((BUILD/'generated').rglob('*.java'))
run([jdk_tool('javac'),'-encoding','UTF-8','--release','8','-classpath',PLATFORM,'-d',BUILD/'classes',*sources])
run([jdk_tool('jar'),'cf',BUILD/'classes.jar','-C',BUILD/'classes','.'])
run([sdk_tool('d8'),'--release','--min-api','26','--lib',PLATFORM,'--output',BUILD/'dex',BUILD/'classes.jar'])
shutil.copy2(BUILD/'base.apk',BUILD/'unsigned.apk')
with zipfile.ZipFile(BUILD/'unsigned.apk','a',compression=zipfile.ZIP_DEFLATED) as apk:
    for dex in (BUILD/'dex').glob('*.dex'):apk.write(dex,dex.name)
run([sdk_tool('zipalign'),'-f','-p','4',BUILD/'unsigned.apk',BUILD/'aligned.apk'])
KEYS=ROOT/'.signing';KEYS.mkdir(exist_ok=True)
password=KEYS/'password.txt';keystore=KEYS/'cambio-claro.jks'
if not keystore.exists():
    password.write_text(secrets.token_urlsafe(32),encoding='utf-8')
    run([jdk_tool('keytool'),'-genkeypair','-keystore',keystore,'-storepass:file',password,'-keypass:file',password,'-alias','cambio-claro','-keyalg','RSA','-keysize','3072','-validity','10000','-dname','CN=Cambio Claro, OU=Independent Android App, O=Cambio Claro, C=BR'])
apk=BUILD/f'CambioClaro-{VERSION}.apk'
run([sdk_tool('apksigner'),'sign','--ks',keystore,'--ks-key-alias','cambio-claro','--ks-pass','file:'+str(password),'--out',apk,BUILD/'aligned.apk'])
run([sdk_tool('apksigner'),'verify','--verbose',apk])
run([sdk_tool('zipalign'),'-c','4',apk])
print('APK_READY',apk,apk.stat().st_size,flush=True)


