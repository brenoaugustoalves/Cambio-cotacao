package br.com.cambioclaro.app.market.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Compares actual price movement over a selected date range with its prior-year range. */
public final class YearOverYearComparison {
    public final MarketObservation currentStart;
    public final MarketObservation currentEnd;
    public final MarketObservation previousYearStart;
    public final MarketObservation previousYearEnd;
    public final BigDecimal currentChange;
    public final BigDecimal currentPercentChange;
    public final BigDecimal previousYearChange;
    public final BigDecimal previousYearPercentChange;
    public final BigDecimal percentagePointDifference;

    private YearOverYearComparison(MarketObservation currentStart, MarketObservation currentEnd,
            MarketObservation previousYearStart, MarketObservation previousYearEnd,
            BigDecimal currentChange, BigDecimal currentPercentChange,
            BigDecimal previousYearChange, BigDecimal previousYearPercentChange,
            BigDecimal percentagePointDifference) {
        this.currentStart = currentStart;
        this.currentEnd = currentEnd;
        this.previousYearStart = previousYearStart;
        this.previousYearEnd = previousYearEnd;
        this.currentChange = currentChange;
        this.currentPercentChange = currentPercentChange;
        this.previousYearChange = previousYearChange;
        this.previousYearPercentChange = previousYearPercentChange;
        this.percentagePointDifference = percentagePointDifference;
    }

    public static YearOverYearComparison compare(MarketObservation currentStart, MarketObservation currentEnd,
            MarketObservation previousYearStart, MarketObservation previousYearEnd) {
        requireQuote(currentStart);
        requireQuote(currentEnd);
        requireQuote(previousYearStart);
        requireQuote(previousYearEnd);
        if (currentStart.publishedAt.isAfter(currentEnd.publishedAt)
                || previousYearStart.publishedAt.isAfter(previousYearEnd.publishedAt))
            throw new IllegalArgumentException("Datas finais devem suceder as datas iniciais");
        BigDecimal currentChange = currentEnd.ask.subtract(currentStart.ask);
        BigDecimal previousYearChange = previousYearEnd.ask.subtract(previousYearStart.ask);
        BigDecimal currentPercent = percent(currentChange, currentStart.ask);
        BigDecimal previousYearPercent = percent(previousYearChange, previousYearStart.ask);
        return new YearOverYearComparison(currentStart, currentEnd, previousYearStart, previousYearEnd,
                currentChange, currentPercent, previousYearChange, previousYearPercent,
                currentPercent.subtract(previousYearPercent));
    }

    private static BigDecimal percent(BigDecimal change, BigDecimal base) {
        return change.multiply(new BigDecimal("100")).divide(base, 6, RoundingMode.HALF_UP);
    }

    private static void requireQuote(MarketObservation observation) {
        if (observation == null || observation.ask.signum() <= 0)
            throw new IllegalArgumentException("Quatro cotações publicadas são necessárias");
    }
}
