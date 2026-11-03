import br.com.cambioclaro.app.conversion.application.ConvertCurrency;
import br.com.cambioclaro.app.conversion.application.ConversionQuoteSource;
import br.com.cambioclaro.app.conversion.domain.ConversionQuote;
import br.com.cambioclaro.app.conversion.domain.ConversionRequest;
import br.com.cambioclaro.app.conversion.domain.ConversionResult;
import java.math.BigDecimal;
import java.time.Instant;

public final class ConversionTest {
    private static int checks;
    private static void check(boolean ok, String label) { if (!ok) throw new AssertionError(label); checks++; }

    private static final class Quotes implements ConversionQuoteSource {
        ConversionQuote market, tourism, ptax;
        int marketCalls, tourismCalls, ptaxCalls;
        @Override public ConversionQuote market(String code) { marketCalls++; return market; }
        @Override public ConversionQuote tourism() { tourismCalls++; return tourism; }
        @Override public ConversionQuote ptax(String code) { ptaxCalls++; return ptax; }
    }

    public static void main(String[] args) {
        Instant published = Instant.parse("2026-10-30T13:00:00Z");
        Quotes source = new Quotes();
        source.market = new ConversionQuote(new BigDecimal("5.3420"), published, ConversionQuote.Source.MARKET);
        ConvertCurrency useCase = new ConvertCurrency(source);

        ConversionResult result = useCase.execute(new ConversionRequest("USD", "100,00", false));
        check(result.status == ConversionResult.Status.SUCCESS, "Market quote produces a successful outcome");
        check(result.amountInReais.equals(new BigDecimal("534.20")), "Conversion uses exact amount and market rate");
        check(result.quote.source == ConversionQuote.Source.MARKET && result.quote.publishedAt.equals(published), "Result retains market origin and publication time");

        source.market = null;
        source.ptax = new ConversionQuote(new BigDecimal("6.1100"), published, ConversionQuote.Source.PTAX);
        result = useCase.execute(new ConversionRequest("EUR", "2", false));
        check(result.status == ConversionResult.Status.SUCCESS && result.amountInReais.equals(new BigDecimal("12.22")), "Missing market quote falls back to saved PTAX for a standard currency");
        check(result.quote.source == ConversionQuote.Source.PTAX, "Fallback origin remains explicit");

        source.ptaxCalls = 0;
        source.tourism = null;
        result = useCase.execute(new ConversionRequest("USD", "1", true));
        check(result.status == ConversionResult.Status.RATE_UNAVAILABLE, "Unavailable tourism quote returns an explicit unavailable state");
        check(source.ptaxCalls == 0, "Tourism never silently falls back to commercial PTAX");

        source.tourism = new ConversionQuote(new BigDecimal("5.8000"), published, ConversionQuote.Source.TOURISM);
        result = useCase.execute(new ConversionRequest("USD", "1", true));
        check(result.status == ConversionResult.Status.SUCCESS && result.amountInReais.equals(new BigDecimal("5.80")), "Tourism uses its own rate");
        check(result.quote.source == ConversionQuote.Source.TOURISM, "Tourism reference remains distinguishable");

        check(useCase.execute(new ConversionRequest("EUR", "1", true)).status == ConversionResult.Status.INVALID_MODE, "Tourism mode is restricted to USD");
        source.marketCalls = 0;
        check(useCase.execute(new ConversionRequest("EURO", "1", false)).status == ConversionResult.Status.INVALID_CURRENCY, "Currency code format is validated");
        check(source.marketCalls == 0, "Invalid currency is rejected before reading data");
        source.marketCalls = 0;
        check(useCase.execute(new ConversionRequest("USD", " ", false)).status == ConversionResult.Status.AMOUNT_REQUIRED, "Blank input is represented explicitly");
        check(source.marketCalls == 0, "Blank input is rejected before reading data");
        check(useCase.execute(new ConversionRequest("USD", "1.000,00", false)).status == ConversionResult.Status.INVALID_AMOUNT, "Ambiguous grouped input is rejected");
        check(useCase.execute(new ConversionRequest("USD", "-1", false)).status == ConversionResult.Status.INVALID_AMOUNT, "Negative amount is rejected by the shared domain rule");

        source.market = null; source.ptax = null;
        check(useCase.execute(new ConversionRequest("CAD", "1", false)).status == ConversionResult.Status.RATE_UNAVAILABLE, "Missing market and PTAX data returns unavailable without estimating");
        System.out.println(checks + " conversion checks passed");
    }
}
