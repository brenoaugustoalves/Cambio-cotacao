# Câmbio Claro 1.1.2 — correção dos temas

Corrigido o problema mostrado no Motorola: o tema claro herdava texto escuro, mas o fundo da janela estava definido como escuro. A tela principal definia seu próprio fundo; preferências e diagnóstico não faziam isso, deixando a combinação ilegível. Era um erro do aplicativo.

## Alterações

- Temas com fundo, texto, texto secundário, dicas de preenchimento, cores dos controles e barras do sistema coerentes.
- Cores explícitas nas telas de preferências e diagnóstico, nos seletores, campos e botões, compartilhadas pelo componente UiStyle.
- Desativada a conversão automática de cores pelo Force Dark nas superfícies controladas pelo app.
- Ajustado o contraste dos rótulos da comparação de moedas. Tabelas e diálogos utilizam o tema selecionado.
- Widget acompanha a aparência escolhida, incluindo fundo, textos, variação e linha do gráfico.
- **Tema escuro selecionado uma vez ao abrir esta atualização**, conforme solicitado. Depois disso, a escolha do usuário é preservada. As opções são Escuro e Claro, em Preferências → Aparência.
- Diagnóstico local da versão 1.1.1 preservado e agora legível nos dois temas.

## Instalar

Instale `CambioClaro-1.1.2.apk` por cima da versão atual, sem desinstalar. O identificador e a assinatura são os mesmos. versionCode: 4. Não houve publicação na Google Play.

## Verificações

- 21 verificações Java de domínio e 135 verificações Android passaram.
- Os testes abriram telas reais de preferências e diagnóstico nos dois temas, e renderizaram RemoteViews do widget. Foi calculado contraste mínimo de 4,5:1 para os textos habilitados e dicas de preenchimento encontrados nessas superfícies, usando as cores efetivas dos controles e de seus fundos. Isso não equivale a uma auditoria completa de acessibilidade de todas as telas ou de todos os aparelhos.
- 17 cenários de navegação/aparência passaram: tema escuro inicial; preferências, menu de moedas, aviso de limite inválido, diagnóstico, tabela, widget e configuração nos dois temas; persistência da escolha; restauração do tema escuro e ausência de crash durante esse percurso.
- APK universal instalado no emulador Android 17; assinatura e AAB verificados. Uma condição de corrida no teste de notificações foi corrigida: o teste agora espera o processamento da notificação pelo Android em vez de exigir que seja síncrono. Não foi uma mudança no funcionamento dos alertas do aplicativo.

As capturas desta entrega mostram a execução real no emulador. Os testes automatizados não substituem a confirmação no Motorola. O encerramento relatado anteriormente ao tocar no widget ainda depende do diagnóstico local do aparelho e não é declarado resolvido por esta correção visual.

Se o widget ainda encerrar o app: abra pelo ícone normal, toque em **Diagnóstico do widget / falhas → Copiar diagnóstico** e cole o texto na conversa. Nada é enviado automaticamente.

## Próximas prioridades de segurança discutidas

Não fazem parte desta correção visual: mascaramento adicional de dados potencialmente sensíveis nas mensagens de diagnóstico; ampliação dos testes de parâmetros externos dos widgets; limites adicionais de repetição de consultas; backup portátil criptografado da chave. A versão mantém as proteções já existentes de HTTPS, dados privados, assinatura, ausência de depuração e permissões limitadas. Não há garantia de ausência de toda vulnerabilidade.
