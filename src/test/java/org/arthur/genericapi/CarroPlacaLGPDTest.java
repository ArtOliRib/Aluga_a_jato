package org.arthur.genericapi;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A placa identifica o veiculo e, por consequencia, a pessoa que dirige.
 * Valida que ela segue o mesmo cofre do CPF: indice para buscar, cifra para
 * guardar, nada em claro no banco.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@DisplayName("Placa do carro protegida sob a LGPD")
class CarroPlacaLGPDTest {

    private static final String PLACA = "KLM4P77";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EntityManager em;

    @Autowired
    private tools.jackson.databind.ObjectMapper objectMapper;

    @Test
    @DisplayName("a placa nao e gravada em claro em nenhuma coluna")
    void nadaEmClaroNoBanco() throws Exception {
        criarCarro(PLACA);

        Map<String, Object> linha = jdbc.queryForMap("select * from carros where placa_indice is not null");

        assertThat(linha).containsKeys("placa_indice", "placa_cifrada");
        assertThat(linha).doesNotContainKey("placa");

        assertThat(linha.values())
                .as("nenhuma coluna pode conter a placa em claro")
                .allSatisfy(valor -> {
                    if (valor instanceof String texto) {
                        assertThat(texto).doesNotContain(PLACA);
                    }
                });
    }

    @Test
    @DisplayName("o indice da placa e um HMAC hex de 64 caracteres")
    void formatoDoIndice() throws Exception {
        criarCarro(PLACA);

        assertThat(jdbc.queryForObject("select placa_indice from carros", String.class))
                .hasSize(64)
                .matches("[0-9a-f]{64}");
    }

    @Test
    @DisplayName("a API decifra e devolve a placa real")
    void apiDecifra() throws Exception {
        UUID id = criarCarro(PLACA);

        mockMvc.perform(get("/api/v1/carros/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.placa").value(PLACA));
    }

    @Test
    @DisplayName("busca por placa ignora a formatacao")
    void buscaIgnoraFormatacao() throws Exception {
        UUID id = criarCarro(PLACA);

        for (String forma : List.of(PLACA, "KLM-4P77", "KLM 4P77", "klm4p77")) {
            mockMvc.perform(get("/api/v1/carros/por-placa/{placa}", forma))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id.toString()))
                    .andExpect(jsonPath("$.placa").value(PLACA));
        }
    }

    @Test
    @DisplayName("placa duplicada responde 422 sem ecoar a placa")
    void duplicidadeNaoVazaPlaca() throws Exception {
        criarCarro(PLACA);

        mockMvc.perform(post("/api/v1/carros")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(corpoDeCarro("KLM4P77")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail", not(containsString(PLACA))))
                .andExpect(jsonPath("$.detail").value(containsString("placa informada")));
    }

    @Test
    @DisplayName("alterar a placa substitui indice e cifra juntos")
    void alterarPlacaReprotege() throws Exception {
        UUID id = criarCarro(PLACA);
        String indiceAntigo = jdbc.queryForObject(
                "select placa_indice from carros where id = ?", String.class, id);

mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/v1/carros/{id}/placa", id)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {"placa":"ZZZ9Z99"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.placa").value("ZZZ9Z99"));

        // A leitura JDBC usa outra conexao: sem o flush o EntityManager ainda
        // nao empurrou o novo indice, e o teste leria o estado antigo.
        em.flush();

        Map<String, Object> linha = jdbc.queryForMap("select placa_indice, placa_cifrada from carros where id = ?", id);

        assertThat(linha.get("placa_indice"))
                .as("o indice antigo nao pode sobreviver a troca")
                .isNotEqualTo(indiceAntigo);
        assertThat((String) linha.get("placa_cifrada")).doesNotContain("ZZZ9Z99", PLACA);

        // A placa antiga deixou de encontrar o carro.
        mockMvc.perform(get("/api/v1/carros/por-placa/{placa}", PLACA))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("a constraint de unicidade da placa existe no banco")
    void constraintDeUnicidade() throws Exception {
        assertThat(jdbc.queryForObject("""
                select count(*) from information_schema.table_constraints
                 where constraint_name = 'UK_CARROS_PLACA_INDICE'
                """, Integer.class)).isEqualTo(1);
    }

    // --- auxiliares ---------------------------------------------------------

    private UUID criarCarro(String placa) throws Exception {
        String resposta = mockMvc.perform(post("/api/v1/carros")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(corpoDeCarro(placa)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        em.flush();
        return UUID.fromString(objectMapper.readTree(resposta).get("id").asText());
    }

    private String corpoDeCarro(String placa) {
        return """
                {"nome":"Uno","marca":"Fiat","modelo":"Uno","ano":2022,"placa":"%s",
                 "quilometragem":10000,"cor":"Prata","categoria":"Economico",
                 "valorDiaria":150.00}
                """.formatted(placa);
    }
}