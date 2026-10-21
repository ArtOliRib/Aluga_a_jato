# Handoff para o Front-end

**Data:** 2026-10-20
**Projeto:** `aluguel` (API de aluguel de carros) — Spring Boot 4 / Java 25
**Autores da sessão:** refatoração de domínio, conteinerização e validação end-to-end

> **Sessão encerrada (2026-10-20).** A stack Docker foi deixada rodando
> (`aluguel-api` + `aluguel-db`, ambos `healthy`) em `http://localhost:8080`.
> Os afazeres pendentes estão como checklist na seção 6.

Este documento consolida **o que foi feito hoje**, **o estado atual da API** e
**o que ainda falta** para que o back-end seja considerada pronto para o
front-end consumir.

---

## 1. Resumo executivo

A API está **funcional e rodando em Docker** (API + Postgres), com o fluxo
completo de cadastro de carro, registro de aluguel, devolução, pagamento,
cancelamento e relatórios já validado end-to-end contra um Postgres real.

O maior risco para o front-end hoje **não** é falta de endpoint: é **tratamento
de erro inconsistente** (alguns erros do cliente retornam `500` em vez de `4xx`)
e a **ausência de CORS**, que impede um app web em outro domínio/porta de chamar
a API. Ambos estão detalhados na seção 6.

**Testes:** `./gradlew clean test` → **53 testes, 0 falhas**.
**Containers:** `aluguel-api` e `aluguel-db`, ambos `healthy`.

---

## 2. O que foi feito hoje

### 2.1 Remoção do cadastro de clientes (ADR-0003)

Mudança de escopo: o sistema **deixou de ter cadastro de clientes**. Agora o
foco é registrar o aluguel e os dados da pessoa no próprio aluguel, para
análise de métricas.

- **Removidos:** `Cliente`, `TokenCliente`, `ClienteService`,
  `ClienteController`, `ClienteRepository`, `TokenClienteRepository`,
  `dto/cliente/*`, `RelatorioClienteResponse`, endpoints `/api/v1/clientes/**`,
  filtro `clienteId` e o relatório por cliente.
- **Novo `Aluguel`:** guarda `nome`, `cpf`, `email`, `telefone`, `cnh`
  **cifrados** (AES-256-GCM) + `nome_indice`/`cpf_indice`/`cnh_indice` (HMAC-SHA-256)
  para busca. **CPF pode se repetir.**
- **Contrato novo:** `POST /api/v1/alugueis` recebe os dados da pessoa; a
  resposta traz `pessoa` (`PessoaResumo`) em vez de `cliente`.
- Decisão registrada em `docs/adr/0003-aluguel-guarda-dados-pessoais.md`.

### 2.2 Conteinerização completa (Docker)

- **`Dockerfile`** multi-stage: build `eclipse-temurin:25-jdk-alpine` → runtime
  `eclipse-temurin:25-jre-alpine`, usuário **não-root** (`app`), labels OCI,
  `bootJar -x test` (testes **não** rodam no build), `HEALTHCHECK` por porta TCP.
- **`docker-compose.yml`** com `name: aluguel`, serviços `aluguel-api` e
  `aluguel-db`, healthchecks e `restart: unless-stopped`.
- **`.dockerignore`** para não enviar `.env`/segredos nem `docs/` à imagem.
- **Nomes padronizados** com `aluguel` como identificador master:

  | Recurso | Nome |
  |---|---|
  | Projeto Gradle / jar | `aluguel` / `aluguel-0.0.1-SNAPSHOT.jar` |
  | Stack (compose project) | `aluguel` |
  | Imagem | `aluguel-api` |
  | Containers | `aluguel-api`, `aluguel-db` |
  | Volume / rede | `aluguel_postgres-data`, `aluguel_app-network` |

### 2.3 Documentação e ambiente

- **`README.md`** criado/atualizado (acionável: como rodar, testar, exemplos).
- **`HELP.md`** (boilerplate do Spring Initializr) removido.
- **`.env.exemplo`** criado; `.env` real é ignorado pelo git.
- **`docs/RELATORIO_TESTES_FALHANDO.md`** marcado como resolvido.

### 2.4 Validação end-to-end (contra Postgres real, via Docker)

Fluxo exercitado com sucesso:

