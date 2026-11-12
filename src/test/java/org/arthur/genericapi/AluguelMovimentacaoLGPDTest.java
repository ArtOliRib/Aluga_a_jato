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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A movimentacao e o dado com maior volume da locacao. Sem cadastro de cliente
 * (ADR-0003), e o proprio aluguel que guarda os dados da pessoa — sempre
 * protegidos: indice (HMAC) para busca e igualdade, cifra (AES-256-GCM) para
 * exibicao. O que vai para o banco e lido por JDBC de proposito: asserir pela
 * API provaria apenas que ela funciona, nao que o dado nunca foi gravado claro.
 *
 * <p>As observacoes sao texto livre e por isso tambem vao cifradas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Movimentacao com dado pessoal protegido sob a LGPD")
class AluguelMovimentacaoLGPDTest {

    private static final String NOME = "Bruno Movimentado";
    private static final String EMAIL = "bruno.movimentado@email.com";
    private static final String TELEFONE = "11955554444";
    private static final String CPF = "39053344705";
    private static final String CNH = "CNH55555";
    private static final String OBSERVACOES = "Pessoa pediu capacete e banco dianteiro quebrado";
    private static final LocalDateTime HOJE = LocalDateTime.now();

    /** Todo valor em claro que nao pode existir em nenhuma coluna de alugueis. */
    private static final List<String> SEGREDOS = List.of(NOME, EMAIL, TELEFONE, CPF, CNH, OBSERVACOES);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EntityManager em;

    @Autowired
    private tools.jackson.databind.ObjectMapper objectMapper;

    @Test
    @DisplayName("a linha do aluguel nao guarda dado pessoal em claro")
    void aluguelNaoCarregaDadosPessoaisEmClaro() throws Exception {
        UUID aluguelId = registrarAluguel();

        Map<String, Object> linha = jdbc.queryForMap("select * from alugueis where id = ?", aluguelId);

        // As colunas protegidas existem; as colunas em claro, nao.
        assertThat(linha).containsKeys("nome_cifrado", "cpf_cifrado", "email_cifrado",
                "telefone_cifrado", "cnh_cifrado", "nome_indice", "cpf_indice", "cnh_indice");
        assertThat(linha).doesNotContainKeys("nome", "cpf", "email", "telefone", "cnh", "cliente_token");

        assertThat(linha.values())
                .as("nenhum dado pessoal em claro na movimentacao")
                .allSatisfy(valor -> {
                    if (valor instanceof String texto) {
                        for (String segredo : SEGREDOS) {
                            assertThat(texto).doesNotContain(segredo);
                        }
                    }
                });
    }

    @Test
    @DisplayName("os indices sao HMAC hex de 64 caracteres")
    void indicesSaoHmacHex() throws Exception {
        UUID aluguelId = registrarAluguel();

        Map<String, Object> linha = jdbc.queryForMap(
                "select nome_indice, cpf_indice, cnh_indice from alugueis where id = ?", aluguelId);

        linha.forEach((coluna, valor) -> assertThat((String) valor)
                .as("%s deve ser HMAC-SHA-256 hex", coluna)
                .hasSize(64)
                .matches("[0-9a-f]{64}"));
    }

