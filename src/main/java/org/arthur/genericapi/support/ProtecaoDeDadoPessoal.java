package org.arthur.genericapi.support;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Cofre dos dados pessoais do controlador (LGPD, art. 5, I).
 *
 * <p>Cada dado e guardado em duas formas, porque um mecanismo so nao atende aos
 * dois requisitos do dominio:
 *
 * <ul>
 *   <li><b>Indice (blind index)</b>: HMAC-SHA-256 do valor normalizado,
 *       deterministico e com chave secreta. E o que permite checar unicidade e
 *       buscar sem guardar o valor em claro. Por ser keyed, um vazamento do
 *       banco nao permite forca bruta: sem a chave, o atacante nao consegue nem
 *       calcular o indice para comparar. Um hash comum (SHA-256 puro) seria
 *       inutil aqui — o espaco de um CPF tem ~10^9 combinacoes e cai em segundos
 *       por GPU, e um nome tem ainda menos.</li>
 *   <li><b>Cifra (AES-256-GCM)</b>: texto claro cifrado com IV aleatorio, para
 *       devolver o valor quando a API realmente precisa mostrar. O IV viaja
 *       junto do ciphertext, porque o GCM exige IV unico por cifragem.</li>
 * </ul>
 *
 * <p>O que <b>nao</b> pode ser protegido por aqui: dado que precisa ser
 * comparado ou agregado em faixa (datas, valores, quilometragem). Cifra nao
 * suporta range query nem ordenacao — por isso esses dados ficam <b>em claro</b>
 * no aluguel, ja que relatorios filtram e somam por eles. Ja a identidade da
 * pessoa no aluguel (nome, cpf, email, telefone, cnh) nao precisa de faixa: fica
 * protegida pela cifra (para devolver o valor) somada ao indice HMAC (para
 * igualdade/busca). A lei admite o indice em igualdade de protecao (art. 5,
 * XVIII e art. 13, § 4).
 *
 * <p>As chaves vem da configuracao e nao tem valor padrao: um default embutido
 * seria pior que nao ter protecao, porque o adversary conheceria a chave.
 * A aplicacao nao sobe sem elas.
 */
@Component
public class ProtecaoDeDadoPessoal {

    private static final String ALGORITMO_INDICE = "HmacSHA256";
    private static final String TRANSFORMACAO_CIFRA = "AES/GCM/NoPadding";
    private static final int TAMANHO_IV = 12;
    private static final int TAMANHO_TAG = 128;
    private static final int TAMANHO_CHAVE_CIFRA = 32;
    private static final int TAMANHO_MINIMO_CHAVE_INDICE = 32;
    private static final String COMANDO_GERACAO = "openssl rand -base64 32";

    private final SecretKeySpec chaveIndice;
    private final SecretKeySpec chaveCifra;
    private final SecureRandom aleatorio = new SecureRandom();

    public ProtecaoDeDadoPessoal(
            @Value("${locadora.dado-pessoal.indice-chave}") String indiceBase64,
            @Value("${locadora.dado-pessoal.cifra-chave}") String cifraBase64) {
        this.chaveIndice = new SecretKeySpec(
                exigir(indiceBase64, TAMANHO_MINIMO_CHAVE_INDICE, TAMANHO_MINIMO_CHAVE_INDICE,
                        "locadora.dado-pessoal.indice-chave"), ALGORITMO_INDICE);
        this.chaveCifra = new SecretKeySpec(
                exigir(cifraBase64, TAMANHO_CHAVE_CIFRA, TAMANHO_CHAVE_CIFRA,
                        "locadora.dado-pessoal.cifra-chave"), "AES");
    }

    /** Indice deterministico do valor ja normalizado. Mesmo valor, mesmo indice. */
    public String gerarIndice(String valorNormalizado) {
        try {
            Mac mac = Mac.getInstance(ALGORITMO_INDICE);
            mac.init(chaveIndice);
            return HexFormat.of().formatHex(
                    mac.doFinal(valorNormalizado.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Falha ao gerar o indice do dado pessoal", e);
        }
    }

    /** Cifra o valor. O IV aleatorio e prefixado ao resultado. */
    public String cifrar(String textoClaro) {
        byte[] iv = new byte[TAMANHO_IV];
        aleatorio.nextBytes(iv);
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMACAO_CIFRA);
            cipher.init(Cipher.ENCRYPT_MODE, chaveCifra, new GCMParameterSpec(TAMANHO_TAG, iv));
            byte[] cifrado = cipher.doFinal(textoClaro.getBytes(StandardCharsets.UTF_8));

            byte[] pacote = new byte[iv.length + cifrado.length];
            System.arraycopy(iv, 0, pacote, 0, iv.length);
            System.arraycopy(cifrado, 0, pacote, iv.length, cifrado.length);
            return Base64.getEncoder().encodeToString(pacote);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Falha ao cifrar o dado pessoal", e);
        }
    }

    public String decifrar(String textoCifrado) {
        byte[] pacote = Base64.getDecoder().decode(textoCifrado);
        byte[] iv = new byte[TAMANHO_IV];
        System.arraycopy(pacote, 0, iv, 0, TAMANHO_IV);
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMACAO_CIFRA);
            cipher.init(Cipher.DECRYPT_MODE, chaveCifra, new GCMParameterSpec(TAMANHO_TAG, iv));
            return new String(cipher.doFinal(pacote, TAMANHO_IV, pacote.length - TAMANHO_IV),
                    StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(
                    "Falha ao decifrar o dado pessoal: chave ausente/divergente ou dado adulterado", e);
        }
    }

    private static byte[] exigir(String valorBase64, int tamanhoMinimo, int tamanhoExato,
                                 String propriedade) {
        if (valorBase64 == null || valorBase64.isBlank()) {
            throw new IllegalStateException("Propriedade obrigatoria ausente: " + propriedade
                    + " (gere com: " + COMANDO_GERACAO + ")");
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(valorBase64.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(propriedade + " deve ser Base64 valido", e);
        }
        if (bytes.length < tamanhoMinimo || (tamanhoExato > 0 && bytes.length != tamanhoExato)) {
            throw new IllegalStateException(propriedade + " deve ter " + tamanhoExato + " bytes ("
                    + (tamanhoExato * 8) + " bits); gere com: " + COMANDO_GERACAO);
        }
        return bytes;
    }
}