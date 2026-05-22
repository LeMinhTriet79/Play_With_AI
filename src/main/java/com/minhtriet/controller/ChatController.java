package com.minhtriet.controller;

import com.minhtriet.model.ChatMessage;
import com.minhtriet.model.ChatSession;
import com.minhtriet.service.AiChatService;
import com.minhtriet.service.DatabaseService;
import com.minhtriet.util.MarkdownParser;
import com.minhtriet.view.MainWindow;

import javax.swing.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ChatController {
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String DEFAULT_SESSION_PREFIX = "Đoạn chat mới: ";

    private final MainWindow view;
    private final AiChatService aiService;
    private final DatabaseService dbService;
    private volatile SwingWorker<String, Void> worker;

    private ChatSession currentSession;

    public ChatController(MainWindow view, AiChatService aiService, DatabaseService dbService) {
        this.view = view;
        this.aiService = aiService;
        this.dbService = dbService;

        // Chat Callbacks
        this.view.setOnSend(this::handleSend);
        this.view.setOnClear(this::handleClear);
        this.view.setOnNewSession(this::handleNewSession);
        this.view.setOnSessionSelected(this::handleSessionSelected);

        // Callbacks Mới: Xóa và Đổi tên
        this.view.setOnRenameSession(this::handleRenameSession);
        this.view.setOnDeleteSession(this::handleDeleteSession);

        // Settings Callbacks
        this.view.setOnAddKey(this::handleAddKey);
        this.view.setOnDeleteKey(this::handleDeleteKey);
        this.view.setOnSetActiveKey(this::handleSetActiveKey);

        loadInitialData();
        refreshApiKeysView();
    }

    private void refreshSessionList() {
        List<ChatSession> sessions = dbService.getAllSessions();
        view.renderSessionList(sessions);
    }

    private void loadInitialData() {
        refreshSessionList();
        List<ChatSession> sessions = dbService.getAllSessions();
        if (sessions.isEmpty()) {
            handleNewSession();
        } else {
            view.selectFirstSession();
        }
    }

    private void refreshApiKeysView() {
        view.renderApiKeys(dbService.getAllApiKeys());
    }

    // --- Quản lý Setting API Keys ---
    private void handleAddKey() {
        String name = view.getNewKeyName();
        String value = view.getNewKeyValue();
        if (name.isEmpty() || value.isEmpty()) {
            view.alert("Vui lòng điền đủ Tên gợi nhớ và Mã API Key.");
            return;
        }
        dbService.addApiKey(name, value);
        view.clearKeyInputs();
        refreshApiKeysView();
        view.alert("Đã lưu Key thành công!");
    }

    private void handleDeleteKey(int keyId) {
        dbService.deleteApiKey(keyId);
        refreshApiKeysView();
    }

    private void handleSetActiveKey(int keyId) {
        dbService.setActiveApiKey(keyId);
        refreshApiKeysView();
        view.alert("Đã áp dụng Key này làm mặc định!");
    }

    // --- Quản lý Chat & Auto-Naming ---
    private void handleNewSession() {
        String time = TS.format(LocalDateTime.now());
        ChatSession newSession = dbService.createNewSession(DEFAULT_SESSION_PREFIX + time);
        if (newSession != null) {
            refreshSessionList();
            view.selectSessionById(newSession.getId()); // NHẢY NGAY VÀO ĐOẠN CHAT VỪA TẠO
            view.focusInput();
        }
    }

    private void handleRenameSession(ChatSession session) {
        if (session == null) return;
        String newName = JOptionPane.showInputDialog(view, "Nhập tên mới cho đoạn chat:", session.getTitle());
        if (newName != null && !newName.trim().isEmpty()) {
            dbService.updateSessionTitle(session.getId(), newName.trim());
            refreshSessionList();
            view.selectSessionById(session.getId()); // Giữ nguyên vị trí bôi đen
        }
    }

    private void handleDeleteSession(ChatSession session) {
        if (session == null) return;
        int confirm = JOptionPane.showConfirmDialog(view,
                "Bạn có chắc muốn xóa vĩnh viễn đoạn chat '" + session.getTitle() + "' không?",
                "Xác nhận xóa", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            dbService.deleteSession(session.getId());
            refreshSessionList();
            view.clearChatHistory();
            currentSession = null;
            // Xóa xong thì tự bôi đen thằng đầu tiên trong danh sách cho khỏi trống
            view.selectFirstSession();
        }
    }

    private void handleSessionSelected(ChatSession session) {
        if (currentSession != null && currentSession.getId() == session.getId()) return;

        this.currentSession = session;
        view.clearChatHistory();

        List<ChatMessage> history = dbService.getMessagesBySession(session.getId());

        for (ChatMessage msg : history) {
            String color = msg.getSender().equals("YOU") ? "#0000FF" : (msg.getSender().equals("AI") ? "#008000" : "#FF0000");
            view.appendMessage(msg.getSender(), MarkdownParser.convertMarkdownToHtml(msg.getContent()), color, msg.getTimestamp());
        }

        aiService.loadHistoryIntoMemory(history);
        view.setStatus("Đã tải: " + session.getTitle());
    }

    private void handleClear() {
        view.clearChatHistory();
        view.setStatus("Ready");
        view.focusInput();
    }

    private void handleSend() {
        if (worker != null && !worker.isDone()) return;
        if (currentSession == null) return;

        String userText = view.getUserInput();
        if (userText.isEmpty()) return;

        String apiKey = dbService.getActiveApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            view.appendMessage("SYSTEM", MarkdownParser.convertMarkdownToHtml("Lỗi: Bạn chưa cài đặt API Key. Hãy sang Tab [Cài đặt Hệ thống] để thêm Key mới nhé!"), "#FF0000", TS.format(LocalDateTime.now()));
            return;
        }

        // TỰ ĐỘNG ĐỔI TÊN NẾU LÀ ĐOẠN CHAT MỚI TINH
        if (currentSession.getTitle().startsWith(DEFAULT_SESSION_PREFIX)) {
            String newTitle = userText.length() > 25 ? userText.substring(0, 25) + "..." : userText;
            dbService.updateSessionTitle(currentSession.getId(), newTitle);
            currentSession.setTitle(newTitle);
            refreshSessionList();
            view.selectSessionById(currentSession.getId());
        }

        String timestampUser = TS.format(LocalDateTime.now());
        view.appendMessage("YOU", MarkdownParser.convertMarkdownToHtml(userText), "#0000FF", timestampUser);
        dbService.saveMessage(currentSession.getId(), "YOU", userText, timestampUser);

        view.clearInput();
        view.focusInput();
        view.setBusy(true);

        String modelName = view.getSelectedModel();
        worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                return aiService.generateResponse(userText, apiKey, modelName);
            }

            @Override
            protected void done() {
                try {
                    String aiText = get();
                    String timestampAi = TS.format(LocalDateTime.now());

                    view.appendMessage("AI", MarkdownParser.convertMarkdownToHtml(aiText), "#008000", timestampAi);
                    dbService.saveMessage(currentSession.getId(), "AI", aiText, timestampAi);

                    view.setStatus("Ready");
                } catch (Exception ex) {
                    view.appendMessage("SYSTEM", MarkdownParser.convertMarkdownToHtml("Lỗi: " + ex.getMessage()), "#FF0000", TS.format(LocalDateTime.now()));
                    view.setStatus("Error");
                } finally {
                    view.setBusy(false);
                }
            }
        };
        worker.execute();
    }
}