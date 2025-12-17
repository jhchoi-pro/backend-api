package com.fullstack.backend_api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fullstack.backend_api.BackendApiApplication;
import com.fullstack.backend_api.domain.Member;
import com.fullstack.backend_api.dto.PostRequestDto;
import com.fullstack.backend_api.dto.PostResponseDto;
import com.fullstack.backend_api.exception.GlobalExceptionHandler;
import com.fullstack.backend_api.exception.ResourceNotFoundException;
import com.fullstack.backend_api.provider.JwtTokenProvider;
import com.fullstack.backend_api.repository.PostRepository;
import com.fullstack.backend_api.repository.UserRepository;
import com.fullstack.backend_api.service.JwtService;
import com.fullstack.backend_api.service.PostService;
import com.fullstack.backend_api.service.PostUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.data.jpa.domain.support.JpaAuditingHandler;
//import org.springframework.data.jpa.mapping.JpaMappingContext;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MockMvcBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PostController.class)
@ActiveProfiles("test")  // test 환경의 application-test.properties를 사용하도록 지정합니다.
@DisplayName("PostController 단위 테스트")
public class PostControllerTest {

    @Autowired
    private WebApplicationContext context;

//    @Autowired
    private MockMvc mockMvc;  // HTTP 요청을 시뮬레이션하는 객체
    
    @Autowired
    private ObjectMapper objectMapper;  // Java 객체를 JSON으로 변환하는 객체
    
//    @Autowired
//    private PostRepository postRepository;  // 데이터 초기화를 위해 필요

    @Autowired
    private GlobalExceptionHandler globalExceptionHandler;

    @Autowired
    private PostController postController;

    @MockBean
    private PostService postService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private PostUserDetailsService postUserDetailsService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private AuthenticationConfiguration authenticationConfiguration;

    @MockBean
    private PasswordEncoder passwordEncoder;  // BCrpytPasswordEncoder 등 구현체가 있다면 Mocking 필요

    private final String API_BASE_URL = "/api/posts";
    private final String TEST_USER_NAME = "testuser@example.com";
    private final String WRITER_USER_NAME = "writer@example.com";
    private final String OTHER_USER_NAME = "otheruser@example.com";

    private final Member TEST_USER_ENTITY = Member.builder()
            .id(1L)
            .username(TEST_USER_NAME)
            .role("ROLE_USER")
            .password("mock")
            .build();

    @BeforeEach
    void setup() {
        // 모든 테스트 전에 Repository 초기화 로직이 들어갈 수 있습니다.
//        when(userRepository.findByUsername(TEST_USER_NAME))
//            .thenReturn(Optional.of(TEST_USER_ENTITY));

//        this.mockMvc = MockMvcBuilders.standaloneSetup(postController)
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // 💡 테스트에 사용할 Member를 DB에 미리 저장 (saveTestPost가 참조하도록)
        // 현재 TEST_USER_ENTITY의 ID가 1L이므로, Post 삽입 시 참조 가능해짐
        userRepository.save(TEST_USER_ENTITY);

        when(postService.createPost(any(PostRequestDto.class), eq(TEST_USER_NAME)))
            .thenReturn(PostResponseDto.builder()
                        .id(1L)
                        .author(TEST_USER_NAME)
                        .build());
    }

    // TODO: 여기에 통합 테스트 메서드를 작성합니다.

    /**
     * 인증된 (로그인된) 사용자로 요청을 생성하는 PostProcessor를 반환합니다.
     * @param username 사용자명 (Principal)
     * @param roles 부여할 권한 (예: "USER", "ADMIN")
     */
    private org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor withAuthUser(String username, String... roles) {
        // UserDetails 객체 생성 (Spring Security의 기본 동작을 시뮬레이션)
        // grantedAuthorities에 role 목록을 SimpleGrantedAuthority로 변환하여 추가합니다.
        List<GrantedAuthority> authorities = Arrays.stream(roles)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        UserDetails principal = new User(username, "", authorities);  // password는 "" 으로 설정

        // principal() 메서드를 사용하여 MockMvc 요청에 UserDetails 객체를 Principal로 주입
        return user(username).roles(roles);
    }

