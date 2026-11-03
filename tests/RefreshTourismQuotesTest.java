import br.com.cambioclaro.app.market.application.RefreshTourismQuotes;
import br.com.cambioclaro.app.market.application.TourismQuoteGateway;
import br.com.cambioclaro.app.market.application.TourismRefreshResult;

public final class RefreshTourismQuotesTest {
    private static int checks;
    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
        checks++;
    }
    private static final class FakeGateway implements TourismQuoteGateway {
        long latest, history;
        int latestCalls, historyCalls;
        boolean failLatest, failHistory;
        public long latestCheckedAt() { return latest; }
        public long historyCheckedAt() { return history; }
        public void refreshLatest() throws Exception { latestCalls++; if (failLatest) throw new java.io.IOException("latest"); }
        public void refreshHistory() throws Exception { historyCalls++; if (failHistory) throw new java.io.IOException("history"); }
    }
    public static void main(String[] args) {
        FakeGateway gateway = new FakeGateway();
        RefreshTourismQuotes refresh = new RefreshTourismQuotes(gateway);
        TourismRefreshResult first = refresh.execute(10000, 5000, false);
        check(first.latestAttempted && first.historyAttempted && gateway.latestCalls == 1 && gateway.historyCalls == 1,
                "Missing timestamps trigger independent quote and history refreshes");
        gateway.latest = 9000; gateway.history = 9000;
        TourismRefreshResult fresh = refresh.execute(11000, 5000, false);
        check(!fresh.latestAttempted && !fresh.historyAttempted && gateway.latestCalls == 1 && gateway.historyCalls == 1,
                "Fresh quote and history do not cause network calls");
        TourismRefreshResult elapsed = refresh.execute(15000, 5000, false);
        check(elapsed.latestAttempted && elapsed.historyAttempted && gateway.latestCalls == 2 && gateway.historyCalls == 2,
                "Both data products refresh when their independent age reaches the interval");
        gateway.failLatest = true;
        TourismRefreshResult partial = refresh.execute(20000, 1, true);
        check(partial.latestError != null && partial.historyError == null && gateway.latestCalls == 3 && gateway.historyCalls == 3,
                "Latest quote failure does not block history refresh");
        gateway.failLatest = false; gateway.failHistory = true;
        TourismRefreshResult historyFailure = refresh.execute(20000, 1, true);
        check(historyFailure.latestError == null && historyFailure.historyError != null && historyFailure.hasError(),
                "History failure is reported separately after latest quote succeeds");
        gateway.failHistory = false; gateway.latest = 25000; gateway.history = 25000;
        TourismRefreshResult clockRollback = refresh.execute(24000, 5000, false);
        check(!clockRollback.latestAttempted && !clockRollback.historyAttempted,
                "Clock moving behind checked time does not trigger redundant requests");
        TourismRefreshResult forced = refresh.execute(26000, 5000, true);
        check(forced.latestAttempted && forced.historyAttempted, "Explicit refresh bypasses cache age");
        boolean invalidInterval = false;
        try { refresh.execute(100, 0, false); } catch (IllegalArgumentException expected) { invalidInterval = true; }
        check(invalidInterval, "Invalid refresh interval is rejected");
        boolean nullGateway = false;
        try { new RefreshTourismQuotes(null); } catch (IllegalArgumentException expected) { nullGateway = true; }
        check(nullGateway, "Missing gateway is rejected");
        System.out.println(checks + " tourism refresh use case checks passed");
    }
}
