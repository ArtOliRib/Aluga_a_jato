import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: 5173,
    // O backend precisa ter esta origem liberada em CORS. O padrao de
    // locadora.cors.origens-permitidas ja inclui 5173, entao funciona sem
    // configuracao extra em desenvolvimento.
    proxy: {
      // Opcional: permite usar caminhos relativos e evita CORS por completo em
      // dev. A API tambem aceita requisicao direta (com CORS configurado).
      '/api': 'http://localhost:8080',
    },
  },
})
