import br.com.cambioclaro.app.market.application.PtaxRefreshPolicy;

public final class PtaxRefreshPolicyTest {
    private static int checks;
    private static void check(boolean condition,String label){if(!condition)throw new AssertionError(label);checks++;}
    public static void main(String[] args){
        long now=100_000_000L,interval=6L*60*60*1000;
        check(PtaxRefreshPolicy.shouldRefresh(now,0,interval),"No stored PTAX lookup triggers a refresh");
        check(!PtaxRefreshPolicy.shouldRefresh(now,now,interval),"Recent PTAX lookup remains inside its window");
        check(!PtaxRefreshPolicy.shouldRefresh(now,now-interval+1,interval),"PTAX is not queried before the exact interval");
        check(PtaxRefreshPolicy.shouldRefresh(now,now-interval,interval),"PTAX becomes due at the interval boundary");
        check(!PtaxRefreshPolicy.shouldRefresh(now,now+1,interval),"Clock rollback does not cause repeated network calls");
        check(PtaxRefreshPolicy.shouldRefresh(now,now-24L*60*60*1000,interval),"Stale PTAX is refreshed even after a longer outage");
        boolean invalid=false;try{PtaxRefreshPolicy.shouldRefresh(now,0,0);}catch(IllegalArgumentException expected){invalid=true;}
        check(invalid,"Invalid interval is rejected");
        System.out.println(checks+" PTAX refresh policy checks passed");
    }
}
