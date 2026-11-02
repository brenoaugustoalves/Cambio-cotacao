# Relatório de testes — Câmbio Claro 1.6.1

Execuções locais em emuladores descartáveis. Nenhuma publicação na Play Store e nenhuma carga artificial foi enviada a fornecedores de cotações.

A matriz completa de compatibilidade Android 8–17 está em [`tests/ANDROID-MATRIX-8-17.md`](tests/ANDROID-MATRIX-8-17.md). As APIs 26–36 passaram com esta rodada; API 37 passou anteriormente na mesma versão 1.6.1.

O escopo e os resultados de estresse por versão estão em [`tests/ANDROID-STRESS-8-17.md`](tests/ANDROID-STRESS-8-17.md). O estresse foi executado em Android 8, 14 e 17; não foi repetido em todas as APIs intermediárias.

## Componentes, compatibilidade e carga local

| Ambiente | Verificações de domínio | Instrumentação Android | Parser local |
|---|---:|---:|---:|
| Android 17 / API 37 (`emulator-5554`) | 21 | 190 | 250 parses de mercado + 25 PTAX em 2.979 ms |
| Android 14 / API 34 (`emulator-5556`) | 21 | 190 | 250 parses de mercado + 25 PTAX em 2.307 ms |

A instrumentação cobre parsing e limites de respostas, cache/offline, preferências, temas, notificações, configuração e renderização do widget, activities privadas, componentes exportados, rede segura, estados indisponíveis e dados sem publicação. A suíte também valida ações e pares de câmbio sem fabricar valores. O tempo do parser mede processamento local; não representa throughput de servidor ou rede.

## Estresse e indisponibilidade

| Ambiente | Ciclos de abertura offline | Eventos de interface | Memória PSS após teste | Resultado |
|---|---:|---:|---:|---|
| Android 17 / API 37 | 12 | 220 | 44.194 KiB | Passou, sem exceção fatal |
| Android 14 / API 34 | 4 | 220 | 84.784 KiB | Passou, sem exceção fatal |

O teste de estresse não envia rajadas a APIs públicas. Ele alterna estado offline e gera ações de interface apenas em emulador descartável. Os tamanhos PSS variam com a carga do emulador e não representam um limite fixo de memória.

O Motorola Edge 60 Pro físico não apareceu na lista ADB desta sessão, portanto a validação física ainda depende de conectar o aparelho com Depuração USB autorizada.

## Reproduzir

```powershell
python verify.py --emulator emulator-5554 --bundle
python verify.py --emulator emulator-5556
python tests/run_resilience.py --emulator emulator-5554 --cycles 12
python tests/run_resilience.py --emulator emulator-5556 --cycles 4
```

O Android pode adiar tarefas em segundo plano e não garante execução ininterrupta por 24 horas. O teste não mede taxa de atualização do fornecedor, nem carga de usuários concorrentes.
