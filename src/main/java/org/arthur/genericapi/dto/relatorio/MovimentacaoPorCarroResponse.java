package org.arthur.genericapi.dto.relatorio;

import java.math.BigDecimal;
import java.util.UUID;

/** Uma linha do ranking de faturamento por veiculo. */
public record MovimentacaoPorCarroResponse(
        UUID carroId,
        String placa,
        String marca,
        String modelo,
        long totalAlugueis,
        BigDecimal receitaTotal,
        BigDecimal receitaPaga,
        BigDecimal receitaPendente,
        long quilometragemPercorrida,
        BigDecimal percentualDaReceita
) {
}