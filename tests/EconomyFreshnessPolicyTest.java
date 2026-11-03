import br.com.cambioclaro.app.market.application.EconomyFreshnessPolicy;

public final class EconomyFreshnessPolicyTest {
    private static int checks;
    private static void check(boolean condition,String label){if(!condition)throw new AssertionError(label);checks++;}
    public static void main(String[] args){
        long now=10_000_000L,ttl=60_000L;
        check(EconomyFreshnessPolicy.isFresh(now,now,ttl),"Just-checked data is fresh");
        check(EconomyFreshnessPolicy.isFresh(now,now+ttl-1,ttl),"Data remains fresh until the exact TTL boundary");
        check(!EconomyFreshnessPolicy.isFresh(now,now+ttl,ttl),"Data expires at the TTL boundary");
        check(!EconomyFreshnessPolicy.isFresh(0,now,ttl),"Missing fetch timestamp is stale");
        check(!EconomyFreshnessPolicy.isFresh(now+1,now,ttl),"Future timestamp is stale after clock rollback");
        check(!EconomyFreshnessPolicy.isFresh(now,now,0),"Nonpositive TTL never marks data fresh");
        check(EconomyFreshnessPolicy.shouldRefresh(true,now,now,ttl),"Explicit refresh bypasses cache freshness");
        check(!EconomyFreshnessPolicy.shouldRefresh(false,now,now,ttl),"Fresh cache avoids another request");
        check(EconomyFreshnessPolicy.shouldRefresh(false,now,now+ttl,ttl),"Expired cache is refreshed");
        check(EconomyFreshnessPolicy.INDICATORS_TTL_MS==6L*60*60*1000&&EconomyFreshnessPolicy.OFFICIAL_NEWS_TTL_MS==3L*60*60*1000&&EconomyFreshnessPolicy.INTERNATIONAL_NEWS_TTL_MS==3L*60*60*1000,"Each macro category retains its configured refresh window");
        System.out.println(checks+" economy freshness policy checks passed");
    }
}
