package org.unizin.cmp.oai.harvester.service.db;

import java.util.Map;

import org.skife.jdbi.v2.DefaultMapper;
import org.skife.jdbi.v2.ResultSetMapperFactory;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

/** A factory that produces {@link CMPMapper} instances. */
@SuppressWarnings("rawtypes") // Alas, the interface specifies them.
public final class CMPMapperFactory implements ResultSetMapperFactory {
    DefaultMapper dm;
    private static final ResultSetMapper MAPPER = new CMPMapper();
    @Override
    public boolean accepts(final Class type, final StatementContext ctx) {
        return Map.class.isAssignableFrom(type);
    }

    @Override
    public ResultSetMapper mapperFor(final Class type,
            final StatementContext ctx) {
        return MAPPER ;
    }
}