    @Test
    @DisplayName("게시글 생성 성공: (정상 요청, 인증된 사용자)")
    void createPost_success() throws Exception {
        // Given (준비): PostRequestDto 객체 생성
        PostRequestDto requestDto = PostRequestDto.builder()
                .title("통합 테스트 제목")
                .content("통합 테스트 내용")
                .build();

        // 💡 1. Service가 반환할 Mock 응답 DTO 생성
        PostResponseDto mockResponse = PostResponseDto.builder()
                .id(1L)
                .title("통합 테스트 제목")     // 👈 요청 DTO와 일치하는 값 설정
                .content("통합 테스트 내용")    // 👈 요청 DTO와 일치하는 값 설정
                .author(TEST_USER_NAME)
                .createdAt(LocalDateTime.now())
                .build();

        // 💡 2. Stubbing: Service가 호출될 때 Mock 응답 객체를 반환하도록 설정
        // when(MockService.method(anyArgument)).thenReturn(MockObject)
        when(postService.createPost(any(PostRequestDto.class), eq(TEST_USER_NAME)))
            .thenReturn(mockResponse); // 👈 Mock Response 반환 설정

        // When (실행): MockMvc를 통해 HTTP POST 요청 시뮬레이션
        mockMvc.perform(post(API_BASE_URL)
                .with(csrf())
                // 인증된 사용자 (USER 권한)로 요청을 보냅니다.
                .with(withAuthUser(TEST_USER_NAME, "USER"))
                .contentType(MediaType.APPLICATION_JSON)
                // DTO 객체를 JSON 문자열로 변환하여 요청 본문에 담습니다.
                .content(objectMapper.writeValueAsString(requestDto)))

        // Then (검증):
                .andExpect(status().isCreated())  // HTTP 상태 코드가 201 Created인지 검증
                .andExpect(jsonPath("$.title").value("통합 테스트 제목"))  // 반환된 JSON 필드 검증
                .andExpect(jsonPath("$.author").value(TEST_USER_NAME))  // 작성자 필드 검증
                .andDo(print());

        // DB 검증 (실제 DB에 저장되었는지 확인)
        // PostRepository를 통해 실제 DB에 데이터가 1개 저장되었는지 확인하는 로직이 추가될 수 있습니다.
        verify(postService, times(1)).createPost(any(PostRequestDto.class), eq(TEST_USER_NAME));
    }

