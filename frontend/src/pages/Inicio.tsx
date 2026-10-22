import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { alugueisApi, carrosApi, relatoriosApi } from '../api/endpoints'
import { ApiError } from '../api/client'
import { Aviso, Botao, Carregando, Cartao, CartaoChaveLink, Metrica, Selo, TituloPagina } from '../components/ui'
import { corDoStatusCarro, dataHora, mesCorrente, moeda, numero, rotulo } from '../components/formato'
import type { Aluguel, Carro, RelatorioMovimentacao } from '../types/api'

/*
  Tela inicial.

  Nao e um dashboard de graficos: e o painel do patio. A pergunta que ela
  responde e "o que eu faco a seguir", nao "como foi o mes".

  Por isso os carros disponiveis sao o elemento mais alto da tela — sao a unica
  coisa ali que representa uma acao possivel agora. Os numeros do mes ficam
  abaixo, em peso de leitura, porque informam mas nao pedem nada.
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
        if (rDisponiveis.status === 'fulfilled') setDisponiveis(rDisponiveis.value.conteudo)
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
    <div className="space-y-10">
      <TituloPagina
        apoio="O que precisa de decisão hoje e o estado da frota."
        acao={
          <Link to="/alugueis/novo">
            <Botao>Entregar um carro</Botao>
          </Link>
        }
      >
        Início
      </TituloPagina>

      {erro && <Aviso>{erro}</Aviso>}

      {/*
        Atrasos vem primeiro, e com peso de alerta. Um aluguel vencido e a unica
        coisa nesta tela que piora sozinha com o tempo.
      */}
      {atrasados.length > 0 && (
        <section className="space-y-3">
          <div className="flex items-baseline gap-3">
            <h2 className="font-titulo text-xl font-semibold text-veloz-azul-leitura">
              Precisam voltar
            </h2>
            <span className="rounded-veloz bg-red-100 px-2 py-0.5 text-xs font-semibold text-red-800">
              {atrasados.length} atrasado{atrasados.length === 1 ? '' : 's'}
            </span>
          </div>
          <div className="grid gap-3 md:grid-cols-2">
            {atrasados.map((a) => (
              <Link key={a.id} to={`/alugueis/${a.id}`}>
                <Cartao
                  interativo
                  className="flex items-center justify-between gap-4 border-l-2 border-red-500 p-4"
                >
                  <div className="min-w-0">
                    <p className="truncate font-semibold text-veloz-grafite">{a.pessoa.nome}</p>
                    <p className="truncate text-sm text-slate-600">
                      {a.carro.marca} {a.carro.modelo} · <span className="font-mono">{a.carro.placa}</span>
                    </p>
                  </div>
                  <div className="shrink-0 text-right">
                    <p className="text-xs text-red-700">previsto para</p>
                    <p className="text-sm font-medium tabular text-slate-700">
                      {dataHora(a.dataPrevistaDevolucao)}
                    </p>
                  </div>
                </Cartao>
              </Link>
            ))}
          </div>
        </section>
      )}

      {/*
        O patio: carros disponiveis como cartoes-chave. So os seis primeiros,
        porque a lista completa vive na Frota — aqui a funcao e facilitar a
        escolha imediata, nao listar estoque.
      */}
      <section className="space-y-3">
        <div className="flex items-baseline justify-between gap-4">
          <h2 className="font-titulo text-xl font-semibold text-veloz-azul-leitura">
            Prontos para sair
          </h2>
          <Link
            to="/frota"
            className="text-sm font-medium text-veloz-azul-leitura underline-offset-4 hover:underline"
          >
            Ver frota completa
          </Link>
        </div>

        {disponiveis.length === 0 ? (
          <Cartao className="p-6">
            <p className="text-sm text-slate-600">
              Nenhum carro disponível agora. Para liberar um, registre a devolução de um aluguel em
              aberto; para aumentar a frota, cadastre um veículo.
            </p>
            <div className="mt-3 flex gap-2">
              <Link to="/alugueis?status=ATIVO">
                <Botao variante="secundario">Ver aluguéis em aberto</Botao>
              </Link>
              <Link to="/frota">
                <Botao variante="secundario">Ir para a frota</Botao>
              </Link>
            </div>
          </Cartao>
        ) : (
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {disponiveis.slice(0, 6).map((c) => (
              <CartaoChaveLink key={c.id} para={`/frota/${c.id}`} className="h-full">
                <span className="flex items-start justify-between gap-3">
                  <span className="min-w-0">
                    <span className="block truncate font-semibold text-veloz-grafite">
                      {c.marca} {c.modelo}
                    </span>
                    <span className="block truncate text-sm text-slate-600">
                      {c.ano} · {c.cor}
                    </span>
                  </span>
                  <Selo cor={corDoStatusCarro[c.status]}>{rotulo(c.status)}</Selo>
                </span>
                <span className="mt-4 flex items-end justify-between">
                  <span className="rounded-veloz bg-slate-100 px-2 py-1 font-mono text-sm font-medium tracking-wider text-slate-700">
                    {c.placa}
                  </span>
                  <span className="text-sm font-semibold tabular text-veloz-azul">
                    {moeda(c.valorDiaria)}
                    <span className="text-xs font-normal text-slate-600">/dia</span>
                  </span>
                </span>
              </CartaoChaveLink>
            ))}
          </div>
        )}
      </section>

      {/*
        Os numeros do mes. Tratados como frase, nao como quatro caixas: o que
        se le primeiro e a receita, e o resto acompanha em texto menor.
      */}
      <section className="space-y-4 border-t border-slate-200 pt-8">
        <h2 className="font-titulo text-xl font-semibold text-veloz-azul-leitura">O mês até agora</h2>

        <div className="grid gap-8 sm:grid-cols-2 lg:grid-cols-4">
          <Metrica
            rotulo="Receita do mês"
            valor={moeda(resumo?.receitaTotal)}
            detalhe={`${moeda(resumo?.receitaPaga)} recebido, ${moeda(resumo?.receitaPendente)} a receber`}
            destaque
          />
          <Metrica
            rotulo="Aluguéis no mês"
            valor={numero(resumo?.totalAlugueis)}
            detalhe={`${numero(resumo?.totalAtivos)} em aberto, ${numero(resumo?.totalConcluidos)} concluídos`}
          />
          <Metrica
            rotulo="Ticket médio"
            valor={moeda(resumo?.ticketMedio)}
            detalhe={`${numero(resumo?.quilometragemPercorrida)} km rodados`}
          />
          <Metrica
            rotulo="Disponíveis agora"
            valor={numero(disponiveis.length)}
            detalhe={`${numero(ativos.length)} alugado${ativos.length === 1 ? '' : 's'} nesta frota`}
          />
        </div>
      </section>
    </div>
  )
}
