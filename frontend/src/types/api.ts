/*
  Tipos espelhando os DTOs do backend.

  Regra: se um campo mudar no Java, muda aqui. Nao ha geracao automatica de
  client (seria via OpenAPI/springdoc, que ainda nao existe), entao estes tipos
  sao a unica fonte de verdade do front — vale conferir contra
  src/main/java/org/arthur/genericapi/dto/ quando a API mudar.
*/

export type StatusCarro = 'DISPONIVEL' | 'ALUGADO' | 'MANUTENCAO' | 'DESATIVADO'
export type StatusAluguel = 'ATIVO' | 'CONCLUIDO' | 'CANCELADO'
export type StatusPagamento = 'PENDENTE' | 'PAGO' | 'CANCELADO'

/** Envelope de paginacao devolvido por todas as listagens. */
export interface PageResponse<T> {
  conteudo: T[]
  pagina: number
  tamanho: number
  totalElementos: number
  totalPaginas: number
  primeira: boolean
  ultima: boolean
}

export interface Carro {
  id: string
  nome: string
  marca: string
  modelo: string
  ano: number
  /** Ja chega decifrada: a API nunca devolve o indice HMAC. */
  placa: string
  quilometragem: number
  cor: string
  categoria: string
  valorDiaria: number
  status: StatusCarro
  disponivelParaAluguel: boolean
  dataCadastro: string
  dataAtualizacao: string
}

export interface CarroRequest {
  nome: string
  marca: string
  modelo: string
  ano: number
  placa: string
  quilometragem: number
  cor: string
  categoria: string
  valorDiaria: number
}

/** Dados da pessoa, decifrados. O id e o do proprio aluguel. */
export interface PessoaResumo {
  id: string
  nome: string
  cpf: string
}

export interface CarroResumo {
  id: string
  placa: string
  marca: string
  modelo: string
  cor: string
  categoria: string
}

export interface Aluguel {
  id: string
  pessoa: PessoaResumo
  carro: CarroResumo
  dataInicio: string
  dataPrevistaDevolucao: string
  dataDevolucao: string | null
  quilometragemInicial: number
  quilometragemFinal: number | null
  quilometragemPercorrida: number | null
  valorDiaria: number
  totalDiarias: number
  valorTotal: number
  status: StatusAluguel
  statusPagamento: StatusPagamento
  atrasado: boolean
  /** Texto livre cifrado. Vem so no detalhe, nunca na listagem. */
  observacoes?: string
  dataCriacao: string
  dataAtualizacao: string
}

export interface AluguelRequest {
  nome: string
  cpf: string
  email: string
  telefone: string
  cnh: string
  carroId: string
  dataPrevistaDevolucao: string
  /** Opcionais: quando ausentes, o servico assume agora / dados do carro. */
  dataInicio?: string
  quilometragemInicial?: number
  valorDiaria?: number
  observacoes?: string
}

export interface DevolucaoRequest {
  dataDevolucao?: string
  quilometragemFinal: number
}

export interface RelatorioMovimentacao {
  periodoInicio: string
  periodoFim: string
  carroId: string | null
  totalAlugueis: number
  totalAtivos: number
  totalConcluidos: number
  receitaTotal: number
  receitaPaga: number
  receitaPendente: number
  ticketMedio: number
  quilometragemPercorrida: number
}

export interface MovimentacaoPorCarro {
  carroId: string
  placa: string
  marca: string
  modelo: string
  totalAlugueis: number
  receitaTotal: number
  receitaPaga: number
  receitaPendente: number
  quilometragemPercorrida: number
  percentualDaReceita: number
}

/** Formato de erro da API (ProblemDetail, RFC 7807). */
export interface ProblemDetail {
  type?: string
  title?: string
  status?: number
  detail?: string
  instance?: string
  erros?: { campo: string; mensagem: string }[]
}
