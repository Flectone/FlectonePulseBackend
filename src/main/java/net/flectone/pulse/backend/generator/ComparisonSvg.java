package net.flectone.pulse.backend.generator;

import org.springframework.data.util.Pair;

import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class ComparisonSvg extends SvgGenerator {

    private static final int BAR_WIDTH = 30;
    private static final int BAR_GAP = 15;
    private static final int GROUP_GAP = 40;
    private static final int MIN_BAR_HEIGHT = 1;
    private static final int LEGEND_SIZE = 15;
    private static final int ARC_RADIUS = 8;
    private static final int SIDE_MARGIN = 50;

    private final Map<String, Pair<Long, Long>> data;
    private final String firstDataLabel;
    private final String secondDataLabel;
    private double scaleFactor = 1.0;

    public ComparisonSvg(Map<String, Pair<Long, Long>> data, String firstDataLabel, String secondDataLabel) {
        this.data = data.entrySet().stream()
                .sorted((e1, e2) -> Long.compare(
                        e2.getValue().getFirst() + e2.getValue().getSecond(),
                        e1.getValue().getFirst() + e1.getValue().getSecond()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, i) -> e1,
                        LinkedHashMap::new));
        this.firstDataLabel = firstDataLabel;
        this.secondDataLabel = secondDataLabel;
    }

    @Override
    protected void generateSvgContent() {
        long maxPlayers = data.values().stream()
                .mapToLong(Pair::getFirst)
                .max().orElse(1);

        long maxServers = data.values().stream()
                .mapToLong(Pair::getSecond)
                .max().orElse(1);

        int requiredWidth = calculateRequiredWidth();
        int availableWidth = dimensions.width() - 2 * SIDE_MARGIN;

        if (requiredWidth > availableWidth) {
            scaleFactor = (double) availableWidth / requiredWidth;
        }

        svg.setFont(new Font("Segoe UI", Font.BOLD, 15));

        int chartWidth = (int)(calculateChartWidth() * scaleFactor);
        int startX = (dimensions.width() - chartWidth) / 2;

        drawBars(maxPlayers, maxServers, startX);
        drawLegend();
    }

    private int calculateRequiredWidth() {
        return calculateChartWidth();
    }

    private int calculateChartWidth() {
        return data.size() * (2 * BAR_WIDTH + BAR_GAP + GROUP_GAP) - GROUP_GAP;
    }

    private void drawBars(long maxFirstValue, long maxSecondValue, int startX) {
        int x = startX;
        int scaledBarWidth = (int)(BAR_WIDTH * scaleFactor);
        int scaledBarGap = (int)(BAR_GAP * scaleFactor);
        int scaledGroupGap = (int)(GROUP_GAP * scaleFactor);

        for (var entry : data.entrySet()) {
            long firstValue = entry.getValue().getFirst();
            long secondValue = entry.getValue().getSecond();

            drawBar(x, calculateBarHeight(firstValue, maxFirstValue), firstValue, colors.primary(), scaledBarWidth);
            drawBar(x + scaledBarWidth + scaledBarGap, calculateBarHeight(secondValue, maxSecondValue), secondValue, colors.secondary(), scaledBarWidth);
            drawLabel(entry.getKey(), x + scaledBarWidth + (scaledBarGap / 2));

            x += 2 * scaledBarWidth + scaledBarGap + scaledGroupGap;
        }
    }

    private void drawBar(int x, int height, long value, Color color, int width) {
        int y = dimensions.margin() + dimensions.graphHeight() - height;

        svg.setPaint(color);
        svg.fillRoundRect(x, y, width, height, ARC_RADIUS, ARC_RADIUS);

        svg.setPaint(color.darker());
        svg.setStroke(new BasicStroke(1f));
        svg.drawRoundRect(x, y, width, height, ARC_RADIUS, ARC_RADIUS);

        svg.setPaint(color.darker());
        svg.drawString(
                String.valueOf(value),
                x + width/2 - svg.getFontMetrics().stringWidth(String.valueOf(value))/2,
                y - 5
        );
    }

    private int calculateBarHeight(long value, long maxValue) {
        return Math.max((int)((double)value / maxValue * dimensions.graphHeight()), MIN_BAR_HEIGHT);
    }

    private void drawLabel(String text, int x) {
        String label = text.length() > 12 ? text.substring(0, 9) + "..." : text;
        int textWidth = svg.getFontMetrics().stringWidth(label);

        svg.setPaint(colors.text());
        svg.drawString(label, x - textWidth/2, dimensions.height() - dimensions.margin() + 20);
    }

    private void drawLegend() {
        int y = dimensions.margin() + dimensions.graphHeight() + 40;
        int centerX = dimensions.width() / 2;

        drawLegendItem(centerX - 100, y, colors.primary(), firstDataLabel);
        drawLegendItem(centerX + 30, y, colors.secondary(), secondDataLabel);
    }

    private void drawLegendItem(int x, int y, Color color, String text) {
        svg.setPaint(color);
        svg.fillRect(x, y, LEGEND_SIZE, LEGEND_SIZE);

        svg.setPaint(colors.text());
        svg.drawString(text, x + LEGEND_SIZE + 5, y + LEGEND_SIZE - 3);
    }
}