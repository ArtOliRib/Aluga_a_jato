import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { alugueisApi, carrosApi } from '../api/endpoints'
import { ApiError } from '../api/client'
import { Aviso, Botao, Campo, Carregando, Cartao, estiloInput } from '../components/ui'
import { moeda, numero, paraIsoLocal, rotulo } from '../components/formato'
import type { Carro } from '../types/api'

/*
  Registro de aluguel: o ato central do sistema.

  Duas decisoes de layout que refletem o dominio:

  1. O carro vem primeiro, como lista de cards selecionaveis, e nao como um
     <select> de nomes. O operador escolhe olhando placa e valor da diaria, que
     sao o que ele precisa conferir; um dropdown esconderia justamente isso.

  2. So aparecem carros DISPONIVEL. Oferecer um carro alugado levaria a um 422
     previsivel — melhor nao oferecer.
*/
export function NovoAluguel() {
  const navigate = useNavigate()

  const [carros, setCarros] = useState<Carro[]>([])
  const [carregando, setCarregando] = useState(true)
  const [selecionado, setSelecionado] = useState<Carro | null>(null)
  const [erro, setErro] = useState<string | null>(null)
  const [errosCampo, setErrosCampo] = useState<Record<string, string>>({})
  const [salvando, setSalvando] = useState(false)

  const [pessoa, setPessoa] = useState({
    nome: '',
    cpf: '',
    email: '',
    telefone: '',
    cnh: '',
    observacoes: '',
  })

  // Prazo padrao: 7 dias. E o caso mais comum e evita campo vazio.
  const [devolucao, setDevolucao] = useState(() => {
    const d = new Date()
    d.setDate(d.getDate() + 7)
    return paraIsoLocal(d)
  })

  useEffect(() => {
    carrosApi
      .disponiveis()
      .then((p) => setCarros(p.conteudo))
      .catch((e) => setErro(e instanceof ApiError ? e.message : 'Falha ao carregar os carros.'))
      .finally(() => setCarregando(false))
  }, [])

  async function enviar(e: React.FormEvent) {
    e.preventDefault()
    if (!selecionado) {
      setErro('Selecione um carro antes de registrar.')
      return
    }

    setSalvando(true)
    setErro(null)
    setErrosCampo({})

    try {
      const aluguel = await alugueisApi.registrar({
        ...pessoa,
        carroId: selecionado.id,
        dataPrevistaDevolucao: devolucao,
      })
      navigate(`/alugueis/${aluguel.id}`)
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.campos.length > 0) {
          setErrosCampo(Object.fromEntries(err.campos.map((c) => [c.campo, c.mensagem])))
        } else {
          setErro(err.message)
        }
      } else {
        setErro('Falha ao registrar o aluguel.')
      }
      setSalvando(false)
    }
  }

  return (
    <div className="space-y-6">
      <Link to="/alugueis" className="text-sm font-medium text-veloz-azul hover:underline">
        ← Aluguéis
      </Link>

      <header>
        <h1 className="text-3xl font-bold tracking-tight text-veloz-grafite">Registrar aluguel</h1>
        <p className="mt-1 text-sm text-slate-500">
          Os dados da pessoa vão cifrados para o banco, com índice para busca.
        </p>
      </header>

      {erro && <Aviso>{erro}</Aviso>}

      <form onSubmit={enviar} className="space-y-6">
        <section className="space-y-3">
          <h2 className="text-lg font-semibold text-veloz-grafite">1. Escolha o carro</h2>

          {carregando ? (
            <Carregando texto="Carregando carros disponíveis..." />
          ) : carros.length === 0 ? (
            <Cartao className="p-6">
              <p className="text-sm text-slate-500">
                Nenhum carro disponível no momento. Cadastre um veículo ou devolva um aluguel
                em aberto.
              </p>
              <Link to="/frota">
                <Botao variante="secundario" className="mt-3">
                  Ir para a frota
                </Botao>
              </Link>
            </Cartao>
          ) : (
            <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
              {carros.map((c) => {
                const ativo = selecionado?.id === c.id
                return (
                  <button
                    key={c.id}
                    type="button"
                    onClick={() => setSelecionado(c)}
                    className={`rounded-2xl p-4 text-left transition ${
                      ativo
                        ? 'bg-veloz-azul text-white shadow-veloz-alta'
                        : 'bg-white shadow-veloz ring-1 ring-slate-900/5 hover:-translate-y-0.5 hover:shadow-veloz-alta'
                    }`}
                  >
                    <p className={`font-semibold ${ativo ? 'text-white' : 'text-veloz-grafite'}`}>
                      {c.marca} {c.modelo}
                    </p>
                    <p className={`text-sm ${ativo ? 'text-blue-100' : 'text-slate-500'}`}>
                      {c.ano} · {c.cor} · {c.categoria}
                    </p>
                    <div className="mt-3 flex items-end justify-between">
                      <span
                        className={`rounded-lg px-2 py-1 font-mono text-sm font-medium tracking-wider ${
                          ativo ? 'bg-white/20 text-white' : 'bg-slate-100 text-slate-700'
                        }`}
                      >
                        {c.placa}
                      </span>
                      <span
                        className={`text-sm font-semibold ${ativo ? 'text-white' : 'text-veloz-azul'}`}
                      >
                        {moeda(c.valorDiaria)}
                        <span
                          className={`text-xs font-normal ${ativo ? 'text-blue-100' : 'text-slate-500'}`}
                        >
                          /dia
                        </span>
                      </span>
                    </div>
                    <p className={`mt-2 text-xs ${ativo ? 'text-blue-100' : 'text-slate-400'}`}>
                      {numero(c.quilometragem)} km
                    </p>
                  </button>
                )
              })}
            </div>
          )}
        </section>

        <section className="space-y-3">
          <h2 className="text-lg font-semibold text-veloz-grafite">2. Dados da pessoa</h2>
          <Cartao className="p-6">
            <div className="grid gap-4 sm:grid-cols-2">
              <Campo rotulo="Nome completo" erro={errosCampo.nome}>
                <input
                  className={estiloInput}
                  value={pessoa.nome}
                  onChange={(e) => setPessoa((p) => ({ ...p, nome: e.target.value }))}
                  placeholder="João Lima"
                />
              </Campo>

              <Campo rotulo="CPF" erro={errosCampo.cpf}>
                <input
                  className={estiloInput}
                  value={pessoa.cpf}
                  onChange={(e) => setPessoa((p) => ({ ...p, cpf: e.target.value }))}
                  placeholder="529.982.247-25"
                />
              </Campo>

              <Campo rotulo="E-mail" erro={errosCampo.email}>
                <input
                  type="email"
                  className={estiloInput}
                  value={pessoa.email}
                  onChange={(e) => setPessoa((p) => ({ ...p, email: e.target.value }))}
                  placeholder="joao@email.com"
                />
              </Campo>

              <Campo rotulo="Telefone" erro={errosCampo.telefone}>
                <input
                  className={estiloInput}
                  value={pessoa.telefone}
                  onChange={(e) => setPessoa((p) => ({ ...p, telefone: e.target.value }))}
                  placeholder="11988887777"
                />
              </Campo>

              <Campo rotulo="CNH" erro={errosCampo.cnh}>
                <input
                  className={estiloInput}
                  value={pessoa.cnh}
                  onChange={(e) => setPessoa((p) => ({ ...p, cnh: e.target.value }))}
                  placeholder="12345678901"
                />
              </Campo>

              <Campo rotulo="Devolução prevista" erro={errosCampo.dataPrevistaDevolucao}>
                <input
                  type="datetime-local"
                  className={estiloInput}
                  value={devolucao.slice(0, 16)}
                  onChange={(e) => setDevolucao(e.target.value + ':00')}
                />
              </Campo>

              <Campo rotulo="Observações (opcional)" erro={errosCampo.observacoes} className="sm:col-span-2">
                <textarea
                  rows={2}
                  className={estiloInput}
                  value={pessoa.observacoes}
                  onChange={(e) => setPessoa((p) => ({ ...p, observacoes: e.target.value }))}
                  placeholder="Cliente pediu cadeirinha"
                />
              </Campo>
            </div>
          </Cartao>
        </section>

        <Cartao className="flex flex-wrap items-center justify-between gap-4 p-5">
          <div>
            {selecionado ? (
              <>
                <p className="text-sm text-slate-500">Carro selecionado</p>
                <p className="font-semibold text-veloz-grafite">
                  {selecionado.marca} {selecionado.modelo} · {selecionado.placa}
                </p>
                <p className="text-xs text-slate-500">
                  Diária de {moeda(selecionado.valorDiaria)} · {rotulo(selecionado.status)}
                </p>
              </>
            ) : (
              <p className="text-sm text-slate-500">Escolha um carro acima para continuar.</p>
            )}
          </div>

          <div className="flex gap-2">
            <Link to="/alugueis">
              <Botao type="button" variante="secundario">
                Cancelar
              </Botao>
            </Link>
            <Botao type="submit" disabled={salvando || !selecionado}>
              {salvando ? 'Registrando...' : 'Registrar aluguel'}
            </Botao>
          </div>
        </Cartao>
      </form>
    </div>
  )
}
