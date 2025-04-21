package net.flectone.pulse.backend.model;

import java.awt.*;

public record SvgColorPalette(Color primary, Color secondary, Color text, Color grid, Color enabled, Color disabled) {
    public static SvgColorPalette defaultPalette() {
        return new SvgColorPalette(
                new Color(100, 210, 255),
                new Color(120, 255, 105),
                new Color(240, 240, 245),
                new Color(70, 70, 80, 150),
                new Color(120, 255, 105),
                new Color(255, 150, 100)
        );
    }
}
