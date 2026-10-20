package org.arthur.genericapi.dto.carro;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record QuilometragemRequest(

        @NotNull(message = "quilometragem e obrigatoria")
        @PositiveOrZero(message = "quilometragem nao pode ser negativa")
        Long quilometragem
) {
}