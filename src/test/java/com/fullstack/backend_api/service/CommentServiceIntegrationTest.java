package com.fullstack.backend_api.service;

// 👇 도메인/엔티티는 com.fullstack.backend_api.domain 에서 import
import com.fullstack.backend_api.domain.Comment;
import com.fullstack.backend_api.domain.Post;
import com.fullstack.backend_api.domain.Member;

// 👇 DTO는 com.fullstack.backend_api.dto 에서 import
import com.fullstack.backend_api.dto.CommentCreateRequest;
import com.fullstack.backend_api.dto.CommentResponseDto;

// 👇 Repository는 com.fullstack.backend_api.repository 에서 import
import com.fullstack.backend_api.repository.CommentRepository;
import com.fullstack.backend_api.repository.PostRepository;
import com.fullstack.backend_api.repository.UserRepository;

// 👇 Service는 com.fullstack.backend_api.service 에서 import

// ... (나머지 JUnit 및 Spring Boot import)

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;


@SpringBootTest
@Transactional
public class CommentServiceIntegrationTest {

    // 필드 주입 시 패키지 경로는 신경 쓰지 않아도 됩니다. (타입만 맞으면 됨)
    @Autowired private CommentService commentService;
    @Autowired private UserRepository userRepository;
    @Autowired private PostRepository postRepository;
    @Autowired private CommentRepository commentRepository;

    private Member testUser;
    private Post testPost;

    @BeforeEach
    void setUp() {
        // 잔여 데이터 삭제
        commentRepository.deleteAll();;
        postRepository.deleteAll();
        userRepository.deleteAll();

        // 유니크한 이름 생성
        String uniqueUsername = "testUser_" + System.currentTimeMillis();

        // 1. 테스트용 User 및 Post 객체를 DB에 실제로 저장
        testUser = userRepository.save(Member.builder()
                .username(uniqueUsername)
                .password("encodePassword")
                .role("ROLE_USER")
                .build());
        testPost = postRepository.save(Post.builder()
                .title("테스트 게시글")
                .content("본문 내용")
                .author(testUser)
                .build());
    }

    @Test
    @DisplayName("I-1. 댓글 생성 통합 테스트: DB 영속화 및 관계 확인")
    void createComment_integration_success() {
        // ... (기존 로직 유지)

        Long postId = testPost.getId();
        Long authorId = testUser.getId();
        String content = "통합 테스트 댓글 내용";

        CommentCreateRequest request = new CommentCreateRequest(content);

        // When
        CommentResponseDto response = commentService.createComment(postId, request, authorId);

        // Then
        // 1. 반환된 DTO의 내용 검증
        // ...

        // 2. DB에서 Comment가 실제로 영속화되었는지 검증
        Comment savedComment = commentRepository.findById(response.getId())
                .orElseThrow(() -> new AssertionError("댓글이 DB에 저장되지 않았습니다."));

        // 3. 엔티티 관계 검증
        // ...
    }
}
