package com.trading.ui.cli;

/**
 * ANSI Escape Code utility for high-resolution terminal styling,
 * color schemes, box drawing, and formatted tables.
 */
public class AnsiConsole {
    public static final String RESET = "\u001B[0m";
    public static final String BOLD = "\u001B[1m";
    public static final String DIM = "\u001B[2m";
    public static final String ITALIC = "\u001B[3m";
    public static final String UNDERLINE = "\u001B[4m";

    // Text Colors
    public static final String BLACK = "\u001B[30m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String MAGENTA = "\u001B[35m";
    public static final String CYAN = "\u001B[36m";
    public static final String WHITE = "\u001B[37m";

    // Bright Colors
    public static final String BRIGHT_RED = "\u001B[91m";
    public static final String BRIGHT_GREEN = "\u001B[92m";
    public static final String BRIGHT_YELLOW = "\u001B[93m";
    public static final String BRIGHT_BLUE = "\u001B[94m";
    public static final String BRIGHT_MAGENTA = "\u001B[95m";
    public static final String BRIGHT_CYAN = "\u001B[96m";
    public static final String BRIGHT_WHITE = "\u001B[97m";

    // Background Colors
    public static final String BG_NAVY = "\u001B[44m";
    public static final String BG_DARK_GRAY = "\u001B[100m";
    public static final String BG_GREEN = "\u001B[42m";
    public static final String BG_RED = "\u001B[41m";

    public static void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    public static String green(String text) {
        return BRIGHT_GREEN + text + RESET;
    }

    public static String red(String text) {
        return BRIGHT_RED + text + RESET;
    }

    public static String cyan(String text) {
        return BRIGHT_CYAN + text + RESET;
    }

    public static String yellow(String text) {
        return BRIGHT_YELLOW + text + RESET;
    }

    public static String bold(String text) {
        return BOLD + text + RESET;
    }

    public static String colorPriceChange(double change, String text) {
        if (change > 0) {
            return BRIGHT_GREEN + "+" + text + RESET;
        } else if (change < 0) {
            return BRIGHT_RED + text + RESET;
        } else {
            return WHITE + text + RESET;
        }
    }

    public static String banner() {
        return BRIGHT_CYAN + BOLD +
                "========================================================================================\n" +
                "   ___ _   __  __ _____ ___ _____   ___ ___    _   ___ ___ _  _  ___   _   ___ ___  \n" +
                "  / __| | |  \\/  |_   _/ _ \\_   _| | _ \\ _ \\  /_\\ |   \\_ _| \\| |/ __| | | | _ \\ _ \\ \n" +
                "  \\__ \\ |_| |\\/| | | || (_) || |   |  _/   / / _ \\| |) | || .` | (_ | |_| |  _/  _/ \n" +
                "  |___/___|_|  |_| |_| \\___/ |_|   |_| |_|_\\/_/ \\_\\___/___|_|\\_|\\___| |___|_| |_|   \n" +
                "      >> Enterprise Quantitative Trading & Portfolio Simulation Engine v1.0.0 <<        \n" +
                "========================================================================================" + RESET;
    }
}
