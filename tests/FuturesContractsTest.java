import br.com.cambioclaro.app.market.domain.FuturesContracts;
import java.time.LocalDate;
import java.util.List;

public final class FuturesContractsTest {
    private static int checks;
    private static void check(boolean condition,String label){if(!condition)throw new AssertionError(label);checks++;}
    public static void main(String[] args){
        FuturesContracts win=FuturesContracts.current("WIN",LocalDate.of(2026,4,16));
        check(win!=null&&win.symbol.equals("WINM26")&&win.expiry.getMonthValue()==6,"WIN code tracks the June 2026 contract after April maturity");
        check(win.lastTrade.equals(LocalDate.of(2026,6,17)),"WIN last trading day matches the published B3 calendar");
        FuturesContracts dol=FuturesContracts.current("DOL",LocalDate.of(2026,10,30));
        check(dol!=null&&dol.symbol.equals("DOLX26")&&dol.expiry.getMonthValue()==11,"DOL selection follows its month and year contract code");
        FuturesContracts wdo=FuturesContracts.current("WDO",LocalDate.of(2026,10,30));
        check(wdo!=null&&wdo.symbol.equals("WDOX26"),"WDO keeps its own contract root");
        check(FuturesContracts.current("DOL",LocalDate.of(2026,10,31)).symbol.equals("DOLZ26"),"DOL rolls after B3's published last trading day");
        List<FuturesContracts> available=FuturesContracts.available("WIN",LocalDate.of(2026,2,1));
        check(available.size()>1&&available.get(0).expiry.isBefore(available.get(1).expiry),"Available contracts are returned in maturity order");
        check(FuturesContracts.current("WIN",LocalDate.of(2028,1,1))==null,"No expiry is inferred after the official calendar ends");
        boolean invalidBase=false;try{FuturesContracts.available("BTC",LocalDate.of(2026,1,1));}catch(IllegalArgumentException expected){invalidBase=true;}
        check(invalidBase,"Unknown contract root is rejected");
        boolean missingDate=false;try{FuturesContracts.available("DOL",null);}catch(IllegalArgumentException expected){missingDate=true;}
        check(missingDate,"Missing date is rejected");
        System.out.println(checks+" B3 futures contract domain checks passed");
    }
}
