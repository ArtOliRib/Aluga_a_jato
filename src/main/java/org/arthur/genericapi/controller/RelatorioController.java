package org.arthur.genericapi.controller;

import lombok.RequiredArgsConstructor;
import org.arthur.genericapi.dto.relatorio.MovimentacaoPorCarroResponse;
import org.arthur.genericapi.dto.relatorio.RelatorioMovimentacaoResponse;
import org.arthur.genericapi.service.RelatorioService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Relatorios de movimentacao financeira. Alugueis cancelados nunca entram nas
 * somas; o periodo considera qualquer aluguel que o sobreponha.
 */
@RestController
@RequestMapping("/api/v1/relatorios")
@RequiredArgsConstructor
public class RelatorioController {

    private final RelatorioService relatorioService;

    @GetMapping("/movimentacao")
    public RelatorioMovimentacaoResponse movimentacao(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime de,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime ate,
            @RequestParam(required = false) UUID carroId) {
        return relatorioService.movimentacao(de, ate, carroId);
    }

    @GetMapping("/movimentacao-por-carro")
    public List<MovimentacaoPorCarroResponse> movimentacaoPorCarro(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime de,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime ate) {
        return relatorioService.movimentacaoPorCarro(de, ate);
    }
}