package com.fullstack.backend_api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration  // Spring 설정 파일임을 명시
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")           // 1. "/api"로 시작하는 모든 요청에 대해 CORS 적용
                .allowedOrigins("http://localhost:3000")   // 2. React 서버의 Origin 명시
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")  // 3. 모든 HTTP 메서드 허용
                .allowedHeaders("*")      // 모든 헤더 허용
                .allowCredentials(true);  // 인증 정보(쿠키 등) 허용
    }
}
