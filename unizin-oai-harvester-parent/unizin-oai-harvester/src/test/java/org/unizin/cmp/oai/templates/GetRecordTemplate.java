package org.unizin.cmp.oai.templates;

import static org.unizin.cmp.oai.templates.Templates.getOAITemplate;
import static org.unizin.cmp.oai.templates.Templates.processTemplate;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.HashMap;
import java.util.Map;

import freemarker.template.Template;
import freemarker.template.TemplateException;

public final class GetRecordTemplate {
    private static final Template TEMPLATE = getOAITemplate("GetRecord");

    private final Map<String, Object> dataModel = new HashMap<>();

    public GetRecordTemplate withIdentifier(final String identifier) {
        dataModel.put("identifier", identifier);
        return this;
    }

    public GetRecordTemplate withResponseDate(
            final TemporalAccessor responseDate) {
        dataModel.put("responseDate",
                DateTimeFormatter.ISO_INSTANT.format(responseDate));
        return this;
    }

    public String process() throws TemplateException, IOException {
        return processTemplate(TEMPLATE, dataModel);
    }
}