| Passo | Endpoint | Resultado |
|---|---|---|
| 1 | `POST /api/v1/carros` | `201 Created` |
| 2 | `POST /api/v1/alugueis` (com dados da pessoa) | `201 Created` |
| 3 | `GET /api/v1/alugueis/{id}` | `200` com dados decifrados |
| 4 | `PATCH /api/v1/alugueis/{id}/devolucao` | `200` (km e valor calculados) |
| 5 | `GET /api/v1/relatorios/movimentacao` | `200` (agregado financeiro) |

Verificações de proteção de dados (LGPD) no Postgres:
- **0** registros com dado pessoal em claro nas colunas `*_cifrado`.
- Índices HMAC válidos (`^[0-9a-f]{64}$`).
- Container da API roda como usuário não-root.

---

## 3. Estado atual — contrato REST para o front-end

Base URL local: `http://localhost:8080`
Content-Type esperado em POST/PUT/PATCH com corpo: **`application/json`**.

### 3.1 Carros — `/api/v1/carros`

| Método | Rota | Descrição |
|---|---|---|
| POST | `/` | Cadastra carro (`201` + header `Location`) |
| GET | `/{id}` | Busca por id |
| GET | `/por-placa/{placa}` | Busca por placa (ignora formatação) |
| GET | `/` | Lista paginada (filtros: `marca`, `status`, `pagina`, `tamanho`, `ordenarPor`, `direcao`) |
| GET | `/disponiveis` | Lista carros disponíveis |
| PUT | `/{id}` | Atualiza dados cadastrais |
| PATCH | `/{id}/status` | Altera status (transições válidas por regra) |
| PUT | `/{id}/placa` | Altera placa |
| PUT | `/{id}/quilometragem` | Registra quilometragem (só cresce) |
| DELETE | `/{id}` | Remove (`204`; bloqueado se houver aluguel) |

**Status possíveis do carro** (`StatusCarro`): `DISPONIVEL`, `ALUGADO`,
`MANUTENCAO`, `DESATIVADO`.
Transições permitidas: `DISPONIVEL → ALUGADO|MANUTENCAO|DESATIVADO`;
`ALUGADO → DISPONIVEL|MANUTENCAO`; `MANUTENCAO → DISPONIVEL|DESATIVADO`;
`DESATIVADO → (nenhuma)`.

**Payload de cadastro (`CarroRequest`):**

```json
{
  "nome": "Uno",
  "marca": "Fiat",
  "modelo": "Uno Vivace",
  "ano": 2022,
  "placa": "ABC1D23",
  "quilometragem": 10000,
  "cor": "Prata",
  "categoria": "Economico",
  "valorDiaria": 150.00
}
```

Regras de validação: `nome`/`marca`/`modelo`/`placa` obrigatórios; `ano ≥ 1900`;
`placa` com `^[0-9A-Za-z\-\s]{7,10}$`; `quilometragem ≥ 0`; `valorDiaria ≥ 0.01`.
**Placa é única** — repetir retorna `422`.

**Resposta (`CarroResponse`)** inclui `id`, `placa` (decifrada), `status`,
`disponivelParaAluguel`, `dataCadastro`, `dataAtualizacao`.

### 3.2 Aluguéis — `/api/v1/alugueis`

| Método | Rota | Descrição |
|---|---|---|
| POST | `/` | Registra aluguel com os dados da pessoa (`201` + `Location`) |
| GET | `/{id}` | Busca por id |
| GET | `/` | Lista paginada (filtros: `carroId`, `status`) |
| PATCH | `/{id}/devolucao` | Registra devolução |
| PATCH | `/{id}/pagamento` | Registra pagamento (somente após devolução) |
| PATCH | `/{id}/cancelamento` | Cancela o aluguel (corpo opcional) |

**Status do aluguel** (`StatusAluguel`): `ATIVO`, `CONCLUIDO`, `CANCELADO`.
**Status de pagamento** (`StatusPagamento`): `PENDENTE`, `PAGO`, `CANCELADO`.

**Payload de registro (`AluguelRequest`):**

```json
{
  "nome": "Joao Lima",
  "cpf": "529.982.247-25",
  "email": "joao.lima@email.com",
  "telefone": "11988887777",
  "cnh": "12345678901",
  "carroId": "<UUID do carro>",
  "dataPrevistaDevolucao": "2026-10-27T10:00:00",
  "observacoes": "Cliente pediu cadeirinha"
}
```

Campos opcionais: `dataInicio` (padrão: agora), `quilometragemInicial` (padrão:
do carro), `valorDiaria` (padrão: do carro), `observacoes`.

