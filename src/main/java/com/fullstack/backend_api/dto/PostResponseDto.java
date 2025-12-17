package com.fullstack.backend_api.dto;

import com.fullstack.backend_api.domain.Post;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder  // Entity에서 DTO로 변환 시 Builder 패턴 사용
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PostResponseDto {

    // 클라이언트에게 보여줄 모든 필드 (DB 관리 필드 포함)
    private Long id;
    @NotBlank(message = "제목은 필수 입력 사항입니다.")
    private String title;
    private String content;
    private String author;
    private LocalDateTime createdAt;

    public PostResponseDto(Post post) {
        this.id = post.getId();
        this.title = post.getTitle();
        this.content = post.getContent();
        this.author = (post.getAuthor() != null) ? post.getAuthor().getUsername() : null;
        this.createdAt = post.getCreatedAt();
    }
}
