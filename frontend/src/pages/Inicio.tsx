import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { alugueisApi, carrosApi, relatoriosApi } from '../api/endpoints'
import { ApiError } from '../api/client'
import { Aviso, Botao, Carregando, Cartao, Metrica, Selo } from '../components/ui'
import { corDoStatusCarro, dataHora, mesCorrente, moeda, numero, rotulo } from '../components/formato'
import type { Aluguel, Carro, RelatorioMovimentacao } from '../types/api'

/*
  Tela inicial.

  Nao e um dashboard de graficos: e a resposta a "como a locadora esta agora".
  Tres numeros do mes corrente, o que precisa de acao (atrasados) e o estado da
  frota. Se um dado nao ajuda a decidir algo hoje, ele nao esta aqui.
*/
export function Inicio() {
  const [resumo, setResumo] = useState<RelatorioMovimentacao | null>(null)
  const [disponiveis, setDisponiveis] = useState<Carro[]>([])
  const [ativos, setAtivos] = useState<Aluguel[]>([])
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState<string | null>(null)

  useEffect(() => {
    const { de, ate } = mesCorrente()

    // As tres chamadas sao independentes: uma falhar nao deve esconder as
    // outras, por isso allSettled em vez de all.
    Promise.allSettled([
      relatoriosApi.movimentacao(de, ate),
      carrosApi.disponiveis(),
      alugueisApi.listar({ status: 'ATIVO', tamanho: 100 }),
    ])
      .then(([rResumo, rDisponiveis, rAtivos]) => {
        if (rResumo.status === 'fulfilled') setResumo(rResumo.value)
        if (rDisponiveis.status === 'fulfilled') setDisponiveis(rDisponiveis.value)
        if (rAtivos.status === 'fulfilled') setAtivos(rAtivos.value.conteudo)

        const falhou = [rResumo, rDisponiveis, rAtivos].find((r) => r.status === 'rejected')
        if (falhou?.status === 'rejected') {
          const causa = falhou.reason
          setErro(causa instanceof ApiError ? causa.message : 'Falha ao carregar os dados.')
        }
      })
      .finally(() => setCarregando(false))
  }, [])

  if (carregando) return <Carregando />

  const atrasados = ativos.filter((a) => a.atrasado)

  return (
    <div className="space-y-8">
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold tracking-tight text-veloz-grafite">Início</h1>
          <p className="mt-1 text-sm text-slate-500">
            Movimento do mês corrente e o que precisa de atenção.
          </p>
        </div>
        <Link to="/alugueis/novo">
          <Botao>Registrar aluguel</Botao>
        </Link>
      </header>

      {erro && <Aviso>{erro}</Aviso>}

      <section className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <Cartao className="p-5">
          <Metrica
            rotulo="Receita do mês"
            valor={moeda(resumo?.receitaTotal)}
            destaque
          />
          <p className="mt-2 text-xs text-slate-500">
            {moeda(resumo?.receitaPaga)} recebido · {moeda(resumo?.receitaPendente)} a receber
          </p>
        </Cartao>

        <Cartao className="p-5">
          <Metrica rotulo="Aluguéis no mês" valor={numero(resumo?.totalAlugueis)} destaque />
          <p className="mt-2 text-xs text-slate-500">
            {numero(resumo?.totalAtivos)} ativos · {numero(resumo?.totalConcluidos)} concluídos
          </p>
        </Cartao>

        <Cartao className="p-5">
          <Metrica rotulo="Ticket médio" valor={moeda(resumo?.ticketMedio)} destaque />
          <p className="mt-2 text-xs text-slate-500">
            {numero(resumo?.quilometragemPercorrida)} km percorridos
          </p>
        </Cartao>

        <Cartao className="p-5">
          <Metrica rotulo="Disponíveis agora" valor={numero(disponiveis.length)} destaque />
          <p className="mt-2 text-xs text-slate-500">{numero(ativos.length)} carros alugados</p>
        </Cartao>
      </section>

      {atrasados.length > 0 && (
        <section className="space-y-3">
          <h2 className="text-lg font-semibold text-veloz-grafite">
            Devoluções atrasadas
            <span className="ml-2 rounded-full bg-red-100 px-2 py-0.5 text-xs font-medium text-red-800">
              {atrasados.length}
            </span>
          </h2>
          <div className="grid gap-3 md:grid-cols-2">
            {atrasados.map((a) => (
              <Link key={a.id} to={`/alugueis/${a.id}`}>
                <Cartao interativo className="flex items-center justify-between gap-4 p-4">
                  <div className="min-w-0">
                    <p className="truncate font-semibold text-veloz-grafite">{a.pessoa.nome}</p>
                    <p className="truncate text-sm text-slate-500">
                      {a.carro.marca} {a.carro.modelo} · {a.carro.placa}
                    </p>
                  </div>
                  <div className="shrink-0 text-right">
                    <p className="text-xs font-medium text-red-700">previsto</p>
                    <p className="text-sm font-medium text-slate-700">
                      {dataHora(a.dataPrevistaDevolucao)}
                    </p>
                  </div>
                </Cartao>
              </Link>
            ))}
          </div>
        </section>
      )}

      <section className="space-y-3">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-semibold text-veloz-grafite">Carros disponíveis</h2>
          <Link to="/frota" className="text-sm font-medium text-veloz-azul hover:underline">
            Ver frota completa
          </Link>
        </div>

        {disponiveis.length === 0 ? (
          <Cartao className="p-6">
            <p className="text-sm text-slate-500">
              Nenhum carro disponível. Cadastre um veículo na frota para começar.
            </p>
            <Link to="/frota">
              <Botao variante="secundario" className="mt-3">
                Ir para a frota
              </Botao>
            </Link>
          </Cartao>
        ) : (
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {disponiveis.slice(0, 6).map((c) => (
              <Link key={c.id} to={`/frota/${c.id}`}>
                <Cartao interativo className="p-4">
                  <div className="flex items-start justify-between gap-3">
                    <div className="min-w-0">
                      <p className="truncate font-semibold text-veloz-grafite">
                        {c.marca} {c.modelo}
                      </p>
                      <p className="truncate text-sm text-slate-500">
                        {c.ano} · {c.cor}
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
                </Cartao>
              </Link>
            ))}
          </div>
        )}
      </section>
    </div>
  )
}
