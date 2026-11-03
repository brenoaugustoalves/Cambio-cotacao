import br.com.cambioclaro.app.Rate;
import br.com.cambioclaro.app.market.presentation.PtaxQuoteDisplay;
import br.com.cambioclaro.app.market.presentation.PtaxQuotePresenter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;

public final class PtaxQuotePresenterTest {
    private static int checks;
    private static void check(boolean condition,String label){if(!condition)throw new AssertionError(label);checks++;}
    private static Rate rate(int day,int hour,String price){BigDecimal sell=new BigDecimal(price);return new Rate(sell.subtract(new BigDecimal("0.01")),sell,LocalDateTime.of(2026,10,day,hour,0));}
    public static void main(String[] args){
        PtaxQuotePresenter presenter=new PtaxQuotePresenter();ZoneId zone=ZoneId.of("America/Sao_Paulo");LocalDate today=LocalDate.of(2026,10,30);
        PtaxQuoteDisplay empty=presenter.present(Collections.emptyList(),today,zone);
        check(empty.value.equals("—")&&empty.publishedAt.contains("Sem cotação")&&empty.change.contains("indisponível"),"No data is explicitly unavailable");
        PtaxQuoteDisplay one=presenter.present(Collections.singletonList(rate(29,13,"5.1234")),today,zone);
        check(one.value.equals("5,1234")&&one.change.contains("Sem publicação PTAX anterior")&&one.publishedAt.contains("Sem publicação de hoje"),"Single old PTAX keeps precision and date without a fabricated change");
        PtaxQuoteDisplay up=presenter.present(Arrays.asList(rate(29,13,"5.00"),rate(30,13,"5.50")),today,zone);
        check(up.direction>0&&up.change.contains("▲ Alta 10,00%")&&up.change.contains("Banco Central"),"Increase identifies the official PTAX comparison and direction");
        PtaxQuoteDisplay down=presenter.present(Arrays.asList(rate(29,13,"5.50"),rate(30,13,"5.00")),today,zone);
        check(down.direction<0&&down.change.contains("▼ Baixa -9,09%"),"Decrease retains signed percentage");
        PtaxQuoteDisplay stable=presenter.present(Arrays.asList(rate(29,13,"5.00"),rate(30,13,"5.00")),today,zone);
        check(stable.direction==0&&stable.change.contains("● Estável 0,00%"),"Unchanged publication is explicitly stable");
        check(up.publishedAt.contains("30/10/2026 às 13:00 (Brasília)"),"PTAX time keeps its source local publication value");
        boolean noList=false;try{presenter.present(null,today,zone);}catch(IllegalArgumentException expected){noList=true;}
        check(noList,"Missing rate list is rejected");
        boolean noZone=false;try{presenter.present(Collections.emptyList(),today,null);}catch(IllegalArgumentException expected){noZone=true;}
        check(noZone,"Missing timezone is rejected");
        System.out.println(checks+" PTAX quote presentation checks passed");
    }
}
