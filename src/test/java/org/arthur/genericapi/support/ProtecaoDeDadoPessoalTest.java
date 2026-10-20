package org.arthur.genericapi.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifica as garantias criptograficas de {@link ProtecaoDeDadoPessoal}.
 * Estes testes falham se alguem trocar o HMAC por hash simples ou remover a
 * cifra — que era exatamente o risco da discussao original.
 */
@DisplayName("Protecao de dado pessoal sob a LGPD")
class ProtecaoDeDadoPessoalTest {

    private static final String INDICE = chaveValida();
    private static final String CIFRA = chaveValida();
    private static final String CPF = "52998224725";

    private final ProtecaoDeDadoPessoal protecao = new ProtecaoDeDadoPessoal(INDICE, CIFRA);

    @Nested
    @DisplayName("indice (blind index)")
    class Indice {

        @Test
        @DisplayName("e deterministico: mesmo valor, mesmo indice")
        void deterministico() {
            assertThat(protecao.gerarIndice(CPF)).isEqualTo(protecao.gerarIndice(CPF));
        }

        @Test
        @DisplayName("e hex de 64 caracteres (HMAC-SHA-256)")
        void formato() {
            assertThat(protecao.gerarIndice(CPF))
                    .hasSize(64)
                    .matches("[0-9a-f]{64}");
        }

        @Test
        @DisplayName("diverge para valores diferentes e para chaves diferentes")
        void divergencia() {
            assertThat(protecao.gerarIndice(CPF)).isNotEqualTo(protecao.gerarIndice("11111111111"));
            assertThat(protecao.gerarIndice(CPF))
                    .isNotEqualTo(new ProtecaoDeDadoPessoal(chaveValida(29), CIFRA).gerarIndice(CPF));
        }

        @Test
        @DisplayName("nao contem o valor em claro")
        void naoExpoeOClear() {
            assertThat(protecao.gerarIndice(CPF)).doesNotContain(CPF);
        }
    }

    @Nested
    @DisplayName("cifra (AES-256-GCM)")
    class Cifra {

        @Test
        @DisplayName("ida e volta preserva o valor")
        void idaEVolta() {
            assertThat(protecao.decifrar(protecao.cifrar(CPF))).isEqualTo(CPF);
        }

        @Test
        @DisplayName("gera IV aleatorio: mesmo texto cifra diferente a cada vez")
        void ivAleatorio() {
            String a = protecao.cifrar(CPF);
            String b = protecao.cifrar(CPF);

            assertThat(a).isNotEqualTo(b);
            assertThat(protecao.decifrar(a)).isEqualTo(protecao.decifrar(b));
        }

        @Test
        @DisplayName("o indice continua estavel apesar da cifra nao-deterministica")
        void indiceEstavelComCifra() {
            String clienteA = protecao.cifrar(CPF);
            String clienteB = protecao.cifrar(CPF);

            assertThat(clienteA).isNotEqualTo(clienteB);
            assertThat(protecao.gerarIndice(CPF))
                    .isEqualTo(protecao.gerarIndice(protecao.decifrar(clienteB)));
        }

        @Test
        @DisplayName("recusa dado adulterado (tag GCM nao confere)")
        void adulteracao() {
            byte[] bruto = Base64.getDecoder().decode(protecao.cifrar(CPF));
            bruto[bruto.length - 1] ^= 0x01;
            String adulterado = Base64.getEncoder().encodeToString(bruto);

            assertThatThrownBy(() -> protecao.decifrar(adulterado))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Falha ao decifrar");
        }

        @Test
        @DisplayName("recusa texto cifrado com outra chave")
        void chaveErrada() {
            String cifradoEmOutraChave = new ProtecaoDeDadoPessoal(INDICE, chaveValida(29)).cifrar(CPF);

            assertThatThrownBy(() -> protecao.decifrar(cifradoEmOutraChave))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("configuracao das chaves")
    class Configuracao {

        @Test
        @DisplayName("falha na inicializacao se a chave nao for informada")
        void chaveAusente() {
            assertThatThrownBy(() -> new ProtecaoDeDadoPessoal(null, CIFRA))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("indice-chave");

            assertThatThrownBy(() -> new ProtecaoDeDadoPessoal(INDICE, "  "))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("cifra-chave");
        }

        @Test
        @DisplayName("falha se a chave for curta demais ou nao for Base64")
        void chaveInvalida() {
            assertThatThrownBy(() -> new ProtecaoDeDadoPessoal(
                    Base64.getEncoder().encodeToString(new byte[16]), CIFRA))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("32 bytes");

            assertThatThrownBy(() -> new ProtecaoDeDadoPessoal("nao-e-base64!!", CIFRA))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Base64");
        }

        @Test
        @DisplayName("exige exatamente 32 bytes na chave de cifra")
        void tamanhoExatoDaCifra() {
            String longa = Base64.getEncoder().encodeToString(new byte[64]);

            assertThatThrownBy(() -> new ProtecaoDeDadoPessoal(INDICE, longa))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("cifra-chave");
        }
    }

    private static String chaveValida() {
        return chaveValida(11);
    }

    /** Semente distinta produz uma chave distinta; a mesma semente, a mesma chave. */
    private static String chaveValida(int semente) {
        byte[] bytes = new byte[32];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) (i * 7 + semente);
        }
        return Base64.getEncoder().encodeToString(bytes);
    }
}