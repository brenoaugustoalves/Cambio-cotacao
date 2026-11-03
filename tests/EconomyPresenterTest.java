import br.com.cambioclaro.app.economy.presentation.EconomyDisplay;
import br.com.cambioclaro.app.economy.presentation.EconomyPresenter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class EconomyPresenterTest {
    private static int checks;
    private static void check(boolean condition,String label){if(!condition)throw new AssertionError(label);checks++;}
    public static void main(String[] args){
        EconomyPresenter presenter=new EconomyPresenter();long now=1_800_000_000_000L;LocalDate date=LocalDate.of(2026,10,30);ZoneId zone=ZoneId.of("America/Sao_Paulo");
        Map<String,EconomyPresenter.Expectation> expectations=new LinkedHashMap<>();
        expectations.put("ipca",new EconomyPresenter.Expectation(new BigDecimal("4.123"),"% a.a.","ano 2026",date,120));
        expectations.put("selic",new EconomyPresenter.Expectation(new BigDecimal("12.5"),"% a.a.","ano 2026",date,115));
        expectations.put("cambio",new EconomyPresenter.Expectation(new BigDecimal("5.43963"),"R$ por US$","ano 2026",date,100));
        expectations.put("pib",new EconomyPresenter.Expectation(new BigDecimal("1.236"),"% a.a.","ano 2026",date,98));
        EconomyDisplay full=presenter.present(new EconomyPresenter.Metric(new BigDecimal("15.00"),date),new EconomyPresenter.Metric(new BigDecimal("0.24"),date),expectations,now-60_000,now,null,zone);
        check(full.selic.contains("15,00% a.a.")&&full.selic.contains("SGS 432"),"Observed Selic shows Brazilian formatting, unit, and source");
        check(full.ipca.contains("0,24% no mês")&&full.ipca.contains("competência 30/10/2026"),"Observed IPCA retains monthly unit and reference date");
        check(full.focusIpca.contains("4,12 % a.a.")&&full.focusIpca.contains("ano 2026")&&full.focusIpca.contains("120 instituições"),"Focus presentation preserves rounding, horizon, and respondent count");
        check(full.focusFx.contains("5,4396")&&full.focusFx.contains("R$ por US$"),"Exchange expectation uses four decimal places");
        check(full.status.contains("Fontes consultadas em")&&full.status.contains("Brasília"),"Fetch timestamp is converted to Brasília time");
        EconomyDisplay missing=presenter.present(null,null,Collections.emptyMap(),0,now,null,zone);
        check(missing.selic.contains("Indisponível")&&missing.focusPib.contains("indisponível")&&missing.status.contains("Sem dados salvos"),"Missing values stay explicit and are never filled with estimates");
        EconomyDisplay partial=presenter.present(null,null,Collections.emptyMap(),0,now,"SGS IPCA indisponível",zone);
        check(partial.status.contains("nenhum valor foi inventado"),"Unavailable sources produce a truthful no-cache status");
        EconomyDisplay stale=presenter.present(new EconomyPresenter.Metric(BigDecimal.ONE,date),null,Collections.emptyMap(),now+1,now,null,zone);
        check(stale.status.contains("Horário da consulta indisponível"),"Future cache time is not presented as a current fetch time");
        Map<String,EconomyPresenter.Expectation> malformed=new LinkedHashMap<>();malformed.put("ipca",new EconomyPresenter.Expectation(BigDecimal.ONE,"%","ano 2026",date,0));
        check(presenter.present(null,null,malformed,now,now,null,zone).focusIpca.contains("indisponível"),"Expectation with no respondents is not presented as valid");
        boolean nullMap=false;try{presenter.present(null,null,null,now,now,null,zone);}catch(IllegalArgumentException expected){nullMap=true;}
        check(nullMap,"Missing presentation data is rejected");
        System.out.println(checks+" economy presentation checks passed");
    }
}
