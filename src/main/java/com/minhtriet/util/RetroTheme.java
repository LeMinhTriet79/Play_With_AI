package com.minhtriet.util;

import javax.swing.UIManager;
import java.awt.Font;

public final class RetroTheme {
    private RetroTheme() {}

    public static void installClassicWindowsLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            Font winFont = new Font("MS Sans Serif", Font.PLAIN, 12);
            java.util.Enumeration<Object> keys = UIManager.getDefaults().keys();
            while (keys.hasMoreElements()) {
                Object key = keys.nextElement();
                Object value = UIManager.get(key);
                if (value instanceof javax.swing.plaf.FontUIResource) {
                    UIManager.put(key, winFont);
                }
            }
        } catch (Exception ignored) {}
    }
}
