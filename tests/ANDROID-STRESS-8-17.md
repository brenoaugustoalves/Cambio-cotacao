# Testes de estresse Android 8–17 — Câmbio Claro 1.6.1

Os testes completos de componentes e compatibilidade foram executados em todas as APIs 26–36 nesta rodada e na API 37 anteriormente. A tabela abaixo registra o teste de estresse em três versões representativas; estresse não foi executado em cada API.

| Android | API | Ciclos offline | Eventos UI | PSS após teste | Resultado |
|---|---:|---:|---:|---:|---|
| Android 8.0 | 26 | 1 | 220 | 25.716 KiB | PASS; inicialização, ciclo offline, estresse limitado e recuperação |
| Android 14 | 34 | 4 | 220 | 84.784 KiB | PASS; inclui navegação entre telas e lista de moedas |
| Android 17 | 37 | 12 | 220 | 44.194 KiB | PASS anterior; sem exceção fatal |

Na imagem API 26, a ferramenta UIAutomator não disponibilizou a hierarquia da interface; por isso, aquele teste foi limitado a ciclo offline e eventos de interface via Monkey. Os testes instrumentados de componentes e navegação passaram na matriz API 26–37, conforme [ANDROID-MATRIX-8-17.md](ANDROID-MATRIX-8-17.md).

O estresse é local em emulador descartável. Não envia rajadas a provedores, não simula usuários concorrentes nem comprova disponibilidade contínua de rede. PSS varia conforme a memória do emulador e não é um limite fixo do aplicativo.
