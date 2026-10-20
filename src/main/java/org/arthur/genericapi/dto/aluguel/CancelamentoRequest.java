package org.arthur.genericapi.dto.aluguel;

import jakarta.validation.constraints.Size;

public record CancelamentoRequest(

        @Size(max = 500, message = "motivo deve ter no maximo 500 caracteres")
        String motivo
) {
}