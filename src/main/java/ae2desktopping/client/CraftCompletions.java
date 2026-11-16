package ae2desktopping.client;

import java.util.concurrent.ConcurrentLinkedQueue;

import appeng.api.storage.data.IAEStack;

/**
 * Hand-off from AE2's packet handler (which may run on the network thread) to the client thread.
 * The mixin only records what arrived and when; everything else happens in {@link ClientTicker}.
 */
public final class CraftCompletions {

    /** Bounds the queue if a server floods completion packets. */
    private static final int MAX_QUEUED = 64;

    public static final class Completion {

        public final IAEStack<?> stack;
        public final long count;
        public final long elapsedNanos;
        public final long arrivedAtNanos;

        Completion(IAEStack<?> stack, long count, long elapsedNanos, long arrivedAtNanos) {
            this.stack = stack;
            this.count = count;
            this.elapsedNanos = elapsedNanos;
            this.arrivedAtNanos = arrivedAtNanos;
        }
    }

    private static final ConcurrentLinkedQueue<Completion> QUEUE = new ConcurrentLinkedQueue<Completion>();

    private CraftCompletions() {}

    public static void post(IAEStack<?> stack, long count, long elapsedNanos) {
        if (stack == null || QUEUE.size() >= MAX_QUEUED) {
            return;
        }

        QUEUE.add(new Completion(stack.copy(), count, elapsedNanos, System.nanoTime()));
    }

    static Completion poll() {
        return QUEUE.poll();
    }
}
