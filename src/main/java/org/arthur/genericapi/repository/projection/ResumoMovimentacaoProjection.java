package org.arthur.genericapi.repository.projection;

import java.math.BigDecimal;

/**
 * Totais de movimentacao no periodo. Projecao de interface evita carregar
 * entidades so para agregar em memoria (a consulta vira um unico SQL).
 */
public interface ResumoMovimentacaoProjection {

    Long getTotalAlugueis();

    Long getTotalConcluidos();

    Long getTotalAtivos();

    BigDecimal getReceitaTotal();

    BigDecimal getReceitaPaga();

    BigDecimal getReceitaPendente();

    Long getQuilometragemPercorrida();
}