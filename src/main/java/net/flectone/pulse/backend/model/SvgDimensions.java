package net.flectone.pulse.backend.model;

public record SvgDimensions(int width, int height, int margin, int graphWidth, int graphHeight) {

    public SvgDimensions(int width, int height, int margin) {
        this(width, height, margin, width - 2 * margin, height - 2 * margin);
    }

}
