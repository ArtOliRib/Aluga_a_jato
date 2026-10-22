import { useEffect, useMemo, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { alugueisApi, carrosApi } from '../api/endpoints'
import { ApiError } from '../api/client'
import {
  Aviso,
  Botao,
  Campo,
  Carregando,
  Cartao,
  CartaoChave,
  estiloInput,
  TituloPagina,
} from '../components/ui'
import { dataHora, moeda, numero, paraIsoLocal } from '../components/formato'
import type { AluguelRequest, Carro } from '../types/api'

/*
  Registro de aluguel: o ato central do sistema.

  A tela e um fluxo de quatro etapas porque a operacao tem uma ordem real:
  primeiro se identifica quem leva, depois qual carro sai, depois por quanto
  tempo e por quanto, e so entao se confirma. O stepper nao e decoracao — ele
  existe porque o conteudo e uma sequencia, e o operador precisa saber onde
  esta e o que falta.

  Duas decisoes que sustentam o resto:

  1. O resumo da reserva fica visivel o tempo inteiro, no desktop. O operador
     nao deve descobrir o valor total so no fim; ele negocia olhando o numero.

  2. O carro e escolhido em cartoes, nao em <select>. Placa, categoria e diaria
     sao o que o operador confere no patio — um dropdown esconderia justamente
     isso. A selecao usa contorno azul, para que a cor de acao continue
     significando uma coisa so.
*/

type Passo = 1 | 2 | 3 | 4

const PASSOS: { numero: Passo; titulo: string; descricao: string }[] = [
  { numero: 1, titulo: 'Condutor', descricao: 'Quem vai levar' },
  { numero: 2, titulo: 'Veículo', descricao: 'Qual carro sai' },
  { numero: 3, titulo: 'Período', descricao: 'Quando volta' },
  { numero: 4, titulo: 'Confirmação', descricao: 'Revise e registre' },
]

interface DadosCondutor {
  nome: string
  cpf: string
  email: string
  telefone: string
  cnh: string
  observacoes: string
}

function validarCondutor(p: DadosCondutor): Record<string, string> {
  const erros: Record<string, string> = {}
  const nome = p.nome.trim()
  const email = p.email.trim()
  const telefone = p.telefone.replace(/\D/g, '')
  const cpf = p.cpf.replace(/\D/g, '')

  if (!nome) erros.nome = 'Informe o nome completo.'
  else if (nome.length < 3) erros.nome = 'O nome precisa ter pelo menos 3 caracteres.'
  else if (nome.length > 150) erros.nome = 'O nome pode ter no máximo 150 caracteres.'

  if (!p.cpf.trim()) erros.cpf = 'Informe o CPF.'
  else if (cpf.length !== 11) erros.cpf = 'O CPF precisa ter 11 dígitos.'

  if (!email) erros.email = 'Informe o e-mail.'
  else if (!/^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(email)) erros.email = 'E-mail inválido.'
  else if (email.length > 150) erros.email = 'O e-mail pode ter no máximo 150 caracteres.'

  if (!p.telefone.trim()) erros.telefone = 'Informe o telefone.'
  else if (telefone.length < 10) erros.telefone = 'O telefone precisa ter DDD e número.'
  else if (p.telefone.length > 25) erros.telefone = 'O telefone pode ter no máximo 25 caracteres.'

  if (!p.cnh.trim()) erros.cnh = 'Informe a CNH.'
  else if (!/^[0-9A-Za-z.\-\s]{5,20}$/.test(p.cnh)) erros.cnh = 'CNH inválida.'

  if (p.observacoes.length > 500)
    erros.observacoes = 'As observações podem ter no máximo 500 caracteres.'

  return erros
}

function validarPeriodo(devolucao: string, km: string, diaria: string): Record<string, string> {
  const erros: Record<string, string> = {}
  const data = new Date(devolucao)

  if (!devolucao || Number.isNaN(data.getTime())) {
    erros.dataPrevistaDevolucao = 'Informe a data prevista de devolução.'
  } else if (data.getTime() <= Date.now()) {
    erros.dataPrevistaDevolucao = 'A devolução precisa ser depois de agora.'
  }

  if (km.trim() && (Number.isNaN(Number(km)) || Number(km) < 0)) {
    erros.quilometragemInicial = 'A quilometragem não pode ser negativa.'
  }

  if (diaria.trim() && (Number.isNaN(Number(diaria)) || Number(diaria) < 0.01)) {
    erros.valorDiaria = 'A diária precisa ser maior que zero.'
  }

  return erros
}

/** Diárias cobradas pela data prevista, com o mínimo de 1 — igual ao backend. */
function diasCobrados(devolucao: string): number {
  const fim = new Date(devolucao)
  if (Number.isNaN(fim.getTime())) return 1

  const hoje = new Date()
  const inicioDia = Date.UTC(hoje.getFullYear(), hoje.getMonth(), hoje.getDate())
  const fimDia = Date.UTC(fim.getFullYear(), fim.getMonth(), fim.getDate())

  return Math.max(1, Math.round((fimDia - inicioDia) / 86_400_000))
}

/** CPF com pontuação ao sair do campo — sem mascarar enquanto a pessoa digita. */
function formatarCpf(valor: string): string {
  const digitos = valor.replace(/\D/g, '').slice(0, 11)
  if (digitos.length !== 11) return valor
  return `${digitos.slice(0, 3)}.${digitos.slice(3, 6)}.${digitos.slice(6, 9)}-${digitos.slice(9)}`
}

export function NovoAluguel() {
  const navigate = useNavigate()

  const [passo, setPasso] = useState<Passo>(1)
  const [carros, setCarros] = useState<Carro[]>([])
  const [carregando, setCarregando] = useState(true)
  const [selecionado, setSelecionado] = useState<Carro | null>(null)
  const [erro, setErro] = useState<string | null>(null)
  const [salvando, setSalvando] = useState(false)

  const [tocado, setTocado] = useState<Record<string, boolean>>({})
  const [tentouAvancar, setTentouAvancar] = useState(false)
  const [errosApi, setErrosApi] = useState<Record<string, string>>({})

  const tituloRef = useRef<HTMLHeadingElement>(null)

  const [pessoa, setPessoa] = useState<DadosCondutor>({
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

  // Saida e diaria vem do carro, mas ficam editaveis: o patio negocia.
  const [kmInicial, setKmInicial] = useState('')
  const [valorDiaria, setValorDiaria] = useState('')

  useEffect(() => {
    carrosApi
      .disponiveis()
      .then((p) => setCarros(p.conteudo))
      .catch((e) => setErro(e instanceof ApiError ? e.message : 'Falha ao carregar os carros.'))
      .finally(() => setCarregando(false))
  }, [])

  // Ao trocar de etapa, o foco vai para o titulo: quem usa leitor de tela ou
  // teclado precisa saber que a tela mudou sem procurar onde esta.
  useEffect(() => {
    tituloRef.current?.focus()
  }, [passo])

  const errosCondutor = validarCondutor(pessoa)
  const errosPeriodo = validarPeriodo(devolucao, kmInicial, valorDiaria)

  const errosDoPasso: Record<string, string> =
    passo === 1
      ? errosCondutor
      : passo === 2
        ? selecionado
          ? {}
          : { carro: 'Escolha um veículo para continuar.' }
        : passo === 3
          ? errosPeriodo
          : {}

  const dias = useMemo(() => diasCobrados(devolucao), [devolucao])
  const diariaEfetiva = Number(valorDiaria) > 0 ? Number(valorDiaria) : (selecionado?.valorDiaria ?? 0)
  const total = dias * diariaEfetiva

  function mostrarErro(campo: string): string | undefined {
    if (!tentouAvancar && !tocado[campo]) return undefined
    return errosApi[campo] ?? errosDoPasso[campo]
  }

  function marcarTocado(campo: string) {
    setTocado((t) => ({ ...t, [campo]: true }))
  }

  function avancar() {
    setTentouAvancar(true)
    if (Object.keys(errosDoPasso).length > 0) return
    if (passo < 4) setPasso((passo + 1) as Passo)
  }

  function voltar() {
    if (passo > 1) {
      setTentouAvancar(false)
      setPasso((passo - 1) as Passo)
    }
  }

  function escolherCarro(c: Carro) {
    setSelecionado(c)
    setKmInicial(String(c.quilometragem))
    setValorDiaria(String(c.valorDiaria))
    setErrosApi({})
  }

  async function enviar(e: React.FormEvent) {
    e.preventDefault()

    if (passo < 4) {
      avancar()
      return
    }

    const todosErros = { ...errosCondutor, ...errosPeriodo }
    if (!selecionado) todosErros.carro = 'Escolha um veículo para continuar.'

    if (Object.keys(todosErros).length > 0) {
      setTentouAvancar(true)
      if (Object.keys(errosCondutor).length > 0) setPasso(1)
      else if (!selecionado) setPasso(2)
      else setPasso(3)
      return
    }

    if (!selecionado) return

    setSalvando(true)
    setErro(null)
    setErrosApi({})

    const corpo: AluguelRequest = {
      nome: pessoa.nome.trim(),
      cpf: pessoa.cpf.trim(),
      email: pessoa.email.trim(),
      telefone: pessoa.telefone.trim(),
      cnh: pessoa.cnh.trim(),
      observacoes: pessoa.observacoes.trim() || undefined,
      carroId: selecionado.id,
      dataPrevistaDevolucao: devolucao,
      quilometragemInicial: kmInicial.trim() ? Number(kmInicial) : undefined,
      valorDiaria: valorDiaria.trim() ? Number(valorDiaria) : undefined,
    }

    try {
      const aluguel = await alugueisApi.registrar(corpo)
      navigate(`/alugueis/${aluguel.id}`)
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.campos.length > 0) {
          const mapa = Object.fromEntries(err.campos.map((c) => [c.campo, c.mensagem]))
          setErrosApi(mapa)
          const camposCondutor = ['nome', 'cpf', 'email', 'telefone', 'cnh', 'observacoes']
          setPasso(camposCondutor.some((c) => mapa[c]) ? 1 : 3)
          setTentouAvancar(true)
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
      <Link
        to="/alugueis"
        className="text-sm font-medium text-veloz-azul-leitura underline-offset-4 hover:underline"
      >
        ← Aluguéis
      </Link>

      <TituloPagina apoio="Os dados da pessoa vão cifrados para o banco, com índice para busca.">
        Entregar um carro
      </TituloPagina>

      <Stepper passo={passo} onIrPara={(p) => p < passo && setPasso(p)} />

      {erro && <Aviso>{erro}</Aviso>}

      <div className="grid items-start gap-6 lg:grid-cols-[minmax(0,1fr)_20rem]">
        <form onSubmit={enviar} noValidate className="min-w-0 space-y-6">
          <h2
            ref={tituloRef}
            tabIndex={-1}
            className="font-titulo text-xl font-semibold text-veloz-azul-leitura outline-none"
          >
            {PASSOS[passo - 1].titulo}
            <span className="ml-2 text-sm font-normal text-slate-500">Etapa {passo} de 4</span>
          </h2>

          <p className="sr-only" aria-live="polite">
            Etapa {passo} de 4: {PASSOS[passo - 1].titulo}
          </p>

          {passo === 1 && (
            <Cartao className="p-6">
              <div className="grid gap-4 sm:grid-cols-2">
                <Campo rotulo="Nome completo" erro={mostrarErro('nome')} className="sm:col-span-2">
                  <input
                    className={estiloInput}
                    value={pessoa.nome}
                    maxLength={150}
                    autoComplete="name"
                    onChange={(e) => setPessoa((p) => ({ ...p, nome: e.target.value }))}
                    onBlur={() => marcarTocado('nome')}
                    placeholder="João Lima"
                  />
                </Campo>

                <Campo rotulo="CPF" erro={mostrarErro('cpf')}>
                  <input
                    className={estiloInput}
                    value={pessoa.cpf}
                    inputMode="numeric"
                    onChange={(e) => setPessoa((p) => ({ ...p, cpf: e.target.value }))}
                    onBlur={() => {
                      setPessoa((p) => ({ ...p, cpf: formatarCpf(p.cpf) }))
                      marcarTocado('cpf')
                    }}
                    placeholder="529.982.247-25"
                  />
                </Campo>

                <Campo rotulo="CNH" erro={mostrarErro('cnh')}>
                  <input
                    className={estiloInput}
                    value={pessoa.cnh}
                    maxLength={20}
                    onChange={(e) => setPessoa((p) => ({ ...p, cnh: e.target.value }))}
                    onBlur={() => marcarTocado('cnh')}
                    placeholder="12345678901"
                  />
                </Campo>

                <Campo rotulo="E-mail" erro={mostrarErro('email')}>
                  <input
                    type="email"
                    className={estiloInput}
                    value={pessoa.email}
                    maxLength={150}
                    autoComplete="email"
                    onChange={(e) => setPessoa((p) => ({ ...p, email: e.target.value }))}
                    onBlur={() => marcarTocado('email')}
                    placeholder="joao@email.com"
                  />
                </Campo>

                <Campo rotulo="Telefone" erro={mostrarErro('telefone')}>
                  <input
                    type="tel"
                    className={estiloInput}
                    value={pessoa.telefone}
                    maxLength={25}
                    autoComplete="tel"
                    onChange={(e) => setPessoa((p) => ({ ...p, telefone: e.target.value }))}
                    onBlur={() => marcarTocado('telefone')}
                    placeholder="(11) 98888-7777"
                  />
                </Campo>
              </div>
            </Cartao>
          )}

          {passo === 2 && (
            <section className="space-y-3">
              {carregando ? (
                <Carregando texto="Carregando carros disponíveis..." />
              ) : carros.length === 0 ? (
                <Cartao className="p-6">
                  <p className="text-sm text-slate-600">
                    Nenhum carro disponível no momento. Registre a devolução de um aluguel em aberto
                    para liberar um veículo, ou cadastre um carro novo.
                  </p>
                  <div className="mt-3 flex flex-wrap gap-2">
                    <Link to="/alugueis?status=ATIVO">
                      <Botao variante="secundario" type="button">
                        Ver aluguéis em aberto
                      </Botao>
                    </Link>
                    <Link to="/frota">
                      <Botao variante="secundario" type="button">
                        Ir para a frota
                      </Botao>
                    </Link>
                  </div>
                </Cartao>
              ) : (
                <>
                  <p className="text-sm text-slate-600">
                    {carros.length} veículo{carros.length === 1 ? '' : 's'} disponível
                    {carros.length === 1 ? '' : 'is'}. Escolha o que vai sair.
                  </p>
                  <div className="grid gap-3 sm:grid-cols-2">
                    {carros.map((c) => {
                      const ativo = selecionado?.id === c.id
                      return (
                        <CartaoChave
                          key={c.id}
                          selecionado={ativo}
                          className="h-full"
                          onClick={() => escolherCarro(c)}
                        >
                          <span className="flex items-start justify-between gap-3">
                            <span className="min-w-0">
                              <span className="block truncate font-semibold text-veloz-grafite">
                                {c.marca} {c.modelo}
                              </span>
                              <span className="mt-0.5 block truncate text-sm text-slate-600">
                                {c.ano} · {c.cor}
                              </span>
                            </span>
                            <span
                              className={`shrink-0 rounded-veloz px-2 py-0.5 text-xs font-medium ring-1 ring-inset ${
                                ativo
                                  ? 'bg-veloz-azul text-white ring-veloz-azul'
                                  : 'bg-slate-50 text-slate-600 ring-slate-900/10'
                              }`}
                            >
                              {c.categoria}
                            </span>
                          </span>

                          <span className="mt-4 flex items-end justify-between gap-3">
                            <span className="rounded-veloz bg-slate-100 px-2 py-1 font-mono text-sm font-medium tracking-wider text-slate-700">
                              {c.placa}
                            </span>
                            <span className="text-right text-sm font-semibold tabular text-veloz-azul">
                              {moeda(c.valorDiaria)}
                              <span className="text-xs font-normal text-slate-600">/dia</span>
                            </span>
                          </span>

                          <span className="mt-2 flex items-center justify-between">
                            <span className="text-xs tabular text-slate-600">
                              {numero(c.quilometragem)} km
                            </span>
                            {ativo && (
                              <span className="text-xs font-medium text-veloz-azul">
                                Selecionado
                              </span>
                            )}
                          </span>
                        </CartaoChave>
                      )
                    })}
                  </div>
                  {mostrarErro('carro') && (
                    <p className="text-xs text-red-600">{mostrarErro('carro')}</p>
                  )}
                </>
              )}
            </section>
          )}

          {passo === 3 && (
            <Cartao className="p-6">
              <div className="grid gap-4 sm:grid-cols-2">
                <Campo rotulo="Devolução prevista" erro={mostrarErro('dataPrevistaDevolucao')}>
                  <input
                    type="datetime-local"
                    className={estiloInput}
                    value={devolucao.slice(0, 16)}
                    min={paraIsoLocal(new Date()).slice(0, 16)}
                    onChange={(e) => setDevolucao(e.target.value + ':00')}
                    onBlur={() => marcarTocado('dataPrevistaDevolucao')}
                  />
                </Campo>

                <Campo rotulo="Quilometragem de saída" erro={mostrarErro('quilometragemInicial')}>
                  <input
                    type="number"
                    min={0}
                    className={estiloInput}
                    value={kmInicial}
                    onChange={(e) => setKmInicial(e.target.value)}
                    onBlur={() => marcarTocado('quilometragemInicial')}
                  />
                </Campo>

                <Campo rotulo="Valor da diária" erro={mostrarErro('valorDiaria')}>
                  <input
                    type="number"
                    min={0.01}
                    step={0.01}
                    className={estiloInput}
                    value={valorDiaria}
                    onChange={(e) => setValorDiaria(e.target.value)}
                    onBlur={() => marcarTocado('valorDiaria')}
                  />
                </Campo>

                <Campo rotulo="Observações (opcional)" erro={mostrarErro('observacoes')}>
                  <textarea
                    rows={2}
                    maxLength={500}
                    className={estiloInput}
                    value={pessoa.observacoes}
                    onChange={(e) => setPessoa((p) => ({ ...p, observacoes: e.target.value }))}
                    onBlur={() => marcarTocado('observacoes')}
                    placeholder="Cliente pediu cadeirinha"
                  />
                </Campo>
              </div>

              <p className="mt-4 border-t border-slate-100 pt-4 text-sm text-slate-600">
                <span className="font-medium text-veloz-grafite tabular">{dias}</span> diária
                {dias === 1 ? '' : 's'} ×{' '}
                <span className="font-medium text-veloz-grafite tabular">{moeda(diariaEfetiva)}</span>{' '}
                = <span className="font-semibold text-veloz-azul tabular">{moeda(total)}</span>
              </p>
            </Cartao>
          )}

          {passo === 4 && selecionado && (
            <div className="space-y-3">
              <Cartao className="p-5">
                <div className="flex items-start justify-between gap-4">
                  <h3 className="font-titulo text-lg font-semibold text-veloz-azul-leitura">
                    Condutor
                  </h3>
                  <button
                    type="button"
                    onClick={() => setPasso(1)}
                    className="text-sm font-medium text-veloz-azul underline-offset-4 hover:underline"
                  >
                    Editar
                  </button>
                </div>
                <dl className="mt-3 grid gap-x-6 gap-y-2 text-sm sm:grid-cols-2">
                  <div>
                    <dt className="text-slate-600">Nome</dt>
                    <dd className="font-medium text-veloz-grafite">{pessoa.nome}</dd>
                  </div>
                  <div>
                    <dt className="text-slate-600">CPF</dt>
                    <dd className="font-medium tabular text-veloz-grafite">{pessoa.cpf}</dd>
                  </div>
                  <div>
                    <dt className="text-slate-600">E-mail</dt>
                    <dd className="break-all font-medium text-veloz-grafite">{pessoa.email}</dd>
                  </div>
                  <div>
                    <dt className="text-slate-600">Telefone</dt>
                    <dd className="font-medium tabular text-veloz-grafite">{pessoa.telefone}</dd>
                  </div>
                  <div>
                    <dt className="text-slate-600">CNH</dt>
                    <dd className="font-medium tabular text-veloz-grafite">{pessoa.cnh}</dd>
                  </div>
                  {pessoa.observacoes.trim() && (
                    <div className="sm:col-span-2">
                      <dt className="text-slate-600">Observações</dt>
                      <dd className="font-medium text-veloz-grafite">{pessoa.observacoes}</dd>
                    </div>
                  )}
                </dl>
              </Cartao>

              <Cartao className="p-5">
                <div className="flex items-start justify-between gap-4">
                  <h3 className="font-titulo text-lg font-semibold text-veloz-azul-leitura">
                    Veículo e período
                  </h3>
                  <button
                    type="button"
                    onClick={() => setPasso(2)}
                    className="text-sm font-medium text-veloz-azul underline-offset-4 hover:underline"
                  >
                    Editar
                  </button>
                </div>
                <dl className="mt-3 grid gap-x-6 gap-y-2 text-sm sm:grid-cols-2">
                  <div>
                    <dt className="text-slate-600">Carro</dt>
                    <dd className="font-medium text-veloz-grafite">
                      {selecionado.marca} {selecionado.modelo}
                    </dd>
                  </div>
                  <div>
                    <dt className="text-slate-600">Placa</dt>
                    <dd className="font-mono font-medium tracking-wider text-veloz-grafite">
                      {selecionado.placa}
                    </dd>
                  </div>
                  <div>
                    <dt className="text-slate-600">Devolução prevista</dt>
                    <dd className="font-medium text-veloz-grafite">{dataHora(devolucao)}</dd>
                  </div>
                  <div>
                    <dt className="text-slate-600">Quilometragem de saída</dt>
                    <dd className="font-medium tabular text-veloz-grafite">
                      {numero(kmInicial.trim() ? Number(kmInicial) : selecionado.quilometragem)} km
                    </dd>
                  </div>
                  <div>
                    <dt className="text-slate-600">Diária</dt>
                    <dd className="font-medium tabular text-veloz-grafite">{moeda(diariaEfetiva)}</dd>
                  </div>
                  <div>
                    <dt className="text-slate-600">Total</dt>
                    <dd className="font-semibold tabular text-veloz-azul">
                      {moeda(total)} · {dias} diária{dias === 1 ? '' : 's'}
                    </dd>
                  </div>
                </dl>
              </Cartao>

              <p className="text-sm text-slate-600">
                Ao registrar, o carro passa para <span className="font-medium">Alugado</span> e os
                dados da pessoa são cifrados no banco.
              </p>
            </div>
          )}

          <div className="flex flex-wrap items-center justify-between gap-3 border-t border-slate-200 pt-4">
            <div className="flex items-center gap-4">
              {passo > 1 && (
                <Botao type="button" variante="secundario" onClick={voltar}>
                  Voltar
                </Botao>
              )}
              {/* No celular o resumo lateral fica depois do formulario; o total
                  precisa continuar visivel ao lado da acao. */}
              {selecionado && (
                <p className="text-sm text-slate-600 lg:hidden">
                  Total{' '}
                  <span className="font-semibold tabular text-veloz-azul">{moeda(total)}</span>
                </p>
              )}
            </div>

            <div className="flex flex-wrap gap-2">
              <Link to="/alugueis">
                <Botao type="button" variante="texto">
                  Cancelar
                </Botao>
              </Link>

              {passo < 4 ? (
                <Botao type="submit" disabled={passo === 2 && carregando}>
                  Continuar
                </Botao>
              ) : (
                <Botao type="submit" disabled={salvando}>
                  {salvando ? 'Registrando...' : 'Registrar aluguel'}
                </Botao>
              )}
            </div>
          </div>
        </form>

        <Resumo
          carro={selecionado}
          devolucao={devolucao}
          dias={dias}
          diaria={diariaEfetiva}
          total={total}
        />
      </div>
    </div>
  )
}

/**
 * Trilha de etapas. Numeros aqui sao informacao, nao enfeite: indicam ordem e
 * progresso. Clicar so volta; avancar exige que a etapa atual esteja valida.
 */
function Stepper({ passo, onIrPara }: { passo: Passo; onIrPara: (p: Passo) => void }) {
  return (
    <nav aria-label="Etapas do registro">
      <ol className="flex items-center gap-2">
        {PASSOS.map((p, i) => {
          const ativo = passo === p.numero
          const concluido = passo > p.numero
          return (
            <li key={p.numero} className="flex flex-1 items-center gap-2">
              <button
                type="button"
                onClick={() => onIrPara(p.numero)}
                disabled={!concluido}
                aria-current={ativo ? 'step' : undefined}
                aria-label={`Etapa ${p.numero}: ${p.titulo}`}
                className={`flex min-w-0 items-center gap-2.5 rounded-veloz text-left transition duration-150 ${
                  concluido ? 'cursor-pointer hover:opacity-80' : 'cursor-default'
                }`}
              >
                <span
                  aria-hidden="true"
                  className={`flex size-9 shrink-0 items-center justify-center rounded-full text-sm font-semibold tabular sm:size-8 ${
                    ativo
                      ? 'bg-veloz-azul text-white shadow-veloz'
                      : concluido
                        ? 'bg-veloz-azul-fundo text-veloz-azul ring-1 ring-inset ring-veloz-azul/30'
                        : 'bg-white text-slate-500 ring-1 ring-inset ring-slate-900/10'
                  }`}
                >
                  {p.numero}
                </span>
                <span className="hidden min-w-0 sm:block">
                  <span
                    className={`block truncate text-sm font-semibold ${
                      ativo ? 'text-veloz-azul-leitura' : concluido ? 'text-slate-700' : 'text-slate-500'
                    }`}
                  >
                    {p.titulo}
                  </span>
                  <span className="hidden truncate text-xs text-slate-500 md:block">
                    {p.descricao}
                  </span>
                </span>
              </button>
              {i < PASSOS.length - 1 && (
                <span
                  aria-hidden="true"
                  className={`h-px flex-1 ${concluido ? 'bg-veloz-azul/40' : 'bg-slate-300'}`}
                />
              )}
            </li>
          )
        })}
      </ol>
    </nav>
  )
}

/**
 * Resumo da reserva. Fica fixo ao lado do formulario no desktop: o valor total
 * e parte da conversa no balcao, nao uma surpresa do ultimo passo.
 */
function Resumo({
  carro,
  devolucao,
  dias,
  diaria,
  total,
}: {
  carro: Carro | null
  devolucao: string
  dias: number
  diaria: number
  total: number
}) {
  return (
    <Cartao className="p-5 lg:sticky lg:top-8">
      <h2 className="font-titulo text-lg font-semibold text-veloz-azul-leitura">Resumo da reserva</h2>

      {carro ? (
        <div className="mt-3 space-y-1">
          <p className="font-semibold text-veloz-grafite">
            {carro.marca} {carro.modelo}
          </p>
          <p className="text-sm text-slate-600">
            {carro.ano} · {carro.cor} · {carro.categoria}
          </p>
          <p className="pt-1 font-mono text-sm tracking-wider text-slate-700">{carro.placa}</p>
        </div>
      ) : (
        <p className="mt-3 text-sm text-slate-600">Nenhum veículo escolhido ainda.</p>
      )}

      <dl className="mt-4 space-y-2 border-t border-slate-100 pt-4 text-sm">
        <div className="flex items-baseline justify-between gap-3">
          <dt className="text-slate-600">Devolução</dt>
          <dd className="text-right font-medium tabular text-veloz-grafite">{dataHora(devolucao)}</dd>
        </div>
        <div className="flex items-baseline justify-between gap-3">
          <dt className="text-slate-600">Diárias</dt>
          <dd className="font-medium tabular text-veloz-grafite">{dias}</dd>
        </div>
        <div className="flex items-baseline justify-between gap-3">
          <dt className="text-slate-600">Diária</dt>
          <dd className="font-medium tabular text-veloz-grafite">
            {diaria > 0 ? moeda(diaria) : '—'}
          </dd>
        </div>
        <div className="flex items-baseline justify-between gap-3 border-t border-slate-100 pt-2">
          <dt className="font-medium text-slate-700">Total</dt>
          <dd className="font-titulo text-2xl font-semibold tabular text-veloz-azul">
            {total > 0 ? moeda(total) : '—'}
          </dd>
        </div>
      </dl>

      <p className="mt-4 border-t border-slate-100 pt-4 text-xs leading-relaxed text-slate-500">
        Nome, CPF, e-mail, telefone e CNH são gravados cifrados. A busca usa índices HMAC — o valor
        não fica legível no banco.
      </p>
    </Cartao>
  )
}
