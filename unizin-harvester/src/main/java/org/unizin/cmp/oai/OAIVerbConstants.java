package org.unizin.cmp.oai;

import static org.unizin.cmp.oai.OAIRequestParameter.FROM;
import static org.unizin.cmp.oai.OAIRequestParameter.IDENTIFIER;
import static org.unizin.cmp.oai.OAIRequestParameter.METADATA_PREFIX;
import static org.unizin.cmp.oai.OAIRequestParameter.RESUMPTION_TOKEN;
import static org.unizin.cmp.oai.OAIRequestParameter.SET;
import static org.unizin.cmp.oai.OAIRequestParameter.UNTIL;

import java.util.Arrays;
import java.util.List;

/**
 * We can't declare these in {@link OAIVerb}, because the compiler complains.
 * Thus we need a separate class to make the compiler happy.
 *
 */
final class OAIVerbConstants {
    /**
    * Parameters valid in {@code ListIdentifiers} and {@code ListRecords}
    * requests.
    */
    static final List<OAIRequestParameter> LIST_PARAMS = Arrays.asList(
            FROM, UNTIL, SET, RESUMPTION_TOKEN, METADATA_PREFIX
    );

    /**
    * Parameters valid in {@code GetRecord} requests.
    */
    static final List<OAIRequestParameter> GET_RECORD_PARAMS =
            Arrays.asList(IDENTIFIER, METADATA_PREFIX);

    /** No instances allowed. */
    private OAIVerbConstants() { }
}
