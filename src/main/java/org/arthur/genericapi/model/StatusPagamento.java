package org.arthur.genericapi.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Situacao financeira do aluguel. Regras de transicao centralizadas (OCP).
 */
public enum StatusPagamento {

    PENDENTE,
    PAGO,
    CANCELADO;

    public boolean podeTransicionarPara(StatusPagamento destino) {
        return transicoesPermitidas().contains(destino);
    }

    public boolean estaQuitado() {
        return this == PAGO;
    }

    private Set<StatusPagamento> transicoesPermitidas() {
        return switch (this) {
            case PENDENTE -> EnumSet.of(PAGO, CANCELADO);
            case PAGO, CANCELADO -> EnumSet.noneOf(StatusPagamento.class);
        };
    }
}