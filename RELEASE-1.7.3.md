# Câmbio Claro 1.7.3 — comparação histórica

## Parte 4: IPCA e meta Selic por competência mensal

- O painel econômico exibe até 12 competências recentes com as observações disponíveis das séries oficiais SGS 433 (IPCA mensal) e SGS 432 (meta Selic).
- A taxa Selic diária é resumida pela última observação disponível no mês. O IPCA mantém o registro mensal da fonte.
- A interface mantém as medidas em linhas separadas, com unidade e data de cada observação. Não normaliza percentuais, não interpola ausências e não mistura IPCA mensal com Selic anualizada numa mesma escala.
- Cada série tem cache independente; a falha de uma não elimina a outra. Cache salvo mostra horário de consulta e status.
- Consultas HTTPS limitadas a dois endpoints fixos do BCB, com timeout, corpo limitado a 256 KiB, 400 observações no máximo, validação de datas e valores e rejeição de série desconhecida.
- `versionCode=16`, `versionName=1.7.3`; assinatura local mantida.
