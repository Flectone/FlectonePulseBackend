package net.flectone.pulse.backend.generator;

import net.flectone.pulse.backend.model.SvgColorPalette;
import net.flectone.pulse.backend.model.SvgDimensions;
import net.flectone.pulse.backend.util.SvgUtils;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.batik.svggen.SVGGraphics2DIOException;
import org.w3c.dom.Document;

import java.awt.*;
import java.io.StringWriter;

public abstract class SvgGenerator {

    protected final SVGGraphics2D svg;
    protected final Document doc;
    protected final SvgDimensions dimensions;
    protected final SvgColorPalette colors;

    public SvgGenerator(SvgDimensions dimensions, SvgColorPalette colors) {
        this.dimensions = dimensions;
        this.colors = colors;
        this.doc = SvgUtils.createSvgDocument(dimensions.width(), dimensions.height());
        this.svg = new SVGGraphics2D(doc);

        svg.setSVGCanvasSize(new Dimension(dimensions.width(), dimensions.height()));
        svg.setFont(new Font("Segoe UI", Font.PLAIN, 12));
    }

    public SvgGenerator() {
        this(new SvgDimensions(1200, 600, 80), SvgColorPalette.defaultPalette());
    }

    protected abstract void generateSvgContent();

    public String generate() throws SVGGraphics2DIOException {
        generateSvgContent();

        StringWriter writer = new StringWriter();
        svg.stream(writer, true);
        return writer.toString();
    }
}