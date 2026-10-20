package org.arthur.genericapi.support;

import org.arthur.genericapi.exception.RegraDeNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifica a garantia central de {@link CampoProtegido}: o indice e sempre
 * calculado sobre o valor normalizado do papel certo, e ninguem consegue montar
 * um par indice+cifra incoerente.
 */
@DisplayName("Campo protegido")
class CampoProtegidoTest {

    private static final String INDICE = chaveValida(11);
    private static final String CIFRA = chaveValida(13);

    private final ProtecaoDeDadoPessoal cofre = new ProtecaoDeDadoPessoal(INDICE, CIFRA);

    @Nested
    @DisplayName("papel define a normalizacao do indice")
    class Papel {

        @Test
        @DisplayName("nome ignora acento e caixa: 'Joao Lima' e 'JOÃO LIMA' geram o mesmo indice")
        void nomeNormaliza() {
            String a = CampoProtegido.deNome("Joao Lima", cofre).indice();
            String b = CampoProtegido.deNome("JOÃO  LIMA", cofre).indice();

            assertThat(a).isEqualTo(b);
        }

        @Test
        @DisplayName("documento ignora pontuacao: '529.982.247-25' e '52998224725' geram o mesmo indice")
        void documentoNormaliza() {
            String a = CampoProtegido.deDocumento("529.982.247-25", cofre).indice();
            String b = CampoProtegido.deDocumento("52998224725", cofre).indice();

            assertThat(a).isEqualTo(b);
        }

        @Test
        @DisplayName("trocar o papel muda o indice: era o bug silencioso que o tipo elimina")
        void papelImporta() {
            String comoNome = CampoProtegido.deNome("ABC1234", cofre).indice();
            String comoDocumento = CampoProtegido.deDocumento("ABC1234", cofre).indice();

            assertThat(comoNome).isNotEqualTo(comoDocumento);
        }

        @Test
        @DisplayName("o indice bate com o do cofre sobre o valor normalizado")
        void coerenteComOCofre() {
            CampoProtegido campo = CampoProtegido.deDocumento("529.982.247-25", cofre);

            assertThat(campo.indice()).isEqualTo(cofre.gerarIndice("52998224725"));
        }

        @Test
        @DisplayName("a cifra preserva o valor original, com pontuacao")
        void cifraPreservaOriginal() {
            CampoProtegido campo = CampoProtegido.deDocumento("529.982.247-25", cofre);

            assertThat(campo.paraExibir(cofre)).isEqualTo("529.982.247-25");
        }
    }

    @Nested
    @DisplayName("busca que nao grava")
    class Busca {

        @Test
        @DisplayName("indiceDeDocumento normaliza e indexa numa chamada")
        void indiceDeDocumento() {
            assertThat(CampoProtegido.indiceDeDocumento("ABC-1234", cofre))
                    .isEqualTo(cofre.gerarIndice("ABC1234"));
        }

        @Test
        @DisplayName("indiceDeNome casa com o indice gravado de um nome")
        void indiceDeNome() {
            String gravado = CampoProtegido.deNome("João Lima", cofre).indice();

            assertThat(CampoProtegido.indiceDeNome("joao  lima", cofre)).isEqualTo(gravado);
        }
    }

    @Nested
    @DisplayName("nulo: recusa na gravacao, tolera na leitura")
    class Nulo {

        @Test
        @DisplayName("nome, documento e texto obrigatorios recusam nulo e em branco")
        void obrigatoriosRecusam() {
            assertThatThrownBy(() -> CampoProtegido.deNome(null, cofre))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("nome");
            assertThatThrownBy(() -> CampoProtegido.deDocumento("  ", cofre))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("documento");
            assertThatThrownBy(() -> CampoProtegido.deTexto(null, cofre))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("texto");
        }

        @Test
        @DisplayName("texto opcional aceita nulo e devolve nulo em tudo")
        void opcionalAceitaNulo() {
            CampoProtegido campo = CampoProtegido.deTextoOpcional(null, cofre);

            assertThat(campo.indice()).isNull();
            assertThat(campo.cifrado()).isNull();
            assertThat(campo.paraExibir(cofre)).isNull();
        }

        @Test
        @DisplayName("texto opcional em branco tambem e tratado como ausente")
        void opcionalEmBranco() {
            assertThat(CampoProtegido.deTextoOpcional("   ", cofre).cifrado()).isNull();
        }

        @Test
        @DisplayName("deCifrado decifra o valor vindo do banco")
        void decifraOVindoDoBanco() {
            String cifrado = cofre.cifrar("52998224725");

            assertThat(CampoProtegido.deCifrado(cifrado, cofre).paraExibir(cofre))
                    .isEqualTo("52998224725");
        }

        @Test
        @DisplayName("deCifrado com nulo nao explode")
        void decifradoNulo() {
            assertThat(CampoProtegido.deCifrado(null, cofre).paraExibir(cofre)).isNull();
        }
    }

    @Nested
    @DisplayName("o valor em claro nao vaza")
    class Sigilo {

        @Test
        @DisplayName("toString nao expoe o valor nem o indice nem a cifra")
        void toStringNaoVaza() {
            CampoProtegido campo = CampoProtegido.deDocumento("52998224725", cofre);
            String texto = campo.toString();

            assertThat(texto).doesNotContain("52998224725");
            assertThat(texto).doesNotContain(campo.indice());
            assertThat(texto).doesNotContain(campo.cifrado());
        }
    }

    private static String chaveValida(int semente) {
        byte[] bytes = new byte[32];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) (i * 7 + semente);
        }
        return Base64.getEncoder().encodeToString(bytes);
    }
}
