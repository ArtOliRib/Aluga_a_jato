package org.arthur.genericapi.dto.aluguel;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/** Devolucao do veiculo. Ausente a data, o servico registra o momento atual. */
public record DevolucaoRequest(

        LocalDateTime dataDevolucao,

        @NotNull(message = "quilometragemFinal e obrigatoria")
        Long quilometragemFinal
) {
}