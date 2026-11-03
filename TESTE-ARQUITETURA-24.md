# Etapa 24 — restauração segura da navegação

## Alterações

- Extraída para `MainNavigationPresenter` a validação dos destinos principais e subtópicos de Moedas.
- Estado ausente ou desatualizado agora volta a **Moedas → Cotação**, em vez de deixar todos os painéis ocultos.
- `MainActivity` aplica a seleção normalizada aos painéis e abas; as regras são testáveis sem Android.

## Verificação

Executar `python verify.py --emulator emulator-5556`.

- 4 testes puros de navegação passaram, cobrindo destinos aceitos, estado desconhecido e ausência de estado.
- 308 verificações instrumentadas no Android API 34 passaram.
- Build assinado e instalação no emulador concluídos; release gate aprovado.
- 300 respostas locais de mercado, PTAX e histórico SGS processadas em 1.462 ms.
- `git diff --check` sem erros.
