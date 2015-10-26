package org.unizin.cmp.oai.mocks;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.message.BasicHttpResponse;
import org.unizin.cmp.oai.harvester.Utils;

public final class MockHttpResponse extends BasicHttpResponse {

	private String entityContent;
	
	public MockHttpResponse(final StatusLine statusline) {
		super(statusline);
	}
	
	public void setEntityContent(final InputStream in) throws IOException {
		final String string = Utils.fromStream(in);
		setEntityContent(string);
	}
	
	public void setEntityContent(final String string) {
		final HttpEntity entity = EntityBuilder.create()
				.setText(string)
				.setContentEncoding(StandardCharsets.UTF_8.toString())
				.build();
		setEntity(entity);
		entityContent = string;
	}
	
	public String getEntityContent() {
		return entityContent;
	}
}
