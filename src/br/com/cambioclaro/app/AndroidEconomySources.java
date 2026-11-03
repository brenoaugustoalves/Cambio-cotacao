package br.com.cambioclaro.app;

import android.content.Context;
import br.com.cambioclaro.app.market.application.RefreshSources;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Android data adapters for the official macro, history and news feeds. */
public final class AndroidEconomySources {
    private final Context context;
    public AndroidEconomySources(Context context) {
        if (context == null) throw new IllegalArgumentException("Contexto obrigatório");
        this.context = context.getApplicationContext();
    }

    public List<RefreshSources.Source> overview(LocalDate today) {
        List<RefreshSources.Source> sources = new ArrayList<>();
        sources.add(source("SGS Meta Selic", () -> { String raw=EconomyClient.download(EconomyClient.SELIC_SOURCE); EconomyClient.parseSgs(raw,432,today); EconomyClient.saveSgs(context,432,raw); }));
        sources.add(source("SGS IPCA", () -> { String raw=EconomyClient.download(EconomyClient.IPCA_SOURCE); EconomyClient.parseSgs(raw,433,today); EconomyClient.saveSgs(context,433,raw); }));
        sources.add(source("expectativas Focus", () -> { String raw=EconomyClient.download(EconomyClient.focusUrl(today.getYear())); EconomyClient.parseFocus(raw,today,today.getYear()); EconomyClient.saveFocus(context,raw); }));
        return sources;
    }

    public List<RefreshSources.Source> history(LocalDate today) {
        List<RefreshSources.Source> sources = new ArrayList<>();
        sources.add(source("SGS IPCA histórico", () -> { String raw=EconomyHistory.download(EconomyHistory.url(EconomyHistory.IPCA_SERIES,today)); EconomyHistory.parse(raw,EconomyHistory.IPCA_SERIES,today); EconomyHistory.save(context,EconomyHistory.IPCA_SERIES,raw,System.currentTimeMillis()); }));
        sources.add(source("SGS Selic histórico", () -> { String raw=EconomyHistory.download(EconomyHistory.url(EconomyHistory.SELIC_SERIES,today)); EconomyHistory.parse(raw,EconomyHistory.SELIC_SERIES,today); EconomyHistory.save(context,EconomyHistory.SELIC_SERIES,raw,System.currentTimeMillis()); }));
        return sources;
    }

    public List<RefreshSources.Source> bcbNews(int year, Instant now) {
        List<RefreshSources.Source> sources = new ArrayList<>();
        sources.add(source("Comunicados do Banco Central", () -> { String raw=BcbNews.download(BcbNews.feedUrl(year)); BcbNews.parse(raw,now); BcbNews.save(context,raw,System.currentTimeMillis()); }));
        return sources;
    }

    public List<RefreshSources.Source> globalNews(boolean force, long now) {
        List<RefreshSources.Source> sources = new ArrayList<>();
        for (GlobalNews.Source feed : GlobalNews.SOURCES) {
            if (!force && GlobalNews.freshSource(context,feed.id,now)) continue;
            sources.add(source("notícias exteriores / "+feed.id, () -> { String raw=GlobalNews.download(feed.id); GlobalNews.parse(feed.id,raw,Instant.ofEpochMilli(now)); GlobalNews.save(context,feed.id,raw,System.currentTimeMillis()); }));
        }
        return sources;
    }

    private RefreshSources.Source source(String id, Work work) {
        return new RefreshSources.Source() {
            @Override public String id() { return id; }
            @Override public void refresh() throws Exception { work.run(); }
        };
    }
    private interface Work { void run() throws Exception; }
}
