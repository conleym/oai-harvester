package org.unizin.cmp.oai.harvester;

import static org.unizin.cmp.oai.harvester.Tests.newParams;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unizin.cmp.oai.OAIVerb;

/**
 * Minimal tests of harvest parameter validation.
 *
 */
public final class TestHarvestParams {
    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());


    private void assertValid(final String message,
            final HarvestParams params) {
        final boolean valid = params.areValid();
        if (! valid) {
            LOGGER.error("Unexpectedly invalid parameters: {}", params);
        }
        Assert.assertTrue(message, valid);
    }

    private void assertInvalid(final String message,
            final HarvestParams params) {
        final boolean valid = params.areValid();
        if (valid) {
            LOGGER.error("Unexpectedly valid parameters: {}", params);
        }
        Assert.assertFalse(message, valid);
    }

    @Test
    public void testValidation() {
        assertValid("Default parameters for testing should be valid.",
                newParams().build());
        assertInvalid("Identifier is not valid with ListRecords.",
                newParams(OAIVerb.LIST_RECORDS)
                    .withIdentifier("not valid")
                    .build());
        assertInvalid("Identifier is required with GetRecord.",
                newParams(OAIVerb.GET_RECORD)
                .build());
        assertValid("Nonstandard parameters are assumed to be valid.",
                newParams().withNonstandardParameter("FOO", "bar")
                .build());
        assertInvalid("Nonstandard parameters don't make invalid standard "
                + "parameters valid.",
                newParams(OAIVerb.GET_RECORD)
                    .withNonstandardParameter("FOO", "bar")
                    .build());
    }
}