    @Test
    @DisplayName("게시글 생성 실패: (400 Bad Request, 유효하지 않은 입력)")
    void createPost_failure_invalidInput() throws Exception {
        // Given (준비): 제목이 빈 문자열인 DTO (DTO의 @NotBlank/@NotEmpty에 의해 검증 실패)
        PostRequestDto requestDto = PostRequestDto.builder()
                .title("")
                .content("유효한 내용")
                .build();

        // When (실행): MockMvc를 통해 HTTP POST 요청 시뮬레이션
        mockMvc.perform(post(API_BASE_URL)
                .with(csrf())
                .with(withAuthUser(TEST_USER_NAME, "USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))

        // Then (검증):
                .andExpect(status().isBadRequest())  // HTTP 상태 코드가 400 Bad Request인지 검증
                .andExpect(jsonPath("$.message").exists());  // 에러 메시지 필드가 존재하는지 검증

        // DB 검증: 실패했으므로 DB에 저장되지 않았는지 확인
        verify(postService, never()).createPost(any(), any());
    }

    // 테스트에서 사용할 초기 게시글을 DB에 저장하고 ID를 반환하는 도우미 메서드
//    private Long saveTestPost(String title, String content, String author) {
//
//        Member authorEntity = userRepository.findByUsername(author)
//                .orElseThrow(() -> new ResourceNotFoundException("사용자", "ID", author));
//
//        Post post = Post.builder()
//                .title(title)
//                .content(content)
//                .author(authorEntity)
//                .build();
//        return postRepository.save(post).getId();
//    }

    @Test
    @DisplayName("게시글 단건 조회 성공: (200 OK)")
    void getPost_success() throws Exception {
        // Given (준비): PostService가 특정 ID 호출 시 Mock DTO를 반환하도록 설정
        Long postId = 1L;
        PostResponseDto mockResponse = PostResponseDto.builder()
                .id(postId)
                .title("조회 테스트 제목")
                .author(TEST_USER_NAME)
                .build();

        // 💡 Mocking: Service가 이 ID로 호출되면 Mock DTO를 반환하도록 설정
        when(postService.getPost(eq(postId))).thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(get(API_BASE_URL + "/{postId}", postId)
                .with(withAuthUser(TEST_USER_NAME)))
                .andExpect(status().isOk()) // 💡 200 OK 상태 코드 검증
                .andExpect(jsonPath("$.id").value(postId));
    }

    @Test
    @DisplayName("게시글 단건 조회 실패: (404 Not Found, 데이터 없음)")
    void getPost_notFound() throws Exception {
        // Given (준비): DB에 존재하지 않는 ID (대부분의 RDBMS에서 ID는 1부터 시작)
        Long nonExistentId = 999L;
        String expectedMessage = String.format("Post, 찾을 수 없습니다. %s : '%s'", "id", nonExistentId);

        when(postService.getPost(eq(nonExistentId)))
            .thenThrow(new ResourceNotFoundException("Post", "id", nonExistentId));

        // When (실행): MockMvc를 통해 HTTP GET 요청 시뮬레이션
        mockMvc.perform(get(API_BASE_URL + "/{postId}", nonExistentId)
                .with(withAuthUser(TEST_USER_NAME, "USER")))

                // Then (검증):
            .andExpect(status().isNotFound()) // 💡 404 Not Found 상태 코드 검증
            .andExpect(jsonPath("$.message").value(expectedMessage))
            .andDo(print());
        
        // Service 호출 검증
        verify(postService, times(1)).getPost(eq(nonExistentId));
    }

    @Test
    @DisplayName("게시글 수정 성공: (200 OK, 작성자 일치)")
    void updatePost_success() throws Exception {
        // Given (준비): PostService가 수정된 DTO를 반환하도록 Mocking
        Long postId = 1L;
        PostRequestDto updateDto = PostRequestDto.builder()
                .title("수정된 제목")
                .content("수정된 내용")
                .build();

        // 💡 Service가 호출되면 Mock Response를 반환하도록 설정
        PostResponseDto mockResponse = PostResponseDto.builder()
                .id(postId)
                .title("수정된 제목")
                .author(TEST_USER_NAME)
                .build();

        // 💡 Service Mocking: 수정 요청 시 성공적인 DTO 반환
        when(postService.updatePost(eq(postId), any(PostRequestDto.class), eq(TEST_USER_NAME)))
            .thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(put(API_BASE_URL + "/{postId}", postId)
                .with(withAuthUser(TEST_USER_NAME, "USER"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
            .andDo(print())
            .andExpect(status().isOk())  // 200 OK 상태 코드 검증
            .andExpect(jsonPath("$.title").value("수정된 제목"));
    }

    @Test
    @DisplayName("게시글 수정 실패: (403 Forbidden, 권한 없음)")
    void updatePost_failure_unauthorized() throws Exception {
        // Given (준비):
        Long postId = 1L;  // Mock ID 설정 (DB 접근 제어)

        // 수정 요청 DTO
        PostRequestDto updateDto = PostRequestDto.builder()
                .title("수정 시도 제목")
                .content("수정 시도 내용")
                .build();

        // 💡 Service Mocking:
        // PostService.updatePost가 다른 사용자(OTHER_USER_NAME)의 요청을 받을 때 SecurityException을 던지도록 설정
        doThrow(new SecurityException("수정 권한이 없습니다."))
                .when(postService)
                .updatePost(eq(postId), any(PostRequestDto.class), eq(OTHER_USER_NAME)); // 👈 다른 사용자의 Username 사용

        // When (실행): MockMvc를 통해 HTTP PUT 요청 시뮬레이션
        mockMvc.perform(put(API_BASE_URL + "/{postId}", postId)
                        // 💡 다른 사용자(OTHER_USER)로 인증하여 요청
                        .with(withAuthUser(OTHER_USER_NAME, "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))

        // Then (검증):
        .andExpect(status().isForbidden());  // 💡 403 Forbidden 상태 코드 검증
    }

    @Test
    @DisplayName("게시글 수정 실패: (401 Unauthorized, 미인증 사용자)")
    void updatePost_failure_unauthenticated() throws Exception {
        // Given (준비): 게시글 ID 준비
        Long postId = 1L;

        // 수정 요청 DTO
        PostRequestDto updateDto = PostRequestDto.builder()
                .title("미인증 시도 제목")
                .content("미인증 시도 내용")
                .build();

        // When (실행): MockMvc를 통해 HTTP PUT 요청 시뮬레이션 (인증 없이 요청)
        mockMvc.perform(put(API_BASE_URL + "/{postId}", postId)
                .with(csrf())  // CSRF 토큰은 추가하되 인증 정보는 없음
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))

        // Then (검증):
                .andExpect(status().isUnauthorized()); // 💡 Spring Security 기본 설정은 인증되지 않은 접근에 403을 반환
    }

    @Test
    @DisplayName("게시글 삭제 성공: (204 No Content, 작성자 일치)")
    void deletePost_success() throws Exception {
        // Given (준비): Service가 deletePost 호출 시 아무것도 반환하지 않음 (void)
        Long postId = 1L;

        // When & Then
        mockMvc.perform(delete(API_BASE_URL + "/{postId}", postId)
                .with(withAuthUser(TEST_USER_NAME, "ADMIN"))
                .with(csrf()))
            .andExpect(status().isNoContent()); // 💡 204 No Content 상태 코드 검증
    }

    @Test
    @DisplayName("게시글 삭제 실패: (403 Forbidden, 권한 없음)")
    void deletePost_failure_unauthorized() throws Exception {
        // Given (준비): TEST_USER가 작성한 게시글을 DB에 삽입
//        Long postId = saveTestPost("삭제 권한 테스트 제목", "삭제 권한 테스트 내용", TEST_USER_NAME);
        Long postId = 1L;

        // 💡 1. Service Mocking:
        // deletePost가 다른 사용자(OTHER_USER_NAME)의 요청을 받을 때 SecurityException을 던지도록 설정
        // ControllerTest가 Service 호출 시 권한 오류가 발생했음을 Mocking
        // Note: deletePost는 void 메서드이므로 doThrow를 사용합니다.
        doThrow(new SecurityException("삭제 권한이 없습니다."))
            .when(postService)
            .deletePost(eq(postId), eq(OTHER_USER_NAME));

        // When (실행): MockMvc를 통해 HTTP DELETE 요청 시뮬레이션
        mockMvc.perform(delete(API_BASE_URL + "/{postId}", postId)
                // 💡 다른 사용자(OTHER_USER)로 인증하여 요청
                .with(withAuthUser(OTHER_USER_NAME, "USER"))
                .with(csrf()))

        // Then (검증):
            .andExpect(status().isForbidden()); // 💡 403 Forbidden 상태 코드 검증
    }

    @Test
    @DisplayName("게시글 삭제 실패: (403 Forbidden, 권한 없는 사용자)")
    void deletePost_failure_unauthenticated() throws Exception {
        // Given (준비): 게시글 ID 준비 (MockMvc는 실제로 Service를 호출하지 않으므로 ID 값은 임의로 설정)
        Long postId = 1L;

        doThrow(new AccessDeniedException("삭제 권한이 없습니다."))
            .when(postService).deletePost(eq(postId), anyString());

        // When (실행): MockMvc를 통해 HTTP DELETE 요청 시뮬레이션
        mockMvc.perform(delete(API_BASE_URL + "/{postId}", postId)
                .with(withAuthUser("unknownUser", "GUEST"))
                .with(csrf())) // 인증 정보 없음

        // Then (검증):
            .andDo(print())
            .andExpect(status().isForbidden()); // 💡 403 Forbidden 상태 코드 검증
    }

    @Test
    @DisplayName("게시글 생성 시 파일 업로드 성공")
    void createPostWithFile_success() throws Exception {
        // 1. 모의 파일 및 DTO 준비
        MockMultipartFile file = new MockMultipartFile("file", "test.png", MediaType.IMAGE_PNG_VALUE, "content".getBytes());
        PostRequestDto requestDto = new PostRequestDto("파일 제목", "파일 내용", TEST_USER_NAME);
        MockMultipartFile postRequest = new MockMultipartFile("post", "", MediaType.APPLICATION_JSON_VALUE, objectMapper.writeValueAsBytes(requestDto));

        // 2. Mock 서비스의 동작 정의
        PostResponseDto responseDto = PostResponseDto.builder()
                .id(1L)
                .title("파일 제목")
                .content("파일 내용")
                .author(TEST_USER_NAME)
                .build();

        // any()를 사용할 때 MultipartFile.class 타입도 명시해줘야 안전합니다.
        when(postService.createPostWithFile(any(PostRequestDto.class), anyString(), any(MultipartFile.class)))
                .thenReturn(responseDto);

        // 3. multipart 요청 실행
        mockMvc.perform(multipart("/api/posts/with-file")
                        .file(file)
                        .file(postRequest)
                        .with(csrf())
                        .with(withAuthUser(TEST_USER_NAME, "USER")))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("파일 제목"));
    }

}
