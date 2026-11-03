package br.com.cambioclaro.app.market.presentation;

import br.com.cambioclaro.app.FxCurrency;
import br.com.cambioclaro.app.Rate;
import br.com.cambioclaro.app.market.domain.MarketObservation;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/** Presents the currency overview using market observations first, then clearly labeled PTAX. */
public final class CurrencyOverviewPresenter {
    private static final Locale BR=new Locale("pt","BR");
    private static final DateTimeFormatter DAY=DateTimeFormatter.ofPattern("dd/MM/uuuu",BR);
    private final MarketMovementPresenter movement=new MarketMovementPresenter();
    public CurrencyOverviewDisplay present(FxCurrency currency,MarketObservation marketQuote,
            List<MarketObservation> marketToday,List<Rate> ptax,boolean sourceFailure,
            LocalDate today,ZoneId zone){
        if(currency==null||marketToday==null||ptax==null||today==null||zone==null)throw new IllegalArgumentException("Dados do resumo de moedas inválidos");
        if(marketQuote!=null){
            String price=number(marketQuote.ask,4)+" por "+currency.name()+" 1";
            if(!marketQuote.publishedAt.atZone(zone).toLocalDate().equals(today))return new CurrencyOverviewDisplay(price,"Mercado · última publicação "+DateTimeFormatter.ofPattern("dd/MM HH:mm",BR).format(marketQuote.publishedAt.atZone(zone))+" · sem observação de hoje",2);
            if(marketToday.size()<2)return new CurrencyOverviewDisplay(price,"Mercado · "+DateTimeFormatter.ofPattern("dd/MM HH:mm",BR).format(marketQuote.publishedAt.atZone(zone))+" · variação indisponível: aguardando outra publicação",0);
            int latest=marketToday.size()-1,direction=movement.direction(marketToday,latest);
            return new CurrencyOverviewDisplay(price,"Mercado · "+movement.summary(marketToday,latest,false,zone)+" · publicação "+DateTimeFormatter.ofPattern("dd/MM HH:mm",BR).format(marketQuote.publishedAt.atZone(zone)),direction);
        }
        if(ptax.isEmpty())return new CurrencyOverviewDisplay(sourceFailure?"Fonte indisponível • tente atualizar":"Aguardando dados do Banco Central","Sem cotação válida para comparar",0);
        Rate latest=ptax.get(ptax.size()-1);String detail="PTAX · publicado em "+DAY.format(latest.at.toLocalDate());
        String price=number(latest.sell,4)+" por "+currency.name()+" 1";
        if(ptax.size()<2)return new CurrencyOverviewDisplay(price,detail+" · sem publicação anterior",0);
        BigDecimal change=Rate.variation(ptax.get(ptax.size()-2).sell,latest.sell);int direction=change.setScale(2,RoundingMode.HALF_UP).signum();
        String arrow=direction>0?"▲ Alta ":direction<0?"▼ Baixa ":"● Estável ";
        return new CurrencyOverviewDisplay(price,arrow+percent(change)+" · "+detail+(sourceFailure?"\nAtualização falhou • dado salvo":""),direction);
    }
    private static String number(BigDecimal value,int digits){NumberFormat f=NumberFormat.getNumberInstance(BR);f.setMinimumFractionDigits(digits);f.setMaximumFractionDigits(digits);return f.format(value);}
    private static String percent(BigDecimal value){NumberFormat f=NumberFormat.getNumberInstance(BR);f.setMinimumFractionDigits(2);f.setMaximumFractionDigits(2);return f.format(value)+"%";}
}
