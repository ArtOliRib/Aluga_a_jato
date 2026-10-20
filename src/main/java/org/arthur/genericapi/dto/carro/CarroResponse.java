package org.arthur.genericapi.dto.carro;

import org.arthur.genericapi.model.StatusCarro;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Resposta do carro. A placa chega aqui ja decifrada: a montagem fica a cargo do
 * service, que e o unico lugar com acesso as chaves. Nao existe factory
 * {@code from(Carro)} justamente porque a entidade so guarda material cifrado.
 */
public record CarroResponse(
        UUID id,
        String nome,
        String marca,
        String modelo,
        Integer ano,
        String placa,
        Long quilometragem,
        String cor,
        String categoria,
        BigDecimal valorDiaria,
        StatusCarro status,
        boolean disponivelParaAluguel,
        LocalDateTime dataCadastro,
        LocalDateTime dataAtualizacao
) {
}