package net.flectone.pulse.backend.generator;

import java.awt.*;
import java.awt.geom.Path2D;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TimeSeriesSvg extends SvgGenerator {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("EEE, dd MMM");
    private static final int CORNER_RADIUS = 24;
    private static final int Y_TICKS = 6;
    private static final float LINE_WIDTH = 2.5f;
    private static final int DOT_SIZE = 14;
    private static final int LEGEND_OFFSET = 100;

    private final Map<Instant, Map<Integer, Long>> firstData;
    private final Map<Instant, Map<Integer, Long>> secondData;
    private final List<Instant> sortedDates;
    private final String firstDataLabel;
    private final String secondDataLabel;
    private final long yMax;
    private final double hourWidth;
    private final int hoursInLastDay;

    public TimeSeriesSvg(Map<Instant, Map<Integer, Long>> firstData,
                         Map<Instant, Map<Integer, Long>> secondData,
                         List<Instant> sortedDates,
                         int hoursInLastDay,
                         String firstDataLabel,
                         String secondDataLabel) {
        this.firstDataLabel = firstDataLabel;
        this.secondDataLabel = secondDataLabel;
        this.firstData = firstData;
        this.secondData = secondData;
        this.sortedDates = sortedDates;
        this.hoursInLastDay = hoursInLastDay;

        long maxFirst = firstData.values().stream()
                .flatMapToLong(m -> m.values().stream().mapToLong(Long::longValue))
                .max().orElse(1);
        long maxSecond = secondData.values().stream()
                .flatMapToLong(m -> m.values().stream().mapToLong(Long::longValue))
                .max().orElse(1);
        this.yMax = Math.max(maxFirst, maxSecond);

        int totalDays = sortedDates.size();
        int totalHours = (totalDays - 1) * 24 + hoursInLastDay;
        this.hourWidth = (double) dimensions.graphWidth() / totalHours;
    }

    @Override
    protected void generateSvgContent() {
        drawBackground();
        drawGrid();
        drawChart();
        drawDateLabels();
        drawLegend();
    }

    private void drawBackground() {
        svg.setPaint(new Color(0, 0, 0, 0));
        svg.fillRoundRect(
                dimensions.margin() - 10,
                dimensions.margin() - 10,
                dimensions.graphWidth() + 20,
                dimensions.graphHeight() + 20,
                CORNER_RADIUS,
                CORNER_RADIUS
        );
    }

    private void drawGrid() {
        svg.setStroke(new BasicStroke(0.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        for (int i = 0; i <= Y_TICKS; i++) {
            int yPosition = calculateYPosition(i);
            drawGridLine(yPosition);
            drawGridLabel(yPosition, i);
        }
    }

    private int calculateYPosition(int tick) {
        return dimensions.margin() + dimensions.graphHeight() - (tick * dimensions.graphHeight() / Y_TICKS);
    }

    private void drawGridLine(int y) {
        svg.setPaint(colors.grid());
        svg.drawLine(
                dimensions.margin() - 30,
                y,
                dimensions.margin() + dimensions.graphWidth() - 10,
                y
        );
    }

    private void drawGridLabel(int y, int tick) {
        svg.setFont(new Font("Segoe UI", Font.BOLD, 15));
        String label = String.valueOf(tick * yMax / Y_TICKS);
        svg.setPaint(colors.text());
        svg.drawString(
                label,
                dimensions.margin() - 30 - svg.getFontMetrics().stringWidth(label),
                y + 4
        );
    }

    private void drawChart() {
        Path2D playersPath = createDataPath(firstData);
        Path2D serversPath = createDataPath(secondData);

        fillPath(playersPath, colors.primary());
        fillPath(serversPath, colors.secondary());

        strokePath(playersPath, colors.primary());
        strokePath(serversPath, colors.secondary());
    }

    private Path2D createDataPath(Map<Instant, Map<Integer, Long>> data) {
        Path2D.Double path = new Path2D.Double();
        path.moveTo(dimensions.margin(), dimensions.margin() + dimensions.graphHeight());

        double x = dimensions.margin();

        for (int dayIndex = 0; dayIndex < sortedDates.size(); dayIndex++) {
            Instant day = sortedDates.get(dayIndex);
            Map<Integer, Long> values = data.getOrDefault(day, Collections.emptyMap());

            int hoursInDay = 24;
            if (dayIndex == sortedDates.size() - 1) {
                hoursInDay = hoursInLastDay;
            }

            for (int hour = 0; hour < hoursInDay; hour++) {
                int totalHour = dayIndex * 24 + hour;
                x = calculateXPosition(totalHour);
                long value = values.getOrDefault(hour, 0L);
                int y = calculateYValue(value);

                if (totalHour == 0) {
                    path.lineTo(x, y);
                } else {
                    addCurveToPath(path, x, y);
                }
            }
        }

        closePath(path, x);
        return path;
    }

    private double calculateXPosition(int totalHour) {
        return Math.min(
                dimensions.margin() + hourWidth * totalHour,
                dimensions.margin() + dimensions.graphWidth()
        );
    }

    private int calculateYValue(long value) {
        return dimensions.margin() + dimensions.graphHeight() -
                (int) ((double) value / yMax * dimensions.graphHeight());
    }

    private void addCurveToPath(Path2D path, double x, double y) {
        double prevX = path.getCurrentPoint().getX();
        double prevY = path.getCurrentPoint().getY();

        path.curveTo(
                prevX + (x - prevX)/3, prevY,
                x - (x - prevX)/3, y,
                x, y
        );
    }

    private void closePath(Path2D path, double lastX) {
        path.lineTo(lastX, dimensions.margin() + dimensions.graphHeight());
        path.lineTo(dimensions.margin(), dimensions.margin() + dimensions.graphHeight());
        path.closePath();
    }

    private void fillPath(Path2D path, Color baseColor) {
        svg.setPaint(new Color(
                baseColor.getRed(),
                baseColor.getGreen(),
                baseColor.getBlue(),
                180
        ));
        svg.fill(path);
    }

    private void strokePath(Path2D path, Color color) {
        svg.setStroke(new BasicStroke(LINE_WIDTH, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        svg.setPaint(color);
        svg.draw(path);
    }

    private void drawDateLabels() {
        svg.setFont(new Font("Segoe UI", Font.BOLD, 15));

        for (int i = 0; i < sortedDates.size(); i++) {
            String label = formatDateLabel(sortedDates.get(i));
            double x = calculateLabelXPosition(i);

            svg.setPaint(colors.text());
            svg.drawString(
                    label,
                    (int) (x - (double) svg.getFontMetrics().stringWidth(label) / 2),
                    dimensions.margin() + dimensions.graphHeight() + 28
            );
        }
    }

    private String formatDateLabel(Instant date) {
        return DATE_FORMAT.format(LocalDateTime.ofInstant(date, ZoneOffset.UTC))
                .replace(".", "")
                .toUpperCase();
    }

    private double calculateLabelXPosition(int dayIndex) {
        return dimensions.margin() + hourWidth * (dayIndex * 24 + 12);
    }

    private void drawLegend() {
        int legendY = dimensions.margin() + dimensions.graphHeight() + 60;
        int startX = dimensions.margin() + (dimensions.graphWidth() / 2) - LEGEND_OFFSET;

        drawLegendItem(startX, legendY, colors.primary(), firstDataLabel, firstData);
        drawLegendItem(startX + 120, legendY, colors.secondary(), secondDataLabel, secondData);
    }

    private void drawLegendItem(int x, int y, Color color, String text, Map<Instant, Map<Integer, Long>> data) {
        svg.setPaint(color);
        svg.fillOval(x, y - DOT_SIZE/2, DOT_SIZE, DOT_SIZE);

        svg.setPaint(colors.text());

        Instant currentDay = sortedDates.get(sortedDates.size() - 1);

        int lastHour = 23;
        if (currentDay.equals(sortedDates.get(sortedDates.size() - 1))) {
            lastHour = hoursInLastDay > 0 ? hoursInLastDay - 1 : 23;
        }

        int count = Math.toIntExact(
                data.getOrDefault(currentDay, Collections.emptyMap())
                        .getOrDefault(lastHour, 0L)
        );

        svg.drawString(count + text, x + DOT_SIZE + 10, y + 5);
    }
}