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

public final class ListRecordsTemplate {
	private static final Template TEMPLATE = getOAITemplate("ListRecords");

	private final Map<String, Object> dataModel = new HashMap<>();
	{
		dataModel.put("records", new ArrayList<Object>());
	}

	public ListRecordsTemplate addRecord(final Map<String, Object> record) {
		@SuppressWarnings("unchecked")
		final List<Object> list = (List<Object>)dataModel.get("records");
		list.add(record);
		dataModel.put("records", list);
		return this;
	}
	
	public ListRecordsTemplate withResumptionToken(
			final Map<String, Object> resumptionToken) {
		dataModel.put("resumptionToken", resumptionToken);
		return this;
	}
	
	public ListRecordsTemplate withResponseDate(final String responseDate) {
		dataModel.put("responseDate", responseDate);
		return this;
	}
	
	public ListRecordsTemplate withMetadataPrefix(final String metadataPrefix) {
		dataModel.put("metadataPrefix", metadataPrefix);
		return this;
	}

	public String process() throws TemplateException, IOException {
		return processTemplate(TEMPLATE, dataModel);
	}
}
