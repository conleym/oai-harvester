package org.unizin.cmp.harvester.agent;

import java.util.concurrent.TimeUnit;


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
}
