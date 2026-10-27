# CONTEXT.md

## Visao geral
API de aluguel de carros (Spring Boot 4, Java 25). Dominio em PT-BR: Carro, Aluguel, Status*. O aluguel guarda os dados da pessoa (nome, cpf, email, telefone, cnh) cifrados + indice HMAC; foco em analise de metricas de aluguel/carro.

> **Mudanca de escopo (2026-10-20, ADR-0003):** o cadastro de clientes foi removido.
> Nao existem mais Cliente, TokenCliente, endpoints `/api/v1/clientes` nem
> relatorio por cliente. Os dados da pessoa vao no `POST /api/v1/alugueis`.

## Objetivo da refatoracao
Tornar o codigo menos verboso, Clean Code, amigavel a iniciantes em Spring Boot, e facil de iniciar testes de acesso a API/operacoes, preservando regras de negocio e LGPD.

## publico-alvo
- Desenvolvedores iniciantes em Spring Boot aprendendo REST
- Quem testa a API manualmente (exploracao/curl/Postman/HTTPie)
- Mantenedor(a) solo/time pequeno

## restricoes (nao quebrar)
- Contratos REST existentes (paths, status HTTP, estrutura de responses)
- LGPD: protecao de dados pessoais (cifragem/descrifragem), chaves via variaveis de ambiente (sem default)
- Testes existentes devem continuar passando (H2 em teste)
- Arquitetura em camadas: Controller -> Service -> Repository; DTOs na borda
- spring.jpa.open-in-view=false

## modelo atual (pos ADR-0003)
- `Aluguel` guarda nome/cpf/email/telefone/cnh cifrados (AES-GCM) + `nome_indice`/`cpf_indice`/`cnh_indice` (HMAC-SHA-256).
- CPF pode se repetir: sem constraint de unicidade.
- Datas, valores e quilometragem ficam em claro para permitir filtro/soma nos relatorios.
- Removidos: Cliente, TokenCliente, `dto/cliente/*`, `RelatorioClienteResponse`, filtro `clienteId`, endpoint `/relatorios/clientes/{id}`.

## convencoes
- Dominio em PT-BR (mantido por didatica)
- DTOs como records (Java 25)
- Validacao com Bean Validation
- Respostas paginadas via PageResponse
- Excecoes mapeadas em GlobalExceptionHandler

## Areas com oportunidade de reducao de verbosidade (inicial)
- Controllers: comentarios redundantes, padronizar retornos
- DTOs: consistencia de validacoes, evitar campos superfluos
- Config/docs: README minimo e acionavel para rodar/testar

## Didatica (meta-chave)
Objetivo: codigo explicito, legivel linha a linha para quem esta aprendendo Spring Boot. Reduzir "magica", evitar padroes avancados nao explicados, priorizar clareza sobre concisa agressiva.
