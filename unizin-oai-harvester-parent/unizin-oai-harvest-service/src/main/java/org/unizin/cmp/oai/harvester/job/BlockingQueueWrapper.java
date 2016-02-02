package org.unizin.cmp.oai.harvester.job;

import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;


/**
 * Wrapper around a blocking queue that exposes only {@code poll} and
 * {@code offer} with timeouts and throw exceptions.
 *
 * @param <T> the type of the blocking queue's elements.
 */
public final class BlockingQueueWrapper<T> {
    private final BlockingQueue<T> queue;
    private final Duration offerTimeout;
    private final Duration pollTimeout;


    public BlockingQueueWrapper(final BlockingQueue<T> queue,
            final Duration offerTimeout, final Duration pollTimeout) {
        this.queue = queue;
        this.offerTimeout = offerTimeout;
        this.pollTimeout = pollTimeout;
    }

    public T poll() throws InterruptedException {
        return queue.poll(pollTimeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    public boolean offer(final T t) throws InterruptedException {
        return queue.offer(t, offerTimeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    public int size() {
        return queue.size();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public Duration getOfferTimeout() {
        return offerTimeout;
    }

    public Duration getPollTimeout() {
        return pollTimeout;
    }
}
