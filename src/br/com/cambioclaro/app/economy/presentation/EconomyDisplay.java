package br.com.cambioclaro.app.economy.presentation;

/** Immutable text model for the observed indicators and Focus expectations section. */
public final class EconomyDisplay {
    public final String selic;
    public final String ipca;
    public final String focusIpca;
    public final String focusSelic;
    public final String focusFx;
    public final String focusPib;
    public final String status;

    EconomyDisplay(String selic, String ipca, String focusIpca, String focusSelic,
            String focusFx, String focusPib, String status) {
        this.selic=selic; this.ipca=ipca; this.focusIpca=focusIpca; this.focusSelic=focusSelic;
        this.focusFx=focusFx; this.focusPib=focusPib; this.status=status;
    }
}
