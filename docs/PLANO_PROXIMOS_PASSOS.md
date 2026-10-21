# Plano de Próximos Passos - Refatoração Controllers + DTOs

> **ARQUIVO HISTÓRICO (encerrado em 2026-10-20).** As fases 1–5 abaixo foram
> **concluídas**. Este documento é o registro de como o trabalho foi planejado e
> executado, não uma lista de tarefas pendentes.
>
> Ele menciona `Cliente`, `ClienteController` e `TokenCliente` — arquivos que
> **não existem mais** (removidos na Fase 5, decisão registrada no ADR-0003).
> As referências são parte do histórico, não do estado atual do projeto.
>
> **Para o que falta hoje**, ver [`HANDOFF_FRONTEND.md`](HANDOFF_FRONTEND.md).

**Projeto:** genericapi (API de Locação de Carros - Spring Boot 4, Java 25)  
**Data:** 2026-10-20  
**Foco:** Refatoração Controllers + DTOs (didática, sem quebrar contratos/LGPD)

> **ATUALIZAÇÃO (2026-10-20):** O escopo mudou. Decisão do mantenedor: o sistema
> **deixa de ter cadastro de clientes**. O `Aluguel` passa a guardar os dados da
> pessoa (nome, cpf, email, telefone, cnh) direto no `POST /api/v1/alugueis`,
> cifrados + índice HMAC. Foco em métricas de aluguel/carro. Ver
> `docs/adr/0003-aluguel-guarda-dados-pessoais.md`.
> Este plano se mantém válido para a parte de Controllers+DTOs já concluída
> (fases 1-4) e ganha uma **Fase 5 - Remoção do cadastro de clientes**.

---

## 1. Diagnóstico por Ticket

| Ticket | Status | Alinhamento | Observações |
|---|---|---|---|
| **T01 - Padronizar respostas dos controllers** | Parcial | ~40% | POSTs usam `ResponseEntity` + `Location`. DELETE usa `@ResponseStatus(NO_CONTENT)`. GET/PUT/PATCH retornam DTOs diretamente. Não há regra única definida e aplicada. RelatorioController usa apenas retornos diretos. |
| **T02 - Simplificar criação de URI no POST** | Não iniciado | 0% | `ServletUriComponentsBuilder` ainda utilizado em `CarroController.cadastrar`, `ClienteController.cadastrar` e `AluguelController.registrar`. |
| **T03 - Remover comentários redundantes** | Parcial/Intencional | ~60% | Comentários óbvios foram reduzidos. Comentários justificativos (PATCH por regra, campos fora de listagem por cifragem, regras de relatório) foram **preservados** - correto para didática e LGPD. |
| **T04 - Padronizar DTOs** | Quase completo | ~90% | Uso correto de records, validações consistentemente, `PageResponse` bem desenhado, comentários LGPD presentes. Pequenos ajustes de consistência ainda possíveis, mas majoritariamente atendido. |
| **T05 - Revisão integrada** | Pendente | 0% | Bloqueado por T02 e T03. Depende também de T01 estar definido/aplicado. | 

---

## 2. Plano de Ação Priorizado

### Fase 1: Definir e aplicar padrão de respostas (T01) - Prioridade CRÍTICA
**Objetivo:** Eliminar ambiguidade, mantendo contratos REST idênticos.

**Ações:**
1. **Decidir o padrão** (documentar decisão). Adotar **Opção B (híbrida já existente):**
   - `POST` → `ResponseEntity.created(...)` com `Location`
   - `DELETE` → `@ResponseStatus(HttpStatus.NO_CONTENT)` sem corpo
   - `GET/PUT/PATCH` → Retorno direto do DTO, exceto se houver necessidade futura de headers especiais

2. **Aplicar uniformemente:**
   - Garantir que todos os POSTs use e `ResponseEntity` + `Location` (já feito)
   - Garantir `DELETE` com `@ResponseStatus(NO_CONTENT)` sem corpo (já feito)
   - Revisar `RelatorioController` - manter retornos diretos (faz sentido)

**Arquivos a alterar:** `CarroController`, `ClienteController`, `AluguelController`, `RelatorioController` (apenas se necessário)  
**Critério de aceite:** Padrão único definido/aplicado consistentemente. Testes existentes devem continuar passando.

---

