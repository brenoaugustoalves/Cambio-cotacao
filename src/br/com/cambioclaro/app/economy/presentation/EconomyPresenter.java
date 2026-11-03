package br.com.cambioclaro.app.economy.presentation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

/** Converts macro observations and expectations into locale-aware, source-conscious text. */
public final class EconomyPresenter {
    public static final class Metric {
        public final BigDecimal value; public final LocalDate date;
        public Metric(BigDecimal value, LocalDate date) { this.value=value; this.date=date; }
    }
    public static final class Expectation {
        public final BigDecimal value; public final String unit, period; public final LocalDate date; public final int respondents;
        public Expectation(BigDecimal value,String unit,String period,LocalDate date,int respondents){this.value=value;this.unit=unit;this.period=period;this.date=date;this.respondents=respondents;}
    }

    private static final Locale PT_BR = new Locale("pt", "BR");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/uuuu", PT_BR);
    private static final DateTimeFormatter FETCHED = DateTimeFormatter.ofPattern("dd/MM HH:mm", PT_BR);

    public EconomyDisplay present(Metric selic, Metric ipca, Map<String,Expectation> expectations,
            long fetchedAt, long now, String failure, ZoneId zone) {
        if (expectations == null || zone == null) throw new IllegalArgumentException("Dados e fuso são obrigatórios");
        boolean hasData=selic!=null||ipca!=null||!expectations.isEmpty();
        String selicText=selic==null?"Meta Selic\nIndisponível na fonte":"Meta Selic · "+number(selic.value,2)+"% a.a.\nSGS 432 · observação de "+date(selic.date);
        String ipcaText=ipca==null?"IPCA mensal\nIndisponível na fonte":"IPCA · "+number(ipca.value,2)+"% no mês\nSGS 433 · competência "+date(ipca.date);
        String focusIpca=line(expectations.get("ipca"),"IPCA",2);
        String focusSelic=line(expectations.get("selic"),"Selic",2);
        String focusFx=line(expectations.get("cambio"),"Câmbio USD/BRL",4);
        String focusPib=line(expectations.get("pib"),"PIB total",2);
        String status;
        if(!hasData) status=failure==null?"Sem dados salvos. Tente atualizar quando houver conexão.":"Fontes indisponíveis; nenhum valor foi inventado. Tente novamente.";
        else if(fetchedAt<=0||fetchedAt>now) status="Horário da consulta indisponível. Confira a data de cada indicador.";
        else {
            String time=FETCHED.format(Instant.ofEpochMilli(fetchedAt).atZone(zone));
            status=failure==null?"Fontes consultadas em "+time+" (Brasília). Dados observados e expectativas têm datas próprias.":"Algumas fontes falharam ("+failure+"). Exibindo apenas dados válidos do cache; confira a data de cada indicador.";
        }
        return new EconomyDisplay(selicText,ipcaText,focusIpca,focusSelic,focusFx,focusPib,status);
    }

    private String line(Expectation item,String title,int digits){
        if(item==null||item.value==null||item.date==null||item.unit==null||item.period==null||item.respondents<1)return title+" · indisponível na fonte";
        return title+" · "+number(item.value,digits)+" "+item.unit+" para "+item.period+"\nPesquisa de "+date(item.date)+" · "+item.respondents+" instituições";
    }
    private String date(LocalDate value){return value==null?"data indisponível":DATE.format(value);}
    private String number(BigDecimal value,int digits){if(value==null)return "indisponível";NumberFormat f=NumberFormat.getNumberInstance(PT_BR);f.setMinimumFractionDigits(digits);f.setMaximumFractionDigits(digits);return f.format(value.setScale(digits,RoundingMode.HALF_UP));}
}
