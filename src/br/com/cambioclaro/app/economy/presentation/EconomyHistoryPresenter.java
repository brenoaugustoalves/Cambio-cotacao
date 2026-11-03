package br.com.cambioclaro.app.economy.presentation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Formats actual monthly SGS observations, preserving missing months and series units. */
public final class EconomyHistoryPresenter {
    public static final class Observation {public final LocalDate date;public final BigDecimal value;public Observation(LocalDate date,BigDecimal value){this.date=date;this.value=value;}}
    public static final class Month {public final YearMonth month;public final Observation ipca,selic;public Month(YearMonth month,Observation ipca,Observation selic){this.month=month;this.ipca=ipca;this.selic=selic;}}
    private static final Locale BR=new Locale("pt","BR");
    private static final DateTimeFormatter MONTH=DateTimeFormatter.ofPattern("MM/uuuu",BR);
    private static final DateTimeFormatter DATE=DateTimeFormatter.ofPattern("dd/MM/uuuu",BR);
    private static final DateTimeFormatter TIME=DateTimeFormatter.ofPattern("dd/MM HH:mm",BR);

    public HistoryDisplay present(List<Month> input,String failure,long fetchedAt,long now,ZoneId zone){
        if(input==null||zone==null)throw new IllegalArgumentException("Histórico e fuso são obrigatórios");
        List<HistoryDisplay.Month> output=new ArrayList<>();
        for(int i=input.size()-1;i>=0;i--){Month month=input.get(i);if(month==null||month.month==null)continue;
            output.add(new HistoryDisplay.Month(MONTH.format(month.month),line("IPCA",month.ipca,2,"% no mês")+"\n"+line("Meta Selic",month.selic,2,"% a.a.")));
        }
        String status;
        if(output.isEmpty())status=failure==null?"Sem observações históricas válidas.":"Falha em fonte histórica ("+failure+"). Nenhum valor estimado.";
        else if(fetchedAt<=0||fetchedAt>now)status="Horário da consulta indisponível. Cada coluna mantém sua unidade e data.";
        else {String at=TIME.format(Instant.ofEpochMilli(fetchedAt).atZone(zone))+" (Brasília)";status=(failure==null?"Consulta ":"Alguma fonte indisponível · cache consultado ")+at+". Cada coluna mantém sua unidade e data.";}
        return new HistoryDisplay(status,output);
    }
    private String line(String name,Observation observation,int digits,String unit){
        if(observation==null||observation.value==null)return name+" · sem observação";
        return name+" · "+number(observation.value,digits)+unit+" · "+(observation.date==null?"data indisponível":DATE.format(observation.date));
    }
    private String number(BigDecimal value,int digits){NumberFormat f=NumberFormat.getNumberInstance(BR);f.setMinimumFractionDigits(digits);f.setMaximumFractionDigits(digits);return f.format(value.setScale(digits,RoundingMode.HALF_UP));}
}
