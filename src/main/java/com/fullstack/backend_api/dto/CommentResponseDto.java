package com.fullstack.backend_api.dto;

import com.fullstack.backend_api.domain.Comment;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CommentResponseDto {

    private final Long id;
    private final String content;
    private final LocalDateTime createdAt;
    private final LocalDateTime modifiedAt;

    // 댓글 작성자 정보 (User 엔티티를 직접 노출하지 않기 위해 별도 DTO 또는 간단한 정보만 포함)
    private final Long authorId;
    private final String authorUsername;

    /**
     * Comment 엔티티를 CommentResponse DTO로 변환하는 정적 팩토리 메서드
     * @param comment 변환할 Comment 엔티티
     * @return CommentResponseDto DTO
     */
    public static CommentResponseDto from(Comment comment) {
        // null 체크를 포함하는 것이 좋습니다.
        if (comment == null) {
            return null;
        }

        // User 객체를 참조하여 작성자 정보를 포함합니다.
        Long authorId = comment.getAuthor() != null ? comment.getAuthor().getId() : null;
        String authorUsername = comment.getAuthor() != null ? comment.getAuthor().getUsername() : "(탈퇴 사용자)";

        return CommentResponseDto.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .modifiedAt(comment.getModifiedAt())
                .authorId(authorId)
                .authorUsername(authorUsername)
                .build();
    }
}