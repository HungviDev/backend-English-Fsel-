package com.smartenglish;

import com.smartenglish.config.AiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@SpringBootApplication
@EnableConfigurationProperties(AiProperties.class)
public class SmartEnglishApplication {

    public static void main(String[] args) {
        loadDotEnv();
        SpringApplication.run(SmartEnglishApplication.class, args);
    }

    /**
     * Tự động nạp các biến môi trường từ file .env vào System Properties
     * Giúp hệ thống đọc được cấu hình dù chạy qua VS Code (F5) hay lệnh 'mvn spring-boot:run'
     */
    private static void loadDotEnv() {
        try {
            Path envPath = Paths.get(".env");
            if (!Files.exists(envPath)) {
                envPath = Paths.get("../.env");
            }
            if (Files.exists(envPath)) {
                List<String> lines = Files.readAllLines(envPath);
                for (String line : lines) {
                    line = line.trim();
                    if (!line.isEmpty() && !line.startsWith("#") && line.contains("=")) {
                        int eqIdx = line.indexOf('=');
                        String key = line.substring(0, eqIdx).trim();
                        String value = line.substring(eqIdx + 1).trim();
                        if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
                            value = value.substring(1, value.length() - 1);
                        }
                        // Chỉ set nếu máy chưa có biến môi trường trùng tên
                        if (System.getProperty(key) == null && System.getenv(key) == null) {
                            System.setProperty(key, value);
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }
}
