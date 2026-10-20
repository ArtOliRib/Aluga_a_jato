package org.arthur.genericapi.dto.aluguel;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Criacao do aluguel. Os dados da pessoa (nome, cpf, email, telefone, cnh) vem
 * no proprio aluguel, ja que nao ha mais cadastro de cliente (ADR-0003): sao
 * protegidos pelo servico antes de tocar o banco.
 *
 * <p>dataInicio, quilometragemInicial e valorDiaria sao opcionais: quando
 * ausentes o servico assume o momento atual, a quilometragem e a diaria
 * cadastrados do carro.
 */
public record AluguelRequest(

        @NotBlank(message = "nome e obrigatorio")
        @Size(max = 150, message = "nome deve ter no maximo 150 caracteres")
        String nome,

        @NotBlank(message = "cpf e obrigatorio")
        @Pattern(regexp = "^[0-9.\\-\\s]{11,14}$", message = "cpf invalido")
        String cpf,

        @NotBlank(message = "email e obrigatorio")
        @Email(message = "email invalido")
        @Size(max = 150, message = "email deve ter no maximo 150 caracteres")
        String email,

        @NotBlank(message = "telefone e obrigatorio")
        @Size(max = 25, message = "telefone deve ter no maximo 25 caracteres")
        String telefone,

        @NotBlank(message = "cnh e obrigatorio")
        @Pattern(regexp = "^[0-9A-Za-z.\\-\\s]{5,20}$", message = "cnh invalida")
        String cnh,

        @NotNull(message = "carroId e obrigatorio")
        UUID carroId,

        LocalDateTime dataInicio,

        @NotNull(message = "dataPrevistaDevolucao e obrigatoria")
        LocalDateTime dataPrevistaDevolucao,

        @PositiveOrZero(message = "quilometragemInicial nao pode ser negativa")
        Long quilometragemInicial,

        @DecimalMin(value = "0.01", message = "valorDiaria deve ser positivo")
        BigDecimal valorDiaria,

        @Size(max = 500, message = "observacoes deve ter no maximo 500 caracteres")
        String observacoes
) {
}
