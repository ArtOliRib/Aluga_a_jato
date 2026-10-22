# Tickets: Refatoracao Controllers + DTOs (didatica)

Refatorar Controllers e DTOs para deixa—s mais explicitos, menos verbosos e mais didaticos para iniciantes em Spring Boot, sem quebrar contratos REST nem regras de LGPD. Fonte: .scratch/refatoracao—spring—clean/spec.md

Trabalhar pela fronteira: qualquer ticket com todos os blockers concluidos.

## T01 - Padronizar respostas dos controllers (claridade didatica)

**O que construir:** Tornar os retornos dos controllers mais consistentes e explicitos (decidir e aplicar padrao unico: sempre ResponseEntity<T> ou padrao uniforme por verbo HTTP), reduzindo "magica" e facilitando o entendimento para iniciantes. Preservar contratos (codigos HTTP, corpo, Location em POST).

**Bloqueado por:** Nenhum - pode iniciar imediatamente.

- [x] Definir padrao de retorno unico e didatico
- [x] Aplicar padrao em CarroController
- [x] Aplicar padrao nos demais controllers
- [x] Manter Location correto em POSTs
- [x] Testes de API existentes continuam passando

## T02 - Simplificar criacao de URI no POST (remover boilerplate avancado)

**O que construir:** Substituir ServletUriComponentsBuilder por forma mais explicita e didatica para construir Location, sem alterar contrato REST.

**Bloqueado por:** T01

- [x] Simplificar construcao de URI em CarroController.cadastrar
- [x] Aplicar abordagem didatica nos demais controllers com POST
- [x] Garantir Location com {id} correto e testes passando
- [x] Codigo permanece explicito e facil de ler linha a linha

## T03 - Remover comentarios redundantes e melhorar nomes didaticos

**O que construir:** Remover comentarios obvios trocando por nomes claros ou movendo explicacoes relevantes para README/CONTEXT.md, preservando explicacoes de regra/LGPD.

**Bloqueado por:** T01

- [x] Limpar comentarios redundantes em controllers
- [x] Ajustar nomes para ficarem autoexplicativos quando trivial
- [x] Preservar explicacoes relevantes de LGPD/regras complexas

## T04 - Padronizar DTOs (validacoes e consistencia)

**O que construir:** Revisar DTOs (records) uniformizando Bean Validation, mensagens claras, evitar campos superfluos, garantir consistencia Request/Response, preservando contratos e LGPD.

**Bloqueado por:** Nenhum - pode iniciar imediatamente.

- [x] Revisar dto/carro/*
- [x] Revisar dto/cliente/* (removido - ja migrado)
- [x] Revisar dto/aluguel/*
- [x] Revisar dto/relatorio/* e PageResponse
- [x] Manter records, simplificar anotacoes redundantes, mensagens didaticas
- [x] Testes existentes continuam passando

## T05 - Revisao integrada Controllers+DTOs (refatoracao incremental, didatica)

**O que construir:** Revisao integrada com foco didatico: legibilidade linha-a-linha, imports limpos, fluxo explicito, sem abstracoes genericas. Pequenos passos. Verificar contratos REST/LGPD.

**Bloqueado por:** T02, T03, T04

- [x] Revisao por controller
- [x] Imports organizados, sem codigo morto
- [x] Fluxo explicito (sem magicas nao explicadas)
- [x] Rodar ./gradlew test e garantir todos passando
-