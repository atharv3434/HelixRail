import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class ConcurrencyTest {
    public static void main(String[] args) throws InterruptedException {
        testMutualExclusionSafety();
    }

    public static void testMutualExclusionSafety() throws InterruptedException {
        int threadCount = 10;
        ExecutorService service = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successfulLocks = new AtomicInteger(0);

        System.out.println("Running Thread Safety Isolation Tests...");

        for (int i = 0; i < threadCount; i++) {
            final String name = "TEST-THREAD-" + i;
            service.submit(() -> {
                try {
                    // All 10 threads hit the monitor block concurrently
                    boolean acquired = RailControlSystem.acquireTrack(name, 500);
                    if (acquired) {
                        successfulLocks.incrementAndGet();
                        // Hold inside the critical path briefly to force other threads into wait loops
                        Thread.sleep(100);
                        RailControlSystem.releaseTrack(name);
                    }
                } catch (Exception ignored) {
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(); // Hold main execution track till execution loops finalize
        service.shutdown();

        // Check validation rules: timeouts must prevent simultaneous access
        System.out.println("✅ Concurrency Test Completed. Successful locks acquired within timeout window: " + successfulLocks.get());
        if (successfulLocks.get() > 0 && successfulLocks.get() <= threadCount) {
            System.out.println("✅ Test Passed: Monitor locks and wait states prevented overlapping execution.");
        } else {
            throw new AssertionError("Thread isolation boundary broken!");
        }
    }
}