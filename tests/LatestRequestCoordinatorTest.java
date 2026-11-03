import br.com.cambioclaro.app.market.application.LatestRequestCoordinator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

public final class LatestRequestCoordinatorTest {
    private static int checks;
    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
        checks++;
    }

    public static void main(String[] args) throws Exception {
        LatestRequestCoordinator coordinator = new LatestRequestCoordinator();
        check(!coordinator.isCurrent(0), "No request is current before the first request starts");

        long first = coordinator.begin();
        FutureTask<Void> firstTask = new FutureTask<>(() -> null);
        coordinator.attach(first, firstTask);
        long second = coordinator.begin();
        check(firstTask.isCancelled(), "Starting a newer request cancels the prior task");
        check(!coordinator.isCurrent(first) && coordinator.isCurrent(second),
                "Only the latest request token may update state");

        FutureTask<Void> lateTask = new FutureTask<>(() -> null);
        coordinator.attach(first, lateTask);
        check(lateTask.isCancelled(), "A task attached after supersession is cancelled immediately");

        FutureTask<Void> secondTask = new FutureTask<>(() -> null);
        coordinator.attach(second, secondTask);
        coordinator.invalidate();
        check(secondTask.isCancelled() && !coordinator.isCurrent(second),
                "Invalidation cancels active work and rejects its callback token");

        boolean rejectedNull = false;
        try { coordinator.attach(coordinator.begin(), null); }
        catch (IllegalArgumentException expected) { rejectedNull = true; }
        check(rejectedNull, "Null task is rejected");

        int workers = 8;
        ExecutorService pool = Executors.newFixedThreadPool(workers);
        CountDownLatch ready = new CountDownLatch(workers);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Long>> submitted = new ArrayList<>();
        for (int i = 0; i < workers; i++) {
            submitted.add(pool.submit(() -> {
                ready.countDown();
                start.await();
                return coordinator.begin();
            }));
        }
        ready.await();
        start.countDown();
        Set<Long> tokens = new HashSet<>();
        for (Future<Long> result : submitted) tokens.add(result.get());
        pool.shutdownNow();
        check(tokens.size() == workers, "Concurrent request starts receive unique tokens");
        check(coordinator.isCurrent(Collections.max(tokens)), "Newest concurrent token remains current");

        System.out.println(checks + " latest request coordinator checks passed");
    }
}
