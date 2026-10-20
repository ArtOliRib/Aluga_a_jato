package org.arthur.genericapi.dto.carro;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CarroRequest(

        @NotBlank(message = "nome e obrigatorio")
        @Size(max = 120, message = "nome deve ter no maximo 120 caracteres")
        String nome,

        @NotBlank(message = "marca e obrigatoria")
        @Size(max = 80, message = "marca deve ter no maximo 80 caracteres")
        String marca,

        @NotBlank(message = "modelo e obrigatorio")
        @Size(max = 80, message = "modelo deve ter no maximo 80 caracteres")
        String modelo,

        @NotNull(message = "ano e obrigatorio")
        @Min(value = 1900, message = "ano invalido")
        Integer ano,

        @NotBlank(message = "placa e obrigatoria")
        @Pattern(regexp = "^[0-9A-Za-z\\-\\s]{7,10}$", message = "placa invalida")
        String placa,

        @NotNull(message = "quilometragem e obrigatoria")
        @PositiveOrZero(message = "quilometragem nao pode ser negativa")
        Long quilometragem,

        @Size(max = 40, message = "cor deve ter no maximo 40 caracteres")
        String cor,

        @Size(max = 40, message = "categoria deve ter no maximo 40 caracteres")
        String categoria,

        @NotNull(message = "valorDiaria e obrigatorio")
        @DecimalMin(value = "0.01", message = "valorDiaria deve ser positivo")
        BigDecimal valorDiaria
) {
}