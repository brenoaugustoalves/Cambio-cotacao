package br.com.cambioclaro.app.conversion.presentation;

import br.com.cambioclaro.app.conversion.domain.ConversionQuote;
import br.com.cambioclaro.app.conversion.domain.ConversionResult;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Maps conversion outcomes into localized, source-aware presentation data. */
public final class ConversionPresenter {
    private static final Locale BRAZIL = new Locale("pt", "BR");
    private static final DateTimeFormatter PUBLICATION_TIME = DateTimeFormatter.ofPattern("dd/MM HH:mm", BRAZIL);
    private static final DateTimeFormatter PUBLICATION_DAY = DateTimeFormatter.ofPattern("dd/MM/uuuu", BRAZIL);

    private ConversionPresenter() { }

    public static ConversionDisplay present(ConversionResult result, String currencyCode,
            boolean tourism, ZoneId displayZone) {
        if (currencyCode == null || displayZone == null) {
            throw new IllegalArgumentException("Moeda e fuso horário são obrigatórios");
        }
        if (result == null) return unavailable("Não foi possível calcular a conversão.");
        if (result.status != ConversionResult.Status.SUCCESS) {
            return unavailable(message(result.status, currencyCode, tourism));
        }

        ConversionQuote quote = result.quote;
        if (quote.source == ConversionQuote.Source.PTAX) {
            String date = quote.publishedAt.atZone(displayZone).format(PUBLICATION_DAY);
            return new ConversionDisplay(result.amountInReais,
                    "Referência: PTAX venda de " + date + ".\nMercado indisponível; usando a última PTAX salva. Sem tarifas ou IOF adicionais.");
        }

        String source = quote.source == ConversionQuote.Source.TOURISM
                ? "dólar turismo (ask)" : "mercado (ask)";
        String time = quote.publishedAt.atZone(displayZone).format(PUBLICATION_TIME);
        return new ConversionDisplay(result.amountInReais,
                "Referência: " + source + " publicada em " + time + ".\nSem tarifas ou IOF adicionais.");
    }

    private static ConversionDisplay unavailable(String note) {
        return new ConversionDisplay(null, note);
    }

    private static String message(ConversionResult.Status status, String currencyCode, boolean tourism) {
        switch (status) {
            case AMOUNT_REQUIRED:
                return "Digite um valor em " + currencyCode + ".";
            case INVALID_AMOUNT:
                return "Digite um valor válido, com até 2 casas decimais e sem separador de milhar.";
            case INVALID_CURRENCY:
                return "Moeda não reconhecida para conversão.";
            case INVALID_MODE:
                return "Dólar turismo está disponível somente para USD.";
            case RATE_UNAVAILABLE:
                return tourism
                        ? "Dólar turismo indisponível. Não usamos a cotação comercial como substituto."
                        : "Sem cotação válida de " + currencyCode + ". Atualize para consultar a fonte.";
            default:
                return "Não foi possível calcular a conversão.";
        }
    }
}
