package com.fullstack.backend_api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private String token;     // 발급된 JWT 토큰
    private String username;
}
