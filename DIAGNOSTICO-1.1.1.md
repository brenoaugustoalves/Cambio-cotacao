# Câmbio Claro 1.1.1 — compatibilidade e diagnóstico

Esta versão responde ao relato de encerramentos no Motorola Edge 60 Pro ao tocar na cotação ou na engrenagem do widget. **A causa específica no aparelho ainda não está confirmada.** Não foi possível reproduzir o encerramento no emulador Android 17, e o celular não está conectado por USB.

## O que mudou

- A aparência das barras do sistema é aplicada depois que a janela está anexada, verificando se o controlador existe. Foi removido o acesso sem verificação na inicialização das telas principal e de preferências. É uma correção preventiva para janelas que não disponibilizam o controlador imediatamente; não prova que esse era o motivo do erro no Motorola.
- O app agora registra localmente a última exceção Java não tratada, preservando o encerramento normal do Android. Não mascara falhas, não tenta reiniciar o aplicativo em ciclo e não envia informações automaticamente.
- O botão **Diagnóstico do widget / falhas**, na tela principal, abre um relatório privado com modelo do aparelho, versão do Android/API, versão do app, data, classe do erro e pilha de chamadas. É possível copiar ou apagar o relatório.

O arquivo é privado, com tamanho limitado. O relatório não lê o cache de cotações, contatos ou dados de outros aplicativos. Mensagens técnicas das exceções fazem parte do relatório e podem conter detalhes relacionados à operação que falhou; revise o texto antes de compartilhar. A política dentro do app explica esse registro. Não captura erros nativos, encerramentos pelo sistema sem exceção Java nem falhas de versões anteriores à instalação desta atualização.

## Como testar no Motorola

1. Instale `CambioClaro-1.1.1.apk` por cima da versão 1.1.0. O pacote e a assinatura permanecem os mesmos; versionCode passou de 2 para 3.
2. Abra o app pelo ícone normal e confirme que a tela principal abre.
3. Adicione novamente o widget e teste a cotação e a engrenagem.
4. Se o erro voltar, abra o app **pelo ícone normal**, toque em **Diagnóstico do widget / falhas → Copiar diagnóstico** e cole o texto nesta conversa.

Se não houver relatório mesmo após a falha, será necessário obter o registro do Android por USB ou detalhar a mensagem exibida. Se o widget continuar causando encerramentos, remova-o enquanto a investigação prossegue; isso não exige apagar os dados do aplicativo.

## Verificação realizada

- 21 verificações Java de domínio e 129 verificações Android passaram no APK universal da versão 1.1.1.
- Oito verificações de navegação/diagnóstico no emulador passaram: abrir cotação, abrir/configurar a engrenagem, repetir após solicitar encerramento do processo em segundo plano, abrir o diagnóstico e verificar ausência de crash inesperado nesse percurso.
- Uma exceção **intencional**, gerada exclusivamente pelo APK de testes, verificou o registro real após encerramento do processo. Ela não integra o APK entregue. O relatório capturado foi exibido após reiniciar o app, o botão de cópia foi acionado e o registro de teste foi apagado.
- APK e AAB foram assinados/validados. O certificado do APK foi comparado com o da versão 1.1.0.

Nada foi publicado na Google Play. O resultado no Motorola depende do novo teste no aparelho; esta entrega não declara o problema físico resolvido sem essa confirmação.
