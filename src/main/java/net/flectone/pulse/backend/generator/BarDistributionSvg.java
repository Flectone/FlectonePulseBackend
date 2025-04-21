package net.flectone.pulse.backend.generator;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.Map;

public class BarDistributionSvg extends SvgGenerator {

    private static final int CORNER_RADIUS = 10;
    private static final int MIN_BAR_WIDTH = 30;
    private static final int MAX_BAR_WIDTH = 80;
    private static final double COLOR_STEP = 0.8;
    private static final int BAR_SPACING = 15;
    private static final int BOTTOM_MARGIN = 40;

    private final Map<String, Long> distribution;
    private final String valueLabel;

    public BarDistributionSvg(Map<String, Long> distribution,
                              String valueLabel) {
        this.distribution = distribution;
        this.valueLabel = valueLabel;
    }

    @Override
    protected void generateSvgContent() {
        if (distribution.isEmpty()) {
            return;
        }

        drawBars();
    }

    private void drawBars() {
        int barCount = distribution.size();
        int barWidth = calculateBarWidth(barCount);
        int totalWidth = barCount * barWidth + (barCount - 1) * BAR_SPACING;
        int startX = (dimensions.width() - totalWidth) / 2; // Центрирование

        long maxCount = getMaxCount();
        int index = 0;

        for (Map.Entry<String, Long> entry : distribution.entrySet()) {
            String ram = entry.getKey();
            long count = entry.getValue();
            int height = calculateBarHeight(count, maxCount);
            Color color = calculateColor(index++, distribution.size());

            drawSingleBar(startX, barWidth, height, ram, count, color);
            startX += barWidth + BAR_SPACING;
        }
    }

    private int calculateBarWidth(int barCount) {
        int availableWidth = dimensions.width() - 2 * dimensions.margin();
        int calculatedWidth = (availableWidth - (barCount - 1) * BAR_SPACING) / barCount;
        return Math.min(MAX_BAR_WIDTH, Math.max(MIN_BAR_WIDTH, calculatedWidth));
    }

    private void drawSingleBar(int x, int barWidth, int height, String ram, long count, Color color) {
        // Bar
        svg.setPaint(color);
        svg.fill(new RoundRectangle2D.Double(
                x, getBarYPosition(height),
                barWidth, height,
                CORNER_RADIUS, CORNER_RADIUS
        ));

        // Value
        svg.setFont(new Font("Segoe UI", Font.BOLD, 15));
        svg.setPaint(colors.text());
        String valueText = String.valueOf(count);
        svg.drawString(valueText,
                x + barWidth/2 - svg.getFontMetrics().stringWidth(valueText)/2,
                getBarYPosition(height) - 5);

        // Label
        svg.setFont(new Font("Segoe UI", Font.BOLD, 12));
        String label = ram + valueLabel;
        svg.drawString(label,
                x + barWidth/2 - svg.getFontMetrics().stringWidth(label)/2,
                dimensions.height() - dimensions.margin() - 15);
    }

    private int getBarYPosition(int height) {
        return dimensions.height() - dimensions.margin() - height - BOTTOM_MARGIN;
    }

    private int calculateBarHeight(long count, long maxCount) {
        return (int) ((double) count / maxCount *
                (dimensions.graphHeight() - BOTTOM_MARGIN));
    }

    private Color calculateColor(int index, int total) {
        float hue = (float) (COLOR_STEP * index / total);
        return Color.getHSBColor(hue, 0.7f, 0.8f);
    }

    private long getMaxCount() {
        return distribution.values().stream()
                .max(Long::compare)
                .orElse(1L);
    }
}