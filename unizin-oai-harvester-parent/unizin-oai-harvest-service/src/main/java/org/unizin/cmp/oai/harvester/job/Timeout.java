package org.unizin.cmp.oai.harvester.job;

import java.util.concurrent.TimeUnit;


/**
 * Combination of time and {@link TimeUnit}.
 *
 */
public final class Timeout {
    private final long time;
    private final TimeUnit unit;


    public Timeout(final long time, final TimeUnit unit) {
        this.time = time;
        this.unit = unit;
    }

    public long getTime() {
        return time;
    }

    public TimeUnit getUnit() {
        return unit;
    }

    @Override
    public String toString() {
        return String.format("%d %s", time, unit);
    }
}
