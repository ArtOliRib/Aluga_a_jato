# ADR-0001: Priorizar simplicidade didatica sobre abstracoes genericas

## Status
Aceito

## Contexto
O projeto visa ser didatico para iniciantes em Spring Boot. Abstracoes prematuras (generic mappers, base controllers) aumentam curva de aprendizado e dificultam leitura.

## Decisao
Priorizar codigo explicito, direto e facil de seguir. So extrair quando houver duplicacao real percebida e o ganho for claro. Manter camadas Controller→Service→Repository e DTOs na borda.

## Consequencias
- Legibilidade para iniciantes
- Menor complexidade desnecessaria
- Refatoracoes incrementais sem quebrar contratos
- Decisoes adiadas ate evidencia concreta

## Alternativas consideradas
- Criar mappers genericos -> rejeitado (obscurece fluxo)
- Usar base classes -> rejeitado (heranca dificil de seguir)