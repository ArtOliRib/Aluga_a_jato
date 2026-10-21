# Relatório: Testes LGPD falhando (4 de 69)

> **RESOLVIDO (2026-10-20):** por decisão do mantenedor, o cadastro de clientes foi
> removido (ADR-0003). `ClienteDadoPessoalLGPDTest` foi deletado (os dados pessoais
> agora vão no aluguel e são cobertos por `AluguelMovimentacaoLGPDTest`). Os demais
> testes foram corrigidos/adaptados. `./gradlew clean test` → **53 testes, 0 falhas**.
> O texto abaixo é o diagnóstico original, mantido como histórico.

**Projeto:** genericapi (API de Locação de Carros - Spring Boot 4, Java 25)  
**Data:** 2026-10-20  
**Origem:** Revisão integrada (T05) da refatoração Controllers + DTOs  
**Escopo do relatório:** descrever cada teste que falha, o que ele testa, quais métodos/comportamentos cobre e a causa provável — para revisão humana antes de qualquer correção.

---

## 0. Resumo da execução

```
> Task :test
69 tests completed, 4 failed
BUILD FAILED
```

Os 4 testes que falham pertencem às suítes de LGPD. Eles **não** exercitam Controllers/DTOs refatorados (esses passaram: `FluxoDeAluguelApiTest` = 15/15). Todos dependem de `Service`, `Support`, `Repository` ou do próprio teste.

| # | Classe | Teste | Tipo de falha |
|---|---|---|---|
| 1 | `AluguelMovimentacaoLGPDTest` | `observacoesOpcional` | `AssertionFailedError` (dado de teste inconsistente) |
| 2 | `CarroPlacaLGPDTest` | `alterarPlacaReprotege` | `AssertionError` (indice de placa não muda — teste/flush) |
| 3 | `ClienteDadoPessoalLGPDTest` | `emailNaoTemIndice` | `IncorrectResultSizeDataAccessException` (query de teste quebrada) |
| 4 | `ClienteDadoPessoalLGPDTest` | `duplicidadeDeEmailNaoVaza` | `AssertionError` (esperava 422, recebeu 201 — regra inexistente) |

---

## 1. `AluguelMovimentacaoLGPDTest.observacoesOpcional`

**Arquivo:** `src/test/java/org/arthur/genericapi/AluguelMovimentacaoLGPDTest.java` (linhas 131-143)

**O que o teste diz testar:** "aluguel **sem observações** deixa a coluna nula".

**Comportamento esperado (asserções):**
1. Após registrar aluguel **sem** observações, a coluna `observacoes_cifradas` no banco deve ser `NULL`.
2. `GET /api/v1/alugueis/{id}` deve responder 200 e o JSON **não** deve conter o campo `observacoes`.

**Métodos/produção envolvidos:**
- `AluguelController.registrar(POST /api/v1/alugueis)` → `AluguelService.registrar(AluguelRequest)` (linhas 74-77): grava `null` quando `observacoes` é nula ou em branco.
- `AluguelService.responder(Aluguel)` (linhas 175-177): devolve `null` se `observacoesCifradas` for `null`.

**Causa da falha (evidência real):**
```
expected: null
 but was: "pXG2KSpqxt0LlTYy9X1QPJ/ARkEI+pHuZzqtLULaeYrhM4HbtO3EXfXhvWy2FCbwHtO+PEau/ahJ+2exRbSwIiqq4HKcY7gaBbJteks="
```
O teste chama `registrarAluguel()` (linha 176-177), que por sua vez chama o auxiliar `criarAluguel(..., OBSERVACOES)` **sempre passando a constante `OBSERVACOES`**. Ou seja, o teste **não cria** um aluguel sem observações — cria **com** observações e depois espera a coluna nula.

**Diagnóstico:** **Bug no próprio teste.** O auxiliar `registrarAluguel()` deveria aceitar/criar sem observações (ex.: `criarAluguel(clienteId, carroId, null)`). A produção está correta: como o aluguel foi criado com observações, a coluna ficou cifrada.

**Observação:** Contradição interna — a asserção 2 (`doesNotExist()`) também nunca seria satisfeita, pois o serviço devolveria o texto decifrado.

