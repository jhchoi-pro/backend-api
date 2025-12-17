package com.fullstack.backend_api.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class ErrorResponse {

    private final LocalDateTime timestamp = LocalDateTime.now();
    private final String message;
    private final String status;  // HTTP 상태 코드 설명 (예: "FORBIDDEN")

    // 이 외에도 에러 코드, 상세 경로 등을 추가할 수 있습니다.
}
