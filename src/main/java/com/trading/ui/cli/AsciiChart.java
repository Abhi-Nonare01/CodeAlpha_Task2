package com.trading.ui.cli;

import java.util.List;

/**
 * High-resolution ASCII line chart and sparkline renderer for terminal dashboards.
 */
public class AsciiChart {

    private static final String[] SPARKLINE_CHARS = {" ", "▂", "▃", "▄", "▅", "▆", "▇", "█"};

    public static String renderSparkline(List<Double> values) {
        if (values == null || values.isEmpty()) return "";
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        for (double v : values) {
            if (v < min) min = v;
            if (v > max) max = v;
        }

        double range = max - min;
        if (range <= 0.0001) range = 1.0;

        StringBuilder sb = new StringBuilder();
        for (double v : values) {
            int idx = (int) Math.round(((v - min) / range) * (SPARKLINE_CHARS.length - 1));
            idx = Math.max(0, Math.min(SPARKLINE_CHARS.length - 1, idx));
            sb.append(SPARKLINE_CHARS[idx]);
        }
        return sb.toString();
    }

    public static String renderLineChart(List<Double> series, int height, int width, String label) {
        if (series == null || series.isEmpty()) {
            return "No chart data available.";
        }

        // Resample series to width
        double[] sampled = new double[width];
        if (series.size() <= width) {
            int startIdx = width - series.size();
            for (int i = 0; i < startIdx; i++) {
                sampled[i] = series.get(0);
            }
            for (int i = 0; i < series.size(); i++) {
                sampled[startIdx + i] = series.get(i);
            }
        } else {
            double step = (double) series.size() / width;
            for (int i = 0; i < width; i++) {
                int srcIdx = (int) Math.min(series.size() - 1, i * step);
                sampled[i] = series.get(srcIdx);
            }
        }

        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        for (double v : sampled) {
            if (v < min) min = v;
            if (v > max) max = v;
        }

        double range = max - min;
        if (range <= 0.0001) range = 1.0;

        char[][] grid = new char[height][width];
        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width; c++) {
                grid[r][c] = ' ';
            }
        }

        for (int c = 0; c < width; c++) {
            int row = height - 1 - (int) Math.round(((sampled[c] - min) / range) * (height - 1));
            row = Math.max(0, Math.min(height - 1, row));
            grid[row][c] = '•';
        }

        StringBuilder sb = new StringBuilder();
        sb.append(AnsiConsole.CYAN).append("  --- ").append(label).append(" (High: $")
                .append(String.format("%.2f", max)).append(" | Low: $")
                .append(String.format("%.2f", min)).append(") ---\n").append(AnsiConsole.RESET);

        for (int r = 0; r < height; r++) {
            double yVal = max - ((double) r / (height - 1)) * range;
            sb.append(String.format("%8.2f ┤ ", yVal));
            for (int c = 0; c < width; c++) {
                char ch = grid[r][c];
                if (ch == '•') {
                    sb.append(AnsiConsole.BRIGHT_GREEN).append("█").append(AnsiConsole.RESET);
                } else {
                    sb.append(AnsiConsole.DIM).append("·").append(AnsiConsole.RESET);
                }
            }
            sb.append("\n");
        }

        sb.append("         └");
        for (int c = 0; c < width; c++) sb.append("─");
        sb.append(" [Time →]\n");

        return sb.toString();
    }
}
