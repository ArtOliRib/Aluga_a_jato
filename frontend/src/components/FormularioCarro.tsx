import { useState } from 'react'
import { carrosApi } from '../api/endpoints'
import { ApiError } from '../api/client'
import { Aviso, Botao, Campo, Cartao, estiloInput } from './ui'
import type { CarroRequest } from '../types/api'

const vazio: CarroRequest = {
  nome: '',
  marca: '',
  modelo: '',
  ano: new Date().getFullYear(),
  placa: '',
  quilometragem: 0,
  cor: '',
  categoria: '',
  valorDiaria: 0,
}

/**
 * Cadastro de carro.
 *
 * Os erros de validacao vem da API campo a campo (`erros[]` no ProblemDetail) e
 * sao posicionados sob o campo correspondente. A alternativa — mostrar so a
 * mensagem geral — obrigaria o usuario a adivinhar qual dos nove campos esta
 * errado.
 */
export function FormularioCarro({
  onSalvo,
  onCancelar,
}: {
  onSalvo: () => void
  onCancelar: () => void
}) {
  const [form, setForm] = useState<CarroRequest>(vazio)
  const [errosCampo, setErrosCampo] = useState<Record<string, string>>({})
  const [erroGeral, setErroGeral] = useState<string | null>(null)
  const [salvando, setSalvando] = useState(false)

  function alterar<K extends keyof CarroRequest>(campo: K, valor: CarroRequest[K]) {
    setForm((f) => ({ ...f, [campo]: valor }))
    setErrosCampo((e) => {
      const { [campo as string]: _, ...resto } = e
      return resto
    })
  }

  async function enviar(e: React.FormEvent) {
    e.preventDefault()
    setSalvando(true)
    setErroGeral(null)
    setErrosCampo({})

    try {
      await carrosApi.criar(form)
      onSalvo()
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.campos.length > 0) {
          setErrosCampo(Object.fromEntries(err.campos.map((c) => [c.campo, c.mensagem])))
        } else {
          setErroGeral(err.message)
        }
      } else {
        setErroGeral('Falha ao cadastrar o carro.')
      }
    } finally {
      setSalvando(false)
    }
  }

  return (
    <Cartao className="p-6">
      <h2 className="text-lg font-semibold text-veloz-grafite">Novo carro</h2>
      <p className="mt-1 text-sm text-slate-600">
        A placa é dado pessoal: vai cifrada para o banco, com índice para busca.
      </p>

      <form onSubmit={enviar} className="mt-5 space-y-4">
        {erroGeral && <Aviso>{erroGeral}</Aviso>}

        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          <Campo rotulo="Nome" erro={errosCampo.nome}>
            <input
              className={estiloInput}
              value={form.nome}
              onChange={(e) => alterar('nome', e.target.value)}
              placeholder="Uno"
            />
          </Campo>

          <Campo rotulo="Marca" erro={errosCampo.marca}>
            <input
              className={estiloInput}
              value={form.marca}
              onChange={(e) => alterar('marca', e.target.value)}
              placeholder="Fiat"
            />
          </Campo>

          <Campo rotulo="Modelo" erro={errosCampo.modelo}>
            <input
              className={estiloInput}
              value={form.modelo}
              onChange={(e) => alterar('modelo', e.target.value)}
              placeholder="Uno Vivace"
            />
          </Campo>

          <Campo rotulo="Ano" erro={errosCampo.ano}>
            <input
              type="number"
              className={estiloInput}
              value={form.ano}
              onChange={(e) => alterar('ano', Number(e.target.value))}
            />
          </Campo>

          <Campo rotulo="Placa" erro={errosCampo.placa}>
            <input
              className={`${estiloInput} font-mono uppercase tracking-wider`}
              value={form.placa}
              onChange={(e) => alterar('placa', e.target.value.toUpperCase())}
              placeholder="ABC1D23"
            />
          </Campo>

          <Campo rotulo="Quilometragem" erro={errosCampo.quilometragem}>
            <input
              type="number"
              className={estiloInput}
              value={form.quilometragem}
              onChange={(e) => alterar('quilometragem', Number(e.target.value))}
            />
          </Campo>

          <Campo rotulo="Cor" erro={errosCampo.cor}>
            <input
              className={estiloInput}
              value={form.cor}
              onChange={(e) => alterar('cor', e.target.value)}
              placeholder="Prata"
            />
          </Campo>

          <Campo rotulo="Categoria" erro={errosCampo.categoria}>
            <input
              className={estiloInput}
              value={form.categoria}
              onChange={(e) => alterar('categoria', e.target.value)}
              placeholder="Economico"
            />
          </Campo>

          <Campo rotulo="Valor da diária (R$)" erro={errosCampo.valorDiaria}>
            <input
              type="number"
              step="0.01"
              className={estiloInput}
              value={form.valorDiaria}
              onChange={(e) => alterar('valorDiaria', Number(e.target.value))}
            />
          </Campo>
        </div>

        <div className="flex gap-2 pt-2">
          <Botao type="submit" disabled={salvando}>
            {salvando ? 'Salvando...' : 'Cadastrar'}
          </Botao>
          <Botao type="button" variante="secundario" onClick={onCancelar}>
            Cancelar
          </Botao>
        </div>
      </form>
    </Cartao>
  )
}
