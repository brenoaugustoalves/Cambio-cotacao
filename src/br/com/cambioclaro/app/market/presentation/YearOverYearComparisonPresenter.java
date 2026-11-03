package br.com.cambioclaro.app.market.presentation;

import br.com.cambioclaro.app.market.domain.MarketObservation;
import br.com.cambioclaro.app.market.domain.YearOverYearComparison;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Formats real movement in the selected period and its prior-year equivalent. */
public final class YearOverYearComparisonPresenter {
    private static final Locale BR = new Locale("pt", "BR");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM", BR);
    private static final DateTimeFormatter FULL_DATE = DateTimeFormatter.ofPattern("dd/MM/uuuu", BR);

    public String present(YearOverYearComparison comparison, ZoneId zone) {
        if (zone == null) throw new IllegalArgumentException("Fuso horário obrigatório");
        if (comparison == null) return "Comparação anual indisponível: faltam ao menos duas publicações reais em cada intervalo correspondente.";
        return "MESMO INTERVALO · COMPARAÇÃO ANUAL\n"
                + period(comparison.previousYearStart, comparison.previousYearEnd, comparison.previousYearChange,
                        comparison.previousYearPercentChange, zone)
                + "\n" + period(comparison.currentStart, comparison.currentEnd, comparison.currentChange,
                        comparison.currentPercentChange, zone)
                + "\nDiferença das variações: " + signedPercent(comparison.percentagePointDifference) + " p.p.";
    }

    private String period(MarketObservation start, MarketObservation end, java.math.BigDecimal change,
            java.math.BigDecimal percent, ZoneId zone) {
        int year = end.publishedAt.atZone(zone).getYear();
        java.time.LocalDate startDate = start.publishedAt.atZone(zone).toLocalDate();
        java.time.LocalDate endDate = end.publishedAt.atZone(zone).toLocalDate();
        String dates = startDate.getYear() == endDate.getYear()
                ? DATE.format(startDate) + "–" + DATE.format(endDate)
                : FULL_DATE.format(startDate) + "–" + FULL_DATE.format(endDate);
        return year + " (" + dates + "): R$ " + value(start.ask) + " → R$ " + value(end.ask)
                + " · " + signedValue(change) + " (" + signedPercent(percent) + "%)";
    }

    private String signedValue(java.math.BigDecimal value) {
        String sign = value.signum() > 0 ? "+" : value.signum() < 0 ? "−" : "";
        return sign + "R$ " + value(value.abs());
    }

    private String signedPercent(java.math.BigDecimal value) {
        String sign = value.signum() > 0 ? "+" : value.signum() < 0 ? "−" : "";
        NumberFormat format = NumberFormat.getNumberInstance(BR);
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(4);
        return sign + format.format(value.abs());
    }

    private String value(java.math.BigDecimal value) {
        NumberFormat format = NumberFormat.getNumberInstance(BR);
        int places = Math.max(4, Math.min(8, value.scale()));
        format.setMinimumFractionDigits(places);
        format.setMaximumFractionDigits(places);
        return format.format(value);
    }
}
