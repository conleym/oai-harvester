package org.unizin.cmp.oai.harvester.service.db;

import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.HashPrefixStatementRewriter;
import org.skife.jdbi.v2.tweak.StatementRewriter;

/**
 * DBI utilities.
 */
public final class DBIUtils {
    private static final StatementRewriter REWRITER =
            new HashPrefixStatementRewriter();

    public static Handle handle(final DBI dbi) {
        final Handle h = dbi.open();
        h.setStatementRewriter(REWRITER);
        return h;
    }

    public static JobJDBI jobDBI(final DBI dbi) {
        return dbi.open(JobJDBI.class);
    }


    /** No instances allowed. */
    private DBIUtils() { }
}
