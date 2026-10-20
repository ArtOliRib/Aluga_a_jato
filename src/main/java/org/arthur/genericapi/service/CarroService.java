package org.arthur.genericapi.service;

import lombok.RequiredArgsConstructor;
import org.arthur.genericapi.dto.PageResponse;
import org.arthur.genericapi.dto.carro.CarroRequest;
import org.arthur.genericapi.dto.carro.CarroResponse;
import org.arthur.genericapi.dto.carro.CarroUpdateRequest;
import org.arthur.genericapi.exception.RecursoNaoEncontradoException;
import org.arthur.genericapi.exception.RegraDeNegocioException;
import org.arthur.genericapi.model.Carro;
import org.arthur.genericapi.model.StatusAluguel;
import org.arthur.genericapi.model.StatusCarro;
import org.arthur.genericapi.repository.AluguelRepository;
import org.arthur.genericapi.repository.CarroRepository;
import org.arthur.genericapi.support.CampoProtegido;
import org.arthur.genericapi.support.ProtecaoDeDadoPessoal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Regras da frota. Cada mudanca de status e traduzida para o metodo
 * correspondente da entidade, nunca para atribuicao direta de campo.
 *
 * <p>A placa e dado pessoal e recebe o mesmo tratamento do CPF: indice para
 * unicidade e busca, cifra para exibicao.
 */
@Service
@RequiredArgsConstructor
public class CarroService {

    private final CarroRepository carroRepository;
    private final AluguelRepository aluguelRepository;
    private final ProtecaoDeDadoPessoal protecao;

    @Transactional
    public CarroResponse cadastrar(CarroRequest request) {
        CampoProtegido placa = CampoProtegido.deDocumento(request.placa(), protecao);
        if (carroRepository.existsByPlacaIndice(placa.indice())) {
            throw new RegraDeNegocioException("Ja existe um carro com a placa informada");
        }
        Carro carro = carroRepository.save(new Carro(
                request.nome(),
                request.marca(),
                request.modelo(),
                request.ano(),
                placa.indice(), placa.cifrado(),
                request.quilometragem(),
                request.cor(),
                request.categoria(),
                request.valorDiaria()
        ));
        return responder(carro);
    }

    @Transactional(readOnly = true)
    public CarroResponse buscarPorId(UUID id) {
        return responder(obterEntidade(id));
    }

    @Transactional(readOnly = true)
    public CarroResponse buscarPorPlaca(String placa) {
        String placaIndice = CampoProtegido.indiceDeDocumento(placa, protecao);
        return carroRepository.findByPlacaIndice(placaIndice)
                .map(this::responder)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Carro nao encontrado com a placa informada"));
    }

    @Transactional(readOnly = true)
    public PageResponse<CarroResponse> listar(String marca, StatusCarro status, Pageable pageable) {
        Page<Carro> pagina;
        if (status != null) {
            pagina = carroRepository.findByStatusOrderByIdAsc(status, pageable);
        } else if (marca != null && !marca.isBlank()) {
            pagina = carroRepository.findByMarcaContainingIgnoreCase(marca.trim(), pageable);
        } else {
            pagina = carroRepository.findAll(pageable);
        }
        return PageResponse.from(pagina.map(this::responder));
    }

    @Transactional(readOnly = true)
    public PageResponse<CarroResponse> listarDisponiveis(Pageable pageable) {
        Page<Carro> pagina = carroRepository.findByStatusOrderByIdAsc(StatusCarro.DISPONIVEL, pageable);
        return PageResponse.from(pagina.map(this::responder));
    }

    @Transactional
    public CarroResponse atualizar(UUID id, CarroUpdateRequest request) {
        Carro carro = obterEntidade(id);
        carro.atualizarDados(
                request.nome(),
                request.marca(),
                request.modelo(),
                request.ano(),
                request.cor(),
                request.categoria(),
                request.valorDiaria()
        );
        return responder(carroRepository.save(carro));
    }

    @Transactional
    public CarroResponse alterarPlaca(UUID id, String placa) {
        Carro carro = obterEntidade(id);
        CampoProtegido novaPlaca = CampoProtegido.deDocumento(placa, protecao);
        carroRepository.findByPlacaIndice(novaPlaca.indice())
                .filter(outro -> !outro.getId().equals(id))
                .ifPresent(outro -> {
                    throw new RegraDeNegocioException("Ja existe um carro com a placa informada");
                });
        carro.alterarPlaca(novaPlaca.indice(), novaPlaca.cifrado());
        return responder(carroRepository.save(carro));
    }

    @Transactional
    public CarroResponse registrarQuilometragem(UUID id, Long quilometragem) {
        Carro carro = obterEntidade(id);
        carro.registrarQuilometragem(quilometragem);
        return responder(carroRepository.save(carro));
    }

    @Transactional
    public CarroResponse alterarStatus(UUID id, StatusCarro destino) {
        Carro carro = obterEntidade(id);
        boolean temAluguelAtivo = aluguelRepository.existsByCarroIdAndStatus(id, StatusAluguel.ATIVO);
        if (temAluguelAtivo && destino != StatusCarro.DISPONIVEL) {
            throw new RegraDeNegocioException(
                    "O carro " + id + " possui aluguel ativo e so pode voltar para DISPONIVEL");
        }
        switch (destino) {
            case DISPONIVEL -> carro.liberar();
            case ALUGADO -> carro.alugar();
            case MANUTENCAO -> carro.enviarParaManutencao();
            case DESATIVADO -> carro.desativar();
        }
        return responder(carroRepository.save(carro));
    }

    @Transactional
    public void remover(UUID id) {
        Carro carro = obterEntidade(id);
        if (aluguelRepository.existsByCarroId(id)) {
            throw new RegraDeNegocioException(
                    "O carro " + id + " possui alugueis vinculados e nao pode ser removido");
        }
        carroRepository.delete(carro);
    }

    private CarroResponse responder(Carro carro) {
        return new CarroResponse(
                carro.getId(),
                carro.getNome(),
                carro.getMarca(),
                carro.getModelo(),
                carro.getAno(),
                protecao.decifrar(carro.getPlacaCifrada()),
                carro.getQuilometragem(),
                carro.getCor(),
                carro.getCategoria(),
                carro.getValorDiaria(),
                carro.getStatus(),
                carro.getStatus().estaDisponivelParaAluguel(),
                carro.getDataCadastro(),
                carro.getDataAtualizacao());
    }

    private Carro obterEntidade(UUID id) {
        return carroRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Carro", id));
    }
}