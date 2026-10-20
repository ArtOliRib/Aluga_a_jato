package org.arthur.genericapi.service;

import lombok.RequiredArgsConstructor;
import org.arthur.genericapi.dto.PageResponse;
import org.arthur.genericapi.dto.aluguel.AluguelRequest;
import org.arthur.genericapi.dto.aluguel.AluguelResponse;
import org.arthur.genericapi.dto.aluguel.CancelamentoRequest;
import org.arthur.genericapi.dto.aluguel.DevolucaoRequest;
import org.arthur.genericapi.exception.RecursoNaoEncontradoException;
import org.arthur.genericapi.exception.RegraDeNegocioException;
import org.arthur.genericapi.model.Aluguel;
import org.arthur.genericapi.model.Carro;
import org.arthur.genericapi.model.StatusAluguel;
import org.arthur.genericapi.repository.AluguelRepository;
import org.arthur.genericapi.repository.CarroRepository;
import org.arthur.genericapi.support.CampoProtegido;
import org.arthur.genericapi.support.ProtecaoDeDadoPessoal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Ciclo de vida do aluguel: retirada, devolucao, cancelamento e pagamento.
 * Cada operacao mexe no status do carro junto, para a frota nunca divergir
 * dos contratos.
 *
 * <p><b>Dados pessoais.</b> Sem cadastro de cliente (ADR-0003), os dados da
 * pessoa chegam no proprio aluguel e este service e quem os protege: normaliza
 * cpf/cnh/nome, gera os indices HMAC onde ha busca e cifra tudo antes de
 * gravar. Ele tambem e o unico que decifra nome/cpf para montar a resposta.
 */
@Service
@RequiredArgsConstructor
public class AluguelService {

    private final AluguelRepository aluguelRepository;
    private final CarroRepository carroRepository;
    private final ProtecaoDeDadoPessoal protecao;

    @Transactional
    public AluguelResponse registrar(AluguelRequest request) {
        Carro carro = carroRepository.findByIdParaAtualizacao(request.carroId())
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Carro", request.carroId()));

        if (!carro.getStatus().estaDisponivelParaAluguel()) {
            throw new RegraDeNegocioException(
                    "O carro " + carro.getId() + " esta com status " + carro.getStatus()
                            + " e nao pode ser alugado");
        }
        if (aluguelRepository.existsByCarroIdAndStatus(carro.getId(), StatusAluguel.ATIVO)) {
            throw new RegraDeNegocioException(
                    "O carro " + carro.getId() + " ja possui um aluguel ativo");
        }

        CampoProtegido nome = CampoProtegido.deNome(request.nome(), protecao);
        CampoProtegido cpf = CampoProtegido.deDocumento(request.cpf(), protecao);
        CampoProtegido cnh = CampoProtegido.deDocumento(request.cnh(), protecao);

        Aluguel aluguel = new Aluguel(
                nome.indice(), nome.cifrado(),
                cpf.indice(), cpf.cifrado(),
                CampoProtegido.deTexto(request.email(), protecao).cifrado(),
                CampoProtegido.deTexto(request.telefone(), protecao).cifrado(),
                cnh.indice(), cnh.cifrado(),
                carro,
                request.dataInicio() != null ? request.dataInicio() : LocalDateTime.now(),
                request.dataPrevistaDevolucao(),
                request.quilometragemInicial() != null ? request.quilometragemInicial() : carro.getQuilometragem(),
                request.valorDiaria() != null ? request.valorDiaria() : carro.getValorDiaria(),
                CampoProtegido.deTextoOpcional(request.observacoes(), protecao).cifrado()
        );

        carro.alugar();
        carroRepository.save(carro);
        return responder(aluguelRepository.save(aluguel));
    }

    @Transactional
    public AluguelResponse registrarDevolucao(UUID id, DevolucaoRequest request) {
        Aluguel aluguel = obterEntidade(id);
        LocalDateTime devolucao = request.dataDevolucao() != null
                ? request.dataDevolucao()
                : LocalDateTime.now();

        aluguel.registrarDevolucao(devolucao, request.quilometragemFinal());
        liberarCarro(aluguel.getCarro().getId(), request.quilometragemFinal());

        return responder(aluguelRepository.save(aluguel));
    }

    @Transactional
    public AluguelResponse registrarPagamento(UUID id) {
        Aluguel aluguel = obterEntidade(id);
        if (aluguel.getStatus() == StatusAluguel.ATIVO) {
            throw new RegraDeNegocioException(
                    "O pagamento so pode ser registrado apos a devolucao do veiculo");
        }
        aluguel.registrarPagamento();
        return responder(aluguelRepository.save(aluguel));
    }

    @Transactional
    public AluguelResponse cancelar(UUID id, CancelamentoRequest request) {
        Aluguel aluguel = obterEntidade(id);
        aluguel.cancelar(CampoProtegido.deTextoOpcional(request.motivo(), protecao).cifrado());
        liberarCarro(aluguel.getCarro().getId(), null);
        return responder(aluguelRepository.save(aluguel));
    }

    @Transactional(readOnly = true)
    public AluguelResponse buscarPorId(UUID id) {
        return responder(obterEntidade(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<AluguelResponse> listar(UUID carroId, StatusAluguel status, Pageable pageable) {
        Page<Aluguel> pagina;
        if (carroId != null) {
            pagina = aluguelRepository.findByCarroIdOrderByDataInicioDesc(carroId, pageable);
        } else if (status != null) {
            pagina = aluguelRepository.findByStatusOrderByDataInicioDesc(status, pageable);
        } else {
            pagina = aluguelRepository.findAll(pageable);
        }
        return PageResponse.from(pagina.map(this::responder));
    }

    /** Devolve o veiculo a frota, atualizando a quilometragem quando houve percorrencia. */
    private void liberarCarro(UUID carroId, Long quilometragemFinal) {
        Carro carro = carroRepository.findByIdParaAtualizacao(carroId)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Carro", carroId));
        if (quilometragemFinal != null) {
            carro.registrarQuilometragem(quilometragemFinal);
        }
        carro.liberar();
        carroRepository.save(carro);
    }

    /**
     * Decifra os dados da pessoa guardados no proprio aluguel. Como nao ha
     * cadastro de cliente, o id do resumo e o do aluguel.
     */
    private AluguelResponse responder(Aluguel aluguel) {
        Carro carro = aluguel.getCarro();

        AluguelResponse.PessoaResumo pessoa = new AluguelResponse.PessoaResumo(
                aluguel.getId(),
                protecao.decifrar(aluguel.getNomeCifrado()),
                protecao.decifrar(aluguel.getCpfCifrado()));

        AluguelResponse.CarroResumo resumoCarro = new AluguelResponse.CarroResumo(
                carro.getId(),
                protecao.decifrar(carro.getPlacaCifrada()),
                carro.getMarca(),
                carro.getModelo(),
                carro.getCor(),
                carro.getCategoria());

        String observacoes = CampoProtegido.deCifrado(aluguel.getObservacoesCifradas(), protecao)
                .paraExibir(protecao);

        return AluguelResponse.from(aluguel, pessoa, resumoCarro, observacoes);
    }

    private Aluguel obterEntidade(UUID id) {
        return aluguelRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Aluguel", id));
    }
}
