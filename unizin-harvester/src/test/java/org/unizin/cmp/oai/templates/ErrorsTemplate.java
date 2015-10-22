package org.unizin.cmp.oai.templates;

import static org.unizin.cmp.oai.templates.Templates.getOAITemplate;
import static org.unizin.cmp.oai.templates.Templates.processTemplate;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.unizin.cmp.oai.OAIError;
import org.unizin.cmp.oai.OAIErrorCode;

import freemarker.template.Template;
import freemarker.template.TemplateException;


public final class ErrorsTemplate {
	private static final Template TEMPLATE = getOAITemplate("errors");

	private static final List<OAIError> DEFAULT_ERROR_LIST = 
			Collections.unmodifiableList(Arrays.asList(
					new OAIError(OAIErrorCode.BAD_ARGUMENT.code())));
	
	public static List<OAIError> defaultErrorList() {
		return DEFAULT_ERROR_LIST;
	}
	
	public static String process() throws TemplateException, IOException {
		return process(DEFAULT_ERROR_LIST, null);
	}
	
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
