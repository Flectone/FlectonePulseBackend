package net.flectone.pulse.backend.generator;

import java.awt.*;
import java.util.*;
import java.util.List;

public class CircleDistributionSvg extends SvgGenerator {

    private static final float STROKE_WIDTH = 2.5f;
    private static final int CIRCLE_PADDING = 20;
    private static final double COLOR_STEP = 0.8;
    private static final int MAX_PLACEMENT_ATTEMPTS = 100;
    private static final int BASE_MIN_RADIUS = 60;
    private static final int BASE_MAX_RADIUS = 100;
    private static final int MIN_CIRCLES_FOR_SCALING = 5;
    private static final int MAX_CIRCLES_FOR_SCALING = 15;

    private final List<DataCircle> circles = new ArrayList<>();
    private final String labelSuffix;
    private final String valueSuffix;
    private final boolean showPercentage;

    private static class DataCircle {
        private final String label;
        private final long value;
        private final double ratio;
        private final int radius;
        private final Color fillColor;
        private final Color borderColor;

        private Point position;

        DataCircle(String label, long value, double ratio, Color fillColor, int minRadius, int maxRadius) {
            this.label = label;
            this.value = value;
            this.ratio = ratio;
            this.fillColor = fillColor;
            this.borderColor = brightenColor(fillColor, 40);
            this.radius = calculateRadius(ratio, minRadius, maxRadius);
        }

        private int calculateRadius(double ratio, int minRadius, int maxRadius) {
            return minRadius + (int)((maxRadius-minRadius)*ratio);
        }

        private static Color brightenColor(Color color, int amount) {
            return new Color(
                    Math.min(color.getRed() + amount, 255),
                    Math.min(color.getGreen() + amount, 255),
                    Math.min(color.getBlue() + amount, 255)
            );
        }
    }

    public CircleDistributionSvg(Map<String, Long> data, String labelSuffix, String valueSuffix, boolean showPercentage) {
        this.labelSuffix = labelSuffix;
        this.valueSuffix = valueSuffix;
        this.showPercentage = showPercentage;

        int count = data.size();
        int minRadius = calculateDynamicRadius(BASE_MIN_RADIUS, count);
        int maxRadius = calculateDynamicRadius(BASE_MAX_RADIUS, count);

        long total = data.values().stream().mapToLong(Long::longValue).sum();

        for (Map.Entry<String, Long> entry : data.entrySet()) {
            double ratio = total > 0 ? (double)entry.getValue() / total : 0;

            circles.add(new DataCircle(
                    entry.getKey(),
                    entry.getValue(),
                    ratio,
                    generateColor(circles.size(), count),
                    minRadius,
                    maxRadius
            ));
        }

        circles.sort(Comparator.comparingLong((DataCircle c) -> c.value).reversed());
        arrangeCircles();
    }

    public CircleDistributionSvg(Map<String, Long> data) {
        this(data, "", "", false);
    }

    public CircleDistributionSvg(Map<String, Long> data, String valueSuffix, boolean showPercentage) {
        this(data, "", valueSuffix, showPercentage);
    }

    private int calculateDynamicRadius(int baseRadius, int circleCount) {
        // Уменьшаем размеры, если элементов много, увеличиваем если мало
        if (circleCount > MAX_CIRCLES_FOR_SCALING) {
            return (int)(baseRadius * 0.7);
        } else if (circleCount < MIN_CIRCLES_FOR_SCALING) {
            return (int)(baseRadius * 1.3);
        }
        return baseRadius;
    }

    private Color generateColor(int index, int total) {
        float hue = (float) (COLOR_STEP * index / total);
        return Color.getHSBColor(hue, 0.7f, 0.8f);
    }

    private void arrangeCircles() {
        if (circles.isEmpty()) return;

        int centerX = dimensions.width()/2;
        int centerY = dimensions.height()/2;

        // Размещаем самый большой круг в центре
        circles.get(0).position = new Point(centerX, centerY);

        // Размещаем остальные круги по спирали
        double angle = 0;
        double radius = circles.get(0).radius + CIRCLE_PADDING;

        for (int i = 1; i < circles.size(); i++) {
            DataCircle circle = circles.get(i);
            Point position = findSuitablePosition(i, centerX, centerY, angle, radius);

            if (position != null) {
                circle.position = position;
            } else {
                // Если не удалось разместить по спирали, используем случайное положение
                circle.position = findFallbackPosition(i, centerX, centerY);
            }
        }
    }

