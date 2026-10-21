import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { alugueisApi } from '../api/endpoints'
import { ApiError } from '../api/client'
import { Aviso, Botao, Campo, Carregando, Cartao, Selo, estiloInput, Vazio } from '../components/ui'
import {
  corDoStatusAluguel,
  corDoStatusPagamento,
  dataHora,
  moeda,
  numero,
  rotulo,
} from '../components/formato'
import type { Aluguel } from '../types/api'

const statusOpcoes = ['ATIVO', 'CONCLUIDO', 'CANCELADO']

/** Lista de alugueis com filtro por status. Filtro vive na URL (compartilhavel). */
export function Alugueis() {
  const [params, setParams] = useSearchParams()
  const [alugueis, setAlugueis] = useState<Aluguel[]>([])
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState<string | null>(null)

  const status = params.get('status') ?? ''

  useEffect(() => {
    setCarregando(true)
    setErro(null)
    alugueisApi
      .listar({ status: status || undefined, tamanho: 100 })
      .then((p) => setAlugueis(p.conteudo))
      .catch((e) => setErro(e instanceof ApiError ? e.message : 'Falha ao carregar os aluguéis.'))
      .finally(() => setCarregando(false))
  }, [status])

  function alterarFiltro(valor: string) {
    const proximo = new URLSearchParams(params)
    if (valor) proximo.set('status', valor)
    else proximo.delete('status')
    setParams(proximo, { replace: true })
  }

  return (
    <div className="space-y-6">
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold tracking-tight text-veloz-grafite">Aluguéis</h1>
          <p className="mt-1 text-sm text-slate-500">
            {numero(alugueis.length)} registro{alugueis.length === 1 ? '' : 's'}
          </p>
        </div>
        <Link to="/alugueis/novo">
          <Botao>Registrar aluguel</Botao>
        </Link>
      </header>

      <Cartao className="p-4">
        <Campo rotulo="Status" className="w-44">
          <select
            className={estiloInput}
            value={status}
            onChange={(e) => alterarFiltro(e.target.value)}
          >
            <option value="">Todos</option>
            {statusOpcoes.map((s) => (
              <option key={s} value={s}>
                {rotulo(s)}
              </option>
            ))}
          </select>
        </Campo>
      </Cartao>

      {erro && <Aviso>{erro}</Aviso>}
      {carregando && <Carregando />}

      {!carregando && !erro && alugueis.length === 0 && (
        <Cartao>
          <Vazio
            titulo="Nenhum aluguel encontrado"
            descricao="Registre um aluguel para ver o histórico aqui."
            acao={
              <Link to="/alugueis/novo">
                <Botao>Registrar aluguel</Botao>
              </Link>
            }
          />
        </Cartao>
      )}

      {!carregando && alugueis.length > 0 && (
        <div className="space-y-2">
          {alugueis.map((a) => (
            <Link key={a.id} to={`/alugueis/${a.id}`}>
              <Cartao
                interativo
                className={`flex flex-wrap items-center justify-between gap-4 p-4 ${
                  a.atrasado ? 'ring-2 ring-red-500/30' : ''
                }`}
              >
                <div className="min-w-0 flex-1">
                  <div className="flex items-center gap-2">
                    <p className="truncate font-semibold text-veloz-grafite">{a.pessoa.nome}</p>
                    {a.atrasado && (
                      <span className="rounded-full bg-red-100 px-2 py-0.5 text-xs font-medium text-red-800">
                        atrasado
                      </span>
                    )}
                  </div>
                  <p className="truncate text-sm text-slate-500">
                    {a.carro.marca} {a.carro.modelo} · {a.carro.placa}
                  </p>
                  <p className="mt-1 text-xs text-slate-400">
                    {dataHora(a.dataInicio)} → {dataHora(a.dataPrevistaDevolucao)}
                  </p>
                </div>

                <div className="flex flex-wrap items-center gap-2">
                  <Selo cor={corDoStatusPagamento[a.statusPagamento]}>
                    {rotulo(a.statusPagamento)}
                  </Selo>
                  <Selo cor={corDoStatusAluguel[a.status]}>{rotulo(a.status)}</Selo>
                </div>

                <div className="text-right">
                  <p className="font-semibold tabular-nums text-veloz-grafite">
                    {moeda(a.valorTotal)}
                  </p>
                  <p className="text-xs text-slate-500">{a.totalDiarias} diária(s)</p>
                </div>
              </Cartao>
            </Link>
          ))}
        </div>
      )}
    </div>
  )
}
