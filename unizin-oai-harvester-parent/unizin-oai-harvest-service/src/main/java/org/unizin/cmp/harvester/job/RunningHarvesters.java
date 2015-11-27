package org.unizin.cmp.harvester.job;

import java.util.HashSet;
import java.util.Set;

import org.unizin.cmp.oai.harvester.Harvester;

final class RunningHarvesters {
    private final Object lock = new Object();
    private final Set<Harvester> harvesters = new HashSet<>();

    private void add(final Harvester harvester) {
        synchronized(lock) {
            harvesters.add(harvester);
        }
    }

    boolean remove(final Harvester harvester) {
        synchronized(lock) {
            return harvesters.remove(harvester);
        }
    }

    /**
     * Are there any running harvests?
     *
     * @return {@code true} iff there are no running harvests.
     */
    boolean isEmpty() {
        synchronized(lock) {
            return harvesters.isEmpty();
        }
    }

    /** Clear the running harvests. */
    void clear() {
        synchronized(lock) {
            harvesters.clear();
        }
    }

    /**
     * Stop all running harvests.
     */
    void stopAll() {
        synchronized(lock) {
            harvesters.forEach(Harvester::stop);
        }
    }

    /**
     * Cancel all running harvests.
     */
    void cancelAll() {
        synchronized(lock) {
            harvesters.forEach(Harvester::cancel);
        }
    }

    /**
     * Create and return a {@code Runnable} for a managed harvest.
     * <p>
     * This method first adds the given key-harvester pair to itself, then
     * produces a new {@code Runnable} that executes the given {@code Runnable}.
     * When its execution completes, the key-harvester pair will be removed.
     *
     * @param harvester
     *            the harvester itself.
     * @param runnable
     *            arbitrary code to run. The most logical use is to run a
     *            harvest, but clients can do anything.
     * @return a {@code Runnable} as described above.
     */
    Runnable wrappedRunnable(final Harvester harvester, final Runnable runnable) {
        add(harvester);
        return () -> {
            try {
                runnable.run();
            } finally {
                remove(harvester);
            }
        };
    }
}
