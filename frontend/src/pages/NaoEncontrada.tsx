import { Link } from 'react-router-dom'
import { Botao, Cartao } from '../components/ui'

export function NaoEncontrada() {
  return (
    <Cartao className="p-10 text-center">
      <p className="font-titulo text-5xl font-semibold tracking-tight text-veloz-azul-claro">404</p>
      <h1 className="mt-3 font-titulo text-2xl font-semibold text-veloz-azul-leitura">
        Página não encontrada
      </h1>
      <p className="mx-auto mt-2 max-w-sm text-sm text-slate-600">
        O endereço acessado não existe nesta aplicação.
      </p>
      <Link to="/">
        <Botao className="mt-5">Voltar ao início</Botao>
      </Link>
    </Cartao>
  )
}
