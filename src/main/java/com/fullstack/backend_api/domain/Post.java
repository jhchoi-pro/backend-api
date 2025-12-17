package com.fullstack.backend_api.domain;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity              // 이 클래스가 데이터베이스 테이블임을 명시
@Getter
@Builder             // 객체 생성을 깔끔하게 해주는 패턴
@NoArgsConstructor(access = AccessLevel.PROTECTED)   // JPA 사용을 위한 기본 생성자 필수
@AllArgsConstructor(access = AccessLevel.PRIVATE)  // 모든 필드를 받는 생성자를 생성
public class Post extends BaseTimeEntity {

    @Id  // Primary Key
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // ID는 DB가 자동 생성
    private Long id;

    @Column(nullable = false, length = 255)  // NOT NULL 제약 및 길이 제한
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)  // 본문은 TEXT 타입으로 지정
    private String content;
    
    private String fileName;  // 저장된 파일명
    private String filePath;  // 저장된 파일 경로

    // 🤝 N:1 관계: 작성자 (User) 매핑
    // Post는 한 명의 User에 의해 작성된다.
    @ManyToOne(fetch = FetchType.LAZY)  // 지연 로딩 설정 (성능 최적화)
    @JoinColumn(name = "author_id", nullable = false)  // 외래 키 컬럼명 지정 및 NOT NULL
    private Member author;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();

    public void update(String title, String content) {
        this.title = title;
        this.content = content;
    }

    public void validateAuthor(String username) {
        if (this.author == null || !this.author.getUsername().equals(username)) {
            throw new SecurityException("권한이 없습니다. 본인 작성글만 수정/삭제할 수 있습니다.");
        }
    }

    public void updateFile(String fileName, String filePath) {
        this.fileName = fileName;
        this.filePath = filePath;
    }
}
