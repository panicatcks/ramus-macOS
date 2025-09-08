package com.ramussoft.gui.common;

import java.awt.Toolkit;
import java.lang.reflect.Method;

public final class PlatformUtils {
    private PlatformUtils() {}

    public static boolean isMac() {
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("mac");
    }

    /**
     * Returns the platform's menu shortcut modifier mask.
     * On macOS this maps to Command, on others – Control.
     * Compatible with Java 8 and later.
     */
    @SuppressWarnings("deprecation")
    public static int getMenuShortcutKeyMask() {
        try {
            // Try Java 9+ getMenuShortcutKeyMaskEx via reflection to avoid compile-time dependency
            Method m = Toolkit.class.getMethod("getMenuShortcutKeyMaskEx");
            Object v = m.invoke(Toolkit.getDefaultToolkit());
            if (v instanceof Integer) return (Integer) v;
        } catch (Throwable ignored) {
            // Fallback to Java 8 method
        }
        return Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
    }
}

