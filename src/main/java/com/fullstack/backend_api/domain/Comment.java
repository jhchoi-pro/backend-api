package com.fullstack.backend_api.domain;

import jakarta.persistence.*;
import lombok.*;


@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString(exclude = {"post", "author"})
public class Comment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String content; // 댓글 내용

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post; // 💡 댓글이 속한 게시글 (FK)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)  // 외래 키 컬럼명 지정 및 NOT NULL
    private Member author;

    /**
     * 비즈니스 로직: 댓글 내용 수정
     * modifiedAt은 BaseTimeEntity와 @EnableJpaAuditing에 의해 자동으로 업데이트됨
     */
    public void updateContent(String content) {
        this.content = content;
    }

    public void validateAuthor(String username) {
        if (this.author == null || !this.author.getUsername().equals(username)) {
            throw new SecurityException("댓글 권한이 없습니다.");
        }
    }
}