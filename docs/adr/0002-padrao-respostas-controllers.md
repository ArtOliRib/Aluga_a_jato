# ADR-0002: Padrao hibrido de respostas nos controllers

## Status
Aceito

## Contexto
Os controllers precisam de um padrao unico e previsivel de resposta HTTP. O contrato REST ja esta publicado e coberto por testes, e o projeto e didatico para iniciantes em Spring Boot (ver ADR-0001), entao a escolha precisa ser explicita, facil de ler e sem "magica" de framework.

## Decisao
Manter o padrao hibrido ja adotado nos controllers (`CarroController`, `ClienteController`, `AluguelController` e `RelatorioController`):

- **POST** -> `ResponseEntity.created(...)` com header `Location` contendo URI relativa, construida de forma explicita com `URI.create("/api/v1/<recurso>/" + id)`.
- **DELETE** -> `@ResponseStatus(HttpStatus.NO_CONTENT)` em metodo `void`, sem corpo de resposta.
- **GET, PUT e PATCH** -> retorno direto do DTO de resposta. Inclui o `RelatorioController`, que e somente leitura e por isso nao tem POST/DELETE.

## Consequencias
- Os contratos REST existentes permanecem inalterados (metodos, status, headers e corpo).
- Clareza didatica: o comportamento de cada metodo HTTP e imediatamente legivel, alinhado ao ADR-0001.
- O caminho base fica duplicado entre `@RequestMapping` e `URI.create(...)`; aceito por ser explicito e por manter a `Location` independente do contexto da requisicao.
- Novos endpoints devem seguir este padrao; desvios sao tratados como inconsistencia de estilo e corrigidos.

## Alternativas consideradas
- `ServletUriComponentsBuilder.fromCurrentRequest()` para montar a `Location` -> rejeitado (esconde a origem da URI e depende do contexto do `HttpServletRequest`; indesejavel em codigo didatico)
- `ResponseEntity<T>` em todos os metodos, inclusive GET/PUT/PATCH -> rejeitado (verbosidade sem ganho; o retorno direto do DTO ja descreve o contrato)
- `ResponseEntity.noContent().build()` no DELETE em vez de `@ResponseStatus` -> rejeitado (exigiria assinatura `ResponseEntity<Void>` e embrulharia a chamada ao service sem ganho)
