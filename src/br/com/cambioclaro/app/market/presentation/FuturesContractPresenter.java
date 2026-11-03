package br.com.cambioclaro.app.market.presentation;

import br.com.cambioclaro.app.market.domain.FuturesContracts;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Presents only calendar fields supplied by B3; no quote feed is currently connected. */
public final class FuturesContractPresenter {
    private static final Locale BR=new Locale("pt","BR");
    private static final DateTimeFormatter DATE=DateTimeFormatter.ofPattern("dd/MM/uuuu",BR);

    public FuturesContractDisplay present(FuturesContracts contract){
        if(contract==null)throw new IllegalArgumentException("Contrato futuro ausente");
        String expiry=String.format(BR,"%02d/%d",contract.expiry.getMonthValue(),contract.expiry.getYear());
        String date=DATE.format(contract.lastTrade);
        return new FuturesContractDisplay(contract.symbol,
                "Vencimento: "+expiry+" · último dia de negociação (B3): "+date+
                "\nÚltima cotação, variação, horário e atraso indisponíveis · provedor de futuros não integrado");
    }
}
