package com.fullstack.backend_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CommentCreateRequest {

    // 댓글 내용은 비어있을 수 없으며, 길이 제한을 설정합니다.
    @NotBlank(message = "댓글 내용은 필수 입력 항목입니다.")
    @Size(min = 1, max = 500, message = "댓글 내용은 1자 이상 500 자 이하로 입력해야 합니다.")
    private String content;

    // 참고:
    // - 댓글 작성자 정보 (User ID)는 SecurityContext에서 가져오므로 DTO에 포함하지 않습니다.
    // - 댓글이 달릴 게시글 ID (Post ID)는 보통 URL 경로에서 가져옵니다.
}
