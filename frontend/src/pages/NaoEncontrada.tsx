import { Link } from 'react-router-dom'
import { Botao, Cartao } from '../components/ui'

export function NaoEncontrada() {
  return (
    <Cartao className="p-10 text-center">
      <p className="text-5xl font-bold text-veloz-azul-claro">404</p>
      <h1 className="mt-3 text-2xl font-bold text-veloz-grafite">Página não encontrada</h1>
      <p className="mx-auto mt-2 max-w-sm text-sm text-slate-500">
        O endereço acessado não existe nesta aplicação.
      </p>
      <Link to="/">
        <Botao className="mt-5">Voltar ao início</Botao>
      </Link>
    </Cartao>
  )
}
