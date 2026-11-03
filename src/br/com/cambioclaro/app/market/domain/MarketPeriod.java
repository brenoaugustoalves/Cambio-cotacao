package br.com.cambioclaro.app.market.domain;

import java.time.LocalDate;

/** Calendar periods selectable in the market chart. Values are stable UI/cache keys. */
public final class MarketPeriod {
    public static final int DAY = 1;
    public static final int FIVE_DAYS = 5;
    public static final int MONTH = 30;
    public static final int TWO_MONTHS = 60;
    public static final int THREE_MONTHS = 90;
    public static final int SIX_MONTHS = 180;
    public static final int YEAR = 365;

    private MarketPeriod() { }

    public static String label(int period) {
        switch (period) {
            case DAY: return "1 dia";
            case FIVE_DAYS: return "5 dias";
            case MONTH: return "1 mês";
            case TWO_MONTHS: return "2 meses";
            case THREE_MONTHS: return "3 meses";
            case SIX_MONTHS: return "6 meses";
            case YEAR: return "1 ano";
            default: throw new IllegalArgumentException("Período não suportado");
        }
    }

    public static LocalDate startDate(int period, LocalDate endDate) {
        if (endDate == null) throw new IllegalArgumentException("Data final obrigatória");
        switch (period) {
            case DAY: return endDate;
            case FIVE_DAYS: return endDate.minusDays(5);
            case MONTH: return endDate.minusMonths(1);
            case TWO_MONTHS: return endDate.minusMonths(2);
            case THREE_MONTHS: return endDate.minusMonths(3);
            case SIX_MONTHS: return endDate.minusMonths(6);
            case YEAR: return endDate.minusYears(1);
            default: throw new IllegalArgumentException("Período não suportado");
        }
    }

    public static LocalDate previousYear(LocalDate date) {
        if (date == null) throw new IllegalArgumentException("Data obrigatória");
        return date.minusYears(1);
    }
}
