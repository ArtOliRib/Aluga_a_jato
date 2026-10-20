package org.arthur.genericapi.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Envelope de paginacao. Evita expor o {@link Page} do Spring direto na API,
 * cujo formato muda entre versoes.
 */
public record PageResponse<T>(
        List<T> conteudo,
        int pagina,
        int tamanho,
        long totalElementos,
        int totalPaginas,
        boolean primeira,
        boolean ultima
) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}