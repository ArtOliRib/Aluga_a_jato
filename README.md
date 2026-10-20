# genericapi — API de Aluguel de Carros

API REST de aluguel de carros em **Spring Boot 4 / Java 25**, com foco em
**métricas de aluguel e frota**. Os dados da pessoa (nome, CPF, e-mail,
telefone, CNH) são enviados direto no registro do aluguel e guardados de forma
protegida (cifra AES-256-GCM + índice HMAC), conforme a LGPD.

> **Nota de escopo:** o cadastro de clientes foi removido (ver
> `docs/adr/0003-aluguel-guarda-dados-pessoais.md`). Não existem mais endpoints
> `/api/v1/clientes`. Os dados da pessoa vão no `POST /api/v1/alugueis`.

---

## Requisitos

- **JDK 25**
- **Docker** (para o Postgres, opcional se você já tem um Postgres local)
- O projeto usa o **Gradle Wrapper** (`./gradlew`), não precisa instalar Gradle.

---

## Como rodar

### 1. Variáveis de ambiente obrigatórias (LGPD)

A aplicação **não sobe sem as chaves** de proteção de dados pessoais — isso é
intencional. Gere duas chaves de 32 bytes:

```bash
openssl rand -base64 32   # -> LOCADORA_DADO_PESSOAL_INDICE_CHAVE
openssl rand -base64 32   # -> LOCADORA_DADO_PESSOAL_CIFRA_CHAVE
```

Copie o arquivo de exemplo e preencha as chaves:

```bash
cp .env.exemplo .env
```

O `.env.exemplo` lista todas as variáveis com instruções; o `.env` real é
ignorado pelo git (`.gitignore`) e **nunca** deve ser versionado.

| Variável | Descrição |
|---|---|
| `LOCADORA_DADO_PESSOAL_INDICE_CHAVE` | **Obrigatória.** Chave HMAC-SHA-256 (índices de busca/igualdade) |
| `LOCADORA_DADO_PESSOAL_CIFRA_CHAVE` | **Obrigatória.** Chave AES-256-GCM (cifra dos dados pessoais) |
| `SPRING_DATASOURCE_URL` | (opcional) URL do Postgres. Padrão: `jdbc:postgresql://localhost:5432/meubanco` |
| `SPRING_DATASOURCE_USERNAME` | (opcional) Padrão: `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | (opcional) Padrão: `secretpassword` |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | (opcional) Padrão: `update` |
| `SPRING_JPA_SHOW_SQL` | (opcional) Padrão: `false` |
| `SERVER_PORT` | (opcional) Padrão: `8080` |

> ⚠️ Perder a chave de **cifra** torna os dados pessoais definitivamente
> ilegíveis. Em produção, guarde em um secret manager (Vault, AWS Secrets
> Manager, etc.). Chave de teste nunca deve ser usada em produção.

### 2. Subir o banco (Docker)

```bash
docker compose up -d aluguel-db
```

Isso sobe um Postgres 16 em `localhost:5432` (banco `meubanco`) no container
`aluguel-db`.

### 3. Subir a aplicação

Com as variáveis exportadas:

```bash
./gradlew bootRun
```

A API sobe em `http://localhost:8080`.

### Alternativa: tudo via Docker Compose

A stack inteira (API + Postgres) está em `docker-compose.yml`, com o projeto
(`name`) e os recursos prefixados por `aluguel`:

| Recurso | Nome |
|---|---|
| Stack (compose project) | `aluguel` |
| Imagem da API | `aluguel-api` |
| Container da API | `aluguel-api` |
| Container do banco | `aluguel-db` |
| Volume do Postgres | `aluguel_postgres-data` |
| Rede | `aluguel_app-network` |

```bash
# as duas chaves precisam estar no ambiente ou num .env (veja .env.exemplo)
docker compose up --build -d       # constroi a imagem e sobe tudo em segundo plano
docker compose ps                  # status dos containers
docker compose logs -f aluguel-api # acompanha o log da API
docker compose down                # derruba (mantem o volume/dados)
docker compose down -v             # derruba e apaga os dados
```

