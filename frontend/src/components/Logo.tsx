/*
  Marca Veloz: um carro visto de lado.

  As cores sao as do tema (index.css), escritas aqui em literal porque SVG
  inline nao enxerga as variaveis do Tailwind sem um wrapper. Se a paleta mudar,
  estes valores mudam junto — por isso ficam todos no topo, e nao espalhados
  pelos elementos.

  Sem gradiente: o corpo do carro e cor cheia, como o resto da interface. O
  volume vem do para-brisa em tom mais claro e das rodas em grafite, que e
  suficiente para ler a forma.
*/
const AZUL = '#1743C4'
const AZUL_CLARO = '#5B87F5'
const PRATA_CLARO = '#E2E8F0'
const GRAFITE = '#0B1220'
const PRATA = '#94A3B8'

export function Logo({ className = 'size-10' }: { className?: string }) {
  return (
    <svg viewBox="0 0 64 64" className={className} role="img" aria-label="Veloz">
      {/* Corpo do carro. */}
      <path
        d="M8 40c0-2 1-4 3-5l3-1 6-9c1-2 3-3 5-3h14c2 0 4 1 5 3l6 9 3 1c2 1 3 3 3 5v5c0 1-1 2-2 2h-3a6 6 0 0 0-12 0H25a6 6 0 0 0-12 0H10c-1 0-2-1-2-2z"
        fill={AZUL}
      />
      {/* Para-brisa em tom claro: da o volume sem precisar de degrade. */}
      <path d="M22 32l5-7c1-1 2-2 4-2h6c2 0 3 1 4 2l5 7z" fill={PRATA_CLARO} />
      {/* Rodas: pneu em grafite, aro em prata. */}
      <circle cx="19" cy="47" r="5" fill={GRAFITE} />
      <circle cx="19" cy="47" r="2.2" fill={PRATA} />
      <circle cx="45" cy="47" r="5" fill={GRAFITE} />
      <circle cx="45" cy="47" r="2.2" fill={PRATA} />
      {/* Marcas de velocidade, o "veloz" da marca. */}
      <path
        d="M4 30h10M2 36h8"
        stroke={AZUL_CLARO}
        strokeWidth="2.5"
        strokeLinecap="round"
      />
    </svg>
  )
}
