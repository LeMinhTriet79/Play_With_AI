package com.minhtriet.service;

import com.minhtriet.model.ChatMessage;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;

import java.util.List;

public class AiChatService {
    private final String defaultModelName;
    private ChatMemory chatMemory;

    public AiChatService(String defaultModelName) {
        this.defaultModelName = defaultModelName;
        this.chatMemory = MessageWindowChatMemory.withMaxMessages(20); // Nhớ 20 tin nhắn gần nhất
    }

    // Nạp lịch sử từ DB vào bộ nhớ của AI khi chuyển Session
    public void loadHistoryIntoMemory(List<ChatMessage> history) {
        chatMemory.clear();
        for (ChatMessage msg : history) {
            if (msg.getSender().equals("YOU")) {
                chatMemory.add(UserMessage.from(msg.getContent()));
            } else if (msg.getSender().equals("AI")) {
                chatMemory.add(AiMessage.from(msg.getContent()));
            }
        }
    }

    public String generateResponse(String prompt, String apiKey, String modelName) {
        String finalModelName = (modelName == null || modelName.isBlank()) ? defaultModelName : modelName;
        ChatLanguageModel model = GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName(finalModelName)
                .build();

        // 1. Thêm câu hỏi mới vào Memory
        chatMemory.add(UserMessage.from(prompt));

        // 2. Gửi toàn bộ Memory (Lịch sử + Câu hỏi mới) lên Gemini
        AiMessage response = model.generate(chatMemory.messages()).content();

        // 3. Lưu câu trả lời vào Memory
        chatMemory.add(response);

        return response.text();
    }
}