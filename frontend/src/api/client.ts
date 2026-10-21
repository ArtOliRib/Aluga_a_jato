import type { ProblemDetail } from '../types/api'

/**
 * URL base da API.
 *
 * Em desenvolvimento fica vazio de proposito: o Vite faz proxy de /api para
 * localhost:8080 (veja vite.config.ts), entao a requisicao sai da mesma origem e
 * CORS nem entra em jogo. Em producao, defina VITE_API_URL com a URL publica.
 */
const BASE_URL = import.meta.env.VITE_API_URL ?? ''

/** Erro de negocio devolvido pela API, ja traduzido para exibicao. */
export class ApiError extends Error {
  readonly status: number
  readonly campos: { campo: string; mensagem: string }[]

  constructor(status: number, mensagem: string, campos: { campo: string; mensagem: string }[] = []) {
    super(mensagem)
    this.name = 'ApiError'
    this.status = status
    this.campos = campos
  }
}

/**
 * Faz a requisicao e normaliza a resposta.
 *
 * O ponto delicado e o erro: a API responde no formato ProblemDetail, mas
 * `erros[]` (validacao campo a campo) so existe em 400. Sem ler esse array, o
 * usuario veria apenas "Um ou mais campos invalidos" sem saber qual campo
 * corrigir — que e justamente onde um formulario ajuda ou atrapalha.
 */
async function request<T>(caminho: string, init?: RequestInit): Promise<T> {
  let resposta: Response

  try {
    resposta = await fetch(`${BASE_URL}${caminho}`, {
      ...init,
      headers: {
        ...(init?.body ? { 'Content-Type': 'application/json' } : {}),
        ...init?.headers,
      },
    })
  } catch {
    // Falha de rede nao chega como resposta HTTP: fetch lanca. A causa mais
    // comum em dev e a API nao estar no ar.
    throw new ApiError(0, 'Nao foi possivel falar com a API. Ela esta rodando em http://localhost:8080?')
  }

  if (resposta.status === 204) {
    return undefined as T
  }

  const texto = await resposta.text()
  const corpo = texto ? JSON.parse(texto) : null

  if (!resposta.ok) {
    const problema = corpo as ProblemDetail | null
    throw new ApiError(
      resposta.status,
      problema?.detail ?? problema?.title ?? `Erro ${resposta.status}`,
      problema?.erros ?? [],
    )
  }

  return corpo as T
}

export const api = {
  get: <T>(caminho: string) => request<T>(caminho),
  post: <T>(caminho: string, corpo: unknown) =>
    request<T>(caminho, { method: 'POST', body: JSON.stringify(corpo) }),
  put: <T>(caminho: string, corpo: unknown) =>
    request<T>(caminho, { method: 'PUT', body: JSON.stringify(corpo) }),
  patch: <T>(caminho: string, corpo?: unknown) =>
    request<T>(caminho, {
      method: 'PATCH',
      ...(corpo === undefined ? {} : { body: JSON.stringify(corpo) }),
    }),
  delete: (caminho: string) => request<void>(caminho, { method: 'DELETE' }),
}
