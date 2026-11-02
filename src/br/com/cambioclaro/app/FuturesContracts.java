package br.com.cambioclaro.app;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Contract dates transcribed from B3's 2026–2027 financial maturity calendar.
 * UDN is the last trading day. No dates are inferred beyond the published calendar.
 * https://www.b3.com.br/pt_br/solucoes/plataformas/puma-trading-system/para-participantes-e-traders/calendario-de-negociacao/vencimentos/calendario-de-vencimentos-de-contratos-financeiros/
 */
public final class FuturesContracts {
    public static final String CALENDAR_URL="https://www.b3.com.br/pt_br/solucoes/plataformas/puma-trading-system/para-participantes-e-traders/calendario-de-negociacao/vencimentos/calendario-de-vencimentos-de-contratos-financeiros/";
    private static final char[] MONTH_CODES={'F','G','H','J','K','M','N','Q','U','V','X','Z'};
    public final String base,symbol;
    public final YearMonth expiry;
    public final LocalDate lastTrade;
    private FuturesContracts(String base,YearMonth expiry,LocalDate lastTrade){
        this.base=base;this.expiry=expiry;this.lastTrade=lastTrade;
        this.symbol=base+MONTH_CODES[expiry.getMonthValue()-1]+String.format(java.util.Locale.ROOT,"%02d",expiry.getYear()%100);
    }
    private static final List<FuturesContracts> ALL;
    static {
        List<FuturesContracts> all=new ArrayList<>();
        // B3 UDN: WIN expires in even months only.
        String[] win26={"2026-02-18","2026-04-15","2026-06-17","2026-08-12","2026-10-14","2026-12-16"};
        String[] win27={"2027-02-17","2027-04-14","2027-06-16","2027-08-18","2027-10-13","2027-12-15"};
        for(int i=0;i<6;i++){all.add(new FuturesContracts("WIN",YearMonth.of(2026,2+i*2),LocalDate.parse(win26[i])));all.add(new FuturesContracts("WIN",YearMonth.of(2027,2+i*2),LocalDate.parse(win27[i])));}
        // B3 UDN: the dollar futures stop trading before the first business day of the expiry month.
        String[] usd26={"2025-12-30","2026-01-30","2026-02-27","2026-03-31","2026-04-30","2026-05-29","2026-06-30","2026-07-31","2026-08-31","2026-09-30","2026-10-30","2026-11-30"};
        String[] usd27={"2026-12-30","2027-01-29","2027-02-26","2027-03-31","2027-04-30","2027-05-31","2027-06-30","2027-07-30","2027-08-31","2027-09-30","2027-10-29","2027-11-30"};
        for(String base:new String[]{"DOL","WDO"})for(int i=0;i<12;i++){
            all.add(new FuturesContracts(base,YearMonth.of(2026,i+1),LocalDate.parse(usd26[i])));
            all.add(new FuturesContracts(base,YearMonth.of(2027,i+1),LocalDate.parse(usd27[i])));
        }
        ALL=Collections.unmodifiableList(all);
    }
    public static List<FuturesContracts> available(String base,LocalDate day){
        if(!base.equals("WIN")&&!base.equals("DOL")&&!base.equals("WDO"))throw new IllegalArgumentException("Contrato desconhecido");
        List<FuturesContracts> result=new ArrayList<>();
        for(FuturesContracts c:ALL)if(c.base.equals(base)&&!day.isAfter(c.lastTrade))result.add(c);
        result.sort((a,b)->a.expiry.compareTo(b.expiry));return result;
    }
    public static FuturesContracts current(String base,LocalDate day){List<FuturesContracts> choices=available(base,day);return choices.isEmpty()?null:choices.get(0);}
    @Override public String toString(){return symbol+" · "+String.format(java.util.Locale.ROOT,"%02d/%d",expiry.getMonthValue(),expiry.getYear());}
}
