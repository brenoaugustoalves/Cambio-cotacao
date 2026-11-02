package br.com.cambioclaro.app;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.*;

public final class Rate {
    public final BigDecimal buy, sell;
    public final LocalDateTime at;
    public Rate(BigDecimal buy, BigDecimal sell, LocalDateTime at) {
        if (buy == null || sell == null || at == null || buy.signum() <= 0 || sell.signum() <= 0 || buy.compareTo(sell)>0)
            throw new IllegalArgumentException("Cotação inválida");
        if(buy.precision()>16 || sell.precision()>16 || buy.scale()<0 || sell.scale()<0 || buy.scale()>8 || sell.scale()>8)
            throw new IllegalArgumentException("Formato de cotação inválido");
        this.buy=buy; this.sell=sell; this.at=at;
    }
    public static List<Rate> normalize(List<Rate> input, LocalDate today) {
        TreeMap<LocalDate,Rate> byDay=new TreeMap<>();
        for(Rate r:input) {
            if(r.at.toLocalDate().isAfter(today)) throw new IllegalArgumentException("Data futura na resposta");
            Rate old=byDay.get(r.at.toLocalDate());
            if(old==null || r.at.isAfter(old.at)) byDay.put(r.at.toLocalDate(),r);
            else if(r.at.equals(old.at) && (r.sell.compareTo(old.sell)!=0 || r.buy.compareTo(old.buy)!=0)) throw new IllegalArgumentException("Registros conflitantes");
        }
        return new ArrayList<>(byDay.values());
    }
    public static BigDecimal variation(BigDecimal first, BigDecimal last) {
        if(first.signum()<=0) throw new IllegalArgumentException("Base inválida");
        return last.subtract(first).multiply(new BigDecimal("100")).divide(first,8,RoundingMode.HALF_UP);
    }
    public static BigDecimal convert(String value, BigDecimal rate) {
        if(value==null || !value.trim().matches("[0-9]{1,12}([,.][0-9]{0,2})?")) throw new IllegalArgumentException("Digite um valor válido, com até 2 casas decimais e sem separador de milhar.");
        BigDecimal amount=new BigDecimal(value.trim().replace(',','.'));
        return amount.multiply(rate).setScale(2,RoundingMode.HALF_UP);
    }
    public static List<Rate> period(List<Rate> all, LocalDate today, int days) {
        List<Rate> result=new ArrayList<>(); LocalDate from=today.minusDays(days-1L);
        for(Rate r:all) if(!r.at.toLocalDate().isBefore(from) && !r.at.toLocalDate().isAfter(today)) result.add(r);
        return result;
    }
}