### Fase 2: Simplificar criação de URI no POST (T02) - Prioridade ALTA
**Objetivo:** Substituir `ServletUriComponentsBuilder` por forma mais explicitá e didática, sem alterar contrato REST.

**Recomendação:** Construção manual da URI - mais didática para iniciantes.

**Implementação exemplo:**
```java
@PostMapping
public ResponseEntity<CarroResponse> cadastrar(@Valid @RequestBody CarroRequest request) {
    CarroResponse criado = carroService.cadastrar(request);
    URI local = URI.create("/api/v1/carros/" + criado.id());
    return ResponseEntity.created(local).body(criado);
}
```

**Arquivos a alterar:** `CarroController.cadastrar`, `ClienteController.cadastrar`, `AluguelController.registrar`  
**Imports a remover:** `org.springframework.web.servlet.support.ServletUriComponentsBuilder`  
**Critério de aceite:** Location header correto, mesmo formato de antes, testes passando.

---

### Fase 3: Limpeza pontual de comentários (T03) - Prioridade MÉDIA
**Objetivo:** Remover comentários redundantes sem perder explicações de regra/LGPD.

**O que REMOVER:**
- Comentários óbvios que apenas repetem o código
- Comentários de cabeçalho redundantes
- Comentários "// TODO" ou "// FIXME" se obsoletos

**O que MANTER (crítico):**
- Justificativas de design (`PATCH` por regra depender do status atual)
- Explicações LGPD/cifragem (`CarroResponse`, `ClienteResponse`)
- Regras de negócio complexas (`RelatorioController`)
- Explicações de campos fora de listagem por cifragem/criptografia
- Comentários em `AluguelRequest` sobre defaults do service

**Arquivos a revisar:** Todos os controllers e DTOs  
**Critério de aceite:** Código mais limpo, mas todas as justificativas importantes preservadas.

---

### Fase 4: Revisão integrada e finalização (T05) - Prioridade ALTA
**Objetivo:** Revisão holística Controllers+DTOs, garantir legibilidade linha-a-linha, imports limpos.

**Checklist:**
- [ ] **Imports:** Remover imports não utilizados
- [ ] **Formatação:** Código consistente
- [ ] **Fluxo explícito:** Verificar ausência de "mágica" não explicada (seguir ADR 0001)
- [ ] **Contratos REST:** Validar paths, métodos HTTP, status codes, Location em POSTs
- [ ] **LGPD:** Garantir que lógica de cifragem/descrifragem não foi alterada (apenas controller/DTO)
- [ ] **DTOs:** Revisar consistência final entre Request/Response
- [ ] **Testes:** Rodar `./gradlew test` e garantir 100% dos testes passando
- [ ] **Build:** Rodar `./gradlew build` para verificar compilação completa

---

## 3. Plano de Execução (Ordem Cronológica)

| Ordem | Tarefa | Tempo estimado | Dependências | Risco |
|---|---|---|---|---|
| **1** | T01 - Definir e aplicar padrão de respostas | 1-2h | Nenhuma | Baixo |
| **2** | T02 - Simplificar URI builders | 30-45min | T01 ideal (mas pode fazer em paralelo) | Baixo |
| **3** | T03 - Limpeza pontual | 1h | T01 concluído | Baixo (cuidado com LGPD) |
| **4** | T05 - Revisão integrada + testes | 1-2h | T01, T02, T03 | Baixo |

**Tempo total estimado:** ~3-5 horas

---

## 4. Validação e Testes

### Testes a executar:
```bash
./gradlew test
```

### Testes críticos:
- `FluxoDeAluguelApiTest` - fluxo completo pela API
- `CarroPlacaLGPDTest` - cifragem/descrifragem de placa
- `ClienteDadoPessoalLGPDTest` - proteção de dados pessoais
- `AluguelMovimentacaoLGPDTest` - regras LGPD em relatórios

---

## 5. Métricas de Sucesso

| Métrica | Atual | Alvo |
|---|---|---|
| Controllers com padrão único definido | Não | Sim |
| `ServletUriComponentsBuilder` removido dos POSTs | Presente (3 controllers) | Removido |
| Comentários redundantes removidos (mantendo LGPD) | Parcial | Completo |
| Todos os testes passando | A validar | 100% |
| Contratos REST inalterados | Sim | Sim |
| LGPD preservada | Sim | Sim |

