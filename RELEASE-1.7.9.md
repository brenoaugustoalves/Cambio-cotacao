# Câmbio Claro 1.7.9

## Gráfico por período

- Adiciona seletores `1D`, `5D`, `1M` e `1A` ao gráfico de cotação de mercado.
- Mantém até 5.000 observações reais por moeda durante no máximo 365 dias no armazenamento privado local.
- Mescla observações da AwesomeAPI sem interpolar valores, mantém timestamp em milissegundos quando fornecido pela fonte e deduplica o mesmo instante.
- Exibe quantos pontos e qual intervalo de tempo estão disponíveis. O histórico se acumula a partir da instalação/uso; não promete dados retroativos ausentes.
- Em período histórico, o eixo termina na última publicação. O USD turismo não usa valores comerciais como substituição para períodos longos.

## Alertas de mercado

- Adiciona alertas de `ask` AwesomeAPI acima/abaixo do limite, independentes dos alertas PTAX.
- Um alerta dispara uma vez por moeda e pode ser rearmado ao salvá-lo novamente.
- Requer uma cotação com no máximo 10 minutos e permissão de notificações.
- Em segundo plano depende do JobScheduler e das condições impostas pelo Android; a UI explica que não é streaming nem garantia em tempo real.
- Alerta de mercado consulta só cotações do mercado e não força consulta PTAX sem necessidade.

## Versão e instalação

- `versionCode=22`, `versionName=1.7.9`.
- O aplicativo continua local; nada foi publicado na Google Play.
- APK assinado com a mesma chave local da instalação anterior.

Ver [README.md](README.md) para arquitetura, fontes, privacidade, build e uso dos recursos.
