package org.arthur.genericapi.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Raiz da API.
 *
 * <p>Serve para duas coisas: dar uma resposta util a quem abre a URL base no
 * navegador (antes disso, {@code GET /} era 404) e dar ao front um ponto barato
 * para checar se a API esta no ar antes de tentar carregar dados.
 *
 * <p>Nao expoe nada do dominio de proposito: e uma porta de entrada, nao um
 * indice de recursos.
 */
@RestController
public class RaizController {

    @GetMapping("/")
    public Map<String, String> raiz() {
        return Map.of(
                "aplicacao", "aluguel",
                "descricao", "API de registro de aluguel de veiculos",
                "versao", "v1",
                "recursos", "/api/v1/carros, /api/v1/alugueis, /api/v1/relatorios"
        );
    }
}
