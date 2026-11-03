package br.com.cambioclaro.app.economy.presentation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable month rows for the macroeconomic history section. */
public final class HistoryDisplay {
    public static final class Month {
        public final String title,observations;
        Month(String title,String observations){this.title=title;this.observations=observations;}
    }
    public final String status;
    public final List<Month> months;
    HistoryDisplay(String status,List<Month> months){this.status=status;this.months=Collections.unmodifiableList(new ArrayList<>(months));}
}