    @Test
    @DisplayName("a API decifra e devolve a pessoa na resposta")
    void apiDecifraAPessoa() throws Exception {
        UUID aluguelId = registrarAluguel();

        mockMvc.perform(get("/api/v1/alugueis/{id}", aluguelId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pessoa.nome").value(NOME))
                .andExpect(jsonPath("$.pessoa.cpf").value(CPF));
    }

    @Test
    @DisplayName("o mesmo cpf pode estar em varios alugueis")
    void mesmoCpfEmVariosAlugueis() throws Exception {
        UUID primeiro = criarAluguel(criarCarro(), CPF, CNH, null);
        UUID segundo = criarAluguel(criarCarro(), CPF, CNH, null);

        assertThat(primeiro).isNotNull();
        assertThat(segundo).isNotNull();

        // Os dois indices de CPF batem (HMAC deterministico): o indice serve ao
        // agrupamento, nunca a unicidade — tanto que os dois alugueis coexistem.
        List<String> indices = jdbc.queryForList(
                "select cpf_indice from alugueis where id in (?, ?)", String.class, primeiro, segundo);
        assertThat(indices).hasSize(2).containsOnly(indices.get(0));
    }

    @Test
    @DisplayName("as observacoes sao cifradas no banco e decifradas na API")
    void observacoesCifradas() throws Exception {
        UUID aluguelId = registrarAluguel();

        String cifrado = jdbc.queryForObject(
                "select observacoes_cifradas from alugueis where id = ?", String.class, aluguelId);

        assertThat(cifrado).isNotBlank().doesNotContain(OBSERVACOES);

        mockMvc.perform(get("/api/v1/alugueis/{id}", aluguelId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.observacoes").value(OBSERVACOES));
    }

    @Test
    @DisplayName("aluguel sem observacoes deixa a coluna nula")
    void observacoesOpcional() throws Exception {
        UUID aluguelId = criarAluguelSemObservacoes();

        assertThat(jdbc.queryForObject(
                "select observacoes_cifradas from alugueis where id = ?", String.class, aluguelId))
                .isNull();

        mockMvc.perform(get("/api/v1/alugueis/{id}", aluguelId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.observacoes").doesNotExist());
    }

    @Test
    @DisplayName("quilometragem e valores ficam em claro para os relatorios")
    void quilometragemEValoresEmClaro() throws Exception {
        UUID aluguelId = registrarAluguel();

        Map<String, Object> linha = jdbc.queryForMap(
                "select quilometragem_inicial, valor_diaria, valor_total from alugueis where id = ?",
                aluguelId);

        // Cifra nao suporta filtro por faixa nem soma: esses campos seguem legiveis de proposito.
        assertThat(((Number) linha.get("quilometragem_inicial")).longValue()).isEqualTo(10_000L);
        assertThat(((Number) linha.get("valor_diaria")).doubleValue()).isEqualTo(150.00);
        assertThat(((Number) linha.get("valor_total")).doubleValue()).isGreaterThan(0.0);
    }

    // --- auxiliares ---------------------------------------------------------

    private UUID registrarAluguel() throws Exception {
        return criarAluguel(criarCarro(), CPF, CNH, OBSERVACOES);
    }

    private UUID criarAluguelSemObservacoes() throws Exception {
        return criarAluguel(criarCarro(), CPF, CNH, null);
    }

    private UUID criarAluguel(UUID carroId, String cpf, String cnh, String observacoes) throws Exception {
        StringBuilder corpo = new StringBuilder()
                .append("{\"nome\":\"").append(NOME)
                .append("\",\"cpf\":\"").append(cpf)
                .append("\",\"email\":\"").append(EMAIL)
                .append("\",\"telefone\":\"").append(TELEFONE)
                .append("\",\"cnh\":\"").append(cnh)
                .append("\",\"carroId\":\"").append(carroId)
                .append("\",\"dataInicio\":\"").append(HOJE.minusDays(2))
                .append("\",\"dataPrevistaDevolucao\":\"").append(HOJE.plusDays(2))
                .append("\",\"quilometragemInicial\":10000,\"valorDiaria\":150.00");
        if (observacoes != null) {
            corpo.append(",\"observacoes\":\"").append(observacoes).append("\"");
        }
        corpo.append("}");

        String resposta = mockMvc.perform(post("/api/v1/alugueis")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(corpo.toString()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        em.flush();
        return UUID.fromString(objectMapper.readTree(resposta).get("id").asText());
    }

    private UUID criarCarro() throws Exception {
        String placa = "MOV" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        String corpo = """
                {"nome":"Uno","marca":"Fiat","modelo":"Uno","ano":2022,"placa":"%s",
                 "quilometragem":10000,"cor":"Prata","categoria":"Economico",
                 "valorDiaria":150.00}
                """.formatted(placa);
        String resposta = mockMvc.perform(post("/api/v1/carros")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        em.flush();
        return UUID.fromString(objectMapper.readTree(resposta).get("id").asText());
    }
}
