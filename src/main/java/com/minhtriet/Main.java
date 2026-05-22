package com.minhtriet;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Main {
    public static void main(String[] args) {
        // Tắt làm mịn chữ để viền chữ gai góc, sắc bén chuẩn màn hình lồi Win 98
        System.setProperty("awt.useSystemAAFontSettings", "off");
        System.setProperty("swing.aatext", "false");

        SwingUtilities.invokeLater(() -> {
            installClassicWindowsLookAndFeel();
            new ClassicWindowsGeminiUI().show();
        });
    }

    private static void installClassicWindowsLookAndFeel() {
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

    static class ClassicWindowsGeminiUI {
        private static final String DEFAULT_MODEL = "gemini-2.5-flash";
        private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        private final JFrame frame = new JFrame("Play With AI - Gemini (Classic Windows 98)");
        private final JEditorPane transcriptArea = new JEditorPane();
        private final JTextArea inputArea = new JTextArea(4, 60);
        private final JButton sendButton = new JButton("Send");
        private final JButton clearButton = new JButton("Clear");
        private final JLabel statusLabel = new JLabel("Ready");
        private final JComboBox<String> modelCombo = new JComboBox<>(new String[]{DEFAULT_MODEL});

        // BIẾN LƯU TRỮ TRẠNG THÁI GIAO DIỆN (FONT & MÀU NỀN)
        private String currentFontFamily = "MS Sans Serif";
        private int currentFontSize = 12;
        private String currentBgColorHex = "#FFFFFF"; // Mặc định nền trắng

        private final JSlider fontSizeSlider = new JSlider(JSlider.HORIZONTAL, 8, 24, currentFontSize);
        private final JLabel fontSizeLabel = new JLabel("Size: " + currentFontSize);

        private volatile SwingWorker<String, Void> worker;
        private final StringBuilder chatHistoryHtml = new StringBuilder();

        void show() {
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.setMinimumSize(new Dimension(1000, 700)); // Mở rộng chút để chứa thanh công cụ mới
            frame.setLocationRelativeTo(null);
            frame.getContentPane().setBackground(new Color(212, 208, 200));
            frame.setLayout(new BorderLayout(8, 8));
            ((JComponent) frame.getContentPane()).setBorder(new EmptyBorder(10, 10, 10, 10));

            // KHUNG KÉO THẢ (JSplitPane)
            JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
            splitPane.setContinuousLayout(true);
            splitPane.setDividerSize(7);
            splitPane.setBorder(null);

            splitPane.setTopComponent(buildChatPanel());
            splitPane.setBottomComponent(buildInputPanel());
            splitPane.setResizeWeight(0.8);

            frame.add(buildTopPanel(), BorderLayout.NORTH);
            frame.add(splitPane, BorderLayout.CENTER);
            frame.add(buildStatusBar(), BorderLayout.SOUTH);

            frame.setVisible(true);
            inputArea.requestFocusInWindow();
        }

        private JPanel buildTopPanel() {
            // SỬ DỤNG DUY NHẤT 1 FLOWLAYOUT ĐỂ MỌI THỨ THẲNG HÀNG NHAU HOÀN TOÀN
            JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 6));
            top.setBackground(new Color(212, 208, 200));
            top.setBorder(new CompoundBorder(new EtchedBorder(EtchedBorder.LOWERED), new EmptyBorder(4, 4, 4, 4)));

            // 1. TÍNH NĂNG CHỌN MÀU NỀN (THAY THẾ CHO API KEY)
            String[] bgColors = {
                    "Trắng (Mặc định)",
                    "Vàng nhạt (Notepad)",
                    "Xanh lơ (Win 98 Desktop)",
                    "Xanh rêu (Matrix)",
                    "Xám (Hộp thoại)"
            };
            JComboBox<String> bgColorCombo = new JComboBox<>(bgColors);

            bgColorCombo.addActionListener(e -> {
                String sel = (String) bgColorCombo.getSelectedItem();
                switch (sel) {
                    case "Vàng nhạt (Notepad)": currentBgColorHex = "#FFFFE1"; break;
                    case "Xanh lơ (Win 98 Desktop)": currentBgColorHex = "#A6CAF0"; break;
                    case "Xanh rêu (Matrix)": currentBgColorHex = "#CCFFCC"; break;
                    case "Xám (Hộp thoại)": currentBgColorHex = "#D4D0C8"; break;
                    default: currentBgColorHex = "#FFFFFF"; break; // Trắng
                }
                // Áp dụng màu nền cho ô gõ phím
                inputArea.setBackground(Color.decode(currentBgColorHex));
                // Render lại HTML để áp dụng màu nền cho khung Chat
                updateTranscriptArea();
            });

            // 2. CHỌN FONT CHỮ
            String[] classicFonts = {
                    "MS Sans Serif", "Times New Roman", "Arial", "Tahoma", "Courier New", "Comic Sans MS"
            };
            JComboBox<String> fontCombo = new JComboBox<>(classicFonts);
            fontCombo.setSelectedItem(currentFontFamily);
            fontCombo.addActionListener(e -> {
                currentFontFamily = (String) fontCombo.getSelectedItem();
                inputArea.setFont(new Font(currentFontFamily, Font.PLAIN, currentFontSize));
                updateTranscriptArea();
            });

            // 3. THANH KÉO SIZE CHỮ
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

            // GẮN TẤT CẢ VÀO THANH CÔNG CỤ THEO THỨ TỰ THẲNG HÀNG
            top.add(new JLabel("Màu nền:"));
            top.add(bgColorCombo);

            top.add(new JSeparator(SwingConstants.VERTICAL)); // Vạch kẻ phân cách

            top.add(new JLabel("Font:"));
            top.add(fontCombo);

            top.add(new JLabel("A-"));
            top.add(fontSizeSlider);
            top.add(new JLabel("A+"));
            top.add(fontSizeLabel);

            top.add(new JSeparator(SwingConstants.VERTICAL)); // Vạch kẻ phân cách

            top.add(new JLabel("Model:"));
            top.add(modelCombo);

            clearButton.addActionListener(e -> {
                chatHistoryHtml.setLength(0);
                updateTranscriptArea();
                setStatus("Ready");
                inputArea.requestFocusInWindow();
            });
            top.add(clearButton);

            return top;
        }

        private JComponent buildChatPanel() {
            transcriptArea.setEditable(false);
            transcriptArea.setContentType("text/html");
            transcriptArea.setEditorKit(new HTMLEditorKit());
            transcriptArea.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
            transcriptArea.setFont(new Font(currentFontFamily, Font.PLAIN, currentFontSize));

            // Áp dụng màu nền đồng bộ
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
            inputArea.setBackground(Color.decode(currentBgColorHex)); // Áp màu nền ban đầu

            JScrollPane inputScroll = new JScrollPane(inputArea);
            inputScroll.setBorder(new CompoundBorder(new EtchedBorder(EtchedBorder.LOWERED), new EmptyBorder(2, 2, 2, 2)));

            JPanel actions = new JPanel(new GridLayout(2, 1, 8, 8));
            actions.setOpaque(false);
            actions.add(sendButton);

            JButton helpButton = new JButton("Help");
            actions.add(helpButton);

            helpButton.addActionListener(e -> showHelp());
            sendButton.addActionListener(e -> send());

            InputMap im = inputArea.getInputMap(JComponent.WHEN_FOCUSED);
            ActionMap am = inputArea.getActionMap();
            im.put(KeyStroke.getKeyStroke("control ENTER"), "SEND");
            am.put("SEND", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) { send(); }
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

        private void showHelp() {
            String msg = "Cách dùng:\n- Nhập câu hỏi.\n- Ctrl + Enter để gửi.\n- Dùng thanh menu phía trên để đổi Font, Size chữ và Màu nền.";
            JOptionPane.showMessageDialog(frame, msg, "Help", JOptionPane.INFORMATION_MESSAGE);
        }

        private void send() {
            if (worker != null && !worker.isDone()) return;
            String userText = inputArea.getText().trim();
            if (userText.isEmpty()) return;

            String apiKey = loadApiKey(); // Hệ thống ngầm nạp API key
            if (apiKey == null || apiKey.isBlank()) {
                appendMessage("SYSTEM", "Lỗi: Chưa thiết lập API Key trong mã nguồn.", "#FF0000");
                return;
            }

            appendMessage("YOU", userText, "#0000FF");
            inputArea.setText("");
            inputArea.requestFocusInWindow();
            setBusy(true);

            ChatLanguageModel model = GoogleAiGeminiChatModel.builder()
                    .apiKey(apiKey)
                    .modelName((String) modelCombo.getSelectedItem())
                    .build();

            worker = new SwingWorker<>() {
                @Override
                protected String doInBackground() { return model.generate(userText); }

                @Override
                protected void done() {
                    try {
                        appendMessage("AI", get(), "#008000");
                        setStatus("Ready");
                    } catch (Exception ex) {
                        appendMessage("SYSTEM", "Lỗi: " + ex.getMessage(), "#FF0000");
                        setStatus("Error");
                    } finally {
                        setBusy(false);
                    }
                }
            };
            worker.execute();
        }

        private void setBusy(boolean busy) {
            sendButton.setEnabled(!busy);
            clearButton.setEnabled(!busy);
            setStatus(busy ? "Thinking..." : "Ready");
        }

        private void setStatus(String text) { statusLabel.setText(text); }

        private void appendMessage(String sender, String text, String headerColor) {
            String timestamp = TS.format(LocalDateTime.now());
            String formattedBody = convertMarkdownToHtml(text);

            chatHistoryHtml.append("<div style='margin-bottom: 15px;'>")
                    .append("<b style='color:").append(headerColor).append(";'>")
                    .append("[").append(timestamp).append("] ").append(sender).append(":</b><br>")
                    .append("<div style='margin-top: 5px;'>").append(formattedBody).append("</div>")
                    .append("</div><hr style='border: 0; border-bottom: 1px solid #808080;'>");

            updateTranscriptArea();
        }

        private void updateTranscriptArea() {
            // CẬP NHẬT MÀU NỀN VÀ FONT ĐỘNG VÀO TRONG MÃ CSS CỦA HTML
            String finalHtml = "<html><body style='background-color: " + currentBgColorHex + "; font-family: \"" + currentFontFamily + "\", sans-serif; padding: 5px; font-size: "
                    + currentFontSize + "px; color: #000;'>"
                    + chatHistoryHtml.toString()
                    + "</body></html>";
            transcriptArea.setText(finalHtml);
            // Đồng bộ màu nền cho JEditorPane để không bị viền trắng
            transcriptArea.setBackground(Color.decode(currentBgColorHex));

            SwingUtilities.invokeLater(() -> transcriptArea.setCaretPosition(transcriptArea.getDocument().getLength()));
        }

        private String convertMarkdownToHtml(String md) {
            if (md == null || md.isBlank()) return "";

            StringBuilder html = new StringBuilder();
            boolean inCodeBlock = false;
            boolean inTable = false;
            boolean isFirstRow = false;
            boolean inUl = false;
            boolean inOl = false;
            String[] lines = md.split("\n");

            for (String line : lines) {
                if (line.trim().startsWith("```")) {
                    if (inTable) { html.append("</table><br>\n"); inTable = false; }
                    if (inUl) { html.append("</ul>\n"); inUl = false; }
                    if (inOl) { html.append("</ol>\n"); inOl = false; }
                    if (inCodeBlock) {
                        html.append("</pre>");
                        inCodeBlock = false;
                    } else {
                        // Khung code có màu nền cố định là trắng để không bị trùng lấp với màu nền mới
                        html.append("<pre style='background-color: #FFFFFF; border-top: 2px solid #808080; border-left: 2px solid #808080; border-right: 2px solid #FFFFFF; border-bottom: 2px solid #FFFFFF; padding: 8px; font-family: \"Courier New\", monospace; margin: 5px 0; color: #000000;'>");
                        inCodeBlock = true;
                    }
                    continue;
                }

                if (inCodeBlock) {
                    html.append(line.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")).append("\n");
                    continue;
                }

                String trimmedLine = line.trim();
                if (trimmedLine.startsWith("|") && trimmedLine.endsWith("|")) {
                    if (inUl) { html.append("</ul>\n"); inUl = false; }
                    if (inOl) { html.append("</ol>\n"); inOl = false; }

                    if (!inTable) {
                        html.append("<table cellpadding='6' cellspacing='0' style='border-collapse: separate; border-top: 2px solid #808080; border-left: 2px solid #808080; border-right: 2px solid #FFFFFF; border-bottom: 2px solid #FFFFFF; width: 100%; margin: 10px 0; background-color: #FFFFFF;'>\n");
                        inTable = true;
                        isFirstRow = true;
                    }

                    if (trimmedLine.matches("^\\|?\\s*[-:]+\\s*\\|.*")) continue;

                    html.append("<tr>");
                    String inner = trimmedLine.substring(1, trimmedLine.length() - 1);
                    String[] cells = inner.split("\\|");

                    for (String cellText : cells) {
                        if (isFirstRow) {
                            html.append("<th style='background-color: #D4D0C8; border-top: 1px solid #FFFFFF; border-left: 1px solid #FFFFFF; border-right: 1px solid #808080; border-bottom: 1px solid #808080; padding: 6px; text-align: left; font-weight: bold; color: #000000;'>");
                            html.append(processInlineMarkdown(cellText.trim()));
                            html.append("</th>");
                        } else {
                            html.append("<td style='border: 1px solid #808080; background-color: #FFFFFF; padding: 6px; color: #000000; vertical-align: top;'>");
                            html.append(processInlineMarkdown(cellText.trim()));
                            html.append("</td>");
                        }
                    }
                    html.append("</tr>\n");
                    isFirstRow = false;
                    continue;
                } else if (inTable) {
                    html.append("</table><br>\n");
                    inTable = false;
                }

                if (trimmedLine.matches("^[-*]\\s+.*")) {
                    if (!inUl) { html.append("<ul style='margin-left: 20px; color: #000000;'>\n"); inUl = true; }
                    html.append("<li>").append(processInlineMarkdown(trimmedLine.substring(2).trim())).append("</li>\n");
                    continue;
                } else if (inUl) { html.append("</ul>\n"); inUl = false; }

                if (trimmedLine.matches("^\\d+\\.\\s+.*")) {
                    if (!inOl) { html.append("<ol style='margin-left: 20px; color: #000000;'>\n"); inOl = true; }
                    html.append("<li>").append(processInlineMarkdown(trimmedLine.substring(trimmedLine.indexOf(".")+1).trim())).append("</li>\n");
                    continue;
                } else if (inOl) { html.append("</ol>\n"); inOl = false; }

                String parsed = processInlineMarkdown(line);
                if (parsed.startsWith("### ")) {
                    parsed = "<h3 style='margin: 8px 0; color: #333; font-weight: bold;'>" + parsed.substring(4) + "</h3>";
                } else if (parsed.startsWith("## ")) {
                    parsed = "<h2 style='margin: 10px 0; color: #222; font-weight: bold;'>" + parsed.substring(3) + "</h2>";
                } else if (parsed.startsWith("# ")) {
                    parsed = "<h1 style='margin: 12px 0; color: #111; font-weight: bold;'>" + parsed.substring(2) + "</h1>";
                } else {
                    parsed += "<br>";
                }
                html.append(parsed).append("\n");
            }

            if (inTable) html.append("</table><br>\n");
            if (inUl) html.append("</ul>\n");
            if (inOl) html.append("</ol>\n");

            return html.toString();
        }

        private String processInlineMarkdown(String text) {
            String parsed = text.replaceAll("(?i)<br\\s*/?>", "[[[BR]]]");
            parsed = parsed.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
            parsed = parsed.replace("[[[BR]]]", "<br>");
            parsed = parsed.replaceAll("\\*\\*(.*?)\\*\\*", "<b>$1</b>");
            parsed = parsed.replaceAll("\\*([^\\*]+)\\*", "<i>$1</i>");
            parsed = parsed.replaceAll("`([^`]+)`", "<code style='background-color: #E8E8E8; color: #800000; font-family: \"Courier New\", monospace; padding: 1px 3px; border: 1px solid #CCC;'>$1</code>");
            return parsed;
        }

        private String loadApiKey() {
            // Bạn nhớ giữ API key tại đây để hệ thống nạp ngầm khi nhấn Send nhé
            return "";
        }
    }
}