package org.arthur.genericapi.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.arthur.genericapi.exception.RegraDeNegocioException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Regras de dominio da Carro. A placa ja entra cifrada e indexada, porque quem
 * produz esses valores e o {@code CarroService} — a entidade nao tem chave.
 */
@DisplayName("Regras de dominio da Carro")
class CarroTest {

    private static final BigDecimal DIARIA = new BigDecimal("150.00");

    private Carro carro() {
        return new Carro("Uno", "Fiat", "Uno Vivace", 2022,
                "indice-placa", "placa-cifrada", 10_000L,
                "Prata", "Economico", DIARIA);
    }

    @Test
    @DisplayName("nasce disponivel e guarda placa somente cifrada")
    void estadoInicial() {
        Carro carro = carro();

        assertThat(carro.getStatus()).isEqualTo(StatusCarro.DISPONIVEL);
        assertThat(carro.getStatus().estaDisponivelParaAluguel()).isTrue();
        assertThat(carro.getPlacaCifrada()).isEqualTo("placa-cifrada");
        assertThat(carro.getPlacaIndice()).isEqualTo("indice-placa");
    }

    @Test
    @DisplayName("recusa quilometragem menor que a atual")
    void quilometragemNaoRetrocede() {
        Carro carro = carro();

        assertThatThrownBy(() -> carro.registrarQuilometragem(9_000L))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("menor que a atual");

        assertThat(carro.getQuilometragem()).isEqualTo(10_000L);
    }

    @Nested
    @DisplayName("transicoes de status")
    class Transicoes {

        @Test
        @DisplayName("permite o caminho DISPONIVEL -> ALUGADO -> DISPONIVEL")
        void cicloNormalDeAluguel() {
            Carro carro = carro();

            carro.alugar();
            assertThat(carro.getStatus()).isEqualTo(StatusCarro.ALUGADO);

            carro.liberar();
            assertThat(carro.getStatus()).isEqualTo(StatusCarro.DISPONIVEL);
        }

        @Test
        @DisplayName("bloqueia voltar um carro desativado")
        void desativadoEhTerminal() {
            Carro carro = carro();
            carro.desativar();

            assertThatThrownBy(carro::liberar)
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("DESATIVADO");
        }

        @Test
        @DisplayName("bloqueia desativar direto a partir de ALUGADO")
        void desativadoExigeDesaluguelAntes() {
            Carro carro = carro();
            carro.alugar();

            assertThatThrownBy(carro::desativar)
                    .isInstanceOf(RegraDeNegocioException.class);
        }
    }
}