> A imagem é construída com `./gradlew bootJar -x test` — os testes **não** rodam
> durante o build da imagem. Rode-os com `./gradlew test` (fora do Docker).
> A API roda como usuário **não-root** (`app`) dentro do container.

> O compose usa `${LOCADORA_...:?mensagem}` nas chaves LGPD, então até um
> `docker compose down` exige que elas estejam definidas no ambiente ou no
> `.env`. Isso é intencional: evita subir a stack sem as chaves.

---

## Como rodar os testes

Os testes usam **H2 em memória** (sem Postgres e sem chaves externas — as chaves
de teste já estão em `src/test/resources/application.properties`).

```bash
./gradlew test
```

---

## Testando a API (exemplos com curl)

### 1. Cadastrar um carro

```bash
curl -i -X POST http://localhost:8080/api/v1/carros \
  -H "Content-Type: application/json" \
  -d '{
    "nome": "Uno",
    "marca": "Fiat",
    "modelo": "Uno Vivace",
    "ano": 2022,
    "placa": "XYZ9K88",
    "quilometragem": 10000,
    "cor": "Prata",
    "categoria": "Economico",
    "valorDiaria": 150.00
  }'
```

Resposta `201 Created` com header `Location: /api/v1/carros/{id}`. Guarde o `id`.

### 2. Registrar um aluguel (dados da pessoa vão aqui)

```bash
curl -i -X POST http://localhost:8080/api/v1/alugueis \
  -H "Content-Type: application/json" \
  -d '{
    "nome": "Joao Lima",
    "cpf": "529.982.247-25",
    "email": "joao.lima@email.com",
    "telefone": "11988887777",
    "cnh": "CNH12345",
    "carroId": "<COLE_O_ID_DO_CARRO>",
    "dataPrevistaDevolucao": "2026-10-27T10:00:00",
    "observacoes": "Cliente pediu cadeirinha"
  }'
```

Campos opcionais do aluguel: `dataInicio` (padrão: agora),
`quilometragemInicial` (padrão: do carro), `valorDiaria` (padrão: do carro),
`observacoes`. Guarde o `id` do aluguel.

### 3. Consultar / devolver / pagar / cancelar

```bash
# consultar aluguel
curl http://localhost:8080/api/v1/alugueis/<ALUGUEL_ID>

# registrar devolucao
curl -i -X PATCH http://localhost:8080/api/v1/alugueis/<ALUGUEL_ID>/devolucao \
  -H "Content-Type: application/json" \
  -d '{"dataDevolucao": "2026-10-27T10:00:00", "quilometragemFinal": 10250}'

# registrar pagamento (somente apos devolucao)
curl -i -X PATCH http://localhost:8080/api/v1/alugueis/<ALUGUEL_ID>/pagamento

# cancelar
curl -i -X PATCH http://localhost:8080/api/v1/alugueis/<ALUGUEL_ID>/cancelamento \
  -H "Content-Type: application/json" \
  -d '{"motivo": "cliente desistiu"}'
```

### 4. Relatórios (métricas)

```bash
# movimentacao financeira do periodo (datas ISO-8601)
curl "http://localhost:8080/api/v1/relatorios/movimentacao?de=2026-10-01T00:00:00&ate=2026-10-31T23:59:59"

# movimentacao por carro
curl "http://localhost:8080/api/v1/relatorios/movimentacao-por-carro?de=2026-10-01T00:00:00&ate=2026-10-31T23:59:59"
```

---

## Endpoints

### Carros — `/api/v1/carros`

| Método | Rota | Descrição |
|---|---|---|
| POST | `/` | Cadastra carro (`201` + `Location`) |
| GET | `/{id}` | Busca por id |
| GET | `/por-placa/{placa}` | Busca por placa (ignora formatação) |
| GET | `/` | Lista paginada (filtros: `marca`, `status`, `pagina`, `tamanho`, `ordenarPor`, `direcao`) |
| GET | `/disponiveis` | Lista carros disponíveis |
| PUT | `/{id}` | Atualiza dados cadastrais |
| PATCH | `/{id}/status` | Altera status (depende do status atual) |
| PUT | `/{id}/placa` | Altera placa |
| PUT | `/{id}/quilometragem` | Registra quilometragem (só cresce) |
| DELETE | `/{id}` | Remove (`204`; bloqueado se houver aluguel) |

