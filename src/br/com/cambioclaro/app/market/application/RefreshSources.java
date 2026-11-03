package br.com.cambioclaro.app.market.application;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Runs independent official-data sources and returns failures without discarding successful updates. */
public final class RefreshSources {
    public interface Source {
        String id();
        void refresh() throws Exception;
    }
    public static final class Failure {
        public final String sourceId;
        public final Exception cause;
        Failure(String sourceId, Exception cause) { this.sourceId = sourceId; this.cause = cause; }
    }
    public static final class Result {
        public final List<String> refreshed;
        public final List<Failure> failures;
        Result(List<String> refreshed, List<Failure> failures) {
            this.refreshed = Collections.unmodifiableList(new ArrayList<>(refreshed));
            this.failures = Collections.unmodifiableList(new ArrayList<>(failures));
        }
        public boolean hasFailures() { return !failures.isEmpty(); }
    }

    public Result execute(List<Source> sources) {
        if (sources == null) throw new IllegalArgumentException("Fontes obrigatórias");
        Set<String> ids = new HashSet<>();
        List<String> refreshed = new ArrayList<>();
        List<Failure> failures = new ArrayList<>();
        for (Source source : sources) {
            if (source == null || source.id() == null || source.id().trim().isEmpty() || !ids.add(source.id()))
                throw new IllegalArgumentException("Fonte ausente ou identificador duplicado");
            try { source.refresh(); refreshed.add(source.id()); }
            catch (Exception error) { failures.add(new Failure(source.id(), error)); }
        }
        return new Result(refreshed, failures);
    }
}
