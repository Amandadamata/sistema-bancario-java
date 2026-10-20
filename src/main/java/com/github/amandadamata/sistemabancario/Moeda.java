package com.github.amandadamata.sistemabancario;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Regras monetárias compartilhadas: valores em reais com exatamente duas casas decimais.
 *
 * <p>{@link BigDecimal} representa valores decimais exatamente, ao contrário de {@code double},
 * que usa frações binárias e acumula erros como {@code 0.1 + 0.2 = 0.30000000000000004}.
 */
public final class Moeda {

    /** Quantidade de casas decimais usada para centavos. */
    public static final int ESCALA = 2;

    /** Arredondamento bancário: em empate (x,xx5), arredonda para o centavo par. */
    public static final RoundingMode ARREDONDAMENTO = RoundingMode.HALF_EVEN;

    private static final Pattern FORMATO_ENTRADA = Pattern.compile("\\d+([.,]\\d{1,2})?");
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private Moeda() {
    }

    /**
     * Converte um texto digitado pelo usuário em valor monetário.
     *
     * <p>Aceita vírgula ou ponto como separador decimal ("100,50" ou "100.50") e até duas casas
     * decimais. Separadores de milhar não são aceitos porque tornariam "1.234" ambíguo.
     *
     * @throws IllegalArgumentException quando o texto não está em um formato aceito
     */
    public static BigDecimal converter(String texto) {
        String valor = texto == null ? "" : texto.trim();
        if (!FORMATO_ENTRADA.matcher(valor).matches()) {
            throw new IllegalArgumentException(
                    "Valor inválido. Use números com até duas casas decimais (ex: 100,50).");
        }
        return new BigDecimal(valor.replace(',', '.')).setScale(ESCALA);
    }

    /** Formata um valor no padrão brasileiro, por exemplo {@code R$ 1.234,56}. */
    public static String formatar(BigDecimal valor) {
        DecimalFormat formato = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(PT_BR));
        formato.setRoundingMode(ARREDONDAMENTO);
        return "R$ " + formato.format(Objects.requireNonNull(valor, "O valor é obrigatório."));
    }

    /**
     * Valida um valor monetário positivo com no máximo duas casas decimais.
     *
     * <p>Valores com mais casas são recusados em vez de arredondados silenciosamente: uma
     * operação de R$ 10,005 não deve virar R$ 10,00 ou R$ 10,01 sem que o usuário saiba.
     */
    static BigDecimal validarValorPositivo(BigDecimal valor, String mensagem) {
        if (valor == null || valor.signum() <= 0) {
            throw new IllegalArgumentException(mensagem);
        }
        return validarEscala(valor);
    }

    static BigDecimal validarValorNaoNegativo(BigDecimal valor, String mensagem) {
        if (valor == null || valor.signum() < 0) {
            throw new IllegalArgumentException(mensagem);
        }
        return validarEscala(valor);
    }

    private static BigDecimal validarEscala(BigDecimal valor) {
        BigDecimal semZerosExtras = valor.stripTrailingZeros();
        if (semZerosExtras.scale() > ESCALA) {
            throw new IllegalArgumentException("O valor deve ter no máximo duas casas decimais.");
        }
        return valor.setScale(ESCALA);
    }
}
