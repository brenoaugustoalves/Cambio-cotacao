import br.com.cambioclaro.app.market.application.RefreshStockQuotes;
import br.com.cambioclaro.app.market.application.StockQuoteGateway;
import br.com.cambioclaro.app.market.application.StockRefreshResult;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class RefreshStockQuotesTest {
    private static int checks;
    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
        checks++;
    }

    private static final class FakeGateway implements StockQuoteGateway {
        final List<String> events = new ArrayList<>();
        boolean indexEnabled, failStocks, failIndex;
        @Override public boolean indexEnabled() { return indexEnabled; }
        @Override public String fetchStocks() throws Exception {
            events.add("fetch-stocks");
            if (failStocks) throw new java.io.IOException("stocks failed");
            return "stocks-response";
        }
        @Override public String fetchIndex() throws Exception {
            events.add("fetch-index");
            if (failIndex) throw new java.io.IOException("index failed");
            return "index-response";
        }
        @Override public void save(String response, boolean index) {
            events.add("save-" + (index ? "index" : "stocks") + ":" + response);
        }
    }

    public static void main(String[] args) {
        FakeGateway both = new FakeGateway();
        both.indexEnabled = true;
        StockRefreshResult result = new RefreshStockQuotes(both).execute();
        check(!result.hasError() && both.events.equals(Arrays.asList("fetch-stocks", "save-stocks:stocks-response",
                "fetch-index", "save-index:index-response")), "Configured sources fetch and save in deterministic order");

        FakeGateway noIndex = new FakeGateway();
        result = new RefreshStockQuotes(noIndex).execute();
        check(!result.indexEnabled && noIndex.events.equals(Arrays.asList("fetch-stocks", "save-stocks:stocks-response")),
                "Unconfigured index does not trigger an HTTP request");

        FakeGateway failedStocks = new FakeGateway();
        failedStocks.indexEnabled = true;
        failedStocks.failStocks = true;
        result = new RefreshStockQuotes(failedStocks).execute();
        check(result.stocksError != null && result.indexError == null && failedStocks.events.contains("save-index:index-response"),
                "Stock failure does not prevent independent index refresh");

        FakeGateway failedIndex = new FakeGateway();
        failedIndex.indexEnabled = true;
        failedIndex.failIndex = true;
        result = new RefreshStockQuotes(failedIndex).execute();
        check(result.stocksError == null && result.indexError != null && result.hasError(),
                "Index failure is returned without hiding stock success");

        boolean rejected = false;
        try { new RefreshStockQuotes(null); }
        catch (IllegalArgumentException expected) { rejected = true; }
        check(rejected, "Missing stock gateway is rejected");

        System.out.println(checks + " stock refresh use case checks passed");
    }
}
