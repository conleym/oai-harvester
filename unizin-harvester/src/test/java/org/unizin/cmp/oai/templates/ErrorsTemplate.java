package org.unizin.cmp.oai.templates;

import static org.unizin.cmp.oai.templates.Templates.getOAITemplate;
import static org.unizin.cmp.oai.templates.Templates.processTemplate;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.unizin.cmp.oai.OAIError;

import freemarker.template.Template;
import freemarker.template.TemplateException;


public final class ErrorsTemplate {
	private static final Template TEMPLATE = getOAITemplate("errors");

	public static String process(final List<OAIError> errors)
			throws TemplateException, IOException {
		return process(errors, null);
	}

	public static String process(final List<OAIError> errors,
			final String responseDate) 
					throws TemplateException, IOException {
		final Map<String, Object> dataModel = new HashMap<>(2);
		dataModel.put("errors", errors);
		dataModel.put("responseDate", responseDate);
		return processTemplate(TEMPLATE, dataModel);
	}
	
	/** No instances allowed. */
	private ErrorsTemplate() {}
}
