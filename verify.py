"""Local release gate. --emulator runs isolated instrumentation on a disposable emulator.
Never publishes or uploads artifacts. Requires the Android SDK, JDK 21 and Python 3.
"""
from pathlib import Path
import argparse,os,subprocess,sys
ROOT=Path(__file__).resolve().parent
parser=argparse.ArgumentParser();parser.add_argument('--emulator');parser.add_argument('--bundle',action='store_true');args=parser.parse_args()
JDK=Path(os.environ.get('JAVA_HOME',r'C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot'))
SDK=Path(os.environ.get('ANDROID_HOME',str(Path.home()/'AppData/Local/Android/Sdk')))
def run(cmd):
    result=subprocess.run([str(s) for s in cmd],cwd=ROOT,capture_output=True,text=True,encoding='utf8',errors='replace')
    print(result.stdout);print(result.stderr,file=sys.stderr);result.check_returncode();return result.stdout
run([sys.executable,'build.py'])
classes=ROOT/'build/test-classes';classes.mkdir(exist_ok=True)
run([JDK/'bin/javac.exe','-encoding','UTF-8','--release','8','-d',classes,ROOT/'src/br/com/cambioclaro/app/Rate.java',ROOT/'src/br/com/cambioclaro/app/EconomicSimulator.java',ROOT/'tests/RateTest.java'])
domain=run([JDK/'bin/java.exe','-cp',classes,'RateTest'])
(ROOT/'build/domain-results.txt').write_text(domain,encoding='utf8')
if args.bundle:run([sys.executable,'build_bundle.py'])
if args.emulator:
    if not args.emulator.startswith('emulator-'):raise SystemExit('Use only a disposable emulator; instrumentation changes test state and notification permission.')
    run([sys.executable,'build_tests.py'])
    adb=[SDK/'platform-tools/adb.exe','-s',args.emulator]
    apk=ROOT/('build/CambioClaro-universal.apk' if args.bundle else 'build/CambioClaro-1.7.9.apk')
    run(adb+['install','-r',apk]);run(adb+['install','-r',ROOT/'build/android-tests/tests.apk'])
    sdk_version=int(run(adb+['shell','getprop','ro.build.version.sdk']).strip())
    if sdk_version>=33:run(adb+['shell','pm','revoke','br.com.cambioclaro.app','android.permission.POST_NOTIFICATIONS'])
    if sdk_version<30:run(adb+['root']);run(adb+['wait-for-device'])
    try:
        run(adb+['shell','svc','wifi','disable']);run(adb+['shell','svc','data','disable']);run(adb+['shell','am','force-stop','br.com.cambioclaro.app'])
        result=run(adb+['shell','am','instrument','-w','br.com.cambioclaro.tests/br.com.cambioclaro.tests.Checks'])
        (ROOT/'build/instrumentation-results.txt').write_text(result,encoding='utf8')
        if 'checks passed.' not in result or 'FAILED' in result:raise SystemExit('Instrumentation failed; release gate rejected this build.')
    finally:
        run(adb+['shell','svc','wifi','enable']);run(adb+['shell','svc','data','enable'])
print('RELEASE_GATE_PASSED')


