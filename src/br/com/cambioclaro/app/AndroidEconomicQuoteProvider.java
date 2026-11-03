package br.com.cambioclaro.app;

import android.content.Context;
import br.com.cambioclaro.app.economy.application.EconomicQuoteProvider;
import br.com.cambioclaro.app.economy.domain.EconomicQuoteReference;
import br.com.cambioclaro.app.market.application.StoreMarketBatch;
import br.com.cambioclaro.app.market.application.StorePtaxRates;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Android adapter selecting a real market or PTAX quote and retaining its source timestamp. */
public final class AndroidEconomicQuoteProvider implements EconomicQuoteProvider {
    private final Context context;
    private final StoreMarketBatch marketBatch;
    private final StorePtaxRates ptaxStore;
    public AndroidEconomicQuoteProvider(Context context){
        if(context==null)throw new IllegalArgumentException("Contexto obrigatório");
        this.context=context.getApplicationContext();
        marketBatch=AndroidMarketRefreshGateway.createBatch(this.context);
        ptaxStore=AndroidPtaxGateway.createStore(this.context);
    }
    @Override public EconomicQuoteReference get(FxCurrency currency,boolean marketQuote,Instant now)throws Exception{
        if(currency==null||now==null)throw new IllegalArgumentException("Moeda e instante são obrigatórios");
        return marketQuote?market(currency,now):ptax(currency,now);
    }
    private EconomicQuoteReference market(FxCurrency currency,Instant now)throws Exception{
        String warning=null;
        try{marketBatch.execute();}catch(Exception failed){Diagnostics.record(context,"simulador / mercado "+currency.name(),failed);warning="Consulta atual falhou; usando a última cotação salva, se ainda disponível.";}
        MarketClient.Quote quote=MarketStore.latest(context,currency);
        if(quote==null)throw new IOException("Sem cotação de mercado válida em cache ou na fonte.");
        if(quote.at.isBefore(now.minusSeconds(6*60*60L)))warning=append(warning,"Referência antiga; confira o horário antes de usar.");
        return new EconomicQuoteReference(quote.ask,"AwesomeAPI · venda (ask)",quote.at,warning);
    }
    private EconomicQuoteReference ptax(FxCurrency currency,Instant now)throws Exception{
        String warning=null;
        try{ptaxStore.execute(currency,BcbClient.today());}catch(Exception failed){Diagnostics.record(context,"simulador / PTAX "+currency.name(),failed);warning="Consulta atual falhou; usando a última PTAX salva, se ainda disponível.";}
        List<Rate> rates=RateStore.read(context,currency);
        if(rates.isEmpty())throw new IOException("Sem PTAX publicada disponível.");
        Rate latest=rates.get(rates.size()-1);Instant published=latest.at.atZone(BcbClient.ZONE).toInstant();
        if(latest.at.toLocalDate().isBefore(BcbClient.today().minusDays(3)))warning=append(warning,"Última publicação antiga; confira a data.");
        return new EconomicQuoteReference(latest.sell,"Banco Central · PTAX venda",published,warning);
    }
    private static String append(String existing,String extra){return existing==null?extra:existing+" "+extra;}
}
