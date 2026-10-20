package org.arthur.genericapi.dto.carro;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.arthur.genericapi.model.StatusCarro;

public record CarroStatusRequest(

        @NotNull(message = "status e obrigatorio")
        StatusCarro status,

        @Size(max = 200, message = "motivo deve ter no maximo 200 caracteres")
        String motivo
) {
}