# Testes — histórico, navegação e atualizador 1.8.0

## Casos adicionados

- Criação do schema SQLite e banco de teste isolado do armazenamento do usuário.
- Migração de histórico JSON legado preservando timestamp e casas decimais.
- Consulta por moeda/período, ordenação cronológica, transação e deduplicação da chave moeda + timestamp.
- Série de fechamentos diários isolada da série intradiária, para USD e EUR; URLs, parser, ordenação por timestamp e limite da API.
- Regras do gráfico para conectar observações reais consecutivas mesmo quando há intervalos longos, sem criar preços intermediários; datas nos períodos longos e resumo de variação reutilizado por USD/EUR.
- Navegação Moedas/Bolsa/Fontes, telas clara/escura, legibilidade e visibilidade de cada seção.
- Seletor independente do conversor para todas as moedas suportadas e separação do USD turismo.
- Navegação inferior, alvos de toque, estado da cotação e conteúdo da faixa de seleção do gráfico.
- Parsing das Releases GitHub estáveis, URL oficial, tamanho, versão, rejeição de host falso/pré-release e componentes privados.
- Regressão de alertas, widgets, caches, fontes, permissões e validações de segurança.

## Execução

```powershell
python verify.py --emulator emulator-5556
```

## Resultado executado

- `python verify.py`: passou; APK compilado e assinado, 29 testes de domínio passaram.
- `python verify.py --emulator emulator-5556`: passou; 308 verificações instrumentadas Android passaram em API 34.
- Carga local dos parsers: 250 respostas de mercado, 25 PTAX e 25 SGS em 1.881 ms.
- Verificação de assinatura APK v2/v3 e alinhamento: passou.
- O emulador foi usado como ambiente descartável; nenhum aparelho físico foi instalado nem atualizado por este teste.

A instrumentação cria um banco temporário com nome próprio e o remove ao terminar, sem alterar as cotações do usuário. Os testes de parser do atualizador usam fixtures e não baixam/instalam uma Release real.
