package br.com.cambioclaro.app.conversion.presentation;

import java.math.BigDecimal;

/** Text and value ready for binding by an Android view. */
public final class ConversionDisplay {
    public final BigDecimal amountInReais;
    public final String note;

    ConversionDisplay(BigDecimal amountInReais, String note) {
        if (note == null) throw new IllegalArgumentException("Mensagem de conversão obrigatória");
        this.amountInReais = amountInReais;
        this.note = note;
    }

    public boolean isAvailable() {
        return amountInReais != null;
    }
}
