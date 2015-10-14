package org.unizin.cmp.retrieve;

import org.ccil.cowan.tagsoup.Parser;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import java.io.IOException;

public class HtmlDocumentBuilder extends DocumentBuilder {

    @Override
    public Document parse(InputSource inputSource) throws
            SAXException, IOException {

        Parser parser = new Parser();
        parser.setFeature(Parser.namespacesFeature, false);
        parser.setFeature(Parser.namespacePrefixesFeature, false);
        parser.setFeature(Parser.ignoreBogonsFeature, true);
        try {
            SAXTransformerFactory stf = (SAXTransformerFactory) TransformerFactory.newInstance();
            TransformerHandler handler = stf.newTransformerHandler();
            DOMResult domResult = new DOMResult();
            handler.setResult(domResult);
            parser.setContentHandler(handler);
            parser.parse(inputSource);
            return (Document) domResult.getNode();
        } catch (TransformerException e) {
            throw new SAXException(e);
        }

    }

    @Override
    public void setErrorHandler(ErrorHandler errorHandler) {

    }

    @Override
    public Document newDocument() {
        return null;
    }

    @Override
    public void setEntityResolver(EntityResolver entityResolver) {

    }

    @Override
    public boolean isValidating() {
        return false;
    }

    @Override
    public DOMImplementation getDOMImplementation() {
        return null;
    }

    @Override
    public boolean isNamespaceAware() {
        return false;
    }
}
