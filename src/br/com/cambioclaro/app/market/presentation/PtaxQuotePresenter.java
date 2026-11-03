package br.com.cambioclaro.app.market.presentation;

import br.com.cambioclaro.app.Rate;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/** Presents official PTAX sale publications independently from continuous market quotes. */
public final class PtaxQuotePresenter {
    private static final Locale BR=new Locale("pt","BR");
    private static final DateTimeFormatter DATE_TIME=DateTimeFormatter.ofPattern("dd/MM/uuuu 'às' HH:mm",BR);
    public PtaxQuoteDisplay present(List<Rate> rates,LocalDate today,ZoneId zone){
        if(rates==null||today==null||zone==null)throw new IllegalArgumentException("Dados PTAX e fuso são obrigatórios");
        if(rates.isEmpty())return new PtaxQuoteDisplay("—","Sem cotação recebida da fonte","Variação indisponível",0);
        Rate latest=rates.get(rates.size()-1);String published="Publicado em "+DATE_TIME.format(latest.at.atZone(zone))+" (Brasília)"+(latest.at.toLocalDate().equals(today)?"":"\nSem publicação de hoje nesta consulta.");
        if(rates.size()<2)return new PtaxQuoteDisplay(number(latest.sell,4),published,"Sem publicação PTAX anterior para comparar",0);
        BigDecimal change=Rate.variation(rates.get(rates.size()-2).sell,latest.sell);int direction=change.signum();
        String delta=(direction>0?"▲ Alta ":direction<0?"▼ Baixa ":"● Estável ")+percent(change)+" vs. publicação PTAX anterior (Banco Central)";
        return new PtaxQuoteDisplay(number(latest.sell,4),published,delta,direction);
    }
    private static String number(BigDecimal amount,int digits){NumberFormat f=NumberFormat.getNumberInstance(BR);f.setMinimumFractionDigits(digits);f.setMaximumFractionDigits(digits);return f.format(amount);}
    private static String percent(BigDecimal change){NumberFormat f=NumberFormat.getNumberInstance(BR);f.setMinimumFractionDigits(2);f.setMaximumFractionDigits(2);return f.format(change)+"%";}
}
