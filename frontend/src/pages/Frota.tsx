import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { carrosApi } from '../api/endpoints'
import { ApiError } from '../api/client'
import {
  Aviso,
  Botao,
  Campo,
  Carregando,
  Cartao,
  Selo,
  estiloInput,
  Vazio,
} from '../components/ui'
import { corDoStatusCarro, moeda, numero, rotulo } from '../components/formato'
import { FormularioCarro } from '../components/FormularioCarro'
import type { Carro, StatusCarro } from '../types/api'

const statusTodos: StatusCarro[] = ['DISPONIVEL', 'ALUGADO', 'MANUTENCAO', 'DESATIVADO']

/**
 * Frota: lista, filtro e cadastro.
 *
 * O filtro vive na URL (?status=...) em vez de em estado local para que a tela
 * seja compartilhavel e o botao voltar do navegador funcione como se espera.
 */
export function Frota() {
  const [params, setParams] = useSearchParams()
  const [carros, setCarros] = useState<Carro[]>([])
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState<string | null>(null)
  const [mostrandoForm, setMostrandoForm] = useState(false)

  const status = (params.get('status') ?? '') as StatusCarro | ''
  const marca = params.get('marca') ?? ''

  function carregar() {
    setCarregando(true)
    setErro(null)
    carrosApi
      .listar({ status: status || undefined, marca: marca || undefined, tamanho: 100 })
      .then((p) => setCarros(p.conteudo))
      .catch((e) => setErro(e instanceof ApiError ? e.message : 'Falha ao carregar a frota.'))
      .finally(() => setCarregando(false))
  }

  useEffect(carregar, [status, marca])

  function alterarFiltro(chave: string, valor: string) {
    const proximo = new URLSearchParams(params)
    if (valor) proximo.set(chave, valor)
    else proximo.delete(chave)
    setParams(proximo, { replace: true })
  }

  return (
    <div className="space-y-6">
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold tracking-tight text-veloz-grafite">Frota</h1>
          <p className="mt-1 text-sm text-slate-500">
            {numero(carros.length)} veículo{carros.length === 1 ? '' : 's'} na lista
          </p>
        </div>
        <Botao onClick={() => setMostrandoForm((v) => !v)}>
          {mostrandoForm ? 'Fechar' : 'Cadastrar carro'}
        </Botao>
      </header>

      {mostrandoForm && (
        <FormularioCarro
          onSalvo={() => {
            setMostrandoForm(false)
            carregar()
          }}
          onCancelar={() => setMostrandoForm(false)}
        />
      )}

      <Cartao className="flex flex-wrap gap-3 p-4">
        <Campo rotulo="Status" className="w-44">
          <select
            className={estiloInput}
            value={status}
            onChange={(e) => alterarFiltro('status', e.target.value)}
          >
            <option value="">Todos</option>
            {statusTodos.map((s) => (
              <option key={s} value={s}>
                {rotulo(s)}
              </option>
            ))}
          </select>
        </Campo>
        <Campo rotulo="Marca" className="w-48">
          <input
            className={estiloInput}
            placeholder="Fiat, VW..."
            value={marca}
            onChange={(e) => alterarFiltro('marca', e.target.value)}
          />
        </Campo>
      </Cartao>

      {erro && <Aviso>{erro}</Aviso>}
      {carregando && <Carregando />}

      {!carregando && !erro && carros.length === 0 && (
        <Cartao>
          <Vazio
            titulo="Nenhum carro encontrado"
            descricao="Ajuste o filtro ou cadastre um veículo novo."
          />
        </Cartao>
      )}

      {!carregando && carros.length > 0 && (
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {carros.map((c) => (
            <Link key={c.id} to={`/frota/${c.id}`}>
              <Cartao interativo className="h-full p-4">
                <div className="flex items-start justify-between gap-3">
                  <div className="min-w-0">
                    <p className="truncate font-semibold text-veloz-grafite">
                      {c.marca} {c.modelo}
                    </p>
                    <p className="truncate text-sm text-slate-500">
                      {c.nome} · {c.ano} · {c.cor}
                    </p>
                  </div>
                  <Selo cor={corDoStatusCarro[c.status]}>{rotulo(c.status)}</Selo>
                </div>

                <div className="mt-4 flex items-end justify-between">
                  <span className="rounded-lg bg-slate-100 px-2 py-1 font-mono text-sm font-medium tracking-wider text-slate-700">
                    {c.placa}
                  </span>
                  <span className="text-sm font-semibold text-veloz-azul">
                    {moeda(c.valorDiaria)}
                    <span className="text-xs font-normal text-slate-500">/dia</span>
                  </span>
                </div>

                <p className="mt-3 text-xs text-slate-500">
                  {numero(c.quilometragem)} km · {c.categoria}
                </p>
              </Cartao>
            </Link>
          ))}
        </div>
      )}
    </div>
  )
}
