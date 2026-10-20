package org.arthur.genericapi.dto.carro;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PlacaRequest(

        @NotBlank(message = "placa e obrigatoria")
        @Pattern(regexp = "^[0-9A-Za-z\\-\\s]{7,10}$", message = "placa invalida")
        String placa
) {
}