package com.fullstack.backend_api.dto;

import com.fullstack.backend_api.domain.Member;
import com.fullstack.backend_api.domain.Post;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

// Lombok @Data를 사용하여 Getter, Setter, EqualsAndHashCode 등을 자동 생성
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostRequestDto {

    // 클라이언트가 입력할 필드만 정의 (id, createdDate 등 DB 관리 필드는 제외)
    @NotBlank(message = "제목은 필수 항목입니다.")
    private String title;

    @NotBlank(message = "내용은 필수 항목입니다.")
    private String content;
    private String author;

    public Post toEntity(Member member) {
        return Post.builder()
                .title(this.title)
                .content(this.content)
                .author(member)
                .build();
    }
}
