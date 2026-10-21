import type { ReactNode } from 'react'

/*
  Pecas visuais reutilizadas. Ficam juntas para que o "acabamento" seja
  consistente: raio de borda, sombra e espacamento aparecem uma vez so.
*/

/** Cartao de superficie. O padrao visual base de quase toda tela. */
export function Cartao({
  children,
  className = '',
  interativo = false,
}: {
  children: ReactNode
  className?: string
  interativo?: boolean
}) {
  return (
    <div
      className={`rounded-2xl bg-white shadow-veloz ring-1 ring-slate-900/5 ${
        interativo
          ? 'transition hover:-translate-y-0.5 hover:shadow-veloz-alta cursor-pointer'
          : ''
      } ${className}`}
    >
      {children}
    </div>
  )
}

/** Etiqueta de status. A cor vem de fora, do mapa em formato.ts. */
export function Selo({ children, cor }: { children: ReactNode; cor: string }) {
  return (
    <span
      className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ring-1 ring-inset ${cor}`}
    >
      {children}
    </span>
  )
}

type Variante = 'primario' | 'secundario' | 'perigo' | 'texto'

const variantes: Record<Variante, string> = {
  primario: 'bg-veloz-azul text-white hover:bg-blue-800 shadow-veloz',
  secundario: 'bg-white text-veloz-grafite ring-1 ring-slate-900/10 hover:bg-slate-50',
  perigo: 'bg-white text-red-700 ring-1 ring-red-600/20 hover:bg-red-50',
  texto: 'text-veloz-azul hover:bg-veloz-azul-fundo',
}

export function Botao({
  children,
  variante = 'primario',
  className = '',
  ...props
}: {
  children: ReactNode
  variante?: Variante
} & React.ButtonHTMLAttributes<HTMLButtonElement>) {
  return (
    <button
      {...props}
      className={`inline-flex items-center justify-center gap-2 rounded-xl px-4 py-2.5 text-sm font-semibold transition disabled:cursor-not-allowed disabled:opacity-50 ${variantes[variante]} ${className}`}
    >
      {children}
    </button>
  )
}

/** Campo de formulario com rotulo e mensagem de erro. */
export function Campo({
  rotulo,
  erro,
  children,
  className = '',
}: {
  rotulo: string
  erro?: string
  children: ReactNode
  className?: string
}) {
  return (
    <label className={`block ${className}`}>
      <span className="mb-1.5 block text-sm font-medium text-slate-700">{rotulo}</span>
      {children}
      {erro && <span className="mt-1 block text-xs text-red-600">{erro}</span>}
    </label>
  )
}

export const estiloInput =
  'block w-full rounded-xl border-0 bg-slate-50 px-3.5 py-2.5 text-sm text-veloz-grafite ring-1 ring-inset ring-slate-900/10 placeholder:text-slate-400 focus:bg-white focus:ring-2 focus:ring-inset focus:ring-veloz-azul outline-none transition'

/** Caixa de aviso. Usada para erro de API e estados vazios relevantes. */
export function Aviso({
  children,
  tipo = 'erro',
}: {
  children: ReactNode
  tipo?: 'erro' | 'info' | 'sucesso'
}) {
  const cores = {
    erro: 'bg-red-50 text-red-800 ring-red-600/20',
    info: 'bg-veloz-azul-fundo text-blue-900 ring-blue-600/20',
    sucesso: 'bg-emerald-50 text-emerald-800 ring-emerald-600/20',
  }[tipo]

  return (
    <div className={`rounded-xl px-4 py-3 text-sm ring-1 ring-inset ${cores}`}>{children}</div>
  )
}

/** Estado de carregamento. */
export function Carregando({ texto = 'Carregando...' }: { texto?: string }) {
  return (
    <div className="flex items-center gap-3 py-12 text-sm text-slate-500">
      <span className="size-4 animate-spin rounded-full border-2 border-slate-300 border-t-veloz-azul" />
      {texto}
    </div>
  )
}

/** Estado vazio, com acao opcional. */
export function Vazio({
  titulo,
  descricao,
  acao,
}: {
  titulo: string
  descricao?: string
  acao?: ReactNode
}) {
  return (
    <div className="flex flex-col items-center gap-3 py-16 text-center">
      <p className="text-base font-semibold text-slate-700">{titulo}</p>
      {descricao && <p className="max-w-sm text-sm text-slate-500">{descricao}</p>}
      {acao}
    </div>
  )
}

/** Numero grande com rotulo. Base dos cartoes de resumo. */
export function Metrica({
  rotulo,
  valor,
  destaque = false,
}: {
  rotulo: string
  valor: string
  destaque?: boolean
}) {
  return (
    <div>
      <p className="text-xs font-medium uppercase tracking-wide text-slate-500">{rotulo}</p>
      <p
        className={`mt-1 font-semibold tabular-nums ${
          destaque ? 'text-3xl text-veloz-azul' : 'text-xl text-veloz-grafite'
        }`}
      >
        {valor}
      </p>
    </div>
  )
}
