package org.arthur.genericapi;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cobre o que o front-end precisa para conseguir falar com a API.
 *
 * <p>Sao testes de regressao: antes deles, {@code Content-Type} diferente de JSON
 * e rota inexistente retornavam <b>500</b>, o que fazia uma requisicao malformada
 * parecer um defeito da API. E nao havia CORS nenhum, entao o navegador
 * bloqueava toda chamada de outra origem.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Acesso do front-end: CORS, erros de protocolo e raiz")
class AcessoDoFrontEndTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Content-Type nao suportado responde 415, nao 500")
    void tipoDeConteudoInvalido() throws Exception {
        mockMvc.perform(post("/api/v1/carros")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("nome=Uno"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415))
                .andExpect(jsonPath("$.title").value("Tipo de conteudo nao suportado"));
    }

    @Test
    @DisplayName("rota inexistente responde 404 com ProblemDetail, nao 500")
    void rotaInexistente() throws Exception {
        mockMvc.perform(get("/api/v1/nao-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Rota nao encontrada"));
    }

    @Test
    @DisplayName("a raiz responde 200 e identifica a aplicacao")
    void raizResponde() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aplicacao").value("aluguel"));
    }

    @Test
    @DisplayName("preflight de origem permitida devolve os headers de CORS")
    void preflightDeOrigemPermitida() throws Exception {
        mockMvc.perform(options("/api/v1/carros")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Methods", org.hamcrest.Matchers.containsString("POST")))
                // O front le este header para descobrir a URL do recurso criado.
                .andExpect(header().string("Access-Control-Expose-Headers", "Location"));
    }

    @Test
    @DisplayName("origem nao listada nao recebe permissao de CORS")
    void origemNaoPermitida() throws Exception {
        mockMvc.perform(options("/api/v1/carros")
                        .header("Origin", "http://site-malicioso.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }
}
