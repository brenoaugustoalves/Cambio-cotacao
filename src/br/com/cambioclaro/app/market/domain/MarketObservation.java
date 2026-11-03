package br.com.cambioclaro.app.market.domain;

import java.math.BigDecimal;
import java.time.Instant;

/** A provider-published market price; it contains no estimated or filled values. */
public final class MarketObservation {
    public final BigDecimal ask;
    public final Instant publishedAt;

    public MarketObservation(BigDecimal ask, Instant publishedAt) {
        if (ask == null || ask.signum() <= 0 || publishedAt == null) {
            throw new IllegalArgumentException("Observação de mercado inválida");
        }
        this.ask = ask;
        this.publishedAt = publishedAt;
    }
}
