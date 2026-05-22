package com.minhtriet.view;

import com.minhtriet.model.ApiKey;
import com.minhtriet.model.ChatSession;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.function.Consumer;

public class MainWindow extends JFrame {
    public static final String DEFAULT_MODEL = "gemini-2.5-flash";

    private final JTabbedPane tabbedPane = new JTabbedPane();

    // --- Tab 1: Chat UI ---
    private final JEditorPane transcriptArea = new JEditorPane();
    private final JTextArea inputArea = new JTextArea(4, 60);
    private final JButton sendButton = new JButton("Send");
    private final JButton helpButton = new JButton("Help");
    private final JButton clearButton = new JButton("Clear");
    private final JLabel statusLabel = new JLabel("Ready");
    private final JComboBox<String> modelCombo = new JComboBox<>(new String[]{
            "gemini-2.5-flash", "gemini-2.0-flash", "gemini-1.5-pro-latest", "gemini-1.5-flash-latest"
    });

    private final DefaultListModel<ChatSession> sessionListModel = new DefaultListModel<>();
    private final JList<ChatSession> sessionList = new JList<>(sessionListModel);
    private final JButton newSessionButton = new JButton("New Chat");

    private final JButton renameSessionBtn = new JButton("Đổi tên");
    private final JButton deleteSessionBtn = new JButton("Xóa");

    // --- Tab 2: Settings UI ---
    private final DefaultListModel<ApiKey> apiKeyListModel = new DefaultListModel<>();
    private final JList<ApiKey> apiKeyList = new JList<>(apiKeyListModel);
    private final JTextField keyNameField = new JTextField(15);
    private final JPasswordField keyValueField = new JPasswordField(25);
    private final JButton addKeyBtn = new JButton("Thêm Key mới");
    private final JButton delKeyBtn = new JButton("Xóa Key");
    private final JButton setActiveKeyBtn = new JButton("Chọn làm Mặc định");

    // --- State ---
    private String currentFontFamily = "MS Sans Serif";
    private int currentFontSize = 12;
    private String currentBgColorHex = "#FFFFFF";
    private final JSlider fontSizeSlider = new JSlider(JSlider.HORIZONTAL, 8, 24, currentFontSize);
    private final JLabel fontSizeLabel = new JLabel("Size: " + currentFontSize);
    private final StringBuilder chatHistoryHtml = new StringBuilder();

    // --- Callbacks ---
    private Runnable onSend;
    private Runnable onClear;
    private Runnable onNewSession;
    private Consumer<ChatSession> onSessionSelected;
    private Consumer<ChatSession> onRenameSession;
    private Consumer<ChatSession> onDeleteSession;

    private Runnable onAddKey;
    private Consumer<Integer> onDeleteKey;
    private Consumer<Integer> onSetActiveKey;

    public MainWindow() {
        super("Play With AI - Gemini (Classic Windows 98)");
        buildUi();
    }

    public void showWindow() {
        setVisible(true);
        inputArea.requestFocusInWindow();
    }

