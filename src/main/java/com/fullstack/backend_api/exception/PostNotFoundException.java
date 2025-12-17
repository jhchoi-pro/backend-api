package com.fullstack.backend_api.exception;

// RuntimeException을 상속받아 런타임에서 처리
public class PostNotFoundException extends RuntimeException {

    // 예외 메시지에 찾을 수 없는 ID를 포함하여 상세 정보를 제공
    public PostNotFoundException(Long id) {
        super("게시글 ID를 찾을 수 없습니다 : " + id);
    }
}
