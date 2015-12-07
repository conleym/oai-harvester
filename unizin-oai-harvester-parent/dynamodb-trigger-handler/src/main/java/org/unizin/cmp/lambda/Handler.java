package org.unizin.cmp.lambda;

import java.util.List;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent.DynamodbStreamRecord;

public final class Handler {

    public String doIt(final DynamodbEvent event,
            final Context context) {
        final LambdaLogger logger = context.getLogger();
        if (event == null) {
            return "Null event.";
        }
        final List<DynamodbStreamRecord> records = event.getRecords();
        if (records == null) {
            return "Null record list.";
        }
        records.forEach((record) -> {
            if (logger != null) {
                if (record == null) {
                    logger.log("Record is null.");
                } else {
                    logger.log(record.toString());
                }
            }
        });
        return "OK!";
    }
}
