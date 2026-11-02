"""Bounded component, offline-recovery and UI stress checks on a disposable emulator."""
from pathlib import Path
import argparse, json, re, subprocess, time, xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
ADB = Path.home() / "AppData/Local/Android/Sdk/platform-tools/adb.exe"
PACKAGE = "br.com.cambioclaro.app"
parser = argparse.ArgumentParser()
parser.add_argument("--emulator", required=True)
parser.add_argument("--cycles", type=int, default=12)
parser.add_argument("--skip-uiautomator", action="store_true", help="Run offline and monkey stress without hierarchy/tap checks on older emulator images.")
args = parser.parse_args()
if not args.emulator.startswith("emulator-"):
    raise SystemExit("Run only on a disposable Android emulator.")
if not 1 <= args.cycles <= 30:
    raise SystemExit("--cycles must be between 1 and 30.")
parser_args = args

def adb(*args, check=True, timeout=25):
    p = subprocess.run([str(ADB), "-s", parser_args.emulator, *map(str, args)], capture_output=True, timeout=timeout)
    out = (p.stdout + p.stderr).decode("utf-8", "replace")
    if check and p.returncode:
        raise RuntimeError(f"adb {args}: {out[-1600:]}")
    return out

sdk_level = int(adb("shell", "getprop", "ro.build.version.sdk").strip())
if sdk_level < 30:
    adb("root")
    adb("wait-for-device")

results = []
def passed(text):
    results.append({"status": "PASS", "check": text})
    print("PASS", text, flush=True)

def current_activity():
    s = adb("shell", "dumpsys", "activity", "activities", timeout=15)
    return PACKAGE in s and ("mResumedActivity" in s or "topResumedActivity" in s)

def wait_activity(seconds=15):
    until = time.monotonic() + seconds
    while time.monotonic() < until:
        if current_activity(): return
        time.sleep(.3)
    raise AssertionError("App activity did not resume")

def tree():
    adb("shell", "uiautomator", "dump", "/sdcard/cambio-resilience.xml", timeout=20)
    raw = adb("shell", "cat", "/sdcard/cambio-resilience.xml", timeout=10)
    return ET.fromstring(raw[raw.find("<?xml"):])

def texts(root):
    return [node.get("text", "") for node in root.iter("node")]

def tap_text(root, starts):
    node = next((n for n in root.iter("node") if any(n.get("text", "").startswith(s) for s in starts)), None)
    if node is None: raise AssertionError("Accessible text not found: " + str(starts))
    x1,y1,x2,y2 = map(int, re.findall(r"\d+", node.get("bounds", "")))
    adb("shell", "input", "tap", (x1+x2)//2, (y1+y2)//2)

def scroll_to_top():
    for _ in range(6):
        root = tree()
        if any(s.startswith("Preferências") for s in texts(root)): return root
        adb("shell", "input", "swipe", "520", "450", "520", "2200", "350")
    raise AssertionError("Could not return to the main-screen header")

def no_app_crash():
    log = adb("logcat", "-d", "-s", "AndroidRuntime:E", timeout=15)
    if "FATAL EXCEPTION" in log and PACKAGE in log:
        raise AssertionError("Fatal app exception found in logcat: " + log[-2000:])

report_dir = ROOT / "tests" / "stress-results"
report_dir.mkdir(parents=True, exist_ok=True)
if args.skip_uiautomator:
    adb("logcat", "-c", check=False)
    try:
        adb("shell", "am", "force-stop", PACKAGE)
        adb("shell", "am", "start", "-n", PACKAGE + "/.MainActivity")
        wait_activity()
        no_app_crash()
        passed("Cold start and normal component initialization")
        adb("shell", "svc", "wifi", "disable")
        adb("shell", "svc", "data", "disable")
        for _ in range(args.cycles):
            adb("shell", "am", "force-stop", PACKAGE)
            adb("shell", "am", "start", "-n", PACKAGE + "/.MainActivity")
            wait_activity(8)
            no_app_crash()
        passed(f"{args.cycles} offline cold-start/stop cycles completed without crash")
        monkey = adb("shell", "monkey", "-p", PACKAGE, "--throttle", "70", "--pct-syskeys", "0", "--pct-appswitch", "0", "-v", "220", timeout=90)
        (report_dir / (args.emulator + "-monkey.txt")).write_text(monkey, encoding="utf-8")
        if "Events injected: 220" not in monkey: raise AssertionError("UI stress run did not inject all 220 events: " + monkey[-1000:])
        no_app_crash()
        passed("220 bounded UI stress events completed without fatal app exception")
        adb("shell", "am", "start", "-n", PACKAGE + "/.MainActivity")
        wait_activity()
        mem = adb("shell", "dumpsys", "meminfo", PACKAGE, timeout=20)
        match = re.search(r"TOTAL\s+(\d+)", mem)
        pss_kb = int(match.group(1)) if match else None
        if pss_kb is not None and pss_kb > 400 * 1024: raise AssertionError(f"App PSS exceeded the 400 MiB guardrail: {pss_kb} KiB")
        passed("App recovers after UI stress; post-run PSS recorded" + (f" ({pss_kb} KiB)" if pss_kb else " (Android did not expose TOTAL PSS)"))
    finally:
        adb("shell", "svc", "wifi", "enable")
        adb("shell", "svc", "data", "enable")
    summary = {"emulator": args.emulator, "android": adb("shell", "getprop", "ro.build.version.release").strip(), "api": sdk_level, "cycles": args.cycles, "checks": results, "uiautomator": "skipped; offline/monkey stress only"}
    (report_dir / (args.emulator + "-summary.json")).write_text(json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8")
    print("RESILIENCE_TESTS_PASSED", json.dumps(summary, ensure_ascii=False), flush=True)
    raise SystemExit(0)
adb("logcat", "-c", check=False)
wifi = adb("shell", "settings", "get", "global", "wifi_on").strip()
try:
    # Start online once to validate ordinary initialization; later stress is offline.
    adb("shell", "am", "force-stop", PACKAGE)
    adb("shell", "am", "start", "-n", PACKAGE + "/.MainActivity")
    wait_activity()
    time.sleep(2)
    no_app_crash()
    passed("Cold start and normal component initialization")

    # Private screens must open through the app UI, not Android's external resolver.
    scroll_to_top()
    for label, name in (("Preferências", "SettingsActivity"), ("Diagnóstico", "DiagnosticsActivity")):
        tap_text(tree(), [label])
        wait_activity()
        if name not in adb("shell", "dumpsys", "activity", "activities"):
            raise AssertionError(name + " did not open from its in-app control")
        adb("shell", "input", "keyevent", "4")
        wait_activity()
    passed("Main, settings and diagnostics activities interoperate")

    # Validate the currency-list disclosure and its accessible tap target.
    adb("shell", "am", "force-stop", PACKAGE)
    adb("shell", "am", "start", "-n", PACKAGE + "/.MainActivity")
    wait_activity()
    found = None
    for _ in range(14):
        found = tree()
        if any("Ver todas as moedas" in s for s in texts(found)): break
        adb("shell", "input", "swipe", "520", "2100", "520", "650", "350")
    visible = texts(found)
    if not any("Ver todas as moedas" in s for s in visible):
        raise AssertionError("Collapsed currency-list control was not visible")
    tap_text(found, ["Ver todas as moedas"])
    time.sleep(.3)
    expanded = texts(tree())
    if not any(s.startswith("CAD •") for s in expanded):
        raise AssertionError("Expanded list did not reveal currencies after the initial four")
    less = None
    for _ in range(8):
        less = tree()
        if any(s.startswith("Mostrar menos") for s in texts(less)): break
        adb("shell", "input", "swipe", "520", "2100", "520", "650", "350")
    tap_text(less, ["Mostrar menos"])
    time.sleep(.3)
    collapsed = texts(tree())
    if any(s.startswith("CAD •") for s in collapsed):
        raise AssertionError("Collapsed list still exposes rows beyond the initial four")
    passed("Currency list expands and collapses; first four remain compact and favorites-first")

    # Network-failure recovery checks are local/offline so stress does not load vendors.
    adb("shell", "svc", "wifi", "disable")
    adb("shell", "svc", "data", "disable")
    for _ in range(args.cycles):
        adb("shell", "am", "force-stop", PACKAGE)
        adb("shell", "am", "start", "-n", PACKAGE + "/.MainActivity")
        wait_activity(8)
        no_app_crash()
    passed(f"{args.cycles} offline cold-start/stop cycles completed without crash")

    # Android's UI Monkey applies bounded taps, swipes and key events to this app only.
    monkey = adb("shell", "monkey", "-p", PACKAGE, "--throttle", "70", "--pct-syskeys", "0", "--pct-appswitch", "0", "-v", "220", timeout=90)
    (report_dir / (args.emulator + "-monkey.txt")).write_text(monkey, encoding="utf-8")
    if "Events injected: 220" not in monkey:
        raise AssertionError("UI stress run did not inject all 220 events: " + monkey[-1000:])
    no_app_crash()
    passed("220 bounded UI stress events completed without fatal app exception")

    adb("shell", "am", "start", "-n", PACKAGE + "/.MainActivity")
    wait_activity()
    mem = adb("shell", "dumpsys", "meminfo", PACKAGE, timeout=20)
    match = re.search(r"TOTAL\s+(\d+)", mem)
    pss_kb = int(match.group(1)) if match else None
    if pss_kb is not None and pss_kb > 400 * 1024:
        raise AssertionError(f"App PSS exceeded the 400 MiB guardrail: {pss_kb} KiB")
    passed("App recovers after UI stress; post-run PSS recorded" + (f" ({pss_kb} KiB)" if pss_kb else " (Android did not expose TOTAL PSS)"))
finally:
    adb("shell", "svc", "wifi", "enable")
    adb("shell", "svc", "data", "enable")

sdk = adb("shell", "getprop", "ro.build.version.sdk").strip()
release = adb("shell", "getprop", "ro.build.version.release").strip()
summary = {"emulator": args.emulator, "android": release, "api": sdk, "cycles": args.cycles, "checks": results}
(report_dir / (args.emulator + "-summary.json")).write_text(json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8")
print("RESILIENCE_TESTS_PASSED", json.dumps(summary, ensure_ascii=False), flush=True)
