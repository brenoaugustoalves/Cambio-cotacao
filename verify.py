"""Local release gate. --emulator runs isolated instrumentation on a disposable emulator.
Never publishes or uploads artifacts. Requires the Android SDK, JDK 21 and Python 3.
"""
from pathlib import Path
import argparse,os,subprocess,sys,xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parent
parser=argparse.ArgumentParser();parser.add_argument('--emulator');parser.add_argument('--bundle',action='store_true');args=parser.parse_args()
JDK=Path(os.environ.get('JAVA_HOME',r'C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot'))
SDK=Path(os.environ.get('ANDROID_HOME',str(Path.home()/'AppData/Local/Android/Sdk')))
PLATFORM=SDK/'platforms/android-37.0/android.jar'
if not PLATFORM.exists():PLATFORM=SDK/'platforms/android-37/android.jar'
WINDOWS=os.name=='nt';ANDROID_NS='{http://schemas.android.com/apk/res/android}'
def jdk_tool(name):return JDK/'bin'/(name+('.exe' if WINDOWS else ''))
VERSION=ET.parse(ROOT/'AndroidManifest.xml').getroot().attrib[ANDROID_NS+'versionName']
def run(cmd):
    result=subprocess.run([str(s) for s in cmd],cwd=ROOT,capture_output=True,text=True,encoding='utf8',errors='replace')
    print(result.stdout);print(result.stderr,file=sys.stderr);result.check_returncode();return result.stdout
run([sys.executable,'build.py'])
classes=ROOT/'build/test-classes';classes.mkdir(exist_ok=True)
conversion=ROOT/'src/br/com/cambioclaro/app/conversion'
run([jdk_tool('javac'),'-encoding','UTF-8','--release','8','-d',classes,
     ROOT/'src/br/com/cambioclaro/app/Rate.java',ROOT/'src/br/com/cambioclaro/app/EconomicSimulator.java',
     conversion/'domain/ConversionQuote.java',conversion/'domain/ConversionRequest.java',
     conversion/'domain/ConversionResult.java',conversion/'application/ConversionQuoteSource.java',
     conversion/'application/ConvertCurrency.java',ROOT/'src/br/com/cambioclaro/app/market/application/MarketRefreshPolicy.java',
     ROOT/'src/br/com/cambioclaro/app/market/application/LatestRequestCoordinator.java',
     conversion/'presentation/ConversionDisplay.java',conversion/'presentation/ConversionPresenter.java',
     ROOT/'src/br/com/cambioclaro/app/FxCurrency.java',ROOT/'src/br/com/cambioclaro/app/market/domain/MarketObservation.java',
     ROOT/'src/br/com/cambioclaro/app/market/application/MarketQuoteSource.java',
     ROOT/'src/br/com/cambioclaro/app/market/application/MarketQuoteRepository.java',
     ROOT/'src/br/com/cambioclaro/app/market/application/MarketHistoryWriter.java',
     ROOT/'src/br/com/cambioclaro/app/market/application/MarketAlertSink.java',
     ROOT/'src/br/com/cambioclaro/app/market/application/StoreMarketBatch.java',
     ROOT/'src/br/com/cambioclaro/app/market/application/RefreshMarketQuotes.java',
     ROOT/'src/br/com/cambioclaro/app/market/application/StockQuoteGateway.java',
     ROOT/'src/br/com/cambioclaro/app/market/application/StockRefreshResult.java',
     ROOT/'src/br/com/cambioclaro/app/market/application/RefreshStockQuotes.java',
     ROOT/'src/br/com/cambioclaro/app/market/application/MarketHistorySource.java',
     ROOT/'src/br/com/cambioclaro/app/market/application/MarketHistoryRepository.java',
     ROOT/'src/br/com/cambioclaro/app/market/application/RefreshMarketHistory.java',
     ROOT/'tests/RateTest.java',ROOT/'tests/ConversionTest.java',ROOT/'tests/ConversionPresenterTest.java',
     ROOT/'tests/MarketRefreshPolicyTest.java',ROOT/'tests/LatestRequestCoordinatorTest.java',
     ROOT/'tests/RefreshMarketQuotesTest.java',ROOT/'tests/RefreshStockQuotesTest.java',
     ROOT/'tests/RefreshMarketHistoryTest.java'])
domain=run([jdk_tool('java'),'-cp',classes,'RateTest'])
(ROOT/'build/domain-results.txt').write_text(domain,encoding='utf8')
conversion_results=run([jdk_tool('java'),'-cp',classes,'ConversionTest'])
(ROOT/'build/conversion-results.txt').write_text(conversion_results,encoding='utf8')
refresh_policy_results=run([jdk_tool('java'),'-cp',classes,'MarketRefreshPolicyTest'])
(ROOT/'build/refresh-policy-results.txt').write_text(refresh_policy_results,encoding='utf8')
presenter_results=run([jdk_tool('java'),'-cp',classes,'ConversionPresenterTest'])
(ROOT/'build/conversion-presenter-results.txt').write_text(presenter_results,encoding='utf8')
coordinator_results=run([jdk_tool('java'),'-cp',classes,'LatestRequestCoordinatorTest'])
(ROOT/'build/latest-request-results.txt').write_text(coordinator_results,encoding='utf8')
market_refresh_results=run([jdk_tool('java'),'-cp',classes,'RefreshMarketQuotesTest'])
(ROOT/'build/market-refresh-results.txt').write_text(market_refresh_results,encoding='utf8')
stock_refresh_results=run([jdk_tool('java'),'-cp',classes,'RefreshStockQuotesTest'])
(ROOT/'build/stock-refresh-results.txt').write_text(stock_refresh_results,encoding='utf8')
history_refresh_results=run([jdk_tool('java'),'-cp',classes,'RefreshMarketHistoryTest'])
(ROOT/'build/history-refresh-results.txt').write_text(history_refresh_results,encoding='utf8')
if args.bundle:run([sys.executable,'build_bundle.py'])
if args.emulator:
    if not args.emulator.startswith('emulator-'):raise SystemExit('Use only a disposable emulator; instrumentation changes test state and notification permission.')
    run([sys.executable,'build_tests.py'])
    adb=[SDK/'platform-tools/adb.exe','-s',args.emulator]
    apk=ROOT/('build/CambioClaro-universal.apk' if args.bundle else f'build/CambioClaro-{VERSION}.apk')
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


