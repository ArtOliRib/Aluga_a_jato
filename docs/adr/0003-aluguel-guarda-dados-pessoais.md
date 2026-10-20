# ADR-0003: Aluguel guarda os dados pessoais; cadastro de cliente deixa de existir

## Status
Aceito

## Contexto
O sistema deixou de ser um cadastro de clientes com locacao. O foco agora e o
registro do aluguel de carros e a analise de metricas (alugueis, carros,
faturamento). Manter a entidade `Cliente`, o `TokenCliente` e os endpoints
`/api/v1/clientes` adiciona uma fronteira (cadastro + pseudonimizacao em tabela
separada) que nao serve mais ao objetivo.

A LGPD continua valendo: dados pessoais (nome, cpf, email, telefone, cnh) devem
ser guardados de forma protegida.

## Decisao
1. **Remover o cadastro de clientes por completo**: entidade `Cliente`,
   entidade `TokenCliente`, `ClienteRepository`, `TokenClienteRepository`,
   `ClienteService`, `ClienteController`, `dto/cliente/*` e os endpoints
   `/api/v1/clientes/**`.
2. **O `Aluguel` passa a guardar os dados da pessoa** enviados no
   `POST /api/v1/alugueis`: nome, cpf, email, telefone e cnh — todos cifrados
   (AES-256-GCM) e com indices HMAC onde houver busca/igualdade.
3. **CPF pode se repetir**: a mesma pessoa pode ter varios alugueis. Nao ha
   constraint de unicidade de CPF no aluguel; o indice de CPF existe apenas para
   busca/agrupamento, nao para unicidade.
4. **Remover o relatorio por cliente** (`/api/v1/relatorios/clientes/{id}` e
   `RelatorioClienteResponse`), pois nao ha mais identidade central.
5. **Remover o filtro `clienteId`** do `GET /api/v1/alugueis`; permanecem
   `carroId` e `status`.
6. A pseudonimizacao nao usa mais tabela-ponte: os unicos dados sensiveis no
   `Aluguel` sao os campos cifrados/indice.

## Consequencias
- O `Aluguel` acumula dado pessoal; a protecao passa a depender inteiramente da
  cifra + indice HMAC aplicados na gravacao.
- Datas, valores e quilometragem continuam em claro (relatorios filtram/somam).
- Contratos REST mudam: `POST /alugueis` ganha campos da pessoa; alguns
  endpoints/rotas sao removidos. Isso e uma mudanca incompatvel e intencional.
- Testes de cliente (cadastro, unicidade de cpf/cnh/email, busca por nome/cpf,
  token, relatorio por cliente) saem; entram testes do novo modelo.

## Alternativas consideradas
- Manter `Cliente` so internamente (sem controller) -> rejeitado: mantem tabela
  de identidade e a fronteira de pseudonimizacao que queremos eliminar.
- Guardar apenas um identificador anonimo, sem dado pessoal -> rejeitado: o
  usuario pediu que os dados da pessoa sejam enviados no aluguel.
- Criar tabela `pessoa` separada -> rejeitado por enquanto: reinroduz a mesma
  indirecao que motivou a remocao do `TokenCliente`.
