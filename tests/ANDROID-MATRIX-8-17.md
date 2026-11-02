# Matriz Android — Câmbio Claro 1.6.1

Suíte de domínio, instrumentação, componentes, widgets, cache/offline, carga local e verificações de segurança executada em emuladores. Cada API passou por 21 verificações de domínio e 190 verificações instrumentadas.

| Android | API | Resultado | Log |
|---|---:|---|---|
| 8.0 Oreo | 26 | PASS · 190 checks | [API26.log](android-matrix-logs/API26.log) |
| 8.1 Oreo | 27 | PASS · 190 checks | [API27.log](android-matrix-logs/API27.log) |
| 9 Pie | 28 | PASS · 190 checks | [API28.log](android-matrix-logs/API28.log) |
| 10 | 29 | PASS · 190 checks (repetido com ADB root para restaurar a conectividade do emulador) | [API29.log](android-matrix-logs/API29.log) |
| 11 | 30 | PASS · 190 checks | [API30.log](android-matrix-logs/API30.log) |
| 12 | 31 | PASS · 190 checks | [API31.log](android-matrix-logs/API31.log) |
| 12L | 32 | PASS · 190 checks | [API32.log](android-matrix-logs/API32.log) |
| 13 | 33 | PASS · 190 checks | [API33.log](android-matrix-logs/API33.log) |
| 14 | 34 | PASS · 190 checks | [API34.log](android-matrix-logs/API34.log) |
| 15 | 35 | PASS · 190 checks | [API35.log](android-matrix-logs/API35.log) |
| 16 | 36 | PASS · 190 checks | [API36.log](android-matrix-logs/API36.log) |
| 17 | 37 | PASS · 190 checks · teste anterior da mesma versão 1.6.1 | [relatório 1.6.1](../TESTE-RESILIENCIA-1.6.1.md) |

Os logs de API 26–36 foram obtidos nesta rodada. A API 37 foi aprovada anteriormente nesta mesma versão do app. Não aceitei a licença opcional/separada da nova imagem microXR que o SDK Manager apresentou; por isso não criei outro emulador API 37 nesta rodada. O teste anterior da API 37 permanece registrado.

## Reprodução

```powershell
python verify.py --emulator emulator-5554
powershell -NoProfile -ExecutionPolicy Bypass -File tests/run_android_matrix.ps1
```

O script da matriz prepara emuladores para API 26–36, executa a suíte em sequência e salva logs individuais. A API 37 requer a imagem já configurada e as condições de licença descritas acima.
