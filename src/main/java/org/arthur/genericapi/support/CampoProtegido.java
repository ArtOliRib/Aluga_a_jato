package org.arthur.genericapi.support;

import org.arthur.genericapi.exception.RegraDeNegocioException;

import java.util.Objects;

/**
 * Um dado pessoal pronto para ser guardado (ou buscado) sob a LGPD.
 *
 * <p><b>O problema que este tipo resolve.</b> Guardar um dado pessoal exige tres
 * passos em sequencia — normalizar, indexar (HMAC, para busca) e cifrar (AES-GCM,
 * para exibicao) — e o normalizador certo depende do <b>papel</b> do dado: nome
 * colapsa acento e caixa, documento remove pontuacao. Antes deste tipo, cada
 * service escrevia os tres passos a mao:
 *
 * <pre>{@code
 * protecao.gerarIndice(Normalizador.nome(request.nome())),
 * protecao.cifrar(request.nome()),
 * }</pre>
 *
 * <p>Esquecer o normalizador, ou trocar {@code nome} por {@code documento},
 * <b>compila e passa em qualquer teste de comportamento</b>: o par gravado parece
 * valido. O defeito so aparece depois, na consulta, quando a busca nao encontra o
 * registro — porque o indice foi calculado sobre um valor que nunca mais sera
 * reproduzido. Aqui os tres passos nascem juntos e o papel escolhe a normalizacao
 * dentro do tipo, entao esse pareamento errado deixa de ser expressavel.
 *
 * <p><b>Uso.</b> Gravar nome, CPF e CNH de um aluguel:
 *
 * <pre>{@code
 * CampoProtegido nome = CampoProtegido.deNome(request.nome(), protecao);
 * CampoProtegido cpf = CampoProtegido.deDocumento(request.cpf(), protecao);
 *
 * new Aluguel(nome.indice(), nome.cifrado(), cpf.indice(), cpf.cifrado(), ...)
 * }</pre>
 *
 * <p>Buscar por placa sem gravar nada:
 *
 * <pre>{@code carroRepository.findByPlacaIndice(CampoProtegido.indiceDeDocumento(placa, protecao))}</pre>
 *
 * <p><b>Nulo.</b> O sistema tem dois regimes, e este tipo os distingue em vez de
 * tratar igual:
 *
 * <ul>
 *   <li>{@link #deNome}, {@link #deDocumento} e {@link #deTexto} recusam nulo.
 *       Sao os dados obrigatorios ({@code nullable = false}): recusar aqui da um
 *       erro nomeado no ponto da chamada, em vez de gravar {@code null} e
 *       estourar a constraint do banco depois, com mensagem generica.</li>
 *   <li>{@link #deTextoOpcional} e {@link #deCifrado} aceitam nulo. Observacoes e
 *       motivo de cancelamento sao opcionais, e a leitura de um campo que nunca
 *       foi gravado tem que devolver nulo, nao explodir.</li>
 * </ul>
 *
 * <p><b>Escopo.</b> Este tipo cobre gravacao e o indice de busca. A <b>leitura</b>
 * de um valor ja cifrado que so precisa ser exibido continua chamando
 * {@code protecao.decifrar(...)} direto: ali nao ha normalizacao nem indice, e um
 * wrapper nao impediria o unico erro possivel, que e passar o campo errado
 * (ver ADR-0004).
 */
public final class CampoProtegido {

    private final String valorOriginal;
    private final String valorNormalizado;
    private final String indice;
    private final String cifrado;

    private CampoProtegido(String valorOriginal, String valorNormalizado, String indice, String cifrado) {
        this.valorOriginal = valorOriginal;
        this.valorNormalizado = valorNormalizado;
        this.indice = indice;
        this.cifrado = cifrado;
    }

    // ------------------------------------------------------------------
    // Fabricas de gravacao
    // ------------------------------------------------------------------

    /**
     * Nome de pessoa: normalizacao de nome (sem acento, minusculo, espacos
     * colapsados), com indice e cifra.
     *
     * @throws RegraDeNegocioException se {@code valor} for nulo ou em branco.
     */
    public static CampoProtegido deNome(String valor, ProtecaoDeDadoPessoal cofre) {
        return deTextoNormalizado(valor, Normalizador.nome(valor), cofre, "nome");
    }

