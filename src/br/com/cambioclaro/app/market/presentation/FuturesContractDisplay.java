package br.com.cambioclaro.app.market.presentation;

/** Texts for one selected futures contract; quote state is never inferred. */
public final class FuturesContractDisplay {
    public final String contractCode;
    public final String details;

    public FuturesContractDisplay(String contractCode,String details){
        this.contractCode=contractCode;
        this.details=details;
    }
}
