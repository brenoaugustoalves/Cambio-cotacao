import br.com.cambioclaro.app.conversion.domain.ConversionQuote;
import br.com.cambioclaro.app.conversion.domain.ConversionResult;
import br.com.cambioclaro.app.conversion.presentation.ConversionDisplay;
import br.com.cambioclaro.app.conversion.presentation.ConversionPresenter;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;

public final class ConversionPresenterTest {
    private static int checks;
    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
        checks++;
    }

    public static void main(String[] args) {
        ZoneId brasilia = ZoneId.of("America/Sao_Paulo");
        Instant published = Instant.parse("2026-10-30T13:00:00Z");
        ConversionQuote marketQuote = new ConversionQuote(new BigDecimal("5.3420"), published,
                ConversionQuote.Source.MARKET);
        ConversionDisplay display = ConversionPresenter.present(
                ConversionResult.success(new BigDecimal("534.20"), marketQuote), "USD", false, brasilia);
        check(display.isAvailable() && display.amountInReais.equals(new BigDecimal("534.20")),
                "Successful conversion remains available with exact amount");
        check(display.note.equals("Referência: mercado (ask) publicada em 30/10 10:00.\nSem tarifas ou IOF adicionais."),
                "Market reference shows localized source and Brasília time");

        ConversionQuote ptaxQuote = new ConversionQuote(new BigDecimal("6.0000"), published,
                ConversionQuote.Source.PTAX);
        display = ConversionPresenter.present(ConversionResult.success(new BigDecimal("12.00"), ptaxQuote),
                "EUR", false, brasilia);
        check(display.isAvailable(), "PTAX conversion remains available");
        check(display.note.contains("PTAX venda de 30/10") && display.note.contains("Mercado indisponível"),
                "PTAX fallback identifies its publication date and fallback reason");

        ConversionQuote tourismQuote = new ConversionQuote(new BigDecimal("5.8000"), published,
                ConversionQuote.Source.TOURISM);
        display = ConversionPresenter.present(ConversionResult.success(BigDecimal.ONE, tourismQuote),
                "USD", true, brasilia);
        check(display.note.contains("dólar turismo (ask)"), "Tourism presentation identifies tourism source");

        display = ConversionPresenter.present(ConversionResult.failure(ConversionResult.Status.AMOUNT_REQUIRED),
                "USD", false, brasilia);
        check(!display.isAvailable() && display.note.contains("Digite um valor em USD"),
                "Required amount has a clear unavailable state");
        display = ConversionPresenter.present(ConversionResult.failure(ConversionResult.Status.INVALID_AMOUNT),
                "USD", false, brasilia);
        check(display.note.contains("sem separador de milhar"), "Invalid amount explains accepted format");
        display = ConversionPresenter.present(ConversionResult.failure(ConversionResult.Status.INVALID_CURRENCY),
                "EUR", false, brasilia);
        check(display.note.equals("Moeda não reconhecida para conversão."), "Invalid currency message is stable");
        display = ConversionPresenter.present(ConversionResult.failure(ConversionResult.Status.INVALID_MODE),
                "EUR", true, brasilia);
        check(display.note.contains("somente para USD"), "Invalid mode explains USD-only tourism");
        display = ConversionPresenter.present(ConversionResult.failure(ConversionResult.Status.RATE_UNAVAILABLE),
                "USD", true, brasilia);
        check(display.note.contains("Não usamos a cotação comercial como substituto"),
                "Unavailable tourism never suggests a different quote");
        display = ConversionPresenter.present(ConversionResult.failure(ConversionResult.Status.RATE_UNAVAILABLE),
                "EUR", false, brasilia);
        check(display.note.contains("Sem cotação válida de EUR"), "Unavailable market quote names selected currency");
        display = ConversionPresenter.present(null, "USD", false, brasilia);
        check(!display.isAvailable() && display.note.contains("Não foi possível calcular"),
                "Null outcome maps to a safe generic presentation");

        boolean rejected = false;
        try { ConversionPresenter.present(null, "USD", false, null); }
        catch (IllegalArgumentException expected) { rejected = true; }
        check(rejected, "Missing timezone is rejected instead of silently guessing");

        System.out.println(checks + " conversion presenter checks passed");
    }
}
