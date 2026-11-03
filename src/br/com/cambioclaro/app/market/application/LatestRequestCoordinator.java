package br.com.cambioclaro.app.market.application;

import java.util.concurrent.Future;

/** Cancels superseded work and identifies which asynchronous response may update state. */
public final class LatestRequestCoordinator {
    private long generation;
    private Future<?> active;

    public synchronized long begin() {
        generation++;
        cancelActive();
        return generation;
    }

    public synchronized void attach(long requestId, Future<?> task) {
        if (task == null) throw new IllegalArgumentException("Tarefa obrigatória");
        if (!isCurrent(requestId)) {
            task.cancel(true);
            return;
        }
        cancelActive();
        active = task;
    }

    public synchronized boolean isCurrent(long requestId) {
        return requestId != 0 && requestId == generation;
    }

    public synchronized void invalidate() {
        generation++;
        cancelActive();
    }

    private void cancelActive() {
        if (active != null) {
            active.cancel(true);
            active = null;
        }
    }
}
