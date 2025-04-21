package net.flectone.pulse.backend.util;

import org.apache.batik.dom.GenericDOMImplementation;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class SvgUtils {
    public static Document createSvgDocument(int width, int height) {
        DOMImplementation domImpl = GenericDOMImplementation.getDOMImplementation();
        Document doc = domImpl.createDocument("http://www.w3.org/2000/svg", "svg", null);
        Element svgRoot = doc.getDocumentElement();
        svgRoot.setAttribute("width", String.valueOf(width));
        svgRoot.setAttribute("height", String.valueOf(height));
        return doc;
    }

}
