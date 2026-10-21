import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { alugueisApi, carrosApi } from '../api/endpoints'
import { ApiError } from '../api/client'
import { Aviso, Botao, Carregando, Cartao, Selo } from '../components/ui'
import {
  corDoStatusAluguel,
  corDoStatusCarro,
  corDoStatusPagamento,
  data as fmtData,
  dataHora,
  moeda,
  numero,
  rotulo,
} from '../components/formato'
import type { Aluguel, Carro, StatusCarro } from '../types/api'

/*
  Transicoes validas de status, espelhando a regra do backend.

  Duplicar a regra aqui e deliberado: sem isso o usuario so descobriria que
  "DESATIVADO -> ALUGADO" e proibido depois de clicar e receber 422. Mostrar
  apenas as transicoes possiveis transforma um erro em uma escolha. Se a regra
  mudar no backend, muda aqui tambem.
*/
const transicoes: Record<StatusCarro, StatusCarro[]> = {
  DISPONIVEL: ['ALUGADO', 'MANUTENCAO', 'DESATIVADO'],
  ALUGADO: ['DISPONIVEL', 'MANUTENCAO'],
  MANUTENCAO: ['DISPONIVEL', 'DESATIVADO'],
  DESATIVADO: [],
}

export function CarroDetalhe() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()

  const [carro, setCarro] = useState<Carro | null>(null)
  const [historico, setHistorico] = useState<Aluguel[]>([])
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState<string | null>(null)
  const [acaoErro, setAcaoErro] = useState<string | null>(null)
  const [ocupado, setOcupado] = useState(false)

  useEffect(() => {
    if (!id) return
    Promise.allSettled([carrosApi.buscar(id), alugueisApi.listar({ carroId: id, tamanho: 20 })])
      .then(([rCarro, rHistorico]) => {
        if (rCarro.status === 'fulfilled') setCarro(rCarro.value)
        else
          setErro(
            rCarro.reason instanceof ApiError ? rCarro.reason.message : 'Carro não encontrado.',
          )
        if (rHistorico.status === 'fulfilled') setHistorico(rHistorico.value.conteudo)
      })
      .finally(() => setCarregando(false))
  }, [id])

  async function mudarStatus(novo: StatusCarro) {
    if (!id) return
    setOcupado(true)
    setAcaoErro(null)
    try {
      setCarro(await carrosApi.alterarStatus(id, novo))
    } catch (e) {
      setAcaoErro(e instanceof ApiError ? e.message : 'Não foi possível alterar o status.')
    } finally {
      setOcupado(false)
    }
  }

  async function remover() {
    if (!id) return
    if (!confirm('Remover este carro? A ação não pode ser desfeita.')) return
    setOcupado(true)
    setAcaoErro(null)
    try {
      await carrosApi.remover(id)
      navigate('/frota')
    } catch (e) {
      setAcaoErro(
        e instanceof ApiError ? e.message : 'Não foi possível remover. Há aluguel vinculado?',
      )
      setOcupado(false)
    }
  }

  if (carregando) return <Carregando />
  if (erro || !carro)
    return (
      <div className="space-y-4">
        <Aviso>{erro ?? 'Carro não encontrado.'}</Aviso>
        <Link to="/frota">
          <Botao variante="secundario">Voltar para a frota</Botao>
        </Link>
      </div>
    )

  const possiveis = transicoes[carro.status]

  return (
    <div className="space-y-6">
      <Link to="/frota" className="text-sm font-medium text-veloz-azul hover:underline">
        ← Frota
      </Link>

      <header className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <div className="flex items-center gap-3">
            <h1 className="text-3xl font-bold tracking-tight text-veloz-grafite">
              {carro.marca} {carro.modelo}
            </h1>
            <Selo cor={corDoStatusCarro[carro.status]}>{rotulo(carro.status)}</Selo>
          </div>
          <p className="mt-1 text-sm text-slate-500">
            {carro.nome} · {carro.ano} · {carro.cor} · {carro.categoria}
          </p>
        </div>
        <span className="rounded-lg bg-slate-100 px-3 py-1.5 font-mono text-base font-medium tracking-wider text-slate-700">
          {carro.placa}
        </span>
      </header>

      {acaoErro && <Aviso>{acaoErro}</Aviso>}

      <section className="grid gap-4 sm:grid-cols-3">
        <Cartao className="p-5">
          <p className="text-xs font-medium uppercase tracking-wide text-slate-500">
            Valor da diária
          </p>
          <p className="mt-1 text-2xl font-semibold text-veloz-azul">
            {moeda(carro.valorDiaria)}
          </p>
        </Cartao>
        <Cartao className="p-5">
          <p className="text-xs font-medium uppercase tracking-wide text-slate-500">
            Quilometragem
          </p>
          <p className="mt-1 text-2xl font-semibold text-veloz-grafite">
            {numero(carro.quilometragem)}
          </p>
        </Cartao>
        <Cartao className="p-5">
          <p className="text-xs font-medium uppercase tracking-wide text-slate-500">Aluguéis</p>
          <p className="mt-1 text-2xl font-semibold text-veloz-grafite">{historico.length}</p>
        </Cartao>
      </section>

      <Cartao className="p-5">
        <h2 className="font-semibold text-veloz-grafite">Alterar status</h2>
        {possiveis.length === 0 ? (
          <p className="mt-2 text-sm text-slate-500">
            Este carro está desativado e não tem transições disponíveis. O status
            <span className="font-medium text-slate-700"> desativado </span>
            é terminal.
          </p>
        ) : (
          <div className="mt-3 flex flex-wrap gap-2">
            {possiveis.map((s) => (
              <Botao
                key={s}
                variante="secundario"
                disabled={ocupado}
                onClick={() => mudarStatus(s)}
              >
                Mover para {rotulo(s)}
              </Botao>
            ))}
          </div>
        )}
      </Cartao>

      <section className="space-y-3">
        <h2 className="text-lg font-semibold text-veloz-grafite">Histórico de aluguéis</h2>
        {historico.length === 0 ? (
          <Cartao className="p-6">
            <p className="text-sm text-slate-500">Este carro ainda não foi alugado.</p>
            <Link to="/alugueis/novo">
              <Botao variante="secundario" className="mt-3">
                Registrar aluguel
              </Botao>
            </Link>
          </Cartao>
        ) : (
          <div className="space-y-2">
            {historico.map((a) => (
              <Link key={a.id} to={`/alugueis/${a.id}`}>
                <Cartao interativo className="flex flex-wrap items-center justify-between gap-3 p-4">
                  <div className="min-w-0">
                    <p className="truncate font-medium text-veloz-grafite">{a.pessoa.nome}</p>
                    <p className="text-sm text-slate-500">
                      {fmtData(a.dataInicio)} → {dataHora(a.dataDevolucao ?? a.dataPrevistaDevolucao)}
                    </p>
                  </div>
                  <div className="flex items-center gap-3">
                    <Selo cor={corDoStatusPagamento[a.statusPagamento]}>
                      {rotulo(a.statusPagamento)}
                    </Selo>
                    <Selo cor={corDoStatusAluguel[a.status]}>{rotulo(a.status)}</Selo>
                    <span className="font-semibold tabular-nums text-veloz-grafite">
                      {moeda(a.valorTotal)}
                    </span>
                  </div>
                </Cartao>
              </Link>
            ))}
          </div>
        )}
      </section>

      <Cartao className="flex flex-wrap items-center justify-between gap-3 p-5">
        <div>
          <h2 className="font-semibold text-veloz-grafite">Remover carro</h2>
          <p className="text-sm text-slate-500">
            Só é possível se não houver nenhum aluguel vinculado.
          </p>
        </div>
        <Botao variante="perigo" disabled={ocupado} onClick={remover}>
          Remover
        </Botao>
      </Cartao>
    </div>
  )
}
