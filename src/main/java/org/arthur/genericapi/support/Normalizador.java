package org.arthur.genericapi.support;

import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * Normalizacao de identificadores, usada tanto na validacao de unicidade
 * (services) quanto na geracao do indice (cofre). Um unico ponto de verdade
 * evita que placa/cpf/cnh/nome sejam comparados de formas diferentes.
 */
public final class Normalizador {

    private static final Pattern MARCA_NAO_CODIFICAVEL = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    private Normalizador() {
    }

    /** Remove pontuacao e espaco, mantendo apenas letras e digitos em maiusculo. */
    public static String documento(String valor) {
        if (valor == null) {
            return null;
        }
        return valor.replaceAll("[^0-9A-Za-z]", "").toUpperCase();
    }

    /**
     * Chave de comparacao de nome: sem acentos, minusculo, espacos colapsados.
     * "Joao Lima", "joao  lima" e "JOÃO LIMA" precisam cair no mesmo indice
     * para que a busca por nome funcione como o usuario espera.
     */
    public static String nome(String valor) {
        if (valor == null) {
            return null;
        }
        String decomposto = Normalizer.normalize(valor, Normalizer.Form.NFD);
        String semAcento = MARCA_NAO_CODIFICAVEL.matcher(decomposto).replaceAll("");
        return semAcento.trim().replaceAll("\\s+", " ").toLowerCase();
    }
}