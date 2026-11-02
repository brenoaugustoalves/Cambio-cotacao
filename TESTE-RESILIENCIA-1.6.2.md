# Verificação do diagnóstico — Câmbio Claro 1.6.2

Verificações preparadas para esta alteração:

- O relatório remove endereços web e texto com credenciais antes de ser salvo.
- A intenção de e-mail usa o endereço de suporte e carrega o relatório no corpo do rascunho.
- O compartilhamento genérico usa texto simples e não faz envio de rede.
- O diagnóstico continua uma activity privada e o registro permanece local até a escolha do usuário.

Resultado nesta máquina: `python verify.py` passou com 21 testes de domínio e compilou/assinou o APK. `python build_tests.py` compilou o APK de instrumentação. As verificações Android instrumentadas não foram executadas: o emulador ADB não ficou online e nenhum dispositivo físico estava conectado. Portanto, os fluxos de compartilhar e abrir e-mail ainda precisam de uma execução real em emulador ou celular. A matriz Android 8–17 e os testes de estresse existentes são referentes à versão 1.6.1; não são apresentados como validação desta nova versão. O Motorola físico também permanece pendente.
