package org.arthur.genericapi.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Ciclo de vida do carro na locadora.
 * As transicoes validas ficam aqui (unico lugar), para que novos estados
 * nao exijam alterar o {@link Carro} (OCP).
 */
public enum StatusCarro {

    DISPONIVEL,
    ALUGADO,
    MANUTENCAO,
    DESATIVADO;

    public boolean podeTransicionarPara(StatusCarro destino) {
        return transicoesPermitidas().contains(destino);
    }

    public boolean estaDisponivelParaAluguel() {
        return this == DISPONIVEL;
    }

    private Set<StatusCarro> transicoesPermitidas() {
        return switch (this) {
            case DISPONIVEL -> EnumSet.of(ALUGADO, MANUTENCAO, DESATIVADO);
            case ALUGADO -> EnumSet.of(DISPONIVEL, MANUTENCAO);
            case MANUTENCAO -> EnumSet.of(DISPONIVEL, DESATIVADO);
            case DESATIVADO -> EnumSet.noneOf(StatusCarro.class);
        };
    }
}