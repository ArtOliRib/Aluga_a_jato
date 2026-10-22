import { useEffect, useState } from 'react'
import { relatoriosApi } from '../api/endpoints'
import { ApiError } from '../api/client'
import {
  Aviso,
  Botao,
  Campo,
  Carregando,
  Cartao,
  Metrica,
  estiloInput,
  TituloPagina,
} from '../components/ui'
import { mesCorrente, moeda, numero } from '../components/formato'
import type { MovimentacaoPorCarro, RelatorioMovimentacao } from '../types/api'

/** Atalhos de periodo. O mes corrente e o caso mais comum. */
function periodoAtalho(tipo: 'mes' | 'mesPassado' | 'ano'): { de: string; ate: string } {
  const agora = new Date()

  if (tipo === 'mes') return mesCorrente()

  if (tipo === 'mesPassado') {
    const inicio = new Date(agora.getFullYear(), agora.getMonth() - 1, 1, 0, 0, 0)
    const fim = new Date(agora.getFullYear(), agora.getMonth(), 0, 23, 59, 59)
    return { de: paraIso(inicio), ate: paraIso(fim) }
  }

  const inicio = new Date(agora.getFullYear(), 0, 1, 0, 0, 0)
  const fim = new Date(agora.getFullYear(), 11, 31, 23, 59, 59)
  return { de: paraIso(inicio), ate: paraIso(fim) }
}