**Resposta (`AluguelResponse`)** — destaques:
- `pessoa`: `{ id, nome, cpf }` (id é o do próprio aluguel; dados decifrados).
- `carro`: `{ id, placa, marca, modelo, cor, categoria }`.
- `dataInicio`, `dataPrevistaDevolucao`, `dataDevolucao`, `quilometragem*`,
  `valorDiaria`, `totalDiarias`, `valorTotal`, `status`, `statusPagamento`,
  `atrasado`, `observacoes`, `dataCriacao`, `dataAtualizacao`.
- `observacoes` **não** aparece na listagem (`GET /`), só no detalhe, por ser
  texto livre cifrado.

**Devolução (`DevolucaoRequest`):**
```json
{ "dataDevolucao": "2026-10-27T10:00:00", "quilometragemFinal": 10250 }
```
(`dataDevolucao` opcional → agora; `quilometragemFinal` obrigatória.)

**Cancelamento (`CancelamentoRequest`):**
```json
{ "motivo": "cliente desistiu" }
```
(corpo inteiro é opcional.)

### 3.3 Relatórios — `/api/v1/relatorios`

| Método | Rota | Descrição |
|---|---|---|
| GET | `/movimentacao` | Fechamento financeiro do período (`de`, `ate`, opcional `carroId`) |
| GET | `/movimentacao-por-carro` | Ranking agregado por carro (`de`, `ate`) |

- `de` e `ate` obrigatórios, formato ISO-8601 (`2026-10-01T00:00:00`).
- Critério: aluguel aparece se **sobrepõe** o período. Cancelados não entram nas somas.
- `movimentacao` devolve: `totalAlugueis`, `totalAtivos`, `totalConcluidos`,
  `receitaTotal`, `receitaPaga`, `receitaPendente`, `ticketMedio`,
  `quilometragemPercorrida`.
- `movimentacao-por-carro` devolve, por carro: `totalAlugueis`, `receitaTotal`,
  `receitaPaga`, `receitaPendente`, `quilometragemPercorrida`, `percentualDaReceita`.

### 3.4 Paginação

`GET` de listagens devolvem `PageResponse`:

```json
{
  "conteudo": [ ... ],
  "pagina": 0,
  "tamanho": 20,
  "totalElementos": 1,
  "totalPaginas": 1,
  "primeira": true,
  "ultima": true
}
```

Parâmetros: `pagina` (0-based), `tamanho`, `ordenarPor`, `direcao` (`asc`/`desc`).
Campos permitidos para ordenação são restritos por endpoint (campos cifrados e
`observacoes` ficam de fora).

### 3.5 Erros

Formato **ProblemDetail (RFC 7807)**: `type`, `title`, `status`, `detail`,
`instance`; validação inclui `erros: [{ campo, mensagem }]`.

| Status | Quando |
|---|---|
| `400` | Corpo/parâmetro inválido (Bean Validation) |
| `404` | Recurso não encontrado |
| `422` | Regra de negócio violada (carro já alugado, placa repetida, km retrocede, pagamento antes da devolução, período inválido) |
| `409` | Conflito de integridade de dados |
| `500` | Erro inesperado |

---

## 4. Como rodar (para o time do front consumir)

```bash
# 1. chaves LGPD (obrigatórias) — o .env.exemplo tem instruções
cp .env.exemplo .env      # e preencha com: openssl rand -base64 32

# 2. sobe API + Postgres
docker compose up --build -d

# 3. a API responde em http://localhost:8080
docker compose ps
```

Testes (fora do Docker, usam H2):
```bash
./gradlew clean test
```

> A stack exige as duas chaves `LOCADORA_DADO_PESSOAL_*` no `.env`/ambiente; o
> compose falha cedo (mensagem clara) se faltarem.

---

## 5. Testes e qualidade

- **53 testes, 0 falhas** (`./gradlew clean test`).
- Cobertura inclui: regras de domínio (`AluguelTest`, `CarroTest`), fluxo REST
  completo (`FluxoDeAluguelApiTest`) e proteção LGPD
  (`AluguelMovimentacaoLGPDTest`, `CarroPlacaLGPDTest`).
- Sem referências a `Cliente`/`TokenCliente`/`clienteId` no código.

---

## 6. Afazeres (checklist priorizado)

> **Status da sessão:** encerrada em 2026-10-20. Stack Docker deixada **no ar**
> (`aluguel-api` e `aluguel-db`, ambos `healthy`) em `http://localhost:8080`.
> Nenhum item abaixo foi iniciado; são o ponto de partida da próxima sessão.