    private Point findSuitablePosition(int index, int centerX, int centerY, double angle, double radius) {
        DataCircle current = circles.get(index);
        double angleStep = Math.PI/6;
        double radiusStep = 1.2;

        for (int attempt = 0; attempt < MAX_PLACEMENT_ATTEMPTS; attempt++) {
            int x = centerX + (int)(radius * Math.cos(angle));
            int y = centerY + (int)(radius * Math.sin(angle));

            if (!hasOverlaps(index, x, y, current.radius) && isWithinBounds(x, y, current.radius)) {
                return new Point(x, y);
            }

            angle += angleStep;
            radius += radiusStep;
        }

        return null;
    }

    private boolean hasOverlaps(int index, int x, int y, int radius) {
        return circles.stream()
                .limit(index)
                .anyMatch(other -> {
                    double distance = Math.hypot(x - other.position.x, y - other.position.y);
                    return distance < radius + other.radius + CIRCLE_PADDING;
                });
    }

    private boolean isWithinBounds(int x, int y, int radius) {
        return x - radius >= 0 && x + radius <= dimensions.width() &&
                y - radius >= 0 && y + radius <= dimensions.height();
    }

    private Point findFallbackPosition(int index, int centerX, int centerY) {
        DataCircle current = circles.get(index);
        Point bestPosition = new Point(centerX, centerY);
        double bestScore = Double.MAX_VALUE;

        for (int i = 0; i < 50; i++) {
            int x = CIRCLE_PADDING + (int)(Math.random() * (dimensions.width() - 2*CIRCLE_PADDING));
            int y = CIRCLE_PADDING + (int)(Math.random() * (dimensions.height() - 2*CIRCLE_PADDING));

            double score = calculatePositionScore(index, x, y, current.radius);

            if (score < bestScore) {
                bestScore = score;
                bestPosition = new Point(x, y);
            }
        }

        return bestPosition;
    }

    private double calculatePositionScore(int index, int x, int y, int radius) {
        double score = circles.stream()
                .limit(index)
                .mapToDouble(other -> {
                    double distance = Math.hypot(x - other.position.x, y - other.position.y);
                    double minDistance = radius + other.radius + CIRCLE_PADDING;
                    return Math.max(0, minDistance - distance);
                })
                .sum();

        if (!isWithinBounds(x, y, radius)) {
            score += 1000;
        }

        return score;
    }

    @Override
    protected void generateSvgContent() {
        circles.forEach(this::drawCircle);
        circles.forEach(this::drawLabel);
    }

    private void drawCircle(DataCircle circle) {
        GradientPaint gradient = new GradientPaint(
                circle.position.x - circle.radius/2f,
                circle.position.y - circle.radius/2f,
                circle.fillColor,
                circle.position.x + circle.radius/2f,
                circle.position.y + circle.radius/2f,
                DataCircle.brightenColor(circle.fillColor, 30)
        );

        svg.setPaint(gradient);
        svg.fillOval(
                circle.position.x - circle.radius,
                circle.position.y - circle.radius,
                circle.radius*2,
                circle.radius*2
        );

        svg.setPaint(circle.borderColor);
        svg.setStroke(new BasicStroke(STROKE_WIDTH));
        svg.drawOval(
                circle.position.x - circle.radius,
                circle.position.y - circle.radius,
                circle.radius*2,
                circle.radius*2
        );
    }

    private void drawLabel(DataCircle circle) {
        String label = abbreviateLabel(circle.label);
        String valueText = formatValueText(circle);
        int fontSize = calculateFontSize(circle.radius);

        drawTextWithShadow(concatenate(label, labelSuffix), circle.position.x, circle.position.y - 8, fontSize);
        drawTextWithShadow(valueText, circle.position.x, circle.position.y + 10, fontSize);
    }

    private String concatenate(String first, String second) {
        return second == null || second.isEmpty() ? first : first + second;
    }

    private String abbreviateLabel(String label) {
        return label.length() > 10 ? label.substring(0, 7) + "..." : label;
    }

    private String formatValueText(DataCircle circle) {
        String value = valueSuffix == null || valueSuffix.isEmpty() ? "" : circle.value + valueSuffix;

        if (showPercentage) {
            return (value.isEmpty() ? "" : value + " ") + String.format("%.1f%%", circle.ratio*100);
        }

        return value;
    }

    private int calculateFontSize(int radius) {
        return Math.max(10, Math.min(20, radius/3));
    }

    private void drawTextWithShadow(String text, int x, int y, int fontSize) {
        svg.setFont(new Font("Segoe UI", Font.BOLD, fontSize));
        int textWidth = svg.getFontMetrics().stringWidth(text);

        svg.setPaint(new Color(0, 0, 0, 120));
        svg.drawString(text, x - textWidth/2 + 1, y + 1);

        svg.setPaint(Color.WHITE);
        svg.drawString(text, x - textWidth/2, y);
    }
}