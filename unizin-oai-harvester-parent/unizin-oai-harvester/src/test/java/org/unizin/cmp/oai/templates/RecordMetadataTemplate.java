package org.unizin.cmp.oai.templates;

import static org.unizin.cmp.oai.templates.Templates.getOAITemplate;
import static org.unizin.cmp.oai.templates.Templates.processTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import freemarker.template.Template;
import freemarker.template.TemplateException;

public final class RecordMetadataTemplate {
    private static final Template TEMPLATE = getOAITemplate("record-metadata");

    private final List<String> titles = new ArrayList<>();
    private final List<String> creators = new ArrayList<>();
    private final List<String> subjects = new ArrayList<>();
    private final List<String> dates = new ArrayList<>();
    private final List<String> descriptions = new ArrayList<>();


    public RecordMetadataTemplate addTitle(final String title) {
        titles.add(title);
        return this;
    }

    public RecordMetadataTemplate addCreator(final String creator) {
        creators.add(creator);
        return this;
    }

    public RecordMetadataTemplate addSubject(final String subject) {
        subjects.add(subject);
        return this;
    }

    public RecordMetadataTemplate addDescription(final String description) {
        descriptions.add(description);
        return this;
    }

    public RecordMetadataTemplate addDate(final String date) {
        dates.add(date);
        return this;
    }

    private Map<String, Object> build() {
        final Map<String, Object> m = new HashMap<>(5);
        m.put("titles", titles);
        m.put("creators", creators);
        m.put("subjects", subjects);
        m.put("descriptions", descriptions);
        m.put("dates", dates);
        return m;
    }

    public String process() throws TemplateException, IOException {
        return processTemplate(TEMPLATE, build());
    }
}
