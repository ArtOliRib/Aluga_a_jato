package org.arthur.genericapi.model;

import org.arthur.genericapi.exception.RegraDeNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Regras de dominio do Aluguel")
class AluguelTest {

    private static final BigDecimal DIARIA = new BigDecimal("100.00");
    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 1, 10, 10, 0);
    private static final LocalDateTime PREVISTA = LocalDateTime.of(2026, 1, 13, 10, 0);

    /**
     * O aluguel guarda os dados da pessoa (ADR-0003). Todos chegam ja
     * protegidos pelo service: a entidade nao possui chave para cifrar nem para
     * gerar indice, entao aqui eles sao apenas strings opacas.
     */
    private Carro carro() {
        return new Carro("Uno", "Fiat", "Uno Vivace", 2022,
                "indice-placa", "placa-cifrada", 10_000L,
                "Prata", "Economico", DIARIA);
    }

    private Aluguel aluguel() {
        return new Aluguel("indice-nome", "nome-cifrado", "indice-cpf", "cifrado-cpf",
                "email-cifrado", "telefone-cifrado", "indice-cnh", "cifrado-cnh",
                carro(), INICIO, PREVISTA, 10_000L, DIARIA, null);
    }

    @Test
    @DisplayName("abre ATIVO, pendente de pagamento, com 3 diarias cobradas")
    void estadoInicial() {
        Aluguel aluguel = aluguel();

        assertThat(aluguel.getStatus()).isEqualTo(StatusAluguel.ATIVO);
        assertThat(aluguel.getStatusPagamento()).isEqualTo(StatusPagamento.PENDENTE);
        assertThat(aluguel.getTotalDiarias()).isEqualTo(3);
        assertThat(aluguel.getValorTotal()).isEqualByComparingTo("300.00");
    }

    @Test
    @DisplayName("cobra pelo menos 1 diaria em aluguel de mesmo dia")
    void diariaMinima() {
        Aluguel mesmoDia = new Aluguel("indice-nome", "nome-cifrado", "indice-cpf", "cifrado-cpf",
                "email-cifrado", "telefone-cifrado", "indice-cnh", "cifrado-cnh",
                carro(), INICIO, INICIO, 10_000L, DIARIA, null);

        assertThat(mesmoDia.getTotalDiarias()).isEqualTo(1);
        assertThat(mesmoDia.getValorTotal()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("recusa devolucao prevista antes do inicio")
    void previstaAntesDoInicio() {
        Carro carro = carro();

        assertThatThrownBy(() -> new Aluguel("indice-nome", "nome-cifrado", "indice-cpf", "cifrado-cpf",
                "email-cifrado", "telefone-cifrado", "indice-cnh", "cifrado-cnh",
                carro, INICIO, INICIO.minusDays(1), 10_000L, DIARIA, null))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("anterior a data de inicio");
    }

    @Test
    @DisplayName("recusa quilometragem inicial abaixo da do carro")
    void quilometragemInicialIncoerente() {
        Carro carro = carro();

        assertThatThrownBy(() -> new Aluguel("indice-nome", "nome-cifrado", "indice-cpf", "cifrado-cpf",
                "email-cifrado", "telefone-cifrado", "indice-cnh", "cifrado-cnh",
                carro, INICIO, PREVISTA, 9_000L, DIARIA, null))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("quilometragem atual do carro");
    }

    @Test
    @DisplayName("devolucao em atraso cobra as diarias reais")
    void devolucaoEmAtrasoRecalculaTotal() {
        Aluguel aluguel = aluguel();

        aluguel.registrarDevolucao(PREVISTA.plusDays(3), 10_300L);

        assertThat(aluguel.getStatus()).isEqualTo(StatusAluguel.CONCLUIDO);
        assertThat(aluguel.getTotalDiarias()).isEqualTo(6);
        assertThat(aluguel.getValorTotal()).isEqualByComparingTo("600.00");
        assertThat(aluguel.getQuilometragemPercorrida()).isEqualTo(300L);
    }

    @Test
    @DisplayName("recusa devolucao com quilometragem menor que a inicial")
    void devolucaoComKmMenor() {
        Aluguel aluguel = aluguel();

        assertThatThrownBy(() -> aluguel.registrarDevolucao(PREVISTA, 9_000L))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("menor que a inicial");
    }

    @Test
    @DisplayName("cancelamento zera o valor e nao permite pagamento")
    void cancelamentoZeraValor() {
        Aluguel aluguel = aluguel();

        aluguel.cancelar("cliente desistiu");

        assertThat(aluguel.getStatus()).isEqualTo(StatusAluguel.CANCELADO);
        assertThat(aluguel.getStatusPagamento()).isEqualTo(StatusPagamento.CANCELADO);
        assertThat(aluguel.getValorTotal()).isEqualByComparingTo("0.00");
        assertThatThrownBy(aluguel::registrarPagamento)
                .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    @DisplayName("aluguel encerrado e terminal: nao aceita segunda devolucao nem cancelamento")
    void encerradoEhTerminal() {
        Aluguel aluguel = aluguel();
        aluguel.registrarDevolucao(PREVISTA, 10_100L);

        assertThatThrownBy(() -> aluguel.registrarDevolucao(PREVISTA, 10_200L))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("CONCLUIDO");
        assertThatThrownBy(() -> aluguel.cancelar("tarde demais"))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    @DisplayName("pagamento so muda PENDENTE -> PAGO uma vez")
    void pagamentoUnico() {
        Aluguel aluguel = aluguel();
        aluguel.registrarDevolucao(PREVISTA, 10_100L);

        aluguel.registrarPagamento();
        assertThat(aluguel.getStatusPagamento()).isEqualTo(StatusPagamento.PAGO);

        assertThatThrownBy(aluguel::registrarPagamento)
                .isInstanceOf(RegraDeNegocioException.class);
    }
}