    /**
     * Documento de identificacao: CPF, CNH ou placa. Normalizacao de documento
     * (so letras e digitos, maiusculo), com indice e cifra.
     *
     * @throws RegraDeNegocioException se {@code valor} for nulo ou em branco.
     */
    public static CampoProtegido deDocumento(String valor, ProtecaoDeDadoPessoal cofre) {
        return deTextoNormalizado(valor, Normalizador.documento(valor), cofre, "documento");
    }

    /**
     * Texto que nao passa por normalizacao: e-mail, telefone. Cifrado, mas sem
     * indice util, porque nao ha busca por ele.
     *
     * @throws RegraDeNegocioException se {@code valor} for nulo ou em branco.
     */
    public static CampoProtegido deTexto(String valor, ProtecaoDeDadoPessoal cofre) {
        exigir(valor, "texto");
        return new CampoProtegido(valor, valor, null, cofre.cifrar(valor));
    }

    /**
     * Texto opcional (observacoes, motivo de cancelamento): nulo continua nulo,
     * e um campo nao obrigatorio nao vira erro de negocio.
     */
    public static CampoProtegido deTextoOpcional(String valor, ProtecaoDeDadoPessoal cofre) {
        if (valor == null || valor.isBlank()) {
            return new CampoProtegido(null, null, null, null);
        }
        return deTexto(valor, cofre);
    }

    // ------------------------------------------------------------------
    // Fabricas de leitura e busca
    // ------------------------------------------------------------------

    /**
     * Um valor que ja esta cifrado no banco, para devolver em claro.
     * Tolerante a nulo: campo opcional nunca gravado devolve nulo.
     */
    public static CampoProtegido deCifrado(String valorCifrado, ProtecaoDeDadoPessoal cofre) {
        if (valorCifrado == null) {
            return new CampoProtegido(null, null, null, null);
        }
        return new CampoProtegido(null, null, null, valorCifrado);
    }

    /**
     * So o indice de um documento, para busca que nao grava nada. Substitui a
     * chamada manual {@code gerarIndice(Normalizador.documento(placa))}, que e
     * exatamente o pareamento onde o erro e mais facil de cometer.
     */
    public static String indiceDeDocumento(String valor, ProtecaoDeDadoPessoal cofre) {
        return deDocumento(valor, cofre).indice();
    }

    /** So o indice de um nome, para busca que nao grava nada. */
    public static String indiceDeNome(String valor, ProtecaoDeDadoPessoal cofre) {
        return deNome(valor, cofre).indice();
    }

    // ------------------------------------------------------------------
    // Conversoes
    // ------------------------------------------------------------------

    /** O indice HMAC do valor normalizado. Nulo se o campo era nulo. */
    public String indice() {
        return indice;
    }

    /** O valor cifrado, pronto para a coluna. Nulo se o campo era nulo. */
    public String cifrado() {
        return cifrado;
    }

    /**
     * O valor em claro, decifrando quando veio do banco. E o que a resposta da
     * API devolve. Nulo se o campo era nulo.
     */
    public String paraExibir(ProtecaoDeDadoPessoal cofre) {
        if (valorOriginal != null) {
            return valorOriginal;
        }
        return cifrado == null ? null : cofre.decifrar(cifrado);
    }

    /** O valor apos a normalizacao do papel. Nulo se o campo era nulo. */
    public String normalizado() {
        return valorNormalizado;
    }

    // ------------------------------------------------------------------

    private static CampoProtegido deTextoNormalizado(String valor, String normalizado,
                                                     ProtecaoDeDadoPessoal cofre, String papel) {
        exigir(valor, papel);
        return new CampoProtegido(valor, normalizado, cofre.gerarIndice(normalizado), cofre.cifrar(valor));
    }

    private static void exigir(String valor, String papel) {
        if (valor == null || valor.isBlank()) {
            throw new RegraDeNegocioException("O " + papel + " e obrigatorio");
        }
    }

    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        if (!(outro instanceof CampoProtegido campo)) {
            return false;
        }
        return Objects.equals(indice, campo.indice) && Objects.equals(cifrado, campo.cifrado);
    }

    @Override
    public int hashCode() {
        return Objects.hash(indice, cifrado);
    }

    /**
     * Nunca imprime o valor em claro nem o cifrado: um {@code toString} de campo
     * pessoal em log e vazamento (LGPD, art. 46).
     */
    @Override
    public String toString() {
        return "CampoProtegido[protegido]";
    }
}
