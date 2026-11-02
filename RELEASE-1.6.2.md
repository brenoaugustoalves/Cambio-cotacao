# Câmbio Claro 1.6.2 — diagnóstico compartilhável

- O diagnóstico local agora registra falhas recuperáveis das consultas de PTAX, mercado, histórico, turismo, ações e atualizações do widget, além dos crashes Java não tratados.
- A tela Diagnóstico permite compartilhar o relatório com o seletor do Android, abrir um e-mail endereçado ao suporte, copiar o texto e apagar o registro.
- O relatório continua privado até o usuário escolher uma ação. A mensagem avisa que dados técnicos devem ser revisados; endereços web e credenciais em mensagens de erro são ocultados.
- O e-mail abre como rascunho para `brenoaugustoalves@gmail.com`. O usuário revisa e decide se envia; o app não transmite relatórios em segundo plano.
- `versionCode=12`, `versionName=1.6.2`, mesma assinatura local para atualizar a instalação anterior.

O diagnóstico registra apenas a falha mais recente, em armazenamento privado e com limite de tamanho. Não captura encerramentos forçados pelo Android sem exceção Java nem erros anteriores à instalação desta versão. Reveja o texto antes de compartilhá-lo.
