package org.arthur.genericapi.repository.projection;

import java.math.BigDecimal;
import java.util.UUID;

/** Uma linha do ranking de faturamento por veiculo. */
public interface MovimentacaoPorCarroProjection {

    UUID getCarroId();

    /** Placa ainda cifrada: a decifracao e do service, que tem a chave. */
    String getPlacaCifrada();

    String getMarca();

    String getModelo();

    Long getTotalAlugueis();

    BigDecimal getReceitaTotal();

    BigDecimal getReceitaPaga();

    Long getQuilometragemPercorrida();
}