package net.flectone.pulse.backend.util;

import org.apache.batik.dom.GenericDOMImplementation;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.awt.*;

public class SvgUtils {
    public static Document createSvgDocument(int width, int height) {
        DOMImplementation domImpl = GenericDOMImplementation.getDOMImplementation();
        Document doc = domImpl.createDocument("http://www.w3.org/2000/svg", "svg", null);
        Element svgRoot = doc.getDocumentElement();
        svgRoot.setAttribute("width", String.valueOf(width));
        svgRoot.setAttribute("height", String.valueOf(height));
        return doc;
    }

    public static Color[] getDefaultColors() {
        return new Color[] {
                new Color(100, 210, 255),
                new Color(120, 255, 105),
                new Color(255, 150, 100),
                new Color(200, 120, 255),
                new Color(255, 220, 100)
        };
    }
}
