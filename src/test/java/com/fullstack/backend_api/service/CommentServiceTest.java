package com.fullstack.backend_api.service;

import com.fullstack.backend_api.domain.Comment;
import com.fullstack.backend_api.domain.Post;
import com.fullstack.backend_api.domain.Member;
import com.fullstack.backend_api.dto.CommentCreateRequest;
import com.fullstack.backend_api.dto.CommentResponseDto;
import com.fullstack.backend_api.dto.CommentUpdateRequest;
import com.fullstack.backend_api.exception.PermissionDeniedException;
import com.fullstack.backend_api.exception.ResourceNotFoundException;
import com.fullstack.backend_api.repository.CommentRepository;
import com.fullstack.backend_api.repository.PostRepository;
import com.fullstack.backend_api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommentService 단위 테스트")
public class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CommentService commentService;

    // 테스트용 사용자 및 데이터
    private final Long TEST_USER_ID = 1L;
    private final String TEST_USER_NAME = "testUser";
    private final Long COMMENT_ID = 100L;
    private Comment existingComment;
    private CommentUpdateRequest updateRequest;

    // 테스트 작성자 User 객체
    private final Member TEST_USER = Member.builder().id(TEST_USER_ID).username(TEST_USER_NAME).build();

    // 다른 사용자 User 객체
    private final Member OTHER_USER = Member.builder().id(2L).username("otherUser").build();

    @BeforeEach
    void setUp() {
        // 1. Mock Authentication 설정: TEST_USER (USER 역할)이 로그인했다고 가정
        UserDetails userDetails = new org.springframework.security.core.userdetails.User(String.valueOf(TEST_USER_ID),
                "dummy_password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));

        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 2. 업데이트 요청 DTO 준비
        updateRequest = new CommentUpdateRequest("수정된 댓글 내용입니다.");
    }

    @Test
    @DisplayName("1-1. 댓글 생성 성공")
    void createComment_success() {
        // Given (준비):
        Long POST_ID = 50L;
        String newContent = "새로 작성한 댓글입니다.";
        CommentCreateRequest createRequest = new CommentCreateRequest(newContent);

        // 1. Mock 데이터 준비
        Post mockPost = Post.builder().id(POST_ID).title("제목").build(); // Post 객체
        Member author = TEST_USER; // 작성자 User 객체

        // 2. Repository Mocking: Service가 의존하는 모든 Repository Stubbing
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(mockPost));
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(author));

        // 3. save() Mocking: 저장된 객체가 반환된다고 가정 (Comment 엔티티 빌더 필요)
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            // 저장 시 ID와 시간을 부여한다고 가정
            Comment savedComment = Comment.builder()
                    .id(200L) // 새로 생성된 ID
                    .content(comment.getContent())
                    .author(comment.getAuthor())
                    .post(comment.getPost())
                    .build();
            return savedComment;
        });

        // When
        CommentResponseDto response = commentService.createComment(POST_ID, createRequest, TEST_USER_ID);

        // Then
        // 1. Repository 상호작용 검증
        verify(postRepository, times(1)).findById(POST_ID);
        verify(userRepository, times(1)).findById(TEST_USER_ID);
        verify(commentRepository, times(1)).save(any(Comment.class));

        // 2. 응답 내용 검증
        assertThat(response.getContent()).isEqualTo(newContent);
        assertThat(response.getAuthorId()).isEqualTo(TEST_USER_ID);
        assertThat(response.getAuthorUsername()).isEqualTo(TEST_USER_NAME);
        assertThat(response.getId()).isNotNull();
    }

    @Test
    @DisplayName("1-2. 댓글 생성 실패: 존재하지 않는 게시글 ID")
    void createComment_invalidPostId_failure() {
        // Given
        Long INVALID_POST_ID = 8888L;
        CommentCreateRequest createRequest = new CommentCreateRequest("댓글 내용");

        // 1. Repository Mocking: PostRepository가 빈 Optional을 반환하도록 설정
        // 이로 인해 Service가 ResourceNotFoundException을 던지게 됩니다.
        when(postRepository.findById(INVALID_POST_ID)).thenReturn(Optional.empty());
        // 2. UserRepository는 호출되지 않거나, 성공적으로 User를 반환한다고 가정

        // When & Then
        assertThatThrownBy(() ->
                commentService.createComment(
                        INVALID_POST_ID,
                        createRequest,
                        TEST_USER_ID
                )
        ).isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("게시글") // 게시글 리소스를 찾지 못했는지 확인
                .hasMessageContaining(String.valueOf(INVALID_POST_ID));

        // 추가 검증: save()는 당연히 호출되지 않았는지 확인
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    @DisplayName("1-3. 댓글 생성 실패: 존재하지 않는 사용자 ID")
    void createComment_invalidUserId_failure() {
        // Given
        Long POST_ID = 50L;
        Long INVALID_USER_ID = 999L;
        CommentCreateRequest createRequest = new CommentCreateRequest("댓글 내용");

        // 1. Repository Mocking: PostRepository는 성공적으로 Post를 반환한다고 가정
        Post mockPost = Post.builder().id(POST_ID).title("제목").build();
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(mockPost));

        // 2. UserRepository Mocking: UserRepository가 빈 Optional을 반환하도록 설정
        when(userRepository.findById(INVALID_USER_ID)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() ->
                commentService.createComment(
                        POST_ID,
                        createRequest,
                        INVALID_USER_ID // 👈 존재하지 않는 사용자 ID 전달
                )
        ).isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("사용자") // 사용자 리소스를 찾지 못했는지 확인
                .hasMessageContaining(String.valueOf(INVALID_USER_ID));

        // 추가 검증: save()는 당연히 호출되지 않았는지 확인
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    @DisplayName("2-1. 댓글 수정 성공: 작성자 본인이 자신의 댓글을 수정")
    void updateComment_owner_success() {
        // Given
        // 1. 기존 Comment 객체 준비 (작성자: TEST_USER)
        LocalDateTime initialTime = LocalDateTime.now().minusMinutes(5);
        Comment existingComment = Comment.builder()
                .id(COMMENT_ID)
                .author(TEST_USER) // 작성자가 로그인된 TEST_USER와 일치
                .content("원래 댓글 내용입니다.")
                // Post 객체도 필요하다면 여기에 추가 .post(...)
                .build();

        // 2. Repository Mocking
        when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(existingComment));

        // 3. ArgumentCaptor 준비
        ArgumentCaptor<Comment> commentCaptor = ArgumentCaptor.forClass(Comment.class);
        when(commentRepository.save(commentCaptor.capture())).thenReturn(existingComment);

        // When
        // currentUserId와 roles는 SecurityContext에서 가져오거나, 메서드 시그니처에 맞게 직접 넘겨줍니다.
        commentService.updateComment(
                COMMENT_ID,
                updateRequest,
                TEST_USER_ID, // Service 메서드 시그니처에 따라 ID 전달
                Collections.singletonList("ROLE_USER") // Service 메서드 시그니처에 따라 Roles 전달
        );

        // Then
        // 1. Repository 상호작용 검증
        verify(commentRepository, times(1)).findById(COMMENT_ID);
        verify(commentRepository, times(1)).save(any(Comment.class));

        // 2. 캡처된 객체를 통해 데이터 검증
        Comment savedComment = commentCaptor.getValue();

        // 내용이 업데이트 되었는지 확인
        assertThat(savedComment.getContent()).isEqualTo(updateRequest.getContent());

        // 수정 시간이 업데이트 되었는지 확인 (Service 로직에서 시간을 변경했다고 가정)
//        assertThat(savedComment.getModifiedAt()).isAfter(initialTime);
    }

    @Test
    @DisplayName("2-2. 댓글 수정 성공: ADMIN이 타인의 댓글을 수정")
    void updateComment_admin_success() {
        // Given
        Long OTHER_USER_ID = 2L;
        String OTHER_USERNAME = "otherUser";

        // 1. ADMIN Authentication 설정 (TEST_USER가 ADMIN 역할을 수행)
        // Service 메서드에 전달할 Role 목록
        Collection<String> adminRoles = Collections.singletonList("ROLE_ADMIN");

        // UserDetails는 ADMIN 역할로 설정 (이 코드는 BeforeEach의 설정 대신 직접 정의하여 오버라이딩한다고 가정)
        UserDetails adminDetails = new org.springframework.security.core.userdetails.User(
                String.valueOf(TEST_USER_ID), // ID는 1L 그대로 사용
                "dummy_password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        Authentication adminAuth = new UsernamePasswordAuthenticationToken(adminDetails, null, adminDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(adminAuth);

        // 2. 타인(OTHER_USER)이 작성한 Comment 객체 준비
        Member otherAuthor = Member.builder().id(OTHER_USER_ID).username(OTHER_USERNAME).build();
        LocalDateTime initialTime = LocalDateTime.now().minusMinutes(5);

        Comment existingComment = Comment.builder()
                .id(COMMENT_ID)
                .author(otherAuthor) // 👈 타인(ID=2L)이 작성자로 설정
                .content("ADMIN이 수정할 내용")
                .build();

        CommentUpdateRequest updateRequest = new CommentUpdateRequest("ADMIN이 수정 완료한 내용!");

        // 3. Repository Mocking
        when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(existingComment));
        ArgumentCaptor<Comment> commentCaptor = ArgumentCaptor.forClass(Comment.class);
        when(commentRepository.save(commentCaptor.capture())).thenReturn(existingComment);

        // When
        commentService.updateComment(
                COMMENT_ID,
                updateRequest,
                TEST_USER_ID, // 👈 ADMIN 권한의 사용자 ID 전달 (1L)
                adminRoles
        );

        // Then
        // 1. Repository 상호작용 검증
        verify(commentRepository, times(1)).save(any(Comment.class));

        // 2. 캡처된 객체를 통해 데이터 검증
        Comment savedComment = commentCaptor.getValue();

        // 내용이 ADMIN의 요청대로 업데이트 되었는지 확인
        assertThat(savedComment.getContent()).isEqualTo(updateRequest.getContent());

        // 🚨 중요: 작성자는 ADMIN이 아닌 원래의 타인(OTHER_USER)으로 유지되는지 확인
        assertThat(savedComment.getAuthor().getId()).isEqualTo(OTHER_USER_ID);
    }

    @Test
    @DisplayName("2-3. 댓글 수정 실패: 일반 USER가 타인의 댓글을 수정 시도 (권한 부족)")
    void updateComment_unauthorized_failure() {
        // Given
        Long OTHER_USER_ID = 99L;
        Member otherAttacker = Member.builder().id(OTHER_USER_ID).username("attacker").build();

        // 1. Authentication 설정: 타인(OTHER_USER)이 로그인했다고 가정
        UserDetails otherUserDetails = new org.springframework.security.core.userdetails.User(
                String.valueOf(OTHER_USER_ID),
                "dummy_password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
        Authentication otherAuth = new UsernamePasswordAuthenticationToken(otherUserDetails, null, otherUserDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(otherAuth);

        // 2. TEST_USER가 작성한 기존 Comment 객체 준비
        Comment existingComment = Comment.builder()
                .id(COMMENT_ID)
                .author(TEST_USER) // 👈 작성자는 ID=1L
                .content("TEST_USER의 댓글")
                .build();

        CommentUpdateRequest updateRequest = new CommentUpdateRequest("공격 시도 내용");

        // 3. Repository Mocking
        when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(existingComment));

        // When & Then
        // updateComment 호출 시 PermissionDeniedException이 발생하는지 검증
        assertThatThrownBy(() ->
                commentService.updateComment(
                        COMMENT_ID,
                        updateRequest,
                        OTHER_USER_ID, // 👈 로그인 ID는 99L
                        Collections.singletonList("ROLE_USER")
                )
        ).isInstanceOf(PermissionDeniedException.class)
                .hasMessageContaining("댓글을 수정할 권한이 없습니다."); // 예외 메시지 확인

        // 추가 검증: save()는 호출되지 않았는지 확인 (권한 검사에서 예외 발생으로 로직 중단)
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    @DisplayName("2-4. 댓글 수정 실패: 미인증 사용자 (currentUserId가 null)")
    void updateComment_unauthenticated_failure() {
        // Given
        // 1. Authentication 설정 제거: SecurityContext를 비워 미인증 상태 시뮬레이션
        SecurityContextHolder.getContext().setAuthentication(null);

        // 2. TEST_USER가 작성한 기존 Comment 객체 준비 (리소스는 존재함)
        Comment existingComment = Comment.builder()
                .id(COMMENT_ID)
                .author(TEST_USER) // 작성자는 TEST_USER
                .content("미인증 사용자가 수정 시도할 댓글")
                .build();

        CommentUpdateRequest updateRequest = new CommentUpdateRequest("공격 시도 내용");

        // 3. Repository Mocking
        when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(existingComment));

        // When & Then
        // updateComment 호출 시, currentUserId에 null을 전달하고, 권한이 없으므로 PermissionDeniedException 발생을 기대
        assertThatThrownBy(() ->
                commentService.updateComment(
                        COMMENT_ID,
                        updateRequest,
                        null, // 👈 currentUserId를 null로 전달하여 미인증 시뮬레이션
                        Collections.emptyList() // 역할 목록도 비어있음
                )
        ).isInstanceOf(PermissionDeniedException.class)
                .hasMessageContaining("댓글을 수정할 권한이 없습니다.");

        // 추가 검증: save()는 호출되지 않았는지 확인
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    @DisplayName("2-5. 댓글 수정 실패: 존재하지 않는 댓글 ID")
    void updateComment_invalidId_failure() {
        // Given
        Long INVALID_COMMENT_ID = 9999L;
        CommentUpdateRequest updateRequest = new CommentUpdateRequest("수정 시도 내용");

        // 1. Repository Mocking: findById 호출 시 빈 Optional 반환 설정
        when(commentRepository.findById(INVALID_COMMENT_ID)).thenReturn(Optional.empty());

        // When & Then
        // updateComment 호출 시 ResourceNotFoundException이 발생하는지 검증
        assertThatThrownBy(() ->
                commentService.updateComment(
                        INVALID_COMMENT_ID,
                        updateRequest,
                        TEST_USER_ID,
                        Collections.singletonList("ROLE_USER")
                )
        ).isInstanceOf(ResourceNotFoundException.class)
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("댓글")
                .hasMessageContaining("찾을 수 없습니다.")
                .hasMessageContaining(String.valueOf(INVALID_COMMENT_ID));

        // 추가 검증: save()는 호출되지 않았는지 확인
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    @DisplayName("3-1. 댓글 삭제 성공: 작성자 본인이 자신의 댓글을 삭제")
    void deleteComment_owner_success() {
        // Given
        // 1. 기존 Comment 객체 준비 (작성자: TEST_USER)
        Comment existingComment = Comment.builder()
                .id(COMMENT_ID)
                .author(TEST_USER)
                .content("삭제될 댓글")
                .build();

        // 2. Repository Mocking
        when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(existingComment));

        // When
        commentService.deleteComment(
                COMMENT_ID,
                TEST_USER_ID,
                Collections.singletonList("ROLE_USER")
        );

        // Then
        // delete() 메소드가 정확히 1번 호출되었는지 검증
        verify(commentRepository, times(1)).findById(COMMENT_ID);
        // 🚨 핵심 검증: delete() 메소드가 호출되었는지 확인
        verify(commentRepository, times(1)).delete(existingComment);
        verify(commentRepository, never()).save(any(Comment.class)); // save는 호출되지 않아야 함
    }

    @Test
    @DisplayName("3-2. 댓글 삭제 실패: 일반 USER가 타인의 댓글 삭제 시도 (권한 부족)")
    void deleteComment_unauthorized_failure() {
        // Given
        Long OTHER_USER_ID = 99L;

        // 1. Authentication 설정: 타인(OTHER_USER)이 로그인했다고 가정
        // (BeforeEach에서 설정된 TEST_USER 대신 임시로 OTHER_USER 권한 설정 필요)

        // 2. TEST_USER가 작성한 기존 Comment 객체 준비
        Comment existingComment = Comment.builder()
                .id(COMMENT_ID)
                .author(TEST_USER) // 👈 작성자는 ID=1L
                .content("TEST_USER의 댓글")
                .build();

        // 3. Repository Mocking
        when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(existingComment));

        // When & Then
        // deleteComment 호출 시 PermissionDeniedException이 발생하는지 검증
        assertThatThrownBy(() ->
                commentService.deleteComment(
                        COMMENT_ID,
                        OTHER_USER_ID, // 👈 로그인 ID는 99L
                        Collections.singletonList("ROLE_USER")
                )
        ).isInstanceOf(PermissionDeniedException.class)
                .hasMessageContaining("댓글을 삭제할 권한이 없습니다.");

        // 🚨 핵심 검증: delete()는 호출되지 않았는지 확인 (권한 검사에서 예외 발생으로 로직 중단)
        verify(commentRepository, never()).delete(any(Comment.class));
    }

    @Test
    @DisplayName("3-3. 댓글 삭제 성공: ADMIN이 타인의 댓글을 삭제")
    void deleteComment_admin_success() {
        // Given
        Long OTHER_USER_ID = 2L;
        Member otherAuthor = Member.builder().id(OTHER_USER_ID).username("otherUser").build();

        // 1. ADMIN 권한 및 역할 설정
        Collection<String> adminRoles = Collections.singletonList("ROLE_ADMIN");

        // 2. 타인(OTHER_USER)이 작성한 Comment 객체 준비
        Comment existingComment = Comment.builder()
                .id(COMMENT_ID)
                .author(otherAuthor) // 👈 타인(ID=2L)이 작성자로 설정
                .content("ADMIN이 삭제할 댓글")
                .build();

        // 3. Repository Mocking
        when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(existingComment));

        // When
        commentService.deleteComment(
                COMMENT_ID,
                TEST_USER_ID, // 👈 ADMIN 권한의 사용자 ID 전달 (1L)
                adminRoles
        );

        // Then
        // delete() 메소드가 1번 호출되었는지 확인
        verify(commentRepository, times(1)).findById(COMMENT_ID);
        verify(commentRepository, times(1)).delete(existingComment);
    }

    @Test
    @DisplayName("3-4. 댓글 삭제 실패: 존재하지 않는 댓글 ID")
    void deleteComment_invalidId_failure() {
        // Given
        Long INVALID_COMMENT_ID = 9999L;

        // 1. Repository Mocking: findById 호출 시 빈 Optional 반환 설정
        when(commentRepository.findById(INVALID_COMMENT_ID)).thenReturn(Optional.empty());

        // When & Then
        // deleteComment 호출 시 ResourceNotFoundException이 발생하는지 검증
        assertThatThrownBy(() ->
                commentService.deleteComment(
                        INVALID_COMMENT_ID,
                        TEST_USER_ID,
                        Collections.singletonList("ROLE_USER")
                )
        ).isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("댓글") // 유연한 메시지 검증
                .hasMessageContaining("찾을 수 없습니다.");

        // delete()는 호출되지 않았는지 확인
        verify(commentRepository, never()).delete(any(Comment.class));
    }

    @Test
    @DisplayName("4-1. 댓글 목록 조회 성공: Pageable 조건에 맞게 DTO 변환 확인")
    void getCommentsByPostId_success() {
        // Given
        Long POST_ID = 50L;
        int pageSize = 3;

        // 1. Pageable 객체 설정 (0페이지, 사이즈 3)
        Pageable pageable = PageRequest.of(0, pageSize, Sort.by("createdAt").descending());

        // 2. Mock 데이터 준비: 3개의 Comment 엔티티 생성
        List<Comment> mockComments = List.of(
                Comment.builder().id(3L).author(TEST_USER).content("세 번째 댓글").build(),
                Comment.builder().id(2L).author(TEST_USER).content("두 번째 댓글").build(),
                Comment.builder().id(1L).author(TEST_USER).content("첫 번째 댓글").build()
        );

        // 3. Page<Comment> Mocking (총 10개 중 3개를 반환한다고 가정)
        Page<Comment> mockPage = new PageImpl<>(mockComments, pageable, 10);
        when(commentRepository.findAllByPostId(POST_ID, pageable)).thenReturn(mockPage);

        // When
        Page<CommentResponseDto> responsePage = commentService.getCommentsByPostId(POST_ID, pageable);

        // Then
        // 1. Repository 상호작용 검증
        verify(commentRepository, times(1)).findAllByPostId(eq(POST_ID), eq(pageable));

        // 2. Pageable 결과 검증
        assertThat(responsePage.getContent()).hasSize(pageSize); // 크기 검증
        assertThat(responsePage.getTotalElements()).isEqualTo(10); // 전체 요소 수 검증
        assertThat(responsePage.getNumber()).isEqualTo(0); // 페이지 번호 검증

        // 3. DTO 변환 및 정렬 검증 (가장 최신 댓글이 목록의 첫 번째인지 확인)
        CommentResponseDto firstComment = responsePage.getContent().get(0);
        assertThat(firstComment.getId()).isEqualTo(3L); // ID 검증
        assertThat(firstComment.getContent()).isEqualTo("세 번째 댓글"); // 내용 검증
        assertThat(firstComment.getAuthorId()).isEqualTo(TEST_USER_ID); // 작성자 ID 검증
    }

    @Test
    @DisplayName("4-2. 댓글 목록 조회 성공: 댓글이 없는 경우 빈 Page 반환")
    void getCommentsByPostId_empty() {
        // Given
        Long POST_ID_WITHOUT_COMMENTS = 90L;
        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());

        // 1. 빈 Page<Comment> Mocking
        Page<Comment> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(commentRepository.findAllByPostId(POST_ID_WITHOUT_COMMENTS, pageable)).thenReturn(emptyPage);

        // When
        Page<CommentResponseDto> responsePage = commentService.getCommentsByPostId(POST_ID_WITHOUT_COMMENTS, pageable);

        // Then
        // 1. Repository 상호작용 검증
        verify(commentRepository, times(1)).findAllByPostId(eq(POST_ID_WITHOUT_COMMENTS), eq(pageable));

        // 2. 결과 검증
        assertThat(responsePage.getContent()).isEmpty(); // 내용이 비어있는지 확인
        assertThat(responsePage.getTotalElements()).isEqualTo(0); // 전체 요소 수가 0인지 확인
        assertThat(responsePage.isLast()).isTrue(); // 마지막 페이지인지 확인
    }

}