param([int[]]$Apis = @(26,27,28,29,30,31,32,33,34,35,36))
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
$results = [System.Collections.Generic.List[object]]::new()
$logs = Join-Path $PSScriptRoot 'android-matrix-logs'
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

function Stop-TestEmulator {
    $devices = & $adb devices 2>&1
    if ($devices -match "$serial\s+device") {
        & $adb -s $serial emu kill 2>&1 | Out-Null
        Start-Sleep -Seconds 3
    }
}

try {
    foreach ($api in $Apis) {
        if (-not $images.ContainsKey($api)) { throw "API não mapeada: $api" }
        $avd = "CambioClaro_API$api"
        $package = $images[$api]
        $imagePath = Join-Path $sdk ($package.Replace(';','\'))
        if (-not (Test-Path (Join-Path $imagePath 'system.img'))) { throw "Imagem ausente para API ${api}: $package" }

        $existing = & $emulator -list-avds
        if ($existing -notcontains $avd) {
            & $avdmanager create avd -n $avd -k $package -d pixel_2 --force
            if ($LASTEXITCODE -ne 0) { throw "Falha criando AVD API $api" }
        }

        Write-Output "START API $api ($avd)"
        Start-Process -FilePath $emulator -ArgumentList @('-avd',$avd,'-no-window','-no-audio','-no-boot-anim','-no-snapshot','-gpu','swiftshader_indirect') -WindowStyle Hidden
        $deviceReady = $false
        for ($i=0; $i -lt 90; $i++) {
            $devices = & $adb devices 2>&1
            if ($devices -match "$serial\s+device") { $deviceReady = $true; break }
            Start-Sleep -Seconds 2
        }
        if (-not $deviceReady) { Stop-TestEmulator; throw "ADB não reconheceu o emulador API $api" }

        $booted = $false
        for ($i=0; $i -lt 180; $i++) {
            $state = (& $adb -s $serial shell getprop sys.boot_completed 2>&1 | Out-String).Trim()
            $loggedBoot = (& $adb -s $serial shell getprop sys.logbootcomplete 2>&1 | Out-String).Trim()
            $animation = (& $adb -s $serial shell getprop init.svc.bootanim 2>&1 | Out-String).Trim()
            if ($state -eq '1' -or ($loggedBoot -eq '1' -and $animation -eq 'stopped')) { $booted = $true; break }
            Start-Sleep -Seconds 2
        }
        if (-not $booted) { Stop-TestEmulator; throw "Timeout de inicialização API $api" }

        $actualApi = (& $adb -s $serial shell getprop ro.build.version.sdk | Out-String).Trim()
        if ($actualApi -ne [string]$api) { Stop-TestEmulator; throw "AVD pediu API $api, inicializou API $actualApi" }

        $log = Join-Path $logs "API$api.log"
        $stderr = Join-Path $logs "API$api.stderr.log"
        $python = (Get-Command python.exe).Source
        $testProcess = Start-Process -FilePath $python -ArgumentList @('verify.py','--emulator',$serial) -WorkingDirectory $root -Wait -PassThru -NoNewWindow -RedirectStandardOutput $log -RedirectStandardError $stderr
        $exit = $testProcess.ExitCode
        if (Test-Path $stderr) { Get-Content -LiteralPath $stderr | Add-Content -LiteralPath $log -Encoding utf8 }
        Get-Content -LiteralPath $log -Tail 14 | Write-Output
        $passed = $exit -eq 0 -and (Select-String -Path $log -Pattern 'RELEASE_GATE_PASSED' -Quiet)
        $checks = ''
        if ($passed) {
            $match = Select-String -Path $log -Pattern '\d+ Android parser, cache, component, widget, load and security checks passed\.' | Select-Object -Last 1
            if ($match) { $checks = $match.Matches[0].Value }
        }
        $results.Add([pscustomobject]@{ API=$api; Android=(& $adb -s $serial shell getprop ro.build.version.release | Out-String).Trim(); Status=if($passed){'PASS'}else{'FAIL'}; Checks=$checks; Log="android-matrix-logs/API$api.log" })
        Stop-TestEmulator
        if (-not $passed) { Write-Warning "Falha na suíte API $api; continuando a matriz." }
    }
} finally {
    Stop-TestEmulator
}

$report = Join-Path $PSScriptRoot 'ANDROID-MATRIX-8-17.md'
$lines = @('# Matriz Android — Câmbio Claro 1.6.1','','| Android | API | Resultado | Instrumentação | Log |','|---|---:|---|---:|---|')
foreach ($item in $results) { $lines += "| $($item.Android) | $($item.API) | $($item.Status) | $($item.Checks) | [$($item.Log)]($($item.Log)) |" }
$lines += '| 17 | 37 | PASS anterior na versão 1.6.1 | 190 checks | [relatório 1.6.1](../TESTE-RESILIENCIA-1.6.1.md) |'
Set-Content -LiteralPath $report -Value $lines -Encoding utf8
$results | Format-Table -AutoSize
Write-Output "MATRIX_REPORT $report"
if (($results | Where-Object Status -ne 'PASS').Count -gt 0) { exit 1 }
