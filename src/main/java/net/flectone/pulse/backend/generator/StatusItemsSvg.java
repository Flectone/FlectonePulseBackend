package net.flectone.pulse.backend.generator;

import net.flectone.pulse.backend.model.SvgColorPalette;
import net.flectone.pulse.backend.model.SvgDimensions;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.*;
import java.util.List;

public class StatusItemsSvg extends SvgGenerator {

    private final Map<String, Long> itemsStats;
    private final long totalCount;
    private final String enabledLabel;
    private final String disabledLabel;
    private static final int CORNER_RADIUS = 12;
    private static final int ITEM_SPACING = 20;
    private static final int ITEM_WIDTH = 220;
    private static final int ITEM_HEIGHT = 60;

    public StatusItemsSvg(Map<String, Long> itemsStats, long totalCount, String enabledLabel, String disabledLabel) {
        super(new SvgDimensions(2400, 1500, 80), SvgColorPalette.defaultPalette());

        this.itemsStats = itemsStats;
        this.totalCount = totalCount;
        this.enabledLabel = enabledLabel;
        this.disabledLabel = disabledLabel;
    }

    @Override
    protected void generateSvgContent() {
        Color enabledColor = colors.enabled();
        Color disabledColor = colors.disabled();

        svg.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        List<String> itemList = new ArrayList<>(itemsStats.keySet());
        Collections.sort(itemList);

        int x = dimensions.margin();
        int y = dimensions.margin() + 40;

        for (String item : itemList) {
            long enabledCount = itemsStats.getOrDefault(item, 0L);
            double ratio = (double) enabledCount / totalCount;

            Color bgColor = blendColors(enabledColor, disabledColor, ratio);
            drawItemBackground(x, y, bgColor);
            drawItemCircle(x, y, bgColor);
            drawItemText(x, y, item, enabledCount, ratio);

            x += ITEM_WIDTH + ITEM_SPACING;
            if (x + ITEM_WIDTH > dimensions.width() - dimensions.margin()) {
                x = dimensions.margin();
                y += ITEM_HEIGHT + ITEM_SPACING;
            }
        }

        drawLegend(enabledColor, disabledColor, y + ITEM_HEIGHT);
    }

    private void drawItemBackground(int x, int y, Color bgColor) {
        svg.setPaint(new Color(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), 30));
        svg.fill(new RoundRectangle2D.Double(
                x, y, ITEM_WIDTH, ITEM_HEIGHT, CORNER_RADIUS, CORNER_RADIUS
        ));

        svg.setPaint(bgColor);
        svg.setStroke(new BasicStroke(1.5f));
        svg.draw(new RoundRectangle2D.Double(
                x, y, ITEM_WIDTH, ITEM_HEIGHT, CORNER_RADIUS, CORNER_RADIUS
        ));
    }

    private void drawItemCircle(int x, int y, Color bgColor) {
        int circleSize = 20;
        svg.setPaint(bgColor);
        svg.fillOval(
                x + 15,
                y + ITEM_HEIGHT/2 - circleSize/2,
                circleSize,
                circleSize
        );
    }

    private void drawItemText(int x, int y, String item, long enabledCount, double ratio) {
        svg.setPaint(colors.text());
        svg.setFont(new Font("Segoe UI", Font.BOLD, 12));
        svg.drawString(
                item.toUpperCase(),
                x + 45,
                y + 25
        );

        String status = String.format("%d/%d (%.0f%%)",
                enabledCount, totalCount, ratio * 100);
        svg.setFont(new Font("Segoe UI", Font.BOLD, 15));
        svg.drawString(
                status,
                x + 45,
                y + 40
        );
    }

    private Color blendColors(Color c1, Color c2, double ratio) {
        int red = (int) (c1.getRed() * ratio + c2.getRed() * (1 - ratio));
        int green = (int) (c1.getGreen() * ratio + c2.getGreen() * (1 - ratio));
        int blue = (int) (c1.getBlue() * ratio + c2.getBlue() * (1 - ratio));
        return new Color(red, green, blue);
    }

    private void drawLegend(Color enabledColor, Color disabledColor, int startY) {
        int legendX = dimensions.width()/2 - 100;
        int legendY = Math.min(startY + 30, dimensions.height() - dimensions.margin() - 30);

        int dotSize = 14;

        svg.setPaint(enabledColor);
        svg.fillOval(legendX, legendY - dotSize/2, dotSize, dotSize);
        svg.setPaint(colors.text());
        svg.drawString(enabledLabel, legendX + dotSize + 10, legendY + 5);

        svg.setPaint(disabledColor);
        svg.fillOval(legendX + 120, legendY - dotSize/2, dotSize, dotSize);
        svg.setPaint(colors.text());
        svg.drawString(disabledLabel, legendX + 120 + dotSize + 10, legendY + 5);
    }
}