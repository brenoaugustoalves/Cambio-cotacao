package br.com.cambioclaro.app.market.application;

/** Refreshes USD tourism quote and history independently with a shared freshness policy. */
public final class RefreshTourismQuotes {
    private final TourismQuoteGateway gateway;

    public RefreshTourismQuotes(TourismQuoteGateway gateway) {
        if (gateway == null) throw new IllegalArgumentException("Gateway turismo obrigatório");
        this.gateway = gateway;
    }

    public TourismRefreshResult execute(long now, long interval, boolean force) {
        if (now <= 0 || interval <= 0) throw new IllegalArgumentException("Janela de atualização inválida");
        boolean latestDue = force || due(now, gateway.latestCheckedAt(), interval);
        boolean historyDue = force || due(now, gateway.historyCheckedAt(), interval);
        Exception latestError = latestDue ? attemptLatest() : null;
        Exception historyError = historyDue ? attemptHistory() : null;
        return new TourismRefreshResult(latestDue, historyDue, latestError, historyError);
    }

    private Exception attemptLatest() {
        try { gateway.refreshLatest(); return null; }
        catch (Exception failure) { return failure; }
    }

    private Exception attemptHistory() {
        try { gateway.refreshHistory(); return null; }
        catch (Exception failure) { return failure; }
    }

    private static boolean due(long now, long checked, long interval) {
        return checked <= 0 || (now >= checked && now - checked >= interval);
    }
}
