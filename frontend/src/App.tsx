import { BrowserRouter, Route, Routes } from 'react-router-dom'
import { Layout } from './components/Layout'
import { Inicio } from './pages/Inicio'
import { Frota } from './pages/Frota'
import { CarroDetalhe } from './pages/CarroDetalhe'
import { Alugueis } from './pages/Alugueis'
import { AluguelDetalhe } from './pages/AluguelDetalhe'
import { NovoAluguel } from './pages/NovoAluguel'
import { Relatorios } from './pages/Relatorios'
import { NaoEncontrada } from './pages/NaoEncontrada'

/*
  Rotas.

  O cadastro de carro nao tem rota propria: ele vive dentro de /frota como
  formulario revelado. Uma rota a mais para um formulario de 9 campos so
  adicionaria navegacao sem mover o usuario para lugar nenhum.
*/
export function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<Layout />}>
          <Route index element={<Inicio />} />

          <Route path="frota" element={<Frota />} />
          <Route path="frota/:id" element={<CarroDetalhe />} />

          <Route path="alugueis" element={<Alugueis />} />
          <Route path="alugueis/novo" element={<NovoAluguel />} />
          <Route path="alugueis/:id" element={<AluguelDetalhe />} />

          <Route path="relatorios" element={<Relatorios />} />

          <Route path="*" element={<NaoEncontrada />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}
