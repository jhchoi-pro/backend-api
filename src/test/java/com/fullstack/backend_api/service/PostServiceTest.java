package com.fullstack.backend_api.service;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

import com.fullstack.backend_api.domain.Post;
import com.fullstack.backend_api.domain.Member;
import com.fullstack.backend_api.dto.PostRequestDto;
import com.fullstack.backend_api.dto.PostResponseDto;
import com.fullstack.backend_api.repository.PostRepository;
import com.fullstack.backend_api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PostService 단위 테스트")
public class PostServiceTest {

    @Mock
    private PostRepository postRepository; // 💡 실제 DB 대신 Mock 객체 사용

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PostService postService; // 💡 테스트 대상 객체 (Mock이 주입됨)

    private Member TEST_USER;
    private static final Long TEST_USER_ID = 1L;
    private static final String TEST_USER_NAME = "testuser@mockito.com";
    private Member WRITER_USER;
    private static final Long WRITER_USER_ID = 2L;
    private static final String WRITER_USER_NAME = "writer@mockito.com";

    @BeforeEach
    void setUp() {
        // setUp 또는 static 블록에서 Mock User 엔티티 초기화
        TEST_USER = Member.builder()
                .id(TEST_USER_ID)
                .username(TEST_USER_NAME)
                .role("ROLE_USER")
                .password("mockedPassword")
                .build();

        WRITER_USER = Member.builder()
                .id(WRITER_USER_ID)
                .username(WRITER_USER_NAME)
                .role("ROLE_ADMIN")
                .password("mockedPassword")
                .build();

        // PostService가 TEST_USERNAME으로 User를 찾을 때, TEST_USER를 반환하도록 Mocking
        when(userRepository.findByUsername(TEST_USER_NAME))
            .thenReturn(Optional.of(TEST_USER));

        when(userRepository.findById(TEST_USER_ID))
            .thenReturn(Optional.of(TEST_USER));
    }

    @Test
    @DisplayName("게시글 생성 성공")
    void createPost_success() {

        // Given (준비): PostRequestDto 객체를 생성
        PostRequestDto requestDto = PostRequestDto.builder()
                .title("테스트 제목")
                .content("테스트 내용")
                .build();

        Post expectedPost = Post.builder()
                .title("테스트 제목")
                .content("테스트 내용")
                .author(TEST_USER)
                .build();

        // 서비스 계층에서 변환될 Post 엔티티를 예상합니다. (author는 임의로 설정)
        // 💡 2. 서비스가 반환할 PostResponseDto를 예상합니다.
//        PostResponseDto expectedResponseDto = PostResponseDto.builder()
//                .id(savedPost.getId())
//                .title(savedPost.getTitle())
//                .content(savedPost.getContent())
//                .author(savedPost.getAuthor())
//                .build();

        // Mocking (가상 동작 정의): postRepository.save(post)가 호출되면,
        // 변환된 Post 객체를 그대로 반환하도록 정의합니다.
        when(postRepository.save(any(Post.class))).thenReturn(expectedPost);

        // When (실행): postService.createPost 메서드를 DTO와 사용자명으로 호출합니다.
        PostResponseDto createdPostDto = postService.createPost(requestDto, TEST_USER_NAME);

        // Then (검증):
        // 1. postRepository.save() 메서드가 1번 호출되었는지 검증
        verify(postRepository, times(1)).save(any(Post.class));

        // 2. 생성된 객체의 제목이 예상대로 "테스트 제목"인지 검증
        assertThat(createdPostDto.getTitle()).isEqualTo(requestDto.getTitle());
        assertThat(createdPostDto.getAuthor()).isEqualTo(TEST_USER_NAME);
    }

