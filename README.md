
# ĐÂY LÀ BẢN DEMO AHIHI

# 🤖 Play_With_AI

> Một ứng dụng Chat AI trên Desktop viết bằng Java Swing  
> Gọi vui là: **"CHƠI VỚI AI"**

---

# 📌 Giới thiệu

`Play_With_AI` là project thử nghiệm xây dựng ứng dụng AI Chat Desktop bằng Java.

Mục tiêu của project:

- Học Java Desktop Architecture
- Tổ chức code chuẩn Software Engineering
- Thực hành MVC / Layered Architecture
- Tích hợp Gemini AI bằng LangChain4j
- Tách biệt UI và Business Logic
- Chuẩn bị nền tảng để scale lớn hơn sau này

---

# 🏗️ Kiến trúc hệ thống

Project được tổ chức theo:

- Separation of Concerns (SoC)
- MVC Pattern
- Layered Architecture

Mục tiêu:

✅ Dễ mở rộng  
✅ Dễ maintain  
✅ Dễ debug  
✅ Dễ tái sử dụng code  
✅ Tránh God Object / Spaghetti Code

---

# 📂 Cấu trúc thư mục

```plaintext
src/
 └── main/
      ├── java/
      │    └── com/minhtriet/
      │
      │         ├── Main.java
      │
      │         ├── model/
      │         │    ├── ChatMessage.java
      │         │    └── AppConfig.java
      │         │
      │         ├── service/
      │         │    ├── AiChatService.java
      │         │    └── DatabaseService.java
      │         │
      │         ├── ui/
      │         │    ├── MainWindow.java
      │         │    ├── components/
      │         │    │    ├── ChatPanel.java
      │         │    │    ├── InputPanel.java
      │         │    │    └── ToolbarPanel.java
      │         │    │
      │         │    └── theme/
      │         │         └── RetroTheme.java
      │         │
      │         ├── controller/
      │         │    └── ChatController.java
      │         │
      │         └── util/
      │              └── MarkdownParser.java
      │
      └── resources/
           ├── icon.png
           └── application.properties
```

---

# 🧠 Kiến trúc hoạt động

```plaintext
User
  ↓
UI Layer
  ↓
Controller Layer
  ↓
Service Layer
  ↓
AI API / Database
  ↓
Controller
  ↓
UI
```

---

# 📦 Giải thích từng tầng

---

## 🎨 UI Layer (`ui/`)

Tầng giao diện.

Chỉ chịu trách nhiệm:

- Render giao diện
- Nhận sự kiện click/phím
- Hiển thị dữ liệu

### ❌ UI KHÔNG được:

- gọi Gemini API
- viết SQL
- xử lý business logic
- parse markdown phức tạp

---

## 🧠 Service Layer (`service/`)

Tầng business logic.

Chịu trách nhiệm:

- Gọi Gemini API
- Xử lý AI
- Database
- Markdown processing
- File handling

### ⚠️ Service không biết Swing/JFrame là gì

Điều này giúp:

- tái sử dụng code
- dễ test
- dễ scale sang Web/Mobile

---

## 🎮 Controller Layer (`controller/`)

Tầng trung gian giữa:

```plaintext
UI ↔ Service
```

Controller nhận event từ UI → gọi Service → trả dữ liệu về UI.

---

## 📦 Model Layer (`model/`)

Chứa các class dữ liệu:

Ví dụ:

```java
ChatMessage
AppConfig
Conversation
```

---

## 🛠️ Utility Layer (`util/`)

Chứa các helper dùng chung:

Ví dụ:

```java
MarkdownParser
FileUtils
JsonUtils
```

---

# 🚀 Công nghệ sử dụng

- Java
- Swing
- Maven
- LangChain4j
- Gemini API
- SQLite (future)
- Markdown parser

---

# 🔥 Điểm quan trọng của project

## UI bị “mù” về AI

`InputPanel.java` không tự gọi Gemini.

Nó chỉ báo cho `ChatController`:

> "User vừa gửi tin nhắn"

---

## Service bị “mù” về Swing

`AiChatService.java` không biết:

- JFrame
- JTextArea
- JButton

Điều này giúp tái sử dụng logic cho:

- Desktop App
- Web App
- Mobile App
- CLI App

---

## Markdown được tách riêng

Thay vì:

```java
convertMarkdownToHtml(...)
```

100 dòng trong `Main.java`

=> sẽ được tách thành:

```java
MarkdownParser.parse(text)
```

Giúp code sạch hơn rất nhiều.

---

# 🎯 Mục tiêu tương lai

- [ ] Multiple AI Providers
- [ ] Streaming Response
- [ ] Chat History
- [ ] SQLite Storage
- [ ] Theme System
- [ ] Plugin System
- [ ] Voice Chat
- [ ] JavaFX Version
- [ ] Clean Architecture

---

# 💡 Bài học rút ra

Project ban đầu từng viết toàn bộ trong:

```java
Main.java
```

Tuy nhiên khi project lớn hơn, việc tách:

```plaintext
UI → Controller → Service → Model
```

giúp hệ thống:

- dễ maintain
- dễ mở rộng
- dễ teamwork
- đúng chuẩn Software Engineering

---

# 📜 License

MIT License
