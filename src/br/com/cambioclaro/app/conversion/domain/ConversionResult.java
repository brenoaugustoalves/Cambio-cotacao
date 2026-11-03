package br.com.cambioclaro.app.conversion.domain;

import java.math.BigDecimal;

/** Explicit outcome; presentation code decides how each state is displayed. */
public final class ConversionResult {
    public enum Status { SUCCESS, AMOUNT_REQUIRED, INVALID_AMOUNT, INVALID_CURRENCY, INVALID_MODE, RATE_UNAVAILABLE }

    public final Status status;
    public final BigDecimal amountInReais;
    public final ConversionQuote quote;

    private ConversionResult(Status status, BigDecimal amountInReais, ConversionQuote quote) {
        if (status == null) throw new IllegalArgumentException("Estado de conversão obrigatório");
        if (status == Status.SUCCESS && (amountInReais == null || amountInReais.signum() < 0 || quote == null)) {
            throw new IllegalArgumentException("Resultado de conversão incompleto");
        }
        if (status != Status.SUCCESS && (amountInReais != null || quote != null)) {
            throw new IllegalArgumentException("Falha não pode conter resultado calculado");
        }
        this.status = status;
        this.amountInReais = amountInReais;
        this.quote = quote;
    }

    public static ConversionResult success(BigDecimal amountInReais, ConversionQuote quote) {
        return new ConversionResult(Status.SUCCESS, amountInReais, quote);
    }

    public static ConversionResult failure(Status status) {
        if (status == null || status == Status.SUCCESS) throw new IllegalArgumentException("Estado de falha inválido");
        return new ConversionResult(status, null, null);
    }
}
