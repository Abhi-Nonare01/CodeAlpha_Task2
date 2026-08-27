package com.trading.ui.web;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;

/**
 * Utility to reliably load and provide HTML/CSS/JS assets for the Web Dashboard.
 */
public class WebStaticAssets {

    public static String getIndexHtml() {
        // Strategy 1: Check local project file (development mode)
        File localFile = new File("src/main/resources/web/index.html");
        if (localFile.exists()) {
            try {
                return Files.readString(localFile.toPath());
            } catch (Exception ignored) {}
        }

        // Strategy 2: Check ClassLoader resource
        try (InputStream is = WebStaticAssets.class.getResourceAsStream("/web/index.html")) {
            if (is != null) {
                return new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {}

        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("web/index.html")) {
            if (is != null) {
                return new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {}

        // Strategy 3: Check target/classes
        File targetFile = new File("target/classes/web/index.html");
        if (targetFile.exists()) {
            try {
                return Files.readString(targetFile.toPath());
            } catch (Exception ignored) {}
        }

        return getEmbeddedHtml();
    }

    private static String getEmbeddedHtml() {
        return "<!DOCTYPE html><html><head><title>QuantumTrade Pro</title><meta http-equiv='refresh' content='1'></head><body style='background:#0f172a;color:#fff;font-family:sans-serif;text-align:center;padding-top:20vh;'><h2>QuantumTrade Pro is Initializing...</h2><p>Please refresh the page in a moment.</p></body></html>";
    }
}
