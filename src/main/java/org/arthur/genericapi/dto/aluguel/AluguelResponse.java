package org.arthur.genericapi.dto.aluguel;

import org.arthur.genericapi.model.Aluguel;
import org.arthur.genericapi.model.StatusAluguel;
import org.arthur.genericapi.model.StatusPagamento;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Resposta do aluguel com os dados da pessoa e o resumo do carro. O resumo
 * existe para a API nao arrastar o grafo inteiro de entidades e para nao expor,
 * por exemplo, o e-mail ou o telefone dentro da listagem de alugueis.
 *
 * <p>Os dados que vem de campos protegidos chegam decifrados por parametro: a
 * entidade guarda material cifrado e nao tem chave para resolver.
 */
public record AluguelResponse(
        UUID id,
        PessoaResumo pessoa,
        CarroResumo carro,
        LocalDateTime dataInicio,
        LocalDateTime dataPrevistaDevolucao,
        LocalDateTime dataDevolucao,
        Long quilometragemInicial,
        Long quilometragemFinal,
        Long quilometragemPercorrida,
        BigDecimal valorDiaria,
        Integer totalDiarias,
        BigDecimal valorTotal,
        StatusAluguel status,
        StatusPagamento statusPagamento,
        boolean atrasado,
        String observacoes,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao
) {

    /**
     * Dados da pessoa decifrados a partir do proprio aluguel. O id e o do
     * aluguel: sem cadastro de cliente, nao ha identidade central para apontar.
     */
    public record PessoaResumo(UUID id, String nome, String cpf) {
    }

    public record CarroResumo(UUID id, String placa, String marca, String modelo, String cor, String categoria) {
    }

    public static AluguelResponse from(Aluguel aluguel,
                                      PessoaResumo pessoa,
                                      CarroResumo carro,
                                      String observacoes) {
        return new AluguelResponse(
                aluguel.getId(),
                pessoa,
                carro,
                aluguel.getDataInicio(),
                aluguel.getDataPrevistaDevolucao(),
                aluguel.getDataDevolucao(),
                aluguel.getQuilometragemInicial(),
                aluguel.getQuilometragemFinal(),
                aluguel.getQuilometragemPercorrida(),
                aluguel.getValorDiaria(),
                aluguel.getTotalDiarias(),
                aluguel.getValorTotal(),
                aluguel.getStatus(),
                aluguel.getStatusPagamento(),
                aluguel.estaAtrasado(),
                observacoes,
                aluguel.getDataCriacao(),
                aluguel.getDataAtualizacao()
        );
    }
}