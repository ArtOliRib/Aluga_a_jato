import { Link, NavLink, Outlet } from 'react-router-dom'
import { Logo } from './Logo'

/*
  Moldura da aplicacao: marca no topo, navegacao a esquerda, conteudo a direita.

  A escolha de barra lateral em vez de menu horizontal e por causa do numero de
  areas (4): em cima, os rotulos competem com o titulo de cada tela; ao lado,
  ficam previsiveis em qualquer resolucao. Abaixo de 768px a lateral vira uma
  faixa horizontal rolavel, para nao comer a largura util no celular.
*/

const navegacao = [
  { para: '/', rotulo: 'Início', fim: true },
  { para: '/frota', rotulo: 'Frota' },
  { para: '/alugueis', rotulo: 'Aluguéis' },
  { para: '/relatorios', rotulo: 'Relatórios' },
]

export function Layout() {
  return (
    <div className="flex min-h-full flex-col md:flex-row">
      <aside className="shrink-0 border-b border-slate-900/5 bg-white md:w-60 md:border-b-0 md:border-r">
        <div className="flex items-center justify-between px-5 py-4 md:block">
          <Link to="/" className="flex items-center gap-2.5">
            <Logo className="size-9" />
            <span className="text-lg font-bold tracking-tight text-veloz-grafite">
              Veloz
            </span>
          </Link>
        </div>

        <nav className="flex gap-1 overflow-x-auto px-3 pb-3 md:flex-col md:overflow-visible md:pb-0">
          {navegacao.map((item) => (
            <NavLink
              key={item.para}
              to={item.para}
              end={item.fim}
              className={({ isActive }) =>
                `whitespace-nowrap rounded-xl px-3.5 py-2 text-sm font-medium transition ${
                  isActive
                    ? 'bg-veloz-azul text-white shadow-veloz'
                    : 'text-slate-600 hover:bg-veloz-azul-fundo hover:text-veloz-azul'
                }`
              }
            >
              {item.rotulo}
            </NavLink>
          ))}
        </nav>
      </aside>

      <main className="flex-1 overflow-x-hidden">
        <div className="mx-auto max-w-6xl px-5 py-8 md:px-8 md:py-10">
          <Outlet />
        </div>
      </main>
    </div>
  )
}
