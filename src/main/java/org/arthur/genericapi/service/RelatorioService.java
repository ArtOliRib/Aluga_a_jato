package org.arthur.genericapi.service;

import lombok.RequiredArgsConstructor;
import org.arthur.genericapi.dto.relatorio.MovimentacaoPorCarroResponse;
import org.arthur.genericapi.dto.relatorio.RelatorioMovimentacaoResponse;
import org.arthur.genericapi.exception.RegraDeNegocioException;
import org.arthur.genericapi.repository.AluguelRepository;
import org.arthur.genericapi.repository.projection.MovimentacaoPorCarroProjection;
import org.arthur.genericapi.repository.projection.ResumoMovimentacaoProjection;
import org.arthur.genericapi.support.ProtecaoDeDadoPessoal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Consultas de leitura sobre a movimentacao. Nao altera nada: apenas agrega o
 * que ja foi registrado pelos demais fluxos.
 *
 * <p>O criterio de inclusao e a sobreposicao do periodo com o aluguel: um
 * aluguel aparece em qualquer periodo que ele toque.
 *
 * <p>Os relatorios entregam apenas agregados de aluguel e carro (valores,
 * quilometragem, total de alugueis). Nao ha identidade central de pessoa: os
 * dados pessoais ficam cifrados dentro do proprio aluguel e nao sao expostos
 * aqui.
 */
@Service
@RequiredArgsConstructor
public class RelatorioService {

    private final AluguelRepository aluguelRepository;
    private final ProtecaoDeDadoPessoal protecao;

    @Transactional(readOnly = true)
    public RelatorioMovimentacaoResponse movimentacao(LocalDateTime de, LocalDateTime ate, UUID carroId) {
        validarPeriodo(de, ate);
        ResumoMovimentacaoProjection resumo =
                aluguelRepository.resumirMovimentacao(de, ate, carroId);
        return RelatorioMovimentacaoResponse.from(resumo, de, ate, carroId);
    }

    @Transactional(readOnly = true)
    public List<MovimentacaoPorCarroResponse> movimentacaoPorCarro(LocalDateTime de, LocalDateTime ate) {
        validarPeriodo(de, ate);
        List<MovimentacaoPorCarroProjection> linhas =
                aluguelRepository.listarMovimentacaoPorCarro(de, ate);

        BigDecimal receitaGeral = linhas.stream()
                .map(linha -> nvl(linha.getReceitaTotal()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return linhas.stream()
                .map(linha -> new MovimentacaoPorCarroResponse(
                        linha.getCarroId(),
                        protecao.decifrar(linha.getPlacaCifrada()),
                        linha.getMarca(),
                        linha.getModelo(),
                        nvl(linha.getTotalAlugueis()),
                        nvl(linha.getReceitaTotal()),
                        nvl(linha.getReceitaPaga()),
                        nvl(linha.getReceitaTotal()).subtract(nvl(linha.getReceitaPaga())),
                        nvl(linha.getQuilometragemPercorrida()),
                        percentual(linha.getReceitaTotal(), receitaGeral)))
                .toList();
    }

    private static BigDecimal percentual(BigDecimal receitaLinha, BigDecimal receitaGeral) {
        if (receitaGeral.signum() == 0) {
            return BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_UP);
        }
        return nvl(receitaLinha).multiply(BigDecimal.valueOf(100))
                .divide(receitaGeral, 2, java.math.RoundingMode.HALF_UP);
    }

    private static BigDecimal nvl(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    private static long nvl(Long valor) {
        return valor == null ? 0L : valor;
    }

    private void validarPeriodo(LocalDateTime de, LocalDateTime ate) {
        if (de == null || ate == null) {
            throw new RegraDeNegocioException("Informe o inicio e o fim do periodo");
        }
        if (de.isAfter(ate)) {
            throw new RegraDeNegocioException("O inicio do periodo nao pode ser depois do fim");
        }
    }
}