Legenda: `[ ]` a fazer · `[x]` feito · `[~]` em andamento · `[!]` bloqueado

### P0 — Bloqueadores diretos para integração (fazer primeiro)

- [ ] **P0-1. Configurar CORS.**
  Não há CORS. Um front em outra origem (ex.: `http://localhost:3000` ou `:5173`)
  não consegue chamar a API pelo navegador.
  *Ação:* adicionar `WebMvcConfigurer` (ou config de CORS) liberando as origens do
  front em dev; restringir a lista em produção.
  *Aceite:* requisição `fetch` do front local retorna `200` sem erro de CORS.

- [ ] **P0-2. Corrigir tratamento de erro (bug confirmado hoje).**
  - `Content-Type` ≠ `application/json` → hoje **500**; deveria ser **415**.
  - Rota inexistente (ex.: `GET /`) → hoje **500**; deveria ser **404**.
  *Causa:* `GlobalExceptionHandler` não trata `HttpMediaTypeNotSupportedException`
  nem `NoResourceFoundException`; ambos caem no `@ExceptionHandler(Exception.class)`.
  *Ação:* adicionar handlers → 415 e 404; ajustar/estender os testes.
  *Aceite:* testes cobrindo 415 e 404; suíte verde.

### P1 — Necessário para uma integração confiável

- [ ] **P1-3. Endpoint raiz / health.**
  Hoje `GET /` retorna erro. Adicionar `GET /` simples e/ou `/actuator/health`
  para o front checar se a API está no ar. (Resolver junto do P0-2.)

- [ ] **P1-4. Autenticação/autorização.**
  A API é **aberta** hoje. Definir estratégia (API key, JWT, etc.) se o front for
  multiusuário ou exposto publicamente.

- [ ] **P1-5. OpenAPI/Swagger.**
  Gerar contrato OpenAPI (ex.: `springdoc-openapi`) para o front gerar client HTTP
  e para documentar payloads automaticamente.

### P2 — Robustez / operação

- [ ] **P2-6. Validação de CPF/CNH reais** (dígito verificador). Avaliar necessidade.
- [ ] **P2-7. Mover credenciais do compose** (`postgres`/`secretpassword`) para
  variáveis/secret antes de ambiente compartilhado.
- [ ] **P2-8. Seed de dados** (carros + aluguéis de exemplo) para acelerar o front.
- [ ] **P2-9. Export de relatórios** (CSV), se o front precisar.
- [ ] **P2-10. Filtros extras / paginação por cursor** na listagem de aluguéis
  (por período, status de pagamento) — avaliar volume.

### P3 — Qualidade contínua

- [ ] **P3-11. CI** rodando `./gradlew test` e o build da imagem em PRs.
- [ ] **P3-12. Observabilidade** (Actuator + métricas) para ambiente com tráfego real.

### Concluído nesta sessão (referência)

- [x] Remoção do cadastro de clientes (ADR-0003) e novo modelo de aluguel.
- [x] Conteinerização completa (Dockerfile + compose, nomes `aluguel`).
- [x] `README.md` acionável + `.env.exemplo` + remoção do `HELP.md`.
- [x] Validação end-to-end contra Postgres real (fluxo completo).
- [x] Suíte verde: **53 testes, 0 falhas**.

---

## 7. Riscos e observações

- **Perder a chave de cifra (`LOCADORA_DADO_PESSOAL_CIFRA_CHAVE`) torna os dados
  pessoais definitivamente ilegíveis.** Em produção, usar secret manager.
- **CPF pode repetir** por decisão de projeto; o front não deve assumir
  unicidade de pessoa por CPF.
- **`observacoes` é texto livre cifrado** e não sai na listagem — só no detalhe.
- **Placa é única** e tratada como dado pessoal (índice HMAC + cifra).
- **Datas em claro** (não cifradas) por necessidade dos relatórios — o front deve
  estar ciente de que o histórico de locomoção fica associado ao aluguel.

---

## 8. Referências

- `README.md` — como rodar, testar e exemplos com `curl`.
- `CONTEXT.md` — visão geral, restrições e convenções.
- `docs/adr/0001-simplicidade-didatica-sobre-abstracoes.md`
- `docs/adr/0002-padrao-respostas-controllers.md`
- `docs/adr/0003-aluguel-guarda-dados-pessoais.md`
- `docs/PLANO_PROXIMOS_PASSOS.md`
- `docs/HISTORICO-testes-lgpd-2026-10.md`
