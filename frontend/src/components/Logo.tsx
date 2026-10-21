/**
 * Marca Veloz.
 *
 * O SVG e inline (nao <img src>) para que o azul acompanhe o tema via
 * currentColor onde faz sentido — assim a mesma marca funciona sobre fundo
 * claro e escuro sem um segundo arquivo.
 */
export function Logo({ className = 'size-10' }: { className?: string }) {
  return (
    <svg viewBox="0 0 64 64" className={className} role="img" aria-label="Veloz">
      <defs>
        <linearGradient id="logo-corpo" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor="#60A5FA" />
          <stop offset="100%" stopColor="#1D4ED8" />
        </linearGradient>
      </defs>

      <path
        d="M8 40c0-2 1-4 3-5l3-1 6-9c1-2 3-3 5-3h14c2 0 4 1 5 3l6 9 3 1c2 1 3 3 3 5v5c0 1-1 2-2 2h-3a6 6 0 0 0-12 0H25a6 6 0 0 0-12 0H10c-1 0-2-1-2-2z"
        fill="url(#logo-corpo)"
      />
      <path d="M22 32l5-7c1-1 2-2 4-2h6c2 0 3 1 4 2l5 7z" fill="#E2E8F0" />
      <circle cx="19" cy="47" r="5" fill="#0F172A" />
      <circle cx="19" cy="47" r="2.2" fill="#94A3B8" />
      <circle cx="45" cy="47" r="5" fill="#0F172A" />
      <circle cx="45" cy="47" r="2.2" fill="#94A3B8" />
      <path
        d="M4 30h10M2 36h8"
        stroke="#60A5FA"
        strokeWidth="2.5"
        strokeLinecap="round"
        opacity="0.55"
      />
    </svg>
  )
}
