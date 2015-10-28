package org.unizin.cmp.retrieval;

import com.google.common.io.CharStreams;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.io.DocumentPipe;
import org.nuxeo.ecm.core.io.DocumentReader;
import org.nuxeo.ecm.core.io.DocumentWriter;
import org.nuxeo.ecm.core.io.impl.DocumentPipeImpl;
import org.nuxeo.ecm.core.io.impl.plugins.DocumentModelWriter;
import org.nuxeo.ecm.core.io.impl.plugins.NuxeoArchiveReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

class RetrievalTestUtils {
    private RetrievalTestUtils() {}

    public static DocumentModel createTestDoc(InputStream archiveStream, CoreSession session) throws
            IOException {
        DocumentReader reader = new NuxeoArchiveReader(archiveStream);
        DocumentWriter writer = new DocumentModelWriter(session, "/");
        DocumentPipe pipe = new DocumentPipeImpl();
        pipe.setReader(reader);
        pipe.setWriter(writer);
        pipe.run();
        session.save();
        return session.getDocument(new PathRef("/Untitled.1444280484660"));
    }

    public static byte[] replacePort(InputStream input, String port) throws
            IOException {
        StringBuilder builder = new StringBuilder();
        InputStreamReader reader = new InputStreamReader(input);
        CharStreams.copy(reader, builder);
        // https://www.youtube.com/watch?v=7P0JM3h7IQk
        return builder.toString().replace(
                "localhost:INVALID", "localhost:" + port).getBytes();
    }
}
