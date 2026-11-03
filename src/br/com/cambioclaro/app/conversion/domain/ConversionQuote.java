package br.com.cambioclaro.app.conversion.domain;

import java.math.BigDecimal;
import java.time.Instant;

/** A conversion reference that preserves its source and publication time. */
public final class ConversionQuote {
    public enum Source { MARKET, TOURISM, PTAX }

    public final BigDecimal reaisPerUnit;
    public final Instant publishedAt;
    public final Source source;

    public ConversionQuote(BigDecimal reaisPerUnit, Instant publishedAt, Source source) {
        if (reaisPerUnit == null || reaisPerUnit.signum() <= 0 || publishedAt == null || source == null) {
            throw new IllegalArgumentException("Referência de conversão inválida");
        }
        this.reaisPerUnit = reaisPerUnit;
        this.publishedAt = publishedAt;
        this.source = source;
    }
}