---

## 2. `CarroPlacaLGPDTest.alterarPlacaReprotege`

**Arquivo:** `src/test/java/org/arthur/genericapi/CarroPlacaLGPDTest.java` (linhas 116-142)

**O que o teste diz testar:** "alterar a placa substitui índice e cifra juntos".

**Comportamento esperado (asserções):**
1. `PUT /api/v1/carros/{id}/placa` responde 200 e devolve a nova placa `ZZZ9Z99`.
2. O `placa_indice` no banco **deve mudar** (índice antigo não sobrevive).
3. `placa_cifrada` **não** deve conter `ZZZ9Z99` nem a placa antiga.
4. `GET /api/v1/carros/por-placa/{PLACA_ANTIGA}` deve responder 404.

**Métodos/produção envolvidos:**
- `CarroController.alterarPlaca(PUT /{id}/placa)` → `CarroService.alterarPlaca(UUID, String)` (linhas 108-120): gera novo índice, valida duplicidade, chama `carro.alterarPlaca(novaPlacaIndice, protecao.cifrar(novaPlaca))` e `save`.
- `Carro.alterarPlaca(...)` (modelo).
- `CarroRepository.findByPlacaIndice(String)`.

**Causa da falha (evidência real):**
```
[o indice antigo nao pode sobreviver a troca]
Expecting actual: "74fbbdaadafcef56f4d8e1f4e76326f32584e441f276fbfcfbaea30a6b56b200"
not to be equal to: "74fbbdaadafcef56f4d8e1f4e76326f32584e441f276fbfcfbaea30a6b56b200"
```
O `placa_indice` lido por JDBC antes e depois da troca é **idêntico**. O teste lê o índice antigo (linha 120-121) logo após criar o carro, mas como a criação/lookup acontecem dentro da mesma transação `@Transactional`, a leitura via `JdbcTemplate` (conexão JDBC separada) pode não enxergar o estado pendente do `EntityManager` — e/ou a mudança feita pelo service não foi "flushada" para a conexão JDBC antes da leitura.

**Diagnóstico:** **Falha de visibilidade transação/JDBC no teste** (o test usa `em.flush()` no auxiliar de criação, mas o `PUT` do service + a leitura JDBC seguinte disputam o flush). O comportamento da produção provavelmente está correto (o índice é regenerado a partir da nova placa por HMAC). Precisa ser confirmado com um `flush()` explícito após o `PUT` (linha ~131) ou reformulação da asserção. **Não é regressão da refatoração** (que só tocou controllers/DTOs).

**Ponto de atenção:** vale checar se o `PUT` de placa realmente persiste o novo índice, ou se há algum cache/save que não altera o índice — mas a evidência aponta primeiro para sincronização de flush/transação.

---

## 3. `ClienteDadoPessoalLGPDTest.emailNaoTemIndice`

**Arquivo:** `src/test/java/org/arthur/genericapi/ClienteDadoPessoalLGPDTest.java` (linhas 239-250)

**O que o teste diz testar:** "o índice do e-mail não é persistido, então não há coluna com o índice dele".

**Comportamento esperado (asserções):**
1. Consultar `information_schema.columns` da tabela `clientes`.
2. A lista de colunas **não** deve conter `email_indice`.
3. Deve conter `email_cifrado`.

**Métodos/produção envolvidos:**
- Modelo `Cliente` / mapeamento JPA da coluna de e-mail (existe apenas `email_cifrado`, sem `email_indice`).
- `ClienteService.cadastrar` — usa `protecao.cifrar(request.email())` (linhas 59), **sem** gerar índice para e-mail (correto: e-mail não é pesquisável/único por índice).

**Causa da falha (evidência real):**
```
IncorrectResultSizeDataAccessException: Incorrect result size: expected 1, actual 11
```
O `jdbc.queryForMap(...)` (linhas 242-245) retorna **todas as colunas** da tabela `clientes` e o `queryForMap` espera **1 única linha**. Como a consulta a `information_schema.columns where table_name='clientes'` retorna **11 linhas** (uma por coluna), o `queryForMap` não é o método adequado — ele lança exceção antes mesmo das asserções.

