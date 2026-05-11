package com.smarthome.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

/**
 * Cấu hình LangChain4j cho AI RAG
 * - ChatLanguageModel: Gemini Pro (gọi API Google)
 * - EmbeddingModel: MiniLM (chạy local, không cần API key)
 * - EmbeddingStore: In-Memory (lưu trong RAM)
 */
@Configuration
public class AiConfig {

    @Value("${app.ai.gemini.api-key}")
    private String geminiApiKey;

    @Value("${app.ai.gemini.model}")
    private String geminiModel;

    /**
     * Chat Model - Gemini 2.0 Flash (miễn phí)
     * Dùng để sinh câu trả lời tư vấn
     */
    @Bean
    public ChatLanguageModel chatLanguageModel() {
        return GoogleAiGeminiChatModel.builder()
                .apiKey(geminiApiKey)
                .modelName(geminiModel)
                .temperature(0.7)
                .maxOutputTokens(1024)
                .build();
    }

    /**
     * Embedding Model - AllMiniLmL6V2 (chạy local)
     * Dùng để tạo vector embedding cho sản phẩm và câu hỏi
     * Không cần API key, model ~22MB load trong RAM
     */
    @Bean
    public EmbeddingModel embeddingModel() {
        return new AllMiniLmL6V2EmbeddingModel();
    }

    /**
     * Embedding Store - In-Memory
     * Lưu vector sản phẩm trong RAM
     * Phù hợp cho dự án nhỏ-vừa (< 10,000 sản phẩm)
     */
    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        return new InMemoryEmbeddingStore<>();
    }
}
