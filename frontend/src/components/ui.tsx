import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'

/*
  Pecas visuais reutilizadas. Ficam juntas para que o "acabamento" seja
  consistente: raio de borda, sombra e espacamento aparecem uma vez so.

  Regra que vale para o arquivo inteiro: rotulo NUNCA em maiuscula. Caixa alta
  em rotulo e o tique mais reconhecivel de interface gerada por maquina — e
  destrui a forma das palavras, que e justamente por onde o olho reconhece o
  rotulo antes de ler. Peso e cor fazem esse trabalho melhor.
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
      className={`rounded-veloz bg-white ring-1 ring-slate-900/5 ${
        interativo ? 'transition duration-150 hover:-translate-y-0.5 hover:shadow-veloz' : ''
      } ${className}`}
    >
      {children}
    </div>
  )
}

/*
  Cartao-chave: a peca-assinatura da interface.

  Leitura literal do chaveiro de patio de locadora: um cartao com um furo
  vazado no canto, como a chave pendurada. E o unico lugar do sistema onde a cor
  cheia e a sombra alta sao liberadas — de proposito, porque a decisao que o
  operador toma olhando para ele (qual carro entregar) e a mais importante da
  tela. Se todo cartao fosse assim, nenhum seria.

  O furo e feito com um circulo vazado por ring, nao por imagem, para acompanhar
  o fundo de qualquer superficie.

  Duas formas de uso, com a mesma aparencia:
  - CartaoChave: <button>, para escolher algo dentro de um formulario.
  - CartaoChaveLink: <Link>, para navegar. Sao componentes separados porque
    botao dentro de link e HTML invalido, e o navegador nao sabe qual dos dois
    deve reagir ao clique.
*/
const estiloChave = (selecionado: boolean, className: string) =>
  `group relative block w-full rounded-veloz-alto p-4 text-left transition duration-150 ${
    selecionado
      ? 'bg-veloz-azul-fundo ring-2 ring-veloz-azul shadow-veloz'
      : 'bg-white ring-1 ring-slate-900/5 hover:-translate-y-0.5 hover:shadow-veloz-alta'
  } ${className}`

function Furo({ selecionado }: { selecionado: boolean }) {
  return (
    <span
      aria-hidden="true"
      className={`absolute left-4 top-4 block size-3 rounded-full ring-1 ring-inset ${
        selecionado ? 'bg-veloz-azul/20 ring-veloz-azul/50' : 'bg-veloz-papel ring-slate-900/10'
      }`}
    />
  )
}

export function CartaoChave({
  children,
  selecionado = false,
  className = '',
  onClick,
}: {
  children: ReactNode
  selecionado?: boolean
  className?: string
  onClick?: () => void
}) {
  return (
    <button type="button" aria-pressed={selecionado} onClick={onClick} className={estiloChave(selecionado, className)}>
      <Furo selecionado={selecionado} />
      <span className="block pl-6">{children}</span>
    </button>
  )
}

export function CartaoChaveLink({
  children,
  para,
  className = '',
}: {
  children: ReactNode
  para: string
  className?: string
}) {
  return (
    <Link to={para} className={estiloChave(false, className)}>
      <Furo selecionado={false} />
      <span className="block pl-6">{children}</span>
    </Link>
  )
}

/** Etiqueta de status. A cor vem de fora, do mapa em formato.ts. */
export function Selo({ children, cor }: { children: ReactNode; cor: string }) {
  return (
    <span
      className={`inline-flex items-center rounded-veloz px-2 py-0.5 text-xs font-medium ring-1 ring-inset ${cor}`}
    >
      {children}
    </span>
  )
}

type Variante = 'primario' | 'secundario' | 'perigo' | 'texto'

const variantes: Record<Variante, string> = {
  primario: 'bg-veloz-azul text-white hover:bg-veloz-azul-leitura shadow-veloz',
  secundario: 'bg-white text-veloz-grafite ring-1 ring-slate-900/10 hover:bg-slate-50',
  perigo: 'bg-white text-red-700 ring-1 ring-red-600/20 hover:bg-red-50',
  texto: 'text-veloz-azul-leitura hover:bg-veloz-azul-fundo',
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
      className={`inline-flex items-center justify-center gap-2 rounded-veloz px-4 py-2.5 text-sm font-semibold transition duration-150 disabled:cursor-not-allowed disabled:opacity-50 ${variantes[variante]} ${className}`}
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
  'block w-full rounded-veloz border-0 bg-slate-50 px-3.5 py-2.5 text-sm text-veloz-grafite ring-1 ring-inset ring-slate-900/10 placeholder:text-slate-400 focus:bg-white focus:ring-2 focus:ring-inset focus:ring-veloz-azul outline-none transition'

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
    info: 'bg-veloz-azul-fundo text-veloz-azul-leitura ring-blue-600/20',
    sucesso: 'bg-emerald-50 text-emerald-800 ring-emerald-600/20',
  }[tipo]

  return (
    <div className={`rounded-veloz px-4 py-3 text-sm ring-1 ring-inset ${cores}`}>{children}</div>
  )
}

/** Estado de carregamento. */
export function Carregando({ texto = 'Carregando...' }: { texto?: string }) {
  return (
    <div className="flex items-center gap-3 py-12 text-sm text-slate-600">
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
      <p className="font-titulo text-lg font-semibold text-slate-700">{titulo}</p>
      {descricao && <p className="max-w-sm text-sm text-slate-600">{descricao}</p>}
      {acao}
    </div>
  )
}

/*
  Numero de destaque.

  Substitui o antigo cartao "numero grande + rotulo pequeno em maiuscula +
  estatisticas de apoio", que e o tratamento padrao de qualquer painel. Aqui o
  numero e tratado como elemento tipografico: serifada, entrelinha apertada e
  um "de" pequeno no lugar do rotulo, de modo que o numero e a frase que o
  explica leiam-se como uma coisa so, e nao como dois blocos empilhados.

  O rotulo continua existindo para quem usa leitor de tela, via aria-label.
*/
export function Metrica({
  rotulo,
  valor,
  detalhe,
  destaque = false,
}: {
  rotulo: string
  valor: string
  detalhe?: string
  destaque?: boolean
}) {
  return (
    <div>
      <p
        className={`font-titulo leading-none tracking-tight tabular ${
          destaque ? 'text-4xl font-semibold text-veloz-azul' : 'text-3xl font-medium text-veloz-grafite'
        }`}
        aria-label={`${rotulo}: ${valor}`}
      >
        {valor}
      </p>
      <p className="mt-2 text-sm text-slate-600">
        <span className="sr-only">{rotulo}. </span>
        {detalhe ?? rotulo}
      </p>
    </div>
  )
}

/** Titulo de pagina com o apoio opcional. Unifica o cabecalho de todas as telas. */
export function TituloPagina({
  children,
  apoio,
  acao,
}: {
  children: ReactNode
  apoio?: ReactNode
  acao?: ReactNode
}) {
  return (
    <header className="flex flex-wrap items-end justify-between gap-4">
      <div>
        <h1 className="font-titulo text-3xl font-semibold tracking-tight text-veloz-azul-leitura">
          {children}
        </h1>
        {apoio && <p className="mt-1 text-sm text-slate-600">{apoio}</p>}
      </div>
      {acao}
    </header>
  )
}