**Diagnóstico:** **Bug no próprio teste** (uso incorreto de `queryForMap` em vez de `queryForList`). A intenção é verificar a presença/ausência de nomes de colunas; deveria usar `queryForList(..., String.class)` e checar a coleção. A produção está adequada (não existe `email_indice`).

---

## 4. `ClienteDadoPessoalLGPDTest.duplicidadeDeEmailNaoVaza`

**Arquivo:** `src/test/java/org/arthur/genericapi/ClienteDadoPessoalLGPDTest.java` (linhas 200-213)

**O que o teste diz testar:** "email duplicado responde 422 sem ecoar o endereço".

**Comportamento esperado (asserções):**
1. Cadastrar um cliente com `EMAIL`.
2. Tentar cadastrar **outro** cliente (CPF/CNH diferentes) com o **mesmo e-mail**.
3. Espera-se HTTP **422 (Unprocessable Entity)**.
4. A mensagem `detail` **não** deve conter o e-mail (evitar oráculo de enumeração).

**Métodos/produção envolvidos:**
- `ClienteController.cadastrar` → `ClienteService.cadastrar(ClienteRequest)` (linhas 42-64).
- Validações de unicidade existentes: `existsByCpfIndice` (linha 49) e `existsByCnhIndice` (linha 52). **Não há verificação de unicidade de e-mail.**
- `ClienteRepository` (linhas 16-25): possui `findByCpfIndice`, `existsByCpfIndice`, `existsByCnhIndice`, `findByNomeIndice`. **Não há `existsByEmailIndice` nem índice de e-mail.**

**Causa da falha (evidência real):**
```
Status expected:<422> but was:<201>
```
Como não existe regra de unicidade para e-mail no domínio, o segundo cadastro é aceito e retorna **201**, não 422.

**Diagnóstico:** **Possível ausência de regra de negócio** (não é bug de teste no sentido estrito). Há duas interpretações:
- **(a)** O e-mail **não deveria** ser único por decisão de design. Então o **teste está errado** e deve ser removido/ajustado.
- **(b)** O e-mail **deveria** ser único (como CPF/CNH), e falta implementar: `email_indice` no modelo, `existsByEmailIndice` no repositório e a checagem no service (mantendo a mensagem sem ecoar o endereço).

**Observação relevante:** Esse teste entra em **conflito direto** com o teste nº 3 (`emailNaoTemIndice`), que **afirma que não existe coluna de índice de e-mail**. Ou seja, a suíte hoje se contradiz: um teste exige ausência de índice de e-mail e o outro exige a regra de unicidade que normalmente demandaria esse índice. **Esta ambiguidade precisa de decisão de produto antes de qualquer correção.**

**Sobre cifra/índice:** e-mail é cifrado com IV aleatório (`AES/GCM`), então não é comparável por igualdade no SQL. Implementar unicidade de e-mail exigiria **índice determinístico (HMAC)** — exatamente o que o teste nº 3 diz que não deve existir.

---

## 5. Conclusão e classificação

| Teste | Classificação | Exige mudança em produção/regra? |
|---|---|---|
| `observacoesOpcional` | Bug no teste (dado inconsistente) | Não |
| `alterarPlacaReprotege` | Bug de sincronização/flush no teste (a confirmar) | Provavelmente não |
| `emailNaoTemIndice` | Bug no teste (`queryForMap` incorreto) | Não |
| `duplicidadeDeEmailNaoVaza` | Ambiguidade de regra de negócio (unicidade de e-mail) | **Sim, se** o e-mail deve ser único |

**Nenhuma das 4 falhas foi causada pela refatoração de Controllers/DTOs** (T01–T04/T05 tocaram apenas controllers e DTOs; `FluxoDeAluguelApiTest` 15/15 e compilação OK).

**Pontos que exigem decisão humana:**
1. E-mail deve ser **único** no cadastro de clientes? (afeta o teste nº 4 e contradiz o nº 3)
2. Corrigir os testes nº 1, nº 2 e nº 3 (que são bugs de teste) — pode ser feito sem tocar em regra de negócio.

**Nada foi alterado.** Este relatório é apenas descritivo, aguardando sua revisão e decisão.