function paraIso(d: Date): string {
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}T${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

/*
  Relatorios: o fechamento do periodo.

  O grafico de barras do ranking e CSS puro (largura percentual), sem biblioteca.
  Uma lib de graficos aqui custaria ~100KB para desenhar uma barra — e as barras
  sao a unica forma que este dado pede.

  A barra e solida, nao em gradiente. Gradiente aqui nao comunicaria nada: a
  informacao esta no comprimento, e um degrade so adiciona ruido visual sobre a
  escala.
*/
export function Relatorios() {
  const inicial = mesCorrente()
  const [de, setDe] = useState(inicial.de.slice(0, 10))
  const [ate, setAte] = useState(inicial.ate.slice(0, 10))

  const [resumo, setResumo] = useState<RelatorioMovimentacao | null>(null)
  const [ranking, setRanking] = useState<MovimentacaoPorCarro[]>([])
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState<string | null>(null)

  function consultar(inicioDia: string, fimDia: string) {
    setCarregando(true)
    setErro(null)

    // A API espera ISO-8601 com hora; o input type=date so da a data. Completar
    // aqui evita o 400 de parametro invalido.
    const deIso = `${inicioDia}T00:00:00`
    const ateIso = `${fimDia}T23:59:59`

    Promise.allSettled([
      relatoriosApi.movimentacao(deIso, ateIso),
      relatoriosApi.movimentacaoPorCarro(deIso, ateIso),
    ])
      .then(([rResumo, rRanking]) => {
        if (rResumo.status === 'fulfilled') setResumo(rResumo.value)
        if (rRanking.status === 'fulfilled') setRanking(rRanking.value)

        const falhou = [rResumo, rRanking].find((r) => r.status === 'rejected')
        if (falhou?.status === 'rejected') {
          const causa = falhou.reason
          setErro(causa instanceof ApiError ? causa.message : 'Falha ao carregar os relatórios.')
        }
      })
      .finally(() => setCarregando(false))
  }

  useEffect(() => consultar(inicial.de.slice(0, 10), inicial.ate.slice(0, 10)), [])

  function aplicarPeriodo(tipo: 'mes' | 'mesPassado' | 'ano') {
    const { de: d, ate: a } = periodoAtalho(tipo)
    const dDia = d.slice(0, 10)
    const aDia = a.slice(0, 10)
    setDe(dDia)
    setAte(aDia)
    consultar(dDia, aDia)
  }

  const maiorReceita = Math.max(...ranking.map((r) => r.receitaTotal), 1)

  return (
    <div className="space-y-6">
      <TituloPagina apoio="Fechamento financeiro do período. Aluguéis cancelados não entram nas somas.">
        Relatórios
      </TituloPagina>

      <Cartao className="flex flex-wrap items-end gap-3 p-4">
        <Campo rotulo="De" className="w-40">
          <input type="date" className={estiloInput} value={de} onChange={(e) => setDe(e.target.value)} />
        </Campo>
        <Campo rotulo="Até" className="w-40">
          <input type="date" className={estiloInput} value={ate} onChange={(e) => setAte(e.target.value)} />
        </Campo>
        <Botao onClick={() => consultar(de, ate)}>Consultar</Botao>

        <div className="ml-auto flex gap-2">
          <Botao variante="secundario" onClick={() => aplicarPeriodo('mes')}>
            Este mês
          </Botao>
          <Botao variante="secundario" onClick={() => aplicarPeriodo('mesPassado')}>
            Mês passado
          </Botao>
          <Botao variante="secundario" onClick={() => aplicarPeriodo('ano')}>
            Este ano
          </Botao>
        </div>
      </Cartao>

      {erro && <Aviso>{erro}</Aviso>}
      {carregando && <Carregando />}

      {!carregando && resumo && (
        <>
          <section className="space-y-4">
            <div className="grid gap-8 sm:grid-cols-2 lg:grid-cols-4">
              <Metrica
                rotulo="Receita total"
                valor={moeda(resumo.receitaTotal)}
                detalhe="No período selecionado"
                destaque
              />
              <Metrica
                rotulo="Recebido"
                valor={moeda(resumo.receitaPaga)}
                detalhe={`${moeda(resumo.receitaPendente)} pendente`}
              />
              <Metrica
                rotulo="Ticket médio"
                valor={moeda(resumo.ticketMedio)}
                detalhe={`${numero(resumo.totalAlugueis)} aluguéis`}
              />
              <Metrica
                rotulo="Km percorridos"
                valor={numero(resumo.quilometragemPercorrida)}
                detalhe={`${numero(resumo.totalAtivos)} em aberto, ${numero(resumo.totalConcluidos)} concluídos`}
              />
            </div>
          </section>

          <section className="space-y-3 border-t border-slate-200 pt-8">
            <h2 className="font-titulo text-xl font-semibold text-veloz-azul-leitura">
              Receita por veículo
            </h2>

            {ranking.length === 0 ? (
              <Cartao className="p-6">
                <p className="text-sm text-slate-600">
                  Nenhum aluguel no período selecionado. Ajuste as datas ou escolha um atalho de
                  período acima.
                </p>
              </Cartao>
            ) : (
              <Cartao className="divide-y divide-slate-100">
                {ranking.map((r) => (
                  <div key={r.carroId} className="p-4">
                    <div className="flex flex-wrap items-center justify-between gap-3">
                      <div className="min-w-0">
                        <p className="truncate font-medium text-veloz-grafite">
                          {r.marca} {r.modelo}
                        </p>
                        <p className="text-sm text-slate-600">
                          <span className="font-mono tracking-wider">{r.placa}</span> ·{' '}
                          {numero(r.totalAlugueis)} aluguel(is) · {numero(r.quilometragemPercorrida)} km
                        </p>
                      </div>
                      <div className="text-right">
                        <p className="font-titulo text-lg font-semibold tabular text-veloz-grafite">
                          {moeda(r.receitaTotal)}
                        </p>
                        <p className="text-xs tabular text-slate-600">
                          {r.percentualDaReceita.toFixed(1)}% da receita
                        </p>
                      </div>
                    </div>

                    {/* Barra proporcional: comunica a comparacao entre carros
                        melhor que o numero sozinho. */}
                    <div className="mt-2.5 h-1.5 overflow-hidden rounded-full bg-slate-100">
                      <div
                        className="h-full rounded-full bg-veloz-azul"
                        style={{ width: `${(r.receitaTotal / maiorReceita) * 100}%` }}
                      />
                    </div>
                  </div>
                ))}
              </Cartao>
            )}
          </section>
        </>
      )}
    </div>
  )
}
