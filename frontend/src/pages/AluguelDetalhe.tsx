import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { alugueisApi } from '../api/endpoints'
import { ApiError } from '../api/client'
import { Aviso, Botao, Campo, Carregando, Cartao, Selo, estiloInput } from '../components/ui'
import {
  corDoStatusAluguel,
  corDoStatusPagamento,
  dataHora,
  moeda,
  numero,
  paraIsoLocal,
  rotulo,
} from '../components/formato'
import type { Aluguel } from '../types/api'

/**
 * Detalhe do aluguel: onde o ciclo de vida acontece.
 *
 * Cada acao so aparece quando e valida no estado atual (devolver so se ATIVO,
 * pagar so se CONCLUIDO e PENDENTE). Isso evita botoes que so podem falhar —
 * o backend rejeita esses casos com 422, mas nao oferecer e melhor que recusar.
 */
export function AluguelDetalhe() {
  const { id } = useParams<{ id: string }>()

  const [aluguel, setAluguel] = useState<Aluguel | null>(null)
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState<string | null>(null)
  const [acaoErro, setAcaoErro] = useState<string | null>(null)
  const [ocupado, setOcupado] = useState(false)

  const [kmFinal, setKmFinal] = useState('')
  const [dataDevolucao, setDataDevolucao] = useState(() => paraIsoLocal(new Date()))
  const [motivo, setMotivo] = useState('')

  useEffect(() => {
    if (!id) return
    alugueisApi
      .buscar(id)
      .then((a) => {
        setAluguel(a)
        // Sugere a km de retorno igual a de saida; o operador ajusta.
        setKmFinal(String(a.quilometragemInicial))
      })
      .catch((e) => setErro(e instanceof ApiError ? e.message : 'Aluguel não encontrado.'))
      .finally(() => setCarregando(false))
  }, [id])

  async function executar(acao: () => Promise<Aluguel>) {
    setOcupado(true)
    setAcaoErro(null)
    try {
      setAluguel(await acao())
    } catch (e) {
      setAcaoErro(e instanceof ApiError ? e.message : 'A ação não pôde ser concluída.')
    } finally {
      setOcupado(false)
    }
  }

  async function devolver(e: React.FormEvent) {
    e.preventDefault()
    if (!id || !aluguel) return

    const km = Number(kmFinal)
    if (km < aluguel.quilometragemInicial) {
      setAcaoErro(
        `A quilometragem final não pode ser menor que a inicial (${numero(aluguel.quilometragemInicial)} km).`,
      )
      return
    }

    await executar(() =>
      alugueisApi.devolver(id, { dataDevolucao: dataDevolucao, quilometragemFinal: km }),
    )
  }

  if (carregando) return <Carregando />
  if (erro || !aluguel)
    return (
      <div className="space-y-4">
        <Aviso>{erro ?? 'Aluguel não encontrado.'}</Aviso>
        <Link to="/alugueis">
          <Botao variante="secundario">Voltar para aluguéis</Botao>
        </Link>
      </div>
    )

  const podeDevolver = aluguel.status === 'ATIVO'
  const podePagar = aluguel.status === 'CONCLUIDO' && aluguel.statusPagamento === 'PENDENTE'
  const podeCancelar = aluguel.status === 'ATIVO'

  return (
    <div className="space-y-6">
      <Link to="/alugueis" className="text-sm font-medium text-veloz-azul hover:underline">
        ← Aluguéis
      </Link>

      <header className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <div className="flex flex-wrap items-center gap-3">
            <h1 className="text-3xl font-bold tracking-tight text-veloz-grafite">
              {aluguel.pessoa.nome}
            </h1>
            <Selo cor={corDoStatusAluguel[aluguel.status]}>{rotulo(aluguel.status)}</Selo>
            <Selo cor={corDoStatusPagamento[aluguel.statusPagamento]}>
              {rotulo(aluguel.statusPagamento)}
            </Selo>
            {aluguel.atrasado && (
              <span className="rounded-full bg-red-100 px-2.5 py-0.5 text-xs font-medium text-red-800">
                atrasado
              </span>
            )}
          </div>
          <p className="mt-1 text-sm text-slate-500">CPF {aluguel.pessoa.cpf}</p>
        </div>

        <div className="text-right">
          <p className="text-xs font-medium uppercase tracking-wide text-slate-500">Total</p>
          <p className="text-3xl font-bold text-veloz-azul">{moeda(aluguel.valorTotal)}</p>
          <p className="text-xs text-slate-500">
            {aluguel.totalDiarias} diária(s) × {moeda(aluguel.valorDiaria)}
          </p>
        </div>
      </header>

      {acaoErro && <Aviso>{acaoErro}</Aviso>}

      <section className="grid gap-4 sm:grid-cols-2">
        <Cartao className="p-5">
          <h2 className="font-semibold text-veloz-grafite">Veículo</h2>
          <p className="mt-2 font-medium text-slate-700">
            {aluguel.carro.marca} {aluguel.carro.modelo}
          </p>
          <p className="text-sm text-slate-500">
            {aluguel.carro.cor} · {aluguel.carro.categoria}
          </p>
          <span className="mt-3 inline-block rounded-lg bg-slate-100 px-3 py-1 font-mono text-sm font-medium tracking-wider text-slate-700">
            {aluguel.carro.placa}
          </span>
        </Cartao>

        <Cartao className="p-5">
          <h2 className="font-semibold text-veloz-grafite">Período</h2>
          <dl className="mt-2 space-y-1.5 text-sm">
            <div className="flex justify-between">
              <dt className="text-slate-500">Retirada</dt>
              <dd className="font-medium text-slate-700">{dataHora(aluguel.dataInicio)}</dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-slate-500">Prevista</dt>
              <dd className="font-medium text-slate-700">
                {dataHora(aluguel.dataPrevistaDevolucao)}
              </dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-slate-500">Devolvido</dt>
              <dd className="font-medium text-slate-700">
                {aluguel.dataDevolucao ? dataHora(aluguel.dataDevolucao) : '—'}
              </dd>
            </div>
          </dl>
        </Cartao>

        <Cartao className="p-5">
          <h2 className="font-semibold text-veloz-grafite">Quilometragem</h2>
          <dl className="mt-2 space-y-1.5 text-sm">
            <div className="flex justify-between">
              <dt className="text-slate-500">Inicial</dt>
              <dd className="font-medium tabular-nums text-slate-700">
                {numero(aluguel.quilometragemInicial)}
              </dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-slate-500">Final</dt>
              <dd className="font-medium tabular-nums text-slate-700">
                {aluguel.quilometragemFinal !== null ? numero(aluguel.quilometragemFinal) : '—'}
              </dd>
            </div>
            <div className="flex justify-between border-t border-slate-100 pt-1.5">
              <dt className="font-medium text-slate-600">Percorrida</dt>
              <dd className="font-semibold tabular-nums text-veloz-azul">
                {aluguel.quilometragemPercorrida !== null
                  ? `${numero(aluguel.quilometragemPercorrida)} km`
                  : '—'}
              </dd>
            </div>
          </dl>
        </Cartao>

        {aluguel.observacoes && (
          <Cartao className="p-5">
            <h2 className="font-semibold text-veloz-grafite">Observações</h2>
            <p className="mt-2 text-sm text-slate-600">{aluguel.observacoes}</p>
            <p className="mt-2 text-xs text-slate-400">
              Texto livre cifrado no banco; aparece só no detalhe.
            </p>
          </Cartao>
        )}
      </section>

      {podeDevolver && (
        <Cartao className="p-6">
          <h2 className="text-lg font-semibold text-veloz-grafite">Registrar devolução</h2>
          <p className="mt-1 text-sm text-slate-500">
            A quilometragem final precisa ser maior ou igual à inicial (
            {numero(aluguel.quilometragemInicial)} km).
          </p>
          <form onSubmit={devolver} className="mt-4 flex flex-wrap items-end gap-3">
            <Campo rotulo="Data da devolução" className="w-60">
              <input
                type="datetime-local"
                className={estiloInput}
                value={dataDevolucao.slice(0, 16)}
                onChange={(e) => setDataDevolucao(e.target.value + ':00')}
              />
            </Campo>
            <Campo rotulo="Quilometragem final" className="w-48">
              <input
                type="number"
                className={estiloInput}
                value={kmFinal}
                onChange={(e) => setKmFinal(e.target.value)}
              />
            </Campo>
            <Botao type="submit" disabled={ocupado}>
              {ocupado ? 'Registrando...' : 'Confirmar devolução'}
            </Botao>
          </form>
        </Cartao>
      )}

      {(podePagar || podeCancelar) && (
        <Cartao className="flex flex-wrap items-center gap-3 p-6">
          {podePagar && (
            <>
              <div className="mr-auto">
                <h2 className="font-semibold text-veloz-grafite">Registrar pagamento</h2>
                <p className="text-sm text-slate-500">
                  Marca {moeda(aluguel.valorTotal)} como recebido.
                </p>
              </div>
              <Botao disabled={ocupado} onClick={() => id && executar(() => alugueisApi.pagar(id))}>
                Marcar como pago
              </Botao>
            </>
          )}

          {podeCancelar && (
            <>
              <div className={podePagar ? '' : 'mr-auto'}>
                <h2 className="font-semibold text-veloz-grafite">Cancelar aluguel</h2>
                <p className="text-sm text-slate-500">
                  O valor sai das somas dos relatórios.
                </p>
              </div>
              <div className="flex items-end gap-2">
                <Campo rotulo="Motivo (opcional)" className="w-56">
                  <input
                    className={estiloInput}
                    value={motivo}
                    onChange={(e) => setMotivo(e.target.value)}
                    placeholder="cliente desistiu"
                  />
                </Campo>
                <Botao
                  variante="perigo"
                  disabled={ocupado}
                  onClick={() => {
                    if (!id) return
                    if (!confirm('Cancelar este aluguel?')) return
                    executar(() => alugueisApi.cancelar(id, motivo || undefined))
                  }}
                >
                  Cancelar
                </Botao>
              </div>
            </>
          )}
        </Cartao>
      )}

      <Cartao className="p-5">
        <h2 className="font-semibold text-veloz-grafite">Veículo no sistema</h2>
        <p className="mt-1 text-sm text-slate-500">
          Estado atual do carro na frota, para conferência.
        </p>
        <Link to={`/frota/${aluguel.carro.id}`}>
          <Botao variante="secundario" className="mt-3">
            Abrir {aluguel.carro.placa}
          </Botao>
        </Link>
      </Cartao>
    </div>
  )
}
