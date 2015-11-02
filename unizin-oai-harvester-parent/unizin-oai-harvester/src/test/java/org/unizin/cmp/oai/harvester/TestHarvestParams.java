package org.unizin.cmp.oai.harvester;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unizin.cmp.oai.OAIVerb;

/**
 * Minimal tests of harvest parameter validation.
 *
 */
public final class TestHarvestParams extends HarvesterTestBase {
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
                defaultTestParams());
        assertInvalid("Identifier is not valid with ListRecords.",
                defaultTestParams(OAIVerb.LIST_RECORDS)
                    .withIdentifier("not valid"));
        assertInvalid("Identifier is required with GetRecord.",
                defaultTestParams(OAIVerb.GET_RECORD));
        assertValid("Nonstandard parameters are assumed to be valid.",
                defaultTestParams().withNonstandardParameter("FOO", "bar"));
        assertInvalid("Nonstandard parameters don't make invalid standard parameters valid.",
                defaultTestParams(OAIVerb.GET_RECORD)
                    .withNonstandardParameter("FOO", "bar"));
    }
}
