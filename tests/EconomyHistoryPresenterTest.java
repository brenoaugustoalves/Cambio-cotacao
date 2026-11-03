import br.com.cambioclaro.app.economy.presentation.EconomyHistoryPresenter;
import br.com.cambioclaro.app.economy.presentation.HistoryDisplay;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;

public final class EconomyHistoryPresenterTest {
    private static int checks;
    private static void check(boolean condition,String label){if(!condition)throw new AssertionError(label);checks++;}
    public static void main(String[] args){
        EconomyHistoryPresenter presenter=new EconomyHistoryPresenter();ZoneId zone=ZoneId.of("America/Sao_Paulo");long fetched=Instant.parse("2026-10-30T13:00:00Z").toEpochMilli(),now=fetched+1000;
        EconomyHistoryPresenter.Month january=new EconomyHistoryPresenter.Month(YearMonth.of(2026,1),new EconomyHistoryPresenter.Observation(LocalDate.of(2026,1,31),new BigDecimal("0.245")),new EconomyHistoryPresenter.Observation(LocalDate.of(2026,1,30),new BigDecimal("15.00")));
        EconomyHistoryPresenter.Month february=new EconomyHistoryPresenter.Month(YearMonth.of(2026,2),null,new EconomyHistoryPresenter.Observation(LocalDate.of(2026,2,27),new BigDecimal("14.75")));
        HistoryDisplay display=presenter.present(Arrays.asList(january,february),null,fetched,now,zone);
        check(display.months.size()==2&&display.months.get(0).title.equals("02/2026")&&display.months.get(1).title.equals("01/2026"),"Months are presented newest first");
        check(display.months.get(1).observations.contains("IPCA · 0,25% no mês · 31/01/2026"),"IPCA is rounded and keeps its monthly reference date");
        check(display.months.get(1).observations.contains("Meta Selic · 15,00% a.a. · 30/01/2026"),"Selic preserves annual unit and the actual observation date");
        check(display.months.get(0).observations.contains("IPCA · sem observação"),"Missing monthly series is explicit and is not interpolated");
        check(display.status.contains("Consulta 30/10 10:00 (Brasília)"),"Cache fetch time is rendered in Brasilia time");
        HistoryDisplay partial=presenter.present(Collections.singletonList(february),"SGS IPCA indisponível",fetched,now,zone);
        check(partial.status.contains("Alguma fonte indisponível")&&partial.months.size()==1,"Valid cached rows remain available after partial source failure");
        HistoryDisplay empty=presenter.present(Collections.emptyList(),"SGS indisponível",0,now,zone);
        check(empty.months.isEmpty()&&empty.status.contains("Nenhum valor estimado"),"Empty history does not invent values");
        HistoryDisplay future=presenter.present(Collections.singletonList(february),null,fetched+1,fetched,zone);
        check(future.status.contains("Horário da consulta indisponível"),"Future cache timestamp is not presented as current");
        boolean invalid=false;try{presenter.present(null,null,0,now,zone);}catch(IllegalArgumentException expected){invalid=true;}
        check(invalid,"Missing history input is rejected");
        System.out.println(checks+" economy history presentation checks passed");
    }
}
