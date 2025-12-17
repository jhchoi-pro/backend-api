package com.fullstack.backend_api.controller; // Controller와 같은 패키지 또는 테스트 패키지

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fullstack.backend_api.dto.CommentUpdateRequest;
import com.fullstack.backend_api.exception.GlobalExceptionHandler;
import com.fullstack.backend_api.exception.PermissionDeniedException;
import com.fullstack.backend_api.provider.JwtTokenProvider;
import com.fullstack.backend_api.service.CommentService;
import com.fullstack.backend_api.dto.CommentCreateRequest;
import com.fullstack.backend_api.dto.CommentResponseDto;
import com.fullstack.backend_api.service.JwtService;
import com.fullstack.backend_api.service.PostUserDetailsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// WebMvcTest를 사용하여 Controller 레이어만 테스트하고 Service는 Mock 처리합니다.
@WebMvcTest(CommentController.class)
@Import(GlobalExceptionHandler.class)
// Security 설정이 필요하다면 @Import(TestSecurityConfig.class) 등을 사용할 수 있습니다.
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc; // HTTP 요청 시뮬레이션 객체

    @Autowired
    private ObjectMapper objectMapper; // JSON 직렬화를 위한 객체

    // CommentController가 의존하는 Service를 Mock 처리
    @MockBean
    private CommentService commentService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private PostUserDetailsService postUserDetailsService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private static final Long TEST_USER_ID = 1L;
    private static final Long POST_ID = 10L;
    private static final String API_URL_PATTERN = "/api/posts/{postId}/comments";

    // ----------------------------------------------------------------------
    // 1. 댓글 생성 시나리오
    // ----------------------------------------------------------------------

    @Test
    @DisplayName("1-1. 댓글 생성 성공: 유효한 요청으로 200 OK 응답 확인")
    @WithMockUser(username = "1", roles = "USER") // 👈 ID=1인 사용자(USER 역할)가 로그인한 상태를 시뮬레이션
    void createComment_success() throws Exception {
        // Given
        String newContent = "새 댓글 내용입니다.";
        CommentCreateRequest request = new CommentCreateRequest(newContent);

        // 1. Service가 반환할 Mock CommentResponse 객체 준비
        CommentResponseDto mockResponse = CommentResponseDto.builder()
                .id(100L)
                .content(newContent)
                .authorId(TEST_USER_ID)
                .authorUsername("testUser")
                .createdAt(LocalDateTime.now())
                .build();

        // 2. Service Mocking: Service 호출 시 Mock Response 반환 설정
        // eq(POST_ID): postId와 정확히 일치하는 인자를 매칭
        // any(CommentCreateRequest.class): request 객체는 타입만 매칭
        // eq(TEST_USER_ID): WithMockUser의 username이 String이지만, Controller에서 Long으로 변환되어 전달되는 ID 매칭
        when(commentService.createComment(
                eq(POST_ID),
                any(CommentCreateRequest.class),
                eq(TEST_USER_ID) // Security Context에서 추출된 ID
        )).thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(post(API_URL_PATTERN, POST_ID)
                        .with(csrf()) // POST, PUT, DELETE 요청에는 CSRF 토큰 필요
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk()) // 👈 HTTP 200 OK 응답 검증
                .andExpect(jsonPath("$.id").value(100L)) // 응답 본문의 ID 검증
                .andExpect(jsonPath("$.content").value(newContent)) // 응답 본문의 내용 검증
                .andExpect(jsonPath("$.authorId").value(TEST_USER_ID)); // 작성자 ID 검증
    }

    @Test
    @DisplayName("2-1. 댓글 생성 실패: 미인증 사용자는 401 Unauthorized 응답")
    void createComment_unauthenticated_fail() throws Exception {
        // Given
        Long UNAUTHENTICATED_USER_ID = 999L; // 사용되지 않지만 명시적으로 선언
        CommentCreateRequest request = new CommentCreateRequest("미인증 사용자의 댓글");

        // Service Mocking은 필요 없음: Controller 진입 전에 Security Filter에서 차단되기 때문

        // When & Then
        mockMvc.perform(post(API_URL_PATTERN, POST_ID)
                        .with(csrf()) // CSRF 토큰은 포함해도 무방하지만, 인증이 먼저 실패
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized()); // 👈 HTTP 401 응답 검증
    }

    @Test
    @DisplayName("2-2. 댓글 생성 실패: 유효성 검사 실패 시 400 Bad Request 응답")
    @WithMockUser(username = "1", roles = "USER")
    void createComment_validation_fail() throws Exception {
        // Given
        // 1. DTO 유효성 검사 실패 조건: content가 Blank이거나 null인 경우
        CommentCreateRequest invalidRequest = new CommentCreateRequest("");

        // Service Mocking: 유효성 검사가 Controller에서 실패하므로 Service는 호출되지 않음

        // When & Then
        mockMvc.perform(post(API_URL_PATTERN, POST_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest()) // 👈 HTTP 400 응답 검증
                // (선택 사항) 응답 본문에 DTO의 에러 메시지가 포함되어 있는지 검증 가능
                .andExpect(jsonPath("$.message").exists());

        // Service가 호출되지 않았음을 검증 (선택적)
        // verify(commentService, never()).createComment(any(), any(), any());
    }

    @Test
    @DisplayName("3-1. 댓글 목록 조회 성공: Pageable 파라미터 및 응답 형식 검증")
    @WithMockUser(username = "1", roles = "USER")
    void getCommentsByPostId_success() throws Exception {
        // Given
        Long POST_ID = 10L;
        int pageNumber = 1;
        int pageSize = 5;

        // 1. Service가 반환할 Mock Page<CommentResponse> 객체 준비
        CommentResponseDto mockComment1 = CommentResponseDto.builder()
                .id(101L).content("댓글 1").authorId(1L).authorUsername("user1")
                .createdAt(LocalDateTime.now()).build();
        CommentResponseDto mockComment2 = CommentResponseDto.builder()
                .id(102L).content("댓글 2").authorId(2L).authorUsername("user2")
                .createdAt(LocalDateTime.now()).build();

        List<CommentResponseDto> mockList = List.of(mockComment1, mockComment2);

        // 2. Pageable 객체 준비 (Service Mocking에 사용)
        // Controller로 넘어오는 Pageable의 기본값은 0페이지, 20개이므로,
        // 테스트에서는 요청 파라미터에 맞게 Service가 호출되는지 확인하는 것이 중요합니다.

        // 실제 Service 호출 시 Pageable 객체가 어떻게 생성되어 전달되는지 예상하고 Mocking
        // 여기서는 PageRequest.of(pageNumber, pageSize, Sort)가 전달된다고 가정합니다.
        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<CommentResponseDto> mockPage = new PageImpl<>(mockList, pageable, 12); // 총 12개 중 2개 반환

        // 3. Service Mocking: findAllByPostId 호출 시 Mock Page 반환 설정
        when(commentService.getCommentsByPostId(
                eq(POST_ID),
                any(Pageable.class) // 실제로는 쿼리 파라미터에 의해 생성된 Pageable 객체가 들어옴
        )).thenReturn(mockPage);

        // When & Then
        // GET 요청: /api/posts/{postId}/comments?page=1&size=5&sort=createdAt,desc
        mockMvc.perform(get(API_URL_PATTERN, POST_ID)
                        .param("page", String.valueOf(pageNumber)) // 페이지 번호 파라미터
                        .param("size", String.valueOf(pageSize))   // 페이지 크기 파라미터
                        .param("sort", "createdAt,desc")         // 정렬 파라미터
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()) // 👈 HTTP 200 OK 응답 검증

                // 4. 응답 본문 (Page DTO 형식) 검증
                .andExpect(jsonPath("$.content").isArray()) // content 필드가 배열인지
                .andExpect(jsonPath("$.content.length()").value(2)) // content 배열의 크기 검증
                .andExpect(jsonPath("$.totalPages").value(3)) // 총 페이지 수 검증 (12 / 5 = 2.4 -> 3)
                .andExpect(jsonPath("$.totalElements").value(12)) // 전체 요소 수 검증
                .andExpect(jsonPath("$.number").value(pageNumber)) // 현재 페이지 번호 검증 (1)

                // 5. content 내부 데이터 검증 (첫 번째 댓글)
                .andExpect(jsonPath("$.content[0].id").value(101L))
                .andExpect(jsonPath("$.content[0].content").value("댓글 1"));
    }

    @Test
    @DisplayName("4-1. 댓글 수정 성공: 작성자 본인 요청 시 200 OK 응답 확인")
    @WithMockUser(username = "1", roles = "USER") // 작성자 ID = 1
    void updateComment_owner_success() throws Exception {
        // Given
        Long COMMENT_ID = 200L;
        String updatedContent = "컨트롤러에서 요청한 수정 내용";
        CommentUpdateRequest request = new CommentUpdateRequest(updatedContent);

        // 1. Service가 반환할 Mock CommentResponse 객체 준비
        CommentResponseDto mockResponse = CommentResponseDto.builder()
                .id(COMMENT_ID).content(updatedContent).authorId(1L)
                .authorUsername("user1").createdAt(LocalDateTime.now()).build();

        // 2. Service Mocking: Service 호출 시 Mock Response 반환 설정
        when(commentService.updateComment(
                eq(COMMENT_ID),
                any(CommentUpdateRequest.class),
                eq(1L), // @WithMockUser의 ID
                any(Collection.class) // USER 역할
        )).thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(put("/api/comments/{commentId}", COMMENT_ID) // PUT 매핑 사용
                        .with(csrf()) // PUT 요청에는 CSRF 토큰 필수
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk()) // 👈 HTTP 200 OK 응답 검증
                .andExpect(jsonPath("$.content").value(updatedContent));
    }

    @Test
    @DisplayName("4-2. 댓글 수정 실패: 권한 부족 시 403 Forbidden 응답 확인")
    @WithMockUser(username = "2", roles = "USER") // 타인 ID = 2
    void updateComment_unauthorized_fail() throws Exception {
        // Given
        Long COMMENT_ID = 200L;
        CommentUpdateRequest request = new CommentUpdateRequest("타인의 댓글 수정 시도");

        // 1. Service Mocking: Service가 PermissionDeniedException을 던지도록 설정
        // ID 2L 사용자가 COMMENT_ID를 수정하려 할 때 예외 발생
        when(commentService.updateComment(
                eq(COMMENT_ID),
                any(CommentUpdateRequest.class),
                eq(2L), // @WithMockUser의 ID
                any(Collection.class)
        )).thenThrow(new PermissionDeniedException("댓글을 수정할 권한이 없습니다."));

        // When & Then
        mockMvc.perform(put("/api/comments/{commentId}", COMMENT_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden()); // 👈 HTTP 403 Forbidden 응답 검증

        // (선택적) 응답 본문에 에러 메시지가 포함되어 있는지 검증 가능
    }

    @Test
    @DisplayName("5-1. 댓글 삭제 성공: 작성자 본인 요청 시 204 No Content 응답 확인")
    @WithMockUser(username = "1", roles = "USER") // 작성자 ID = 1
    void deleteComment_owner_success() throws Exception {
        // Given
        Long COMMENT_ID = 300L;

        // 1. Service Mocking: Service는 void를 반환하므로 doNothing() 설정
        doNothing().when(commentService).deleteComment(
                eq(COMMENT_ID),
                eq(1L), // @WithMockUser의 ID
                any(Collection.class) // USER 역할
        );

        // When & Then
        mockMvc.perform(delete("/api/comments/{commentId}", COMMENT_ID) // DELETE 매핑 사용
                        .with(csrf())) // DELETE 요청에는 CSRF 토큰 필수
                .andExpect(status().isNoContent()); // 👈 HTTP 204 No Content 응답 검증

        // 2. Service가 실제로 호출되었는지 검증
        verify(commentService, times(1)).deleteComment(
                eq(COMMENT_ID),
                eq(1L),
                any(Collection.class)
        );
    }

    @Test
    @DisplayName("5-2. 댓글 삭제 실패: 권한 부족 시 403 Forbidden 응답 확인")
    @WithMockUser(username = "2", roles = "USER") // 타인 ID = 2
    void deleteComment_unauthorized_fail() throws Exception {
        // Given
        Long COMMENT_ID = 300L;

        // 1. Service Mocking: PermissionDeniedException을 던지도록 설정
        // ID 2L 사용자가 COMMENT_ID를 삭제하려 할 때 예외 발생
        doThrow(new PermissionDeniedException("댓글을 삭제할 권한이 없습니다.")).when(commentService).deleteComment(
                eq(COMMENT_ID),
                eq(2L), // @WithMockUser의 ID
                any(Collection.class)
        );

        // When & Then
        mockMvc.perform(delete("/api/comments/{commentId}", COMMENT_ID)
                        .with(csrf()))
                .andExpect(status().isForbidden()); // 👈 HTTP 403 Forbidden 응답 검증

        // Service가 호출되었으나 예외로 종료되었음을 검증
        verify(commentService, times(1)).deleteComment(
                eq(COMMENT_ID),
                eq(2L),
                any(Collection.class)
        );
    }

}