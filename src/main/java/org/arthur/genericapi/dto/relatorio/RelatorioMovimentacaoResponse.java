package org.arthur.genericapi.dto.relatorio;

import org.arthur.genericapi.repository.projection.ResumoMovimentacaoProjection;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

/** Fechamento de movimentacao da receita no periodo. */
public record RelatorioMovimentacaoResponse(
        LocalDateTime periodoInicio,
        LocalDateTime periodoFim,
        UUID carroId,
        long totalAlugueis,
        long totalAtivos,
        long totalConcluidos,
        BigDecimal receitaTotal,
        BigDecimal receitaPaga,
        BigDecimal receitaPendente,
        BigDecimal ticketMedio,
        long quilometragemPercorrida
) {

    public static RelatorioMovimentacaoResponse from(ResumoMovimentacaoProjection projection,
                                                    LocalDateTime de, LocalDateTime ate, UUID carroId) {
        long total = nvl(projection.getTotalAlugueis());
        BigDecimal receita = nvlDecimal(projection.getReceitaTotal());
        BigDecimal ticketMedio = total == 0
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : receita.divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);

        return new RelatorioMovimentacaoResponse(
                de,
                ate,
                carroId,
                total,
                nvl(projection.getTotalAtivos()),
                nvl(projection.getTotalConcluidos()),
                receita,
                nvlDecimal(projection.getReceitaPaga()),
                nvlDecimal(projection.getReceitaPendente()),
                ticketMedio,
                nvl(projection.getQuilometragemPercorrida())
        );
    }

    private static long nvl(Long valor) {
        return valor == null ? 0L : valor;
    }

    private static BigDecimal nvlDecimal(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }
}