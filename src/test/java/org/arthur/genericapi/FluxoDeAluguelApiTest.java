package org.arthur.genericapi;

import org.arthur.genericapi.dto.aluguel.AluguelResponse;
import org.arthur.genericapi.model.StatusAluguel;
import org.arthur.genericapi.model.StatusPagamento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Percorre o fluxo completo pela API: cadastro de carro, retirada, devolucao,
 * pagamento e fechamento da movimentacao. Sem cadastro de cliente (ADR-0003),
 * os dados da pessoa vem no proprio corpo do aluguel. O {@code @Transactional}
 * da classe faz rollback ao fim de cada teste, mantendo o banco limpo entre os
 * cenarios.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@DisplayName("Fluxo completo de aluguel pela API")
class FluxoDeAluguelApiTest {

    private static final LocalDateTime HOJE = LocalDateTime.now();

    /** Dados da pessoa usados nos cenarios. Cada caso informa o CPF que fizer sentido. */
    private static final String NOME = "Carlos Prado";
    private static final String EMAIL = "carlos.prado@email.com";
    private static final String TELEFONE = "11988887777";
    private static final String CNH = "CNH99999";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("cadastro de carro e aluguel com pessoa responde 201 e devolve o id")
    void cadastro() throws Exception {
        UUID carroId = criarCarro("XYZ9K88", 10_000L, "150.00");

        assertThat(carroId).isNotNull();

        mockMvc.perform(get("/api/v1/carros/por-placa/{placa}", "xyz-9k88"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.placa").value("XYZ9K88"))
                .andExpect(jsonPath("$.status").value("DISPONIVEL"))
                .andExpect(jsonPath("$.disponivelParaAluguel").value(true));

        AluguelResponse aluguel = registrarAluguel(carroId, "52998224725", HOJE.minusDays(2),
                HOJE.plusDays(2), 10_000L, "150.00", null);

        assertThat(aluguel.id()).isNotNull();
        assertThat(aluguel.pessoa().nome()).isEqualTo(NOME);
        assertThat(aluguel.pessoa().cpf()).isEqualTo("52998224725");
    }

    @Test
    @DisplayName("ids sao UUID, nao inteiros sequenciais")
    void idsNaoSequenciais() throws Exception {
        UUID carroId = criarCarro("SEQ0001", 1_000L, "50.00");

        assertThat(carroId.toString()).hasSize(36).contains("-");
    }

    @Test
    @DisplayName("body invalido responde 400 com a lista de campos")
    void validacaoDeBody() throws Exception {
        UUID carroId = criarCarro("VAL0001", 1_000L, "50.00");

        mockMvc.perform(post("/api/v1/alugueis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"","cpf":"123","email":"nao-e-email",
                                 "telefone":"","cnh":"x","carroId":"%s",
                                 "dataPrevistaDevolucao":"%s"}
                                """.formatted(carroId, HOJE.plusDays(2))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Erro de validacao"))
                .andExpect(jsonPath("$.erros").isArray());
    }

    @Test
    @DisplayName("recursos inexistentes respondem 404")
    void naoEncontrado() throws Exception {
        UUID inexistente = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/carros/{id}", inexistente))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/alugueis/{id}", inexistente))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("fluxo completo: retira, devolve, paga e aparece no relatorio")
    void fluxoCompletoComRelatorio() throws Exception {
        UUID carroId = criarCarro("KLM4P77", 20_000L, "200.00");

        AluguelResponse aluguel = registrarAluguel(carroId, "98765432100",
                HOJE.minusDays(10), HOJE.minusDays(7), null, null, null);

        assertThat(aluguel.status()).isEqualTo(StatusAluguel.ATIVO);
        assertThat(aluguel.statusPagamento()).isEqualTo(StatusPagamento.PENDENTE);
        assertThat(aluguel.totalDiarias()).isEqualTo(3);
        assertThat(aluguel.valorTotal()).isEqualByComparingTo("600.00");
        assertThat(aluguel.pessoa().id()).isEqualTo(aluguel.id());
        assertThat(aluguel.pessoa().nome()).isEqualTo(NOME);
        assertThat(aluguel.carro().placa()).isEqualTo("KLM4P77");

        // O veiculo sai da disponibilidade assim que o aluguel e criado.
        mockMvc.perform(get("/api/v1/carros/{id}", carroId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ALUGADO"))
                .andExpect(jsonPath("$.disponivelParaAluguel").value(false));

        // Um segundo aluguel para o mesmo carro e recusado.
        mockMvc.perform(post("/api/v1/alugueis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"%s","cpf":"98765432100","email":"%s","telefone":"%s","cnh":"%s",
                                 "carroId":"%s","dataPrevistaDevolucao":"%s"}
                                """.formatted(NOME, EMAIL, TELEFONE, CNH, carroId, HOJE.plusDays(2))))
                .andExpect(status().isUnprocessableEntity());

        // Pagamento antes da devolucao nao e aceito.
        mockMvc.perform(patch("/api/v1/alugueis/{id}/pagamento", aluguel.id()))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(patch("/api/v1/alugueis/{id}/devolucao", aluguel.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dataDevolucao":"%s","quilometragemFinal":20150}
                                """.formatted(HOJE.minusDays(7))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONCLUIDO"))
                .andExpect(jsonPath("$.quilometragemPercorrida").value(150));

        AluguelResponse pago = ler(mockMvc.perform(patch("/api/v1/alugueis/{id}/pagamento", aluguel.id()))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(pago.statusPagamento()).isEqualTo(StatusPagamento.PAGO);

        // Devolucao devolve o carro a frota com a quilometragem atualizada.
        mockMvc.perform(get("/api/v1/carros/{id}", carroId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISPONIVEL"))
                .andExpect(jsonPath("$.quilometragem").value(20150));

        // Datas seguem como texto ISO-8601, nunca como array numerico.
        mockMvc.perform(get("/api/v1/alugueis/{id}", aluguel.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pessoa.nome").value(NOME))
                .andExpect(jsonPath("$.dataInicio").isString());

        // Fechamento financeiro do periodo.
        mockMvc.perform(get("/api/v1/relatorios/movimentacao")
                        .param("de", HOJE.minusDays(30).toString())
                        .param("ate", HOJE.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAlugueis").value(1))
                .andExpect(jsonPath("$.totalConcluidos").value(1))
                .andExpect(jsonPath("$.totalAtivos").value(0))
                .andExpect(jsonPath("$.receitaTotal").value(600.00))
                .andExpect(jsonPath("$.receitaPaga").value(600.00))
                .andExpect(jsonPath("$.receitaPendente").value(0))
                .andExpect(jsonPath("$.quilometragemPercorrida").value(150))
                .andExpect(jsonPath("$.ticketMedio").value(600.00));

        mockMvc.perform(get("/api/v1/relatorios/movimentacao-por-carro")
                        .param("de", HOJE.minusDays(30).toString())
                        .param("ate", HOJE.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].placa").value("KLM4P77"))
                .andExpect(jsonPath("$[0].receitaTotal").value(600.00))
                .andExpect(jsonPath("$[0].percentualDaReceita").value(100.00));
    }

    @Test
    @DisplayName("relatorio aceita o filtro por carro")
    void relatorioComFiltroDeCarro() throws Exception {
        UUID carroId = criarCarro("FIL0001", 5_000L, "100.00");
        registrarAluguel(carroId, "11111111111", HOJE.minusDays(2), HOJE.minusDays(1), null, null, null);

        mockMvc.perform(get("/api/v1/relatorios/movimentacao")
                        .param("de", HOJE.minusDays(5).toString())
                        .param("ate", HOJE.toString())
                        .param("carroId", carroId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.carroId").value(carroId.toString()))
                .andExpect(jsonPath("$.totalAlugueis").value(1));
    }

    @Test
    @DisplayName("periodo invertido responde 422")
    void periodoInvertido() throws Exception {
        mockMvc.perform(get("/api/v1/relatorios/movimentacao")
                        .param("de", HOJE.toString())
                        .param("ate", HOJE.minusDays(5).toString()))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("carro com aluguel vinculado nao pode ser removido")
    void carroComAluguelNaoRemove() throws Exception {
        UUID carroId = criarCarro("FRO0001", 4_000L, "70.00");
        registrarAluguel(carroId, "66677788899", HOJE.minusDays(1), HOJE.plusDays(1), null, null, null);

        mockMvc.perform(delete("/api/v1/carros/{id}", carroId))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("quilometragem nao retrocede e devolve 422")
    void quilometragemNaoRetrocede() throws Exception {
        UUID carroId = criarCarro("QUI0001", 50_000L, "60.00");

        mockMvc.perform(put("/api/v1/carros/{id}/quilometragem", carroId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quilometragem":49000}
                                """))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(put("/api/v1/carros/{id}/quilometragem", carroId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quilometragem":50500}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quilometragem").value(50500));
    }

    @Test
    @DisplayName("cancelamento zera o valor e nao aparece no relatorio")
    void cancelamento() throws Exception {
        UUID carroId = criarCarro("CAN0001", 2_000L, "90.00");
        AluguelResponse aluguel = registrarAluguel(carroId, "33344455566",
                HOJE.minusDays(1), HOJE.plusDays(1), null, null, null);

        mockMvc.perform(patch("/api/v1/alugueis/{id}/cancelamento", aluguel.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"motivo":"cliente desistiu"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/alugueis/{id}", aluguel.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADO"))
                .andExpect(jsonPath("$.statusPagamento").value("CANCELADO"))
                .andExpect(jsonPath("$.valorTotal").value(0.00))
                .andExpect(jsonPath("$.observacoes").value("cliente desistiu"));

        // Cancelado libera o carro e some do fechamento financeiro.
        mockMvc.perform(get("/api/v1/carros/{id}", carroId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISPONIVEL"));

        mockMvc.perform(get("/api/v1/relatorios/movimentacao")
                        .param("de", HOJE.minusDays(5).toString())
                        .param("ate", HOJE.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAlugueis").value(0))
                .andExpect(jsonPath("$.receitaTotal").value(0));
    }

    @Test
    @DisplayName("listagem de alugueis filtra por carro e por status")
    void listagemComFiltros() throws Exception {
        UUID carroId = criarCarro("LST0001", 3_000L, "75.00");
        registrarAluguel(carroId, "44455566677", HOJE.minusDays(1), HOJE.plusDays(1), null, null, null);

        mockMvc.perform(get("/api/v1/alugueis").param("carroId", carroId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo.length()").value(1))
                .andExpect(jsonPath("$.totalElementos").value(1));

        mockMvc.perform(get("/api/v1/alugueis").param("status", "ATIVO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1));

        mockMvc.perform(get("/api/v1/alugueis").param("status", "CONCLUIDO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(0));

        mockMvc.perform(get("/api/v1/carros/disponiveis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo.length()").value(0));
    }

    @Test
    @DisplayName("ordenar por coluna cifrada ou inexistente responde 422, nao 500")
    void ordenacaoComWhitelist() throws Exception {
        for (String campo : List.of("cpf_cifrado", "nome_cifrado", "xyz", "senha")) {
            mockMvc.perform(get("/api/v1/alugueis").param("ordenarPor", campo))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.detail").value(containsString("Ordenacao invalida")));
        }

        mockMvc.perform(get("/api/v1/carros").param("ordenarPor", "placa_cifrada"))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(get("/api/v1/alugueis").param("ordenarPor", "observacoes_cifradas"))
                .andExpect(status().isUnprocessableEntity());

        // Campos permitidos continuam funcionando.
        mockMvc.perform(get("/api/v1/alugueis").param("ordenarPor", "valorTotal").param("direcao", "desc"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/carros").param("ordenarPor", "ano"))
                .andExpect(status().isOk());
    }

    // --- auxiliares ---------------------------------------------------------

    private UUID criarCarro(String placa, Long quilometragem, String diaria) throws Exception {
        String corpo = """
                {"nome":"Uno","marca":"Fiat","modelo":"Uno","ano":2022,"placa":"%s",
                 "quilometragem":%d,"cor":"Prata","categoria":"Economico","valorDiaria":%s}
                """.formatted(placa, quilometragem, diaria);
        return id(mockMvc.perform(post("/api/v1/carros")
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private AluguelResponse registrarAluguel(UUID carroId, String cpf, LocalDateTime inicio,
                                             LocalDateTime prevista, Long kmInicial, String diaria,
                                             String observacoes) throws Exception {
        StringBuilder corpo = new StringBuilder()
                .append("{\"nome\":\"").append(NOME)
                .append("\",\"cpf\":\"").append(cpf)
                .append("\",\"email\":\"").append(EMAIL)
                .append("\",\"telefone\":\"").append(TELEFONE)
                .append("\",\"cnh\":\"").append(CNH)
                .append("\",\"carroId\":\"").append(carroId)
                .append("\",\"dataInicio\":\"").append(inicio)
                .append("\",\"dataPrevistaDevolucao\":\"").append(prevista).append("\"");
        if (kmInicial != null) {
            corpo.append(",\"quilometragemInicial\":").append(kmInicial);
        }
        if (diaria != null) {
            corpo.append(",\"valorDiaria\":").append(diaria);
        }
        if (observacoes != null) {
            corpo.append(",\"observacoes\":\"").append(observacoes).append("\"");
        }
        corpo.append("}");

        return ler(mockMvc.perform(post("/api/v1/alugueis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo.toString()))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private AluguelResponse ler(MvcResult resultado) throws Exception {
        return objectMapper.readValue(resultado.getResponse().getContentAsString(), AluguelResponse.class);
    }

    private UUID id(MvcResult resultado) throws Exception {
        return UUID.fromString(objectMapper.readTree(resultado.getResponse().getContentAsString())
                .get("id").asString());
    }
}
