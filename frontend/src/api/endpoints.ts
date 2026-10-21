import { api } from './client'
import type {
  Aluguel,
  AluguelRequest,
  Carro,
  CarroRequest,
  DevolucaoRequest,
  MovimentacaoPorCarro,
  PageResponse,
  RelatorioMovimentacao,
  StatusCarro,
} from '../types/api'

/** Monta query string ignorando valores vazios. */
function query(params: Record<string, string | number | undefined | null>): string {
  const partes = Object.entries(params)
    .filter(([, v]) => v !== undefined && v !== null && v !== '')
    .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(String(v))}`)
  return partes.length ? `?${partes.join('&')}` : ''
}

export interface FiltroCarros {
  marca?: string
  status?: StatusCarro
  pagina?: number
  tamanho?: number
  ordenarPor?: string
  direcao?: 'asc' | 'desc'
}

export const carrosApi = {
  listar: (filtro: FiltroCarros = {}) =>
    api.get<PageResponse<Carro>>(`/api/v1/carros${query({ ...filtro })}`),

  disponiveis: () => api.get<Carro[]>('/api/v1/carros/disponiveis'),

  buscar: (id: string) => api.get<Carro>(`/api/v1/carros/${id}`),

  buscarPorPlaca: (placa: string) =>
    api.get<Carro>(`/api/v1/carros/por-placa/${encodeURIComponent(placa)}`),

  criar: (corpo: CarroRequest) => api.post<Carro>('/api/v1/carros', corpo),

  atualizar: (id: string, corpo: Partial<CarroRequest>) =>
    api.put<Carro>(`/api/v1/carros/${id}`, corpo),

  alterarStatus: (id: string, status: StatusCarro) =>
    api.patch<Carro>(`/api/v1/carros/${id}/status`, { status }),

  remover: (id: string) => api.delete(`/api/v1/carros/${id}`),
}

export interface FiltroAlugueis {
  carroId?: string
  status?: string
  pagina?: number
  tamanho?: number
}

export const alugueisApi = {
  listar: (filtro: FiltroAlugueis = {}) =>
    api.get<PageResponse<Aluguel>>(`/api/v1/alugueis${query({ ...filtro })}`),

  buscar: (id: string) => api.get<Aluguel>(`/api/v1/alugueis/${id}`),

  registrar: (corpo: AluguelRequest) => api.post<Aluguel>('/api/v1/alugueis', corpo),

  devolver: (id: string, corpo: DevolucaoRequest) =>
    api.patch<Aluguel>(`/api/v1/alugueis/${id}/devolucao`, corpo),

  pagar: (id: string) => api.patch<Aluguel>(`/api/v1/alugueis/${id}/pagamento`),

  cancelar: (id: string, motivo?: string) =>
    api.patch<Aluguel>(`/api/v1/alugueis/${id}/cancelamento`, motivo ? { motivo } : {}),
}

export const relatoriosApi = {
  movimentacao: (de: string, ate: string, carroId?: string) =>
    api.get<RelatorioMovimentacao>(
      `/api/v1/relatorios/movimentacao${query({ de, ate, carroId })}`,
    ),

  movimentacaoPorCarro: (de: string, ate: string) =>
    api.get<MovimentacaoPorCarro[]>(
      `/api/v1/relatorios/movimentacao-por-carro${query({ de, ate })}`,
    ),
}
