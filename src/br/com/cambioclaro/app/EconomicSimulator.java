package br.com.cambioclaro.app;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Transparent, non-advisory arithmetic for user-entered currency purchase costs. */
public final class EconomicSimulator {
    private static final BigDecimal HUNDRED=new BigDecimal("100");
    private EconomicSimulator(){}
    public static BigDecimal parse(String text,int maxScale,boolean allowZero){
        if(text==null||text.length()>24||maxScale<0||maxScale>8)throw new IllegalArgumentException("Digite um número válido.");
        String value=text.trim();String fraction=maxScale==0?"":"([,.][0-9]{1,"+maxScale+"})?";
        if(!value.matches("[0-9]{1,12}"+fraction))throw new IllegalArgumentException("Use um número sem separador de milhar e com até "+maxScale+" casas decimais.");
        BigDecimal result=new BigDecimal(value.replace(',','.'));if((!allowZero&&result.signum()<=0)||result.signum()<0)throw new IllegalArgumentException("O valor precisa ser positivo.");return result;
    }
    public static Result calculate(BigDecimal foreignAmount,BigDecimal referenceRate,BigDecimal spreadPercent,BigDecimal extraPercent,BigDecimal fixedFee){
        if(foreignAmount==null||foreignAmount.signum()<=0||referenceRate==null||referenceRate.signum()<=0||spreadPercent==null||extraPercent==null||fixedFee==null||spreadPercent.signum()<0||extraPercent.signum()<0||fixedFee.signum()<0||spreadPercent.compareTo(new BigDecimal("1000"))>0||extraPercent.compareTo(new BigDecimal("1000"))>0||fixedFee.compareTo(new BigDecimal("1000000"))>0)throw new IllegalArgumentException("Confira os valores informados.");
        BigDecimal base=foreignAmount.multiply(referenceRate).setScale(2,RoundingMode.HALF_UP);
        BigDecimal spread=base.multiply(spreadPercent).divide(HUNDRED,2,RoundingMode.HALF_UP);
        BigDecimal extra=base.multiply(extraPercent).divide(HUNDRED,2,RoundingMode.HALF_UP);
        BigDecimal fee=fixedFee.setScale(2,RoundingMode.HALF_UP);return new Result(base,spread,extra,fee,base.add(spread).add(extra).add(fee));
    }
    public static final class Result {
        public final BigDecimal base,spread,extra,fixedFee,total;
        Result(BigDecimal base,BigDecimal spread,BigDecimal extra,BigDecimal fixedFee,BigDecimal total){this.base=base;this.spread=spread;this.extra=extra;this.fixedFee=fixedFee;this.total=total;}
    }
}