    // --- Hàm tiện ích tạo khối đổ bóng 3D nổi cộm chuẩn Windows 98 ---
    private void apply3DStyle(JButton btn) {
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createRaisedBevelBorder(),
                new EmptyBorder(4, 12, 4, 12)
        ));
        btn.setBackground(new Color(212, 208, 200));
        btn.setFocusPainted(false);
    }

    // --- Setters cho Callbacks ---
    public void setOnSend(Runnable onSend) { this.onSend = onSend; }
    public void setOnClear(Runnable onClear) { this.onClear = onClear; }
    public void setOnNewSession(Runnable onNewSession) { this.onNewSession = onNewSession; }
    public void setOnSessionSelected(Consumer<ChatSession> onSessionSelected) { this.onSessionSelected = onSessionSelected; }
    public void setOnRenameSession(Consumer<ChatSession> onRenameSession) { this.onRenameSession = onRenameSession; }
    public void setOnDeleteSession(Consumer<ChatSession> onDeleteSession) { this.onDeleteSession = onDeleteSession; }

    public void setOnAddKey(Runnable onAddKey) { this.onAddKey = onAddKey; }
    public void setOnDeleteKey(Consumer<Integer> onDeleteKey) { this.onDeleteKey = onDeleteKey; }
    public void setOnSetActiveKey(Consumer<Integer> onSetActiveKey) { this.onSetActiveKey = onSetActiveKey; }

    // --- Getters / UI Controls ---
    public String getUserInput() { return inputArea.getText().trim(); }
    public void clearInput() { inputArea.setText(""); }
    public void focusInput() { inputArea.requestFocusInWindow(); }
    public String getSelectedModel() { return (String) modelCombo.getSelectedItem(); }

    public String getNewKeyName() { return keyNameField.getText().trim(); }
    public String getNewKeyValue() { return new String(keyValueField.getPassword()).trim(); }
    public void clearKeyInputs() { keyNameField.setText(""); keyValueField.setText(""); }

    public void setBusy(boolean busy) {
        sendButton.setEnabled(!busy);
        statusLabel.setText(busy ? "Thinking..." : "Ready");
        sessionList.setEnabled(!busy);
        renameSessionBtn.setEnabled(!busy);
        deleteSessionBtn.setEnabled(!busy);
    }

    public void setStatus(String text) { statusLabel.setText(text); }

    public void appendMessage(String sender, String formattedBody, String headerColor, String timestamp) {
        chatHistoryHtml.append("<div style='margin-bottom: 15px;'>")
                .append("<b style='color:").append(headerColor).append(";'>")
                .append("[").append(timestamp).append("] ").append(sender).append(":</b><br>")
                .append("<div style='margin-top: 5px;'>").append(formattedBody).append("</div>")
                .append("</div><hr style='border: 0; border-bottom: 1px solid #808080;'>");
        updateTranscriptArea();
    }

    public void clearChatHistory() {
        chatHistoryHtml.setLength(0);
        updateTranscriptArea();
    }

    public void renderSessionList(List<ChatSession> sessions) {
        sessionListModel.clear();
        for (ChatSession s : sessions) sessionListModel.addElement(s);
    }

    public void selectSessionById(int id) {
        for (int i = 0; i < sessionListModel.size(); i++) {
            if (sessionListModel.get(i).getId() == id) {
                sessionList.setSelectedIndex(i);
                sessionList.ensureIndexIsVisible(i);
                break;
            }
        }
    }

    public void selectFirstSession() {
        if (!sessionListModel.isEmpty()) {
            sessionList.setSelectedIndex(0);
            sessionList.ensureIndexIsVisible(0);
        }
    }

    public ChatSession getSelectedSession() {
        return sessionList.getSelectedValue();
    }

    public void renderApiKeys(List<ApiKey> keys) {
        apiKeyListModel.clear();
        for (ApiKey k : keys) apiKeyListModel.addElement(k);
    }

    public void alert(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Thông báo", JOptionPane.INFORMATION_MESSAGE);
    }

    // --- Build UI ---
    private void buildUi() {
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 700));
        setLocationRelativeTo(null);
        getContentPane().setBackground(new Color(212, 208, 200));

        tabbedPane.setFont(new Font("MS Sans Serif", Font.BOLD, 12));

        JPanel chatTab = new JPanel(new BorderLayout(8, 8));
        chatTab.setOpaque(false);
        chatTab.setBorder(new EmptyBorder(8, 8, 8, 8));

        JSplitPane rightSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        rightSplit.setContinuousLayout(true);
        rightSplit.setDividerSize(7);
        rightSplit.setBorder(null);
        rightSplit.setTopComponent(buildChatPanel());
        rightSplit.setBottomComponent(buildInputPanel());
        rightSplit.setResizeWeight(0.85);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplit.setContinuousLayout(true);
        mainSplit.setDividerSize(7);
        mainSplit.setBorder(null);
        mainSplit.setLeftComponent(buildSidebarPanel());
        mainSplit.setRightComponent(rightSplit);
        mainSplit.setResizeWeight(0.2);

        chatTab.add(buildChatToolbar(), BorderLayout.NORTH);
        chatTab.add(mainSplit, BorderLayout.CENTER);
        chatTab.add(buildStatusBar(), BorderLayout.SOUTH);

        JPanel settingsTab = buildSettingsTab();

        tabbedPane.addTab("Hội thoại AI", chatTab);
        tabbedPane.addTab("Cài đặt Hệ thống", settingsTab);

        add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel buildSettingsTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(new Color(212, 208, 200));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel leftPanel = new JPanel(new BorderLayout(5, 5));
        leftPanel.setOpaque(false);
        leftPanel.setBorder(BorderFactory.createTitledBorder("Danh sách API Key"));

        apiKeyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        apiKeyList.setFont(new Font("MS Sans Serif", Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(apiKeyList);

        JPanel btnPanel = new JPanel(new GridLayout(1, 2, 5, 0));
        btnPanel.setOpaque(false);

        // Đổ bóng 3D cho các nút Settings
        apply3DStyle(setActiveKeyBtn);
        apply3DStyle(delKeyBtn);

        setActiveKeyBtn.addActionListener(e -> {
            ApiKey selected = apiKeyList.getSelectedValue();
            if (selected != null && onSetActiveKey != null) onSetActiveKey.accept(selected.getId());
        });
        delKeyBtn.addActionListener(e -> {
            ApiKey selected = apiKeyList.getSelectedValue();
            if (selected != null && onDeleteKey != null) onDeleteKey.accept(selected.getId());
        });
        btnPanel.add(setActiveKeyBtn);
        btnPanel.add(delKeyBtn);

        leftPanel.add(scroll, BorderLayout.CENTER);
        leftPanel.add(btnPanel, BorderLayout.SOUTH);
        leftPanel.setPreferredSize(new Dimension(350, 0));

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        rightPanel.setOpaque(false);
        rightPanel.setBorder(BorderFactory.createTitledBorder("Thêm Key Dự phòng"));

        rightPanel.add(new JLabel("Tên gợi nhớ (VD: Key chính):"));
        rightPanel.add(keyNameField);
        rightPanel.add(new JLabel("Mã API Key:"));
        rightPanel.add(keyValueField);

        // Đổ bóng 3D cho nút Thêm Key
        apply3DStyle(addKeyBtn);
        addKeyBtn.addActionListener(e -> { if(onAddKey != null) onAddKey.run(); });
        rightPanel.add(addKeyBtn);

        panel.add(leftPanel, BorderLayout.WEST);
        panel.add(rightPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel buildSidebarPanel() {
        JPanel sidebar = new JPanel(new BorderLayout(4, 4));
        sidebar.setOpaque(false);
        sidebar.setBorder(new EmptyBorder(0, 0, 0, 8));

        // Đổ bóng 3D cho nút New Chat
        apply3DStyle(newSessionButton);
        newSessionButton.setFont(new Font("MS Sans Serif", Font.BOLD, 12));
        newSessionButton.addActionListener(e -> { if(onNewSession != null) onNewSession.run(); });

        sessionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        sessionList.setBackground(Color.WHITE);
        sessionList.setSelectionBackground(new Color(0, 0, 128));
        sessionList.setSelectionForeground(Color.WHITE);
        sessionList.setFont(new Font("MS Sans Serif", Font.PLAIN, 12));

        sessionList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && sessionList.getSelectedValue() != null) {
                if (onSessionSelected != null) onSessionSelected.accept(sessionList.getSelectedValue());
            }
        });

        JScrollPane scroll = new JScrollPane(sessionList);
        scroll.setBorder(new CompoundBorder(new EtchedBorder(EtchedBorder.LOWERED), new EmptyBorder(2, 2, 2, 2)));

        JPanel sidebarActions = new JPanel(new GridLayout(1, 2, 4, 0));
        sidebarActions.setOpaque(false);

        // Đổ bóng 3D cho nút Đổi Tên và Xóa
        apply3DStyle(renameSessionBtn);
        apply3DStyle(deleteSessionBtn);

        renameSessionBtn.addActionListener(e -> {
            if(onRenameSession != null) onRenameSession.accept(getSelectedSession());
        });
        deleteSessionBtn.addActionListener(e -> {
            if(onDeleteSession != null) onDeleteSession.accept(getSelectedSession());
        });
        sidebarActions.add(renameSessionBtn);
        sidebarActions.add(deleteSessionBtn);

        sidebar.add(newSessionButton, BorderLayout.NORTH);
        sidebar.add(scroll, BorderLayout.CENTER);
        sidebar.add(sidebarActions, BorderLayout.SOUTH);
        sidebar.setMinimumSize(new Dimension(160, 0));
        return sidebar;
    }

    private JPanel buildChatToolbar() {
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 6));
        top.setBackground(new Color(212, 208, 200));
        top.setBorder(new CompoundBorder(new EtchedBorder(EtchedBorder.LOWERED), new EmptyBorder(4, 4, 4, 4)));

        String[] bgColors = {"Trắng (Mặc định)", "Vàng nhạt (Notepad)", "Xanh lơ (Win 98 Desktop)", "Xanh rêu (Matrix)", "Xám (Hộp thoại)"};
        JComboBox<String> bgColorCombo = new JComboBox<>(bgColors);
        bgColorCombo.addActionListener(e -> {
            String sel = (String) bgColorCombo.getSelectedItem();
            switch (sel) {
                case "Vàng nhạt (Notepad)": currentBgColorHex = "#FFFFE1"; break;
                case "Xanh lơ (Win 98 Desktop)": currentBgColorHex = "#A6CAF0"; break;
                case "Xanh rêu (Matrix)": currentBgColorHex = "#CCFFCC"; break;
                case "Xám (Hộp thoại)": currentBgColorHex = "#D4D0C8"; break;
                default: currentBgColorHex = "#FFFFFF"; break;
            }
            inputArea.setBackground(Color.decode(currentBgColorHex));
            updateTranscriptArea();
        });

        String[] classicFonts = {"MS Sans Serif", "Times New Roman", "Arial", "Tahoma", "Courier New", "Comic Sans MS"};
        JComboBox<String> fontCombo = new JComboBox<>(classicFonts);
        fontCombo.setSelectedItem(currentFontFamily);
        fontCombo.addActionListener(e -> {
            currentFontFamily = (String) fontCombo.getSelectedItem();
            inputArea.setFont(new Font(currentFontFamily, Font.PLAIN, currentFontSize));
            updateTranscriptArea();
        });

        fontSizeSlider.setMajorTickSpacing(4);
        fontSizeSlider.setMinorTickSpacing(1);
        fontSizeSlider.setPaintTicks(true);
        fontSizeSlider.setOpaque(false);
        fontSizeSlider.setPreferredSize(new Dimension(100, 40));
        fontSizeSlider.addChangeListener(e -> {
            currentFontSize = fontSizeSlider.getValue();
            fontSizeLabel.setText("Size: " + currentFontSize);
            inputArea.setFont(new Font(currentFontFamily, Font.PLAIN, currentFontSize));
            updateTranscriptArea();
        });

        top.add(new JLabel("Màu nền:"));
        top.add(bgColorCombo);
        top.add(new JSeparator(SwingConstants.VERTICAL));
        top.add(new JLabel("Font:"));
        top.add(fontCombo);
        top.add(new JLabel("A-"));
        top.add(fontSizeSlider);
        top.add(new JLabel("A+"));
        top.add(fontSizeLabel);
        top.add(new JSeparator(SwingConstants.VERTICAL));
        top.add(new JLabel("Model:"));
        top.add(modelCombo);

        // Đổ bóng 3D cho nút Clear
        apply3DStyle(clearButton);
        clearButton.addActionListener(e -> { if(onClear != null) onClear.run(); });
        top.add(clearButton);

        return top;
    }

    private JComponent buildChatPanel() {
        transcriptArea.setEditable(false);
        transcriptArea.setContentType("text/html");
        transcriptArea.setEditorKit(new HTMLEditorKit());
        transcriptArea.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
        transcriptArea.setFont(new Font(currentFontFamily, Font.PLAIN, currentFontSize));
        transcriptArea.setBackground(Color.decode(currentBgColorHex));

        JScrollPane scroll = new JScrollPane(transcriptArea);
        scroll.setBorder(new CompoundBorder(new EtchedBorder(EtchedBorder.LOWERED), new EmptyBorder(2, 2, 2, 2)));
        scroll.setMinimumSize(new Dimension(200, 150));
        return scroll;
    }

    private JPanel buildInputPanel() {
        JPanel bottom = new JPanel(new BorderLayout(8, 8));
        bottom.setOpaque(false);
        bottom.setBorder(new EmptyBorder(8, 0, 0, 0));

        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        inputArea.setBorder(new EmptyBorder(6, 6, 6, 6));
        inputArea.setFont(new Font(currentFontFamily, Font.PLAIN, currentFontSize));
        inputArea.setBackground(Color.decode(currentBgColorHex));

        JScrollPane inputScroll = new JScrollPane(inputArea);
        inputScroll.setBorder(new CompoundBorder(new EtchedBorder(EtchedBorder.LOWERED), new EmptyBorder(2, 2, 2, 2)));

        JPanel actions = new JPanel(new GridLayout(2, 1, 8, 8));
        actions.setOpaque(false);

        // Đổ bóng 3D cho nút Send & Help
        apply3DStyle(sendButton);
        sendButton.setFont(new Font("MS Sans Serif", Font.BOLD, 12));
        apply3DStyle(helpButton);

        actions.add(sendButton);
        actions.add(helpButton);

        helpButton.addActionListener(e -> JOptionPane.showMessageDialog(this, "Cách dùng:\n- Nhập câu hỏi.\n- Ctrl + Enter để gửi."));
        sendButton.addActionListener(e -> { if (onSend != null) onSend.run(); });

        InputMap im = inputArea.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap am = inputArea.getActionMap();
        im.put(KeyStroke.getKeyStroke("control ENTER"), "SEND");
        am.put("SEND", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) { if (onSend != null) onSend.run(); }
        });

        bottom.add(inputScroll, BorderLayout.CENTER);
        bottom.add(actions, BorderLayout.EAST);
        bottom.setMinimumSize(new Dimension(200, 100));
        return bottom;
    }

    private JPanel buildStatusBar() {
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(new Color(212, 208, 200));
        statusBar.setBorder(new CompoundBorder(new EtchedBorder(EtchedBorder.LOWERED), new EmptyBorder(4, 6, 4, 6)));
        statusBar.add(statusLabel, BorderLayout.WEST);
        return statusBar;
    }

    private void updateTranscriptArea() {
        String finalHtml = "<html><body style='background-color: " + currentBgColorHex + "; font-family: \"" + currentFontFamily + "\", sans-serif; padding: 5px; font-size: "
                + currentFontSize + "px; color: #000;'>"
                + chatHistoryHtml.toString()
                + "</body></html>";
        transcriptArea.setText(finalHtml);
        transcriptArea.setBackground(Color.decode(currentBgColorHex));
        SwingUtilities.invokeLater(() -> transcriptArea.setCaretPosition(transcriptArea.getDocument().getLength()));
    }
}