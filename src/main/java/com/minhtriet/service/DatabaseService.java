package com.minhtriet.service;

import com.minhtriet.model.ApiKey;
import com.minhtriet.model.ChatMessage;
import com.minhtriet.model.ChatSession;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseService {
    private static final String DB_URL = "jdbc:sqlite:chat_history.db";

    public DatabaseService() {
        initDatabase();
    }

    private void initDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {

            stmt.execute("CREATE TABLE IF NOT EXISTS t_session (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "title TEXT NOT NULL, " +
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP)");

            stmt.execute("CREATE TABLE IF NOT EXISTS t_message (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "session_id INTEGER, " +
                    "sender TEXT, " +
                    "content TEXT, " +
                    "timestamp TEXT, " +
                    "FOREIGN KEY(session_id) REFERENCES t_session(id))");

            // Bảng lưu API Key mới
            stmt.execute("CREATE TABLE IF NOT EXISTS t_api_key (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "name TEXT NOT NULL, " +
                    "value TEXT NOT NULL, " +
                    "is_active INTEGER DEFAULT 0)");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // --- CÁC HÀM XỬ LÝ ĐOẠN CHAT VÀ TIN NHẮN ---
    public ChatSession createNewSession(String title) {
        String sql = "INSERT INTO t_session(title) VALUES(?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, title);
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) return new ChatSession(rs.getInt(1), title);
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    // Hàm mới: Đổi tên đoạn chat tự động
    public void updateSessionTitle(int sessionId, String newTitle) {
        String sql = "UPDATE t_session SET title = ? WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newTitle);
            pstmt.setInt(2, sessionId);
            pstmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public List<ChatSession> getAllSessions() {
        List<ChatSession> sessions = new ArrayList<>();
        String sql = "SELECT * FROM t_session ORDER BY id DESC";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) sessions.add(new ChatSession(rs.getInt("id"), rs.getString("title")));
        } catch (SQLException e) { e.printStackTrace(); }
        return sessions;
    }

    public void saveMessage(int sessionId, String sender, String content, String timestamp) {
        String sql = "INSERT INTO t_message(session_id, sender, content, timestamp) VALUES(?,?,?,?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, sessionId);
            pstmt.setString(2, sender);
            pstmt.setString(3, content);
            pstmt.setString(4, timestamp);
            pstmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public List<ChatMessage> getMessagesBySession(int sessionId) {
        List<ChatMessage> messages = new ArrayList<>();
        String sql = "SELECT * FROM t_message WHERE session_id = ? ORDER BY id ASC";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, sessionId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) messages.add(new ChatMessage(rs.getString("sender"), rs.getString("content"), rs.getString("timestamp")));
        } catch (SQLException e) { e.printStackTrace(); }
        return messages;
    }

    public void deleteSession(int sessionId) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            // Xóa toàn bộ tin nhắn thuộc về Session này trước (Ràng buộc khóa ngoại)
            try (PreparedStatement pstmt1 = conn.prepareStatement("DELETE FROM t_message WHERE session_id = ?")) {
                pstmt1.setInt(1, sessionId);
                pstmt1.executeUpdate();
            }
            // Sau đó xóa tên Session
            try (PreparedStatement pstmt2 = conn.prepareStatement("DELETE FROM t_session WHERE id = ?")) {
                pstmt2.setInt(1, sessionId);
                pstmt2.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // --- CÁC HÀM XỬ LÝ API KEY ---
    public List<ApiKey> getAllApiKeys() {
        List<ApiKey> keys = new ArrayList<>();
        String sql = "SELECT * FROM t_api_key ORDER BY id ASC";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) keys.add(new ApiKey(rs.getInt("id"), rs.getString("name"), rs.getString("value"), rs.getInt("is_active") == 1));
        } catch (SQLException e) { e.printStackTrace(); }
        return keys;
    }

    public String getActiveApiKey() {
        String sql = "SELECT value FROM t_api_key WHERE is_active = 1 LIMIT 1";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getString("value");
        } catch (SQLException e) { e.printStackTrace(); }
        return null; // Trả về null nếu chưa có key nào
    }

    public void addApiKey(String name, String value) {
        String sql = "INSERT INTO t_api_key(name, value, is_active) VALUES(?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, value);
            // Nếu là key đầu tiên được thêm vào, tự động set làm mặc định
            pstmt.setInt(3, getAllApiKeys().isEmpty() ? 1 : 0);
            pstmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void deleteApiKey(int id) {
        String sql = "DELETE FROM t_api_key WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void setActiveApiKey(int id) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            // Tắt tất cả các key
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("UPDATE t_api_key SET is_active = 0");
            }
            // Bật key được chọn
            try (PreparedStatement pstmt = conn.prepareStatement("UPDATE t_api_key SET is_active = 1 WHERE id = ?")) {
                pstmt.setInt(1, id);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }
}