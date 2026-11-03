package br.com.cambioclaro.app;

import android.content.Context;
import android.content.SharedPreferences;
import br.com.cambioclaro.app.market.application.RefreshTourismQuotes;
import br.com.cambioclaro.app.market.application.TourismQuoteGateway;

/** Android adapter for the commercial USD/BRLT quote and intraday-history source. */
public final class AndroidTourismGateway implements TourismQuoteGateway {
    private final Context context;
    private final SharedPreferences preferences;

    public AndroidTourismGateway(Context context) {
        if (context == null) throw new IllegalArgumentException("Contexto obrigatório");
        this.context = context.getApplicationContext();
        this.preferences = this.context.getSharedPreferences("tourism_v1", Context.MODE_PRIVATE);
    }

    public static RefreshTourismQuotes create(Context context) {
        return new RefreshTourismQuotes(new AndroidTourismGateway(context));
    }

    @Override public long latestCheckedAt() { return preferences.getLong("checked", 0); }
    @Override public long historyCheckedAt() { return preferences.getLong("history_checked", 0); }
    @Override public void refreshLatest() throws Exception { Tourism.saveLatest(context, Tourism.fetchLatest()); }
    @Override public void refreshHistory() throws Exception { Tourism.saveHistory(context, Tourism.fetchHistory()); }
}
