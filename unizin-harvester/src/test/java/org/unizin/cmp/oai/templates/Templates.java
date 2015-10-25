package org.unizin.cmp.oai.templates;

import java.io.IOException;
import java.io.StringWriter;

import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * Freemarker template methods and objects used to test the harvester.
 *
 */
final class Templates {

	private static final Configuration CONFIGURATION = new Configuration(
			Configuration.getVersion());
	static {
		CONFIGURATION.setTemplateLoader(new ClassTemplateLoader(Templates.class,
				"/oai-response-templates/"));
	}

	static Template getOAITemplate(final String name) {
		try {
			return CONFIGURATION.getTemplate(templateName(name));
		} catch (final IOException e) {
			/*
			 * If this were production code, we'd want a separate exception
			 * type, but this'll do for test code.
			 */
			throw new RuntimeException(e);
		}
	}

	private static String templateName(final String basename) {
		return basename + ".ftl.xml";
	}

	static String processTemplate(final Template template,
			final Object dataModel) throws TemplateException, IOException {
		final StringWriter w = new StringWriter();
		template.process(dataModel, w);
		return w.toString();
	}
	
	/** No instances allowed. */
	private Templates() {}
}
