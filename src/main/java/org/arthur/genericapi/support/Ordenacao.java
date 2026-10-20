package org.arthur.genericapi.support;

import org.arthur.genericapi.exception.RegraDeNegocioException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.Arrays;

/**
 * Paginação e ordenação com lista fechada de campos.
 *
 * <p>Existe por causa de dois problemas que a triplicacao original carregava em
 * cada controller:
 *
 * <ol>
 *   <li><b>Campo livre do usuario virando coluna.</b> Sem lista fechada,
 *       {@code ?ordenarPor=xyz} estourava {@code PropertyReferenceException}
 *       e caia no handler generico: HTTP 500 em vez de 400. E
 *       {@code ?ordenarPor=cpf_cifrado} respondia 200, confirmando a existencia
 *       da coluna.</li>
 *   <li><b>Campo cifrado ordenavel.</b> Ordenar por material cifrado nao produz
 *       ordem nenhuma — so adiciona uma via de oracle sobre o schema. Por isso
 *       {@code nome}, {@code placa} e {@code observacoes} ficam fora das listas.</li>
 * </ol>
 */
public final class Ordenacao {

    public static final int TAMANHO_MAXIMO_PAGINA = 100;

    private Ordenacao() {
    }

    /** Pagina sem ordenacao explicita, pelo id. */
    public static PageRequest page(int pagina, int tamanho) {
        return page(pagina, tamanho, "id", "asc", "id");
    }

    public static PageRequest page(int pagina, int tamanho, String campo, String direcao,
                                   String... camposPermitidos) {
        if (!Arrays.asList(camposPermitidos).contains(campo)) {
            throw new RegraDeNegocioException(
                    "Ordenacao invalida. Campos permitidos: " + String.join(", ", camposPermitidos));
        }
        Sort.Direction sentido = "desc".equalsIgnoreCase(direcao) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return PageRequest.of(Math.max(pagina, 0), tamanhoSeguro(tamanho))
                .withSort(Sort.by(sentido, campo));
    }

    private static int tamanhoSeguro(int tamanho) {
        return Math.min(Math.max(tamanho, 1), TAMANHO_MAXIMO_PAGINA);
    }
}