package org.unizin.cmp.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;

public final class Handler {

    public String doIt(final DynamodbEvent event,
            final Context context) {
        final LambdaLogger logger = context.getLogger();
        logger.log(event.toString());
        event.getRecords().forEach((record) -> {
           logger.log(record.toString());
        });
        return "";
    }
}
