package com.smartenglish.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "ai.llm")
public class AiProperties {

    /**
     * URL API LLM (OpenAI chat completions hoặc proxy Gemini / OpenRouter)
     */
    private String apiUrl = "https://api.openai.com/v1/chat/completions";

    /**
     * API Key để xác thực với LLM Provider
     */
    private String apiKey = "mock-key";

    /**
     * Model định danh (e.g. gpt-4o-mini, gemini-1.5-flash)
     */
    private String model = "gpt-4o-mini";

    /**
     * Chế độ Mock: Nếu true, trả về kết quả giả lập 7 ngày hoàn chỉnh mà không tốn phí gọi API
     */
    private Boolean mockEnabled = true;

    public boolean isMockEnabled() {
        return Boolean.TRUE.equals(mockEnabled);
    }
}