    @Test
    @DisplayName("게시글 생성 실패: 필수 필드 누락 (제목)")
    void createPost_failure_titleMissing() {
        // Given (준비): 제목이 빈 문자열인 DTO를 생성합니다.
        PostRequestDto requestDto = PostRequestDto.builder()
                .title("")
                .content("테스트 내용")
                .build();

        // When/Then (실행 및 검증): PostService.createPost 호출 시,
        // 지정된 예외(IllegalStateException)가 발생하는지 검증합니다.
        assertThatThrownBy(() -> postService.createPost(requestDto, TEST_USER_NAME))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("제목은 필수 항목입니다.");

        // 검증: Repository의 save 메서드가 호출되지 않았는지 확인
        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    @DisplayName("게시글 생성 실패: 필수 필드 누락 (내용)")
    void createPost_failure_contentMissing() {
        // Given (준비): 내용이 빈 문자열인 DTO를 생성합니다.
        PostRequestDto requestDto = PostRequestDto.builder()
                .title("유효한 제목")
                .content("")  // 내용 누락
                .build();

        // When/Then (실행 및 검증): PostService.createPost 호출 시,
        // 지정된 예외(IllegalStateException)가 발생하는지 검증합니다.
        assertThatThrownBy(() -> postService.createPost(requestDto, TEST_USER.getUsername()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("내용은 필수 항목입니다.");

        // 검증: Repository의 save 메서드가 호출되지 않았는지 확인
        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    @DisplayName("게시글 단건 조회 성공")
    void getPost_success() {
        // Given (준비): ID가 1L인 Post 엔티티를 생성합니다.
        Long postId = 1L;
        Post post = Post.builder()
                .id(postId)
                .title("조회 테스트")
                .content("조회 내용")
                .author(TEST_USER)
                .build();

        // Mocking: postRepository.findById(1L)이 호출되면,
        // Optional.of(post) (즉, 데이터가 존재함)를 반환하도록 정의합니다.
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        // When (실행): postService.getPost(1L) 메서드를 호출합니다.
        PostResponseDto foundPostDto = postService.getPost(postId);

        // Then (검증):
        // 1. findById()가 1번 호출되었는지 확인
        verify(postRepository, times(1)).findById(postId);

        // 2. 반환된 DTO의 ID가 예상대로 1L인지 확인
        assertThat(foundPostDto.getId()).isEqualTo(postId);
        assertThat(foundPostDto.getTitle()).isEqualTo("조회 테스트");
    }

    @Test
    @DisplayName("게시글 단건 조회 실패: 게시글 없음")
    void getPost_notFound() {
        // Given (준비): 존재하지 않는 ID를 설정합니다.
        Long notFoundId = 999L;

        // Mocking: postRepository.findById(999L)이 호출되면,
        // Optional.empty() (즉, 데이터가 없음)를 반환하도록 정의합니다.
        when(postRepository.findById(notFoundId)).thenReturn(Optional.empty());

        // When/Then (실행 및 검증): postService.getPost 호출 시,
        // 지정된 예외(IllegalArgumentException)가 발생하는지 검증합니다.
        assertThatThrownBy(() -> postService.getPost(notFoundId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("해당 게시글이 존재하지 않습니다. ID: " + notFoundId);

        // 검증: Repository의 findById 메서드가 1번 호출되었는지 확인
        verify(postRepository, times(1)).findById(notFoundId);
    }

    @Test
    @DisplayName("게시글 수정 성공")
    void updatePost_success() {
        Long postId = 1L;

        // 1. 기존 Post 엔티티 (작성자는 user@test.com)
        Post existingPost = Post.builder()
                .id(postId)
                .title("기존 제목")
                .content("기존 내용")
                .author(TEST_USER)
                .build();

        // 2. 수정 요청 DTO
        PostRequestDto updateDto = PostRequestDto.builder()
                .title("수정된 제목")
                .content("수정된 내용")
                .build();

        // Mocking 1: findById 호출 시 기존 게시글을 반환하도록 설정
        when(postRepository.findById(postId)).thenReturn(Optional.of(existingPost));

        // 참고: 서비스 로직에서 수정된 Post 엔티티를 바로 반환하거나,
        // save()를 호출하지 않고 dirty checking으로 업데이트한다고 가정합니다.
        // 저희는 간단하게 업데이트 후, 기존 엔티티의 필드를 업데이트했다고 가정합니다.

        // When (실행): postService.updatePost 메서드 호출 (인증된 사용자 user@test.com으로 가정)
        // 실제 PostService의 updatePost 메서드 시그니처가 (Long postId, PostRequestDto dto, String currentUsername) 형태여야 합니다.
        // 임시로 user@test.com을 현재 사용자로 가정하여 호출합니다.
        PostResponseDto updatedDto = postService.updatePost(postId, updateDto, TEST_USER.getUsername());

        // Then (검증):
        // 1. findById()가 1번 호출되었는지 확인
        verify(postRepository, times(1)).findById(postId);

        // 2. 반환된 DTO의 내용이 수정된 내용과 알치하는지 확인
        assertThat(updatedDto.getId()).isEqualTo(postId);
        assertThat(updatedDto.getTitle()).isEqualTo("수정된 제목");
        assertThat(updatedDto.getContent()).isEqualTo("수정된 내용");
        assertThat(updatedDto.getAuthor()).isEqualTo("testuser@mockito.com");
    }

    @Test
    @DisplayName("게시글 수정 실패: 권한 없음 (작성자 불일치)")
    void updatePost_unauthorized() {
        Long postId = 2L;

        // 1. 기존 Post 엔티티 (작성자는 writer@test.com)
        Post existingPost = Post.builder()
                .id(postId)
                .title("기존 제목")
                .content("기존 내용")
                .author(WRITER_USER)
                .build();

        // 2. 수정 요청 DTO (수정 내용은 중요하지 않음)
        PostRequestDto updateDto = PostRequestDto.builder()
                .title("새 제목")
                .content("새 내용")
                .build();

        // 3. 수정 요청자 (currentUsername은 backer@test.com)
        String unauthorizedUser = "hacker@test.com";

        // Mocking 1: findById 호출 시 기존 게시글을 반환
        when(postRepository.findById(postId)).thenReturn(Optional.of(existingPost));

        // When/Then (실행 및 검증):
        // updatePost 호출 시 SecurityException이 발생하는지 검증합니다.
        assertThatThrownBy(() -> postService.updatePost(postId, updateDto, unauthorizedUser))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("권한이 없습니다. 본인 작성글만 수정/삭제할 수 있습니다.");

        // 검증: Repository의 save/update 메서드는 호출되지 않았는지 확인
        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    @DisplayName("게시글 삭제 성공")
    void deletePost_success() {
        Long postId = 3L;

        // 1. 기존 Post 엔티티
        Post existingPost = Post.builder()
                .id(postId)
                .title("삭제 대상")
                .content("삭제 내용")
                .author(TEST_USER)
                .build();

        // Mocking 1: findById 호출 시 기존 게시글을 반환하도록 설정
        when(postRepository.findById(postId)).thenReturn(Optional.of(existingPost));

        // Mocking 2: deleteById 호출에 대해서는 별다른 반환 값이 없으므로 void로 처리
        doNothing().when(postRepository).deleteById(postId);

        // When (실행): postService.deletePost 메서드 호출
        postService.deletePost(postId, TEST_USER_NAME);

        // Then (검증):
        // 1. findById()가 1번 호출되었는지 확인
        verify(postRepository, times(1)).findById(postId);
        // 2. deleteById() 메서드가 1번 호출되었는지 확인
        verify(postRepository, times(1)).deleteById(postId);

        // Then (검증):
//        verify(postRepository, times(1)).delete(existingPost);
    }

    @Test
    @DisplayName("게시글 삭제 실패: 권한 없음 (작성자 불일치)")
    void deletePost_unauthorized() {
        Long postId = 4L;
        String OTHER_USER_NAME = "other_user";

        // 1. 기존 Post 엔티티 (작성자는 writer@test.com)
        Post existingPost = Post.builder()
                .id(postId)
                .title("삭제 대상")
                .content("삭제 내용")
                .author(TEST_USER)
                .build();

        // 2. 삭제 요청자
//        String unauthorizedUser = "hacker@test.com";

        // Mocking 1: findById 호출 시 기존 게시글을 반환
        when(postRepository.findById(postId)).thenReturn(Optional.of(existingPost));

        // When/Then (실행 및 검증):
        // deletePost 호출 시 SecurityException이 발생하는지 검증합니다.
        assertThatThrownBy(() -> postService.deletePost(postId, OTHER_USER_NAME))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("권한이 없습니다. 본인 작성글만 수정/삭제할 수 있습니다.");

        // 검증: deleteById 메서드가 호출되지 않았는지 확인 (권한 예외가 발생했으므로 호출되면 안됨)
        verify(postRepository, never()).deleteById(postId);
    }

    @Test
    @DisplayName("게시글 목록 페이징 조회 성공")
    void getPosts_paging_success() {
        // Given
        Pageable pageable = PageRequest.of(0, 5, Sort.by("createdAt").descending());
        List<Post> posts = Arrays.asList(
                Post.builder().title("제목1").content("내용1").author(TEST_USER).build(),
                Post.builder().title("제목2").content("내용2").author(TEST_USER).build()
        );
        Page<Post> postPage = new PageImpl<>(posts, pageable, posts.size());

        when(postRepository.findAll(pageable)).thenReturn(postPage);

        // When
        Page<PostResponseDto> result = postService.getposts(pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("제목1");
        verify(postRepository, times(1)).findAll(pageable);
    }
}