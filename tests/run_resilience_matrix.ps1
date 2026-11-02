param([int[]]$Apis = @(26,27,28,29,30,31,32,33,34,35,36),[int]$Cycles = 1)
$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $false
$root = Split-Path -Parent $PSScriptRoot
$sdk = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
$env:ANDROID_HOME = $sdk
$env:ANDROID_SDK_ROOT = $sdk
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot'
$env:PATH = "$env:JAVA_HOME\bin;$sdk\cmdline-tools\latest\bin;$env:PATH"
$adb = Join-Path $sdk 'platform-tools\adb.exe'
$emulator = Join-Path $sdk 'emulator\emulator.exe'
$avdmanager = Join-Path $sdk 'cmdline-tools\latest\bin\avdmanager.bat'
$serial = 'emulator-5554'
$logs = Join-Path $PSScriptRoot 'android-resilience-logs'
New-Item -ItemType Directory -Path $logs -Force | Out-Null
$images = @{
    26='system-images;android-26;google_apis;x86_64'
    27='system-images;android-27;default;x86_64'
    28='system-images;android-28;google_apis;x86_64'
    29='system-images;android-29;google_apis;x86_64'
    30='system-images;android-30;aosp_atd;x86_64'
    31='system-images;android-31;aosp_atd;x86_64'
    32='system-images;android-32;aosp_atd;x86_64'
    33='system-images;android-33;aosp_atd;x86_64'
    34='system-images;android-34;google_apis;x86_64'
    35='system-images;android-35;aosp_atd;x86_64'
    36='system-images;android-36;aosp_atd;x86_64'
}
$results = [System.Collections.Generic.List[object]]::new()
function Stop-TestEmulator {
    $devices = & $adb devices 2>&1
    if ($devices -match "$serial\s+device") { & $adb -s $serial emu kill 2>&1 | Out-Null; Start-Sleep -Seconds 3 }
}
try {
    foreach ($api in $Apis) {
        $avd = "CambioClaro_API$api"
        $package = $images[$api]
        $imagePath = Join-Path $sdk ($package.Replace(';','\'))
        if (-not (Test-Path (Join-Path $imagePath 'system.img'))) { throw "Imagem ausente API ${api}: $package" }
        if ((& $emulator -list-avds) -notcontains $avd) {
            & $avdmanager create avd -n $avd -k $package -d pixel_2 --force
            if ($LASTEXITCODE -ne 0) { throw "Falha criando AVD API $api" }
        }
        Write-Output "STRESS API $api"
        Start-Process -FilePath $emulator -ArgumentList @('-avd',$avd,'-no-window','-no-audio','-no-boot-anim','-no-snapshot','-gpu','swiftshader_indirect') -WindowStyle Hidden
        $deviceReady=$false
        for($i=0;$i -lt 90;$i++){if((& $adb devices 2>&1) -match "$serial\s+device"){$deviceReady=$true;break};Start-Sleep -Seconds 2}
        if(-not $deviceReady){Stop-TestEmulator;throw "ADB não encontrou API $api"}
        $booted=$false
        for($i=0;$i -lt 180;$i++){
            $state=(& $adb -s $serial shell getprop sys.boot_completed 2>&1 | Out-String).Trim()
            $logboot=(& $adb -s $serial shell getprop sys.logbootcomplete 2>&1 | Out-String).Trim()
            $anim=(& $adb -s $serial shell getprop init.svc.bootanim 2>&1 | Out-String).Trim()
            if($state -eq '1' -or ($logboot -eq '1' -and $anim -eq 'stopped')){$booted=$true;break};Start-Sleep -Seconds 2
        }
        if(-not $booted){Stop-TestEmulator;throw "Timeout boot API $api"}
        $actual=(& $adb -s $serial shell getprop ro.build.version.sdk 2>&1 | Out-String).Trim()
        if($actual -ne [string]$api){Stop-TestEmulator;throw "API esperada $api; inicializou $actual"}
        $out=Join-Path $logs "API$api.log";$err=Join-Path $logs "API$api.stderr.log"
        $python=(Get-Command python.exe).Source
        $stressArgs=@('tests/run_resilience.py','--emulator',$serial,'--cycles',[string]$Cycles)
        if($api -eq 26){$stressArgs+='--skip-uiautomator'}
        $proc=Start-Process -FilePath $python -ArgumentList $stressArgs -WorkingDirectory $root -Wait -PassThru -NoNewWindow -RedirectStandardOutput $out -RedirectStandardError $err
        if(Test-Path $err){Get-Content $err | Add-Content $out -Encoding utf8}
        $pass=$proc.ExitCode -eq 0 -and (Select-String -Path $out -Pattern 'RESILIENCE_TESTS_PASSED' -Quiet)
        $status=if($pass){'PASS'}else{'FAIL'}
        $results.Add([pscustomobject]@{Android=(& $adb -s $serial shell getprop ro.build.version.release 2>&1 | Out-String).Trim();API=$api;Stress=$status;Log="android-resilience-logs/API$api.log"})
        Get-Content $out -Tail 8 | Write-Output
        Stop-TestEmulator
    }
} finally { Stop-TestEmulator }
$report=Join-Path $PSScriptRoot 'ANDROID-STRESS-8-16.md'
$lines=@('# Estresse Android 8–16 — Câmbio Claro 1.6.1','','| Android | API | Resultado | Log |','|---|---:|---|---|')
foreach($item in $results){$lines += "| $($item.Android) | $($item.API) | $($item.Stress) | [$($item.Log)]($($item.Log)) |"}
$lines += '| Android 17 | 37 | PASS anterior · 220 eventos UI e 12 ciclos offline | [relatório de resiliência](../TESTE-RESILIENCIA-1.6.1.md) |'
Set-Content -LiteralPath $report -Value $lines -Encoding utf8
$results | Format-Table -AutoSize
Write-Output "STRESS_REPORT $report"
if(($results | Where-Object Stress -ne 'PASS').Count -gt 0){exit 1}
