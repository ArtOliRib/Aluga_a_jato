import type { StatusAluguel, StatusCarro, StatusPagamento } from '../types/api'

/** Formata valor em real. A API manda numero cru; a moeda e decisao de exibicao. */
export function moeda(valor: number | null | undefined): string {
  if (valor === null || valor === undefined) return '—'
  return valor.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
}

/** Numero com separador de milhar. */
export function numero(valor: number | null | undefined): string {
  if (valor === null || valor === undefined) return '—'
  return valor.toLocaleString('pt-BR')
}

/** Data e hora curtas, sem segundos. */
export function dataHora(iso: string | null | undefined): string {
  if (!iso) return '—'
  return new Date(iso).toLocaleString('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

/** Data sem hora, para campos onde o horario e ruido. */
export function data(iso: string | null | undefined): string {
  if (!iso) return '—'
  return new Date(iso).toLocaleDateString('pt-BR')
}

/** Converte um Date local para o formato ISO que a API espera (sem timezone). */
export function paraIsoLocal(d: Date): string {
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}T${p(d.getHours())}:${p(d.getMinutes())}:00`
}

/** Inicio e fim do mes corrente, para os filtros de relatorio. */
export function mesCorrente(): { de: string; ate: string } {
  const agora = new Date()
  const inicio = new Date(agora.getFullYear(), agora.getMonth(), 1, 0, 0, 0)
  const fim = new Date(agora.getFullYear(), agora.getMonth() + 1, 0, 23, 59, 59)
  return { de: paraIsoLocal(inicio), ate: paraIsoLocal(fim) }
}

/*
  Cores por status.

  Ficam agrupadas aqui, e nao espalhadas pelos componentes, porque o mesmo
  status aparece em telas diferentes (card, tabela, filtro) e precisa parecer o
  mesmo em todas. Um verde diferente em cada tela destroi a leitura de cor.
*/
export const corDoStatusCarro: Record<StatusCarro, string> = {
  DISPONIVEL: 'bg-emerald-100 text-emerald-800 ring-emerald-600/20',
  ALUGADO: 'bg-blue-100 text-blue-800 ring-blue-600/20',
  MANUTENCAO: 'bg-amber-100 text-amber-800 ring-amber-600/20',
  DESATIVADO: 'bg-slate-200 text-slate-600 ring-slate-500/20',
}

export const corDoStatusAluguel: Record<StatusAluguel, string> = {
  ATIVO: 'bg-blue-100 text-blue-800 ring-blue-600/20',
  CONCLUIDO: 'bg-emerald-100 text-emerald-800 ring-emerald-600/20',
  CANCELADO: 'bg-slate-200 text-slate-600 ring-slate-500/20',
}

export const corDoStatusPagamento: Record<StatusPagamento, string> = {
  PENDENTE: 'bg-amber-100 text-amber-800 ring-amber-600/20',
  PAGO: 'bg-emerald-100 text-emerald-800 ring-emerald-600/20',
  CANCELADO: 'bg-slate-200 text-slate-600 ring-slate-500/20',
}

/** Rotulo legivel: DISPONIVEL -> Disponivel */
export function rotulo(status: string): string {
  return status.charAt(0) + status.slice(1).toLowerCase()
}
