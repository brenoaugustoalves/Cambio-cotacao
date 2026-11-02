# Relatório de testes — Câmbio Claro 1.6.0

Execução local em emuladores descartáveis. Nenhuma publicação na Play Store e nenhuma carga artificial foi enviada aos provedores de cotações.

## Componentes, integração e compatibilidade

| Ambiente | Domínio | Instrumentação Android | Resultado |
|---|---:|---:|---|
| Android 17 / API 37 (`emulator-5554`) | 21 | 189 | Passou |
| Android 14 / API 34 (`emulator-5556`) | 21 | 189 | Passou |

A instrumentação cobre parsing e limites de resposta, cache e dados offline, preferências, tema, notificações, configuração e atualização do widget, activities privadas, componentes exportados, rede segura e condições de erro. O teste de integração também abriu Preferências e Diagnóstico pela tela principal e validou expandir/recolher a lista de moedas.

O teste local de carga repetiu 250 parses completos de respostas de mercado e 25 parses de séries PTAX, sem rede: **2.369 ms** no Android 17 e **1.753 ms** no Android 14. É um teste de carga do processamento local; não mede usuários simultâneos em servidor.

## Estresse e indisponibilidade

| Ambiente | Ciclos de abrir/parar sem rede | Eventos UI Monkey | PSS após estresse | Resultado |
|---|---:|---:|---:|---|
| Android 17 / API 37 | 12 | 220 | 49.630 KiB | Passou; sem exceção fatal do app |
| Android 14 / API 34 | 4 | 220 | 95.516 KiB | Passou; sem exceção fatal do app |

Nos ciclos offline, Wi‑Fi e dados móveis foram desligados no emulador e restaurados ao final. O app voltou a abrir depois dos eventos de interface. A verificação não executou chamadas em paralelo nem um teste de throughput contra a AwesomeAPI, BCB ou brapi: isso exigiria um endpoint de homologação autorizado para evitar carga sobre serviços externos.

Uma tentativa inicial no emulador Android 14 mostrou a caixa do sistema “System UI não está respondendo” durante a automação. Após reiniciar o emulador, a execução controlada passou; não foi encontrada exceção fatal do Câmbio Claro nos logs coletados. Isso permanece uma ocorrência do ambiente de teste a observar e não substitui um ensaio no Motorola físico.

## Reproduzir

```powershell
python verify.py --emulator emulator-5554 --bundle
python verify.py --emulator emulator-5556
python tests/run_resilience.py --emulator emulator-5554 --cycles 12
python tests/run_resilience.py --emulator emulator-5556 --cycles 4
```

Use somente emuladores descartáveis: os testes alternam Wi‑Fi/dados e geram eventos de interface. O script recusa destinos ADB que não comecem com `emulator-`.
