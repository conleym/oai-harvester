package org.unizin.cmp.oai.mocks;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * An {@code InputStream} that forwards all operations to another
 * {@code InputStream}.
 *
 */
public abstract class ForwardingInputStream<T extends InputStream>
    extends InputStream {
    public static class BasicForwardingInputStream<T extends InputStream>
        extends ForwardingInputStream<T> {
        private final T delegate;
        public BasicForwardingInputStream(final T delegate) {
            Objects.requireNonNull(delegate, "delegate");
            this.delegate = delegate;
        }

        @Override
        protected final T delegate() { return delegate; }
    }

    protected abstract T delegate();

    @Override
    public int available() throws IOException {
        return delegate().available();
    }

    @Override
    public void close() throws IOException {
        delegate().close();
    }

    @Override
    public void mark(final int readlimit) {
        delegate().mark(readlimit);
    }

    @Override
    public boolean markSupported() {
        return delegate().markSupported();
    }

    @Override
    public int read() throws IOException {
        return delegate().read();
    }

    @Override
    public int read(final byte[] b, final int off, final int len)
    		throws IOException {
        return delegate().read(b, off, len);
    }

    @Override
    public int read(final byte[] b) throws IOException {
        return delegate().read(b);
    }

    @Override
    public void reset() throws IOException {
        delegate().reset();
    }

    @Override
    public long skip(final long n) throws IOException {
        return delegate().skip(n);
    }

    @Override
    public String toString() {
        return delegate().toString();
    }
}
