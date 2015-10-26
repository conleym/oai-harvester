package org.unizin.cmp.oai.mocks;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.message.BasicHttpResponse;

public final class MockHttpResponse extends BasicHttpResponse {

    private String entityContent;

    public MockHttpResponse(final StatusLine statusline) {
        super(statusline);
    }

    /**
    * Set the response's entity's content to the given stream.
    * <p>
    * Note that, when using this method, the entity content is not available
    * via {@link #getEntityContent()}.
    *
    * @param in
    *            the stream containing the entity's content.
    */
    public void setEntityContent(final InputStream in) {
        final HttpEntity entity = EntityBuilder.create()
                .setStream(in)
                .setContentEncoding(StandardCharsets.UTF_8.toString())
                .build();
        setEntity(entity);
    }

    /**
    * Set the response's entity's content to the given string.
    * <p>
    * The content will also be available via {@link #getEntityContent()} for
    * later comparison and verification.
    *
    * @param string
    *            the string containing the entity's content.
    */
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