### Aluguéis — `/api/v1/alugueis`

| Método | Rota | Descrição |
|---|---|---|
| POST | `/` | Registra aluguel com os dados da pessoa (`201` + `Location`) |
| GET | `/{id}` | Busca por id |
| GET | `/` | Lista paginada (filtros: `carroId`, `status`) |
| PATCH | `/{id}/devolucao` | Registra devolução |
| PATCH | `/{id}/pagamento` | Registra pagamento |
| PATCH | `/{id}/cancelamento` | Cancela o aluguel |

### Relatórios — `/api/v1/relatorios`

| Método | Rota | Descrição |
|---|---|---|
| GET | `/movimentacao` | Fechamento financeiro do período (`de`, `ate`, opcional `carroId`) |
| GET | `/movimentacao-por-carro` | Movimentação agregada por carro (`de`, `ate`) |

---

## Erros

As respostas de erro seguem **ProblemDetail (RFC 7807)**:

| Status | Quando |
|---|---|
| `400` | Corpo/parâmetro inválido (Bean Validation) — inclui lista `erros` |
| `404` | Recurso não encontrado |
| `422` | Regra de negócio violada (ex.: carro já alugado, placa repetida, km retrocede) |
| `409` | Conflito de integridade de dados |
| `500` | Erro inesperado |

> ⚠️ **Pendência conhecida (ver `docs/HANDOFF_FRONTEND.md`):** `Content-Type`
> diferente de `application/json` e rotas inexistentes retornam **500** hoje,
> quando deveriam retornar **415** e **404**. Já cadastrado como tarefa de
> correção antes da entrega ao front-end.

---

## Modelo de dados (resumo)

- **`carros`**: dados do veículo. Placa é dado pessoal → `placa_indice` (HMAC,
  único) + `placa_cifrada` (AES-GCM).
- **`alugueis`**: vínculo carro × dados da pessoa no período. Guarda o histórico
  de locomoção e os dados pessoais:
  - Cifrados: `nome_cifrado`, `cpf_cifrado`, `email_cifrado`,
    `telefone_cifrado`, `cnh_cifrado`, `observacoes_cifradas`.
  - Índices HMAC: `nome_indice`, `cpf_indice`, `cnh_indice`.
  - Em claro (para relatórios filtrar/somar): datas, `quilometragem*`, `valor_*`,
    `status`, `status_pagamento`.
  - **CPF pode se repetir** (a mesma pessoa pode ter vários aluguéis).

---

## Estrutura do projeto

```
src/main/java/org/arthur/genericapi/
├── controller/   # AluguelController, CarroController, RelatorioController
├── service/      # regras de negócio
├── repository/   # Spring Data JPA
├── model/        # entidades (Carro, Aluguel, Status*)
├── dto/          # records de request/response (borda)
├── exception/    # GlobalExceptionHandler (ProblemDetail)
└── support/      # Normalizador, Ordenacao, ProtecaoDeDadoPessoal
```

Arquitetura em camadas: **Controller → Service → Repository**, com DTOs na borda.
`spring.jpa.open-in-view=false`.

---

## Documentação do projeto

- `CONTEXT.md` — visão geral, restrições e convenções
- `docs/HANDOFF_FRONTEND.md` — **handoff para o front-end**: o que está pronto,
  contrato REST detalhado, exemplos e o que ainda falta
- `docs/adr/` — decisões de arquitetura (ADR-0001 simplicidade didática,
  ADR-0002 padrão de respostas, ADR-0003 dados pessoais no aluguel)
- `docs/PLANO_PROXIMOS_PASSOS.md` — plano de refatoração
- `AGENTS.md` — guias para agentes/contribuição

---

## Stack

- Spring Boot 4.1.1 (Web MVC, Data JPA, Validation)
- Java 25
- PostgreSQL 16 (runtime) / H2 (testes)
- Lombok, Gradle
