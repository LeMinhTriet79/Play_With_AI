package com.minhtriet;

import com.minhtriet.controller.ChatController;
import com.minhtriet.service.AiChatService;
import com.minhtriet.service.DatabaseService;
import com.minhtriet.util.RetroTheme;
import com.minhtriet.view.MainWindow;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "off");
        System.setProperty("swing.aatext", "false");

        SwingUtilities.invokeLater(() -> {
            RetroTheme.installClassicWindowsLookAndFeel();

            MainWindow view = new MainWindow();
            AiChatService aiService = new AiChatService(MainWindow.DEFAULT_MODEL);
            DatabaseService dbService = new DatabaseService();

            // Xóa bỏ truyền ApiKeyProvider. ChatController giờ sẽ tự gọi DbService để xin Key.
            new ChatController(view, aiService, dbService);

            view.showWindow();
        });
    }
}