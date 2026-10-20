# Refatoracao: tornar menos verboso, clean-code, amigavel para iniciantes, facil de testar API

## Objetivo
Reduzir verbosidade, melhorar legibilidade (Clean Code), facilitar aprendizado de Spring Boot e simplificar o inicio de testes de acesso a API/operacoes, preservando regras de negocio e LGPD (cifragem de dados pessoais).

## Escopo inicial
Foco 1: Controllers + DTOs
- Simplificar verbosidade (records, padroes, mapeamentos, validacoes)
- Tornar endpoints mais autoexplicativos e consistentes
- Facilitar testar via curl/Postman/HTTPie sem complicacao
- Manter contratos existentes quando possivel; documentar mudancas

## Prioridades
1. Controllers/DTOs (inicio)
2. Services/Model
3. Infra/Config (H2, application*.properties, build)
4. Documentacao/README + como rodar + como testar API

## Requisitos nao funcionais
- Preservar testes existentes (nao quebrar sem justificativa e com cobertura)
- Manter LGPD: nao remover logica de protecao de dados pessoais
- API REST consistente (status HTTP corretos, Location em creates)
- Facilidade para rodar com ./gradlew bootRun e testes com H2

## Decisoes a capturar (ADRs)
Registrar escolhas que sao dificeis de reverter (ex: adocao de padroes, simplificacoes, convencoes de nomenclatura, remocao de boilerplate).

## Metricas de sucesso
- Menos linhas por classe, menos imports desnecessarios
- Controllers menores, responsabilidades claras
- README minimo e acionavel (como subir, como testar 1 endpoint)
- Testes de API ainda passam; facil rodar ./gradlew test