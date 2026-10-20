package org.arthur.genericapi.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Ciclo de vida do aluguel. Regras de transicao centralizadas (OCP).
 */
public enum StatusAluguel {

    ATIVO,
    CONCLUIDO,
    CANCELADO;

    public boolean podeTransicionarPara(StatusAluguel destino) {
        return transicoesPermitidas().contains(destino);
    }

    public boolean estaEncerrado() {
        return this == CONCLUIDO || this == CANCELADO;
    }

    private Set<StatusAluguel> transicoesPermitidas() {
        return switch (this) {
            case ATIVO -> EnumSet.of(CONCLUIDO, CANCELADO);
            case CONCLUIDO, CANCELADO -> EnumSet.noneOf(StatusAluguel.class);
        };
    }
}