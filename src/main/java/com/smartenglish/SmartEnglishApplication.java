package com.smartenglish;

import com.smartenglish.config.AiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AiProperties.class)
public class SmartEnglishApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartEnglishApplication.class, args);
    }
}

