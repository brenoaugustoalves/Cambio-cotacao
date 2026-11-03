package br.com.cambioclaro.app.economy.domain;

import java.math.BigDecimal;
import java.time.Instant;

/** A published exchange-rate reference and the metadata needed to qualify a simulation. */
public final class EconomicQuoteReference {
    public final BigDecimal rate;
    public final String source;
    public final Instant publishedAt;
    public final String warning;
    public EconomicQuoteReference(BigDecimal rate,String source,Instant publishedAt,String warning){
        if(rate==null||rate.signum()<=0||source==null||source.trim().isEmpty()||publishedAt==null)
            throw new IllegalArgumentException("Referência de câmbio inválida");
        this.rate=rate;this.source=source;this.publishedAt=publishedAt;this.warning=warning;
    }
}