---

## 6. Recomendações Finais

1. **Não ser excessivamente agressivo na limpeza:** Manter comentários que explicam *porquê* (não *o quê*).
2. **Priorizar clareza:** `URI.create("/api/v1/.../" + id)` é perfeito para o público-alvo didático.
3. **Validar LGPD após mudanças:** Rodar testes LGPD específicos é mandatório.
4. **Manter foco em didática:** Seguir ADR 0001 e `CONTEXT.md` em todas as decisões. 

---

## 7. Fase 5 - Remoção do cadastro de clientes (NOVO ESCOPO)

**Decisão (ADR-0003):** Remover tudo relacionado a Cliente. O `Aluguel` passa a
receber e guardar os dados da pessoa no `POST /api/v1/alugueis`
(nome, cpf, email, telefone, cnh), cifrados (AES-GCM) + índice HMAC. CPF **pode
repetir** (sem unicidade). Remover relatório por cliente e o filtro `clienteId`.

### 5.1 - Remover arquivos de Cliente
- `controller/ClienteController.java`
- `service/ClienteService.java`
- `repository/ClienteRepository.java`, `repository/TokenClienteRepository.java`
- `model/Cliente.java`, `model/TokenCliente.java`
- `dto/cliente/ClienteRequest.java`, `ClienteResponse.java`, `ClienteUpdateRequest.java`
- `dto/relatorio/RelatorioClienteResponse.java`

### 5.2 - Reestruturar o Aluguel
- `model/Aluguel.java`: remover FK `tokenCliente`; adicionar campos protegidos
  `nome_cifrado`/`nome_indice`, `cpf_cifrado`/`cpf_indice`,
  `email_cifrado`, `telefone_cifrado`, `cnh_cifrado`/`cnh_indice`.
  (CPF sem unique constraint.)
- `dto/aluguel/AluguelRequest.java`: adicionar nome/cpf/email/telefone/cnh com
  Bean Validation (mesmas regras dos antigos DTOs de cliente).
- `dto/aluguel/AluguelResponse.java`: substituir `ClienteResumo` por `PessoaResumo`
  (nome, cpf) montado a partir dos campos cifrados do aluguel.
- `service/AluguelService.java`: cifrar/gerar índices na gravação; decifrar na
  resposta; remover resolucao de token.
- `repository/AluguelRepository.java`: remover `existsByTokenCliente_Cliente_Id`,
  `countByTokenCliente_Cliente_Id`,
  `findByTokenCliente_Cliente_IdOrderByDataInicioDesc`,
  `somarReceitaPorCliente`; remover filtro `clienteId` do service/lista.

### 5.3 - Reestruturar relatórios
- `RelatorioController`: remover endpoint `/clientes/{id}`.
- `RelatorioService`: remover método `cliente(...)` e dependência de `ClienteRepository`.
- Remover `RelatorioClienteResponse`.

### 5.4 - Reestruturar testes
- `FluxoDeAluguelApiTest`: remover casos de cliente; adaptar `criarAluguel` para
  enviar dados da pessoa; remover filtro `clienteId` e relatório por cliente.
- `ClienteDadoPessoalLGPDTest`: **remover** (não há mais cadastro de cliente).
- `AluguelMovimentacaoLGPDTest`: reescrever para o novo modelo (dados da pessoa
  cifrados no aluguel; sem token; observacoes opcionais).
- `CarroPlacaLGPDTest`: corrigir o teste `alterarPlacaReprotege` (flush).
- `AluguelTest` (modelo): adaptar construtor (sem `TokenCliente`).

### Critério de aceite da Fase 5
- `./gradlew test` 100% verde.
- Nenhuma referência a `Cliente`/`TokenCliente` no código de produção.
- Dados pessoais no `alugueis` sempre cifrados + índice; nada em claro.

### Status da Fase 5 (2026-10-20) - CONCLUÍDA
- 5.1 Remoção dos arquivos de Cliente ✅
- 5.2 Reestruturação do Aluguel ✅
- 5.3 Relatórios (remoção do relatório por cliente) ✅
- 5.4 Testes reestruturados ✅ (`./gradlew clean test` → 53 testes, 0 falhas)
- ADR-0003 registrado; `CONTEXT.md` atualizado.

