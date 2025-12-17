package com.fullstack.backend_api.controller;

import com.fullstack.backend_api.dto.PostRequestDto;
import com.fullstack.backend_api.dto.PostResponseDto;
import com.fullstack.backend_api.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;  // HTTP 상태 코드
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;  // Spring Web 어노테이션 (@RestController, @PostMapping 등)
import org.springframework.beans.factory.annotation.Autowired;
import com.fullstack.backend_api.domain.Member;
import com.fullstack.backend_api.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController  // 이 클래스가 REST API 컨트롤러임을 Spring에게 알림
@RequiredArgsConstructor
@RequestMapping("/api/posts")  //이 컨트롤러의 기본 경로를 /api로 설정
public class PostController {

    private final PostService postService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initUser() {
        return args -> {
            // 1. 일반 사용자 ('testuser', ROLE_USER) 생성
            if (userRepository.findByUsername("testuser").isEmpty()) {
                // User 객체 생성 및 비밀번호 암호화
                Member testUser = Member.builder()
                        .username("testuser")
                        .password(passwordEncoder.encode("password123"))
                        .email("test@example.com")
                        .role("ROLE_USER")
                        .build();
                userRepository.save(testUser);
                System.out.println("테스트 사용자 'testUser'가 생성되었습니다.");
            }

            // 2. 관리자 ('adminuser', ROLE_ADMIN) 생성
            if (userRepository.findByUsername("adminuser").isEmpty()) {
                Member adminUser = Member.builder()
                        .username("adminuser")
                        .password(passwordEncoder.encode("adminpass"))
                        .email("admin@example.com")
                        .role("ROLE_ADMIN")
                        .build();
                userRepository.save(adminUser);
                System.out.println("테스트 관리자 'adminuser'가 생성되었습니다.");
            }
        };
    }

    // Read: 게시글 조회
    @GetMapping("/posts")
    public List<PostResponseDto> getPosts() {
        return postService.findAll();
    }
    @GetMapping
    public ResponseEntity<Page<PostResponseDto>> getPosts(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<PostResponseDto> posts = postService.getposts(pageable);
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/{postId}")
    public ResponseEntity<PostResponseDto> getPost(@PathVariable Long postId) {
        PostResponseDto post = postService.getPost(postId);
        return ResponseEntity.ok(post);
    }

    // Create: 게시글 생성
    @PostMapping
    public ResponseEntity<PostResponseDto> createPost(
            @RequestBody @Valid PostRequestDto requestDto,
            @AuthenticationPrincipal UserDetails userDetails) {

        String currentUsername = userDetails.getUsername();

        PostResponseDto createdPost = postService.createPost(requestDto, currentUsername);

        // 201 Created 상태 코드를 반환
        return ResponseEntity.status(HttpStatus.CREATED).body(createdPost);
    }

    // Update: 게시글 수정
    @PutMapping("/{postId}")
    public ResponseEntity<PostResponseDto> updatePost(
            @PathVariable Long postId,
            @RequestBody @Valid PostRequestDto requestDto,
            @AuthenticationPrincipal UserDetails userDetails) {

        String currentUsername = userDetails.getUsername();

        PostResponseDto updatedPost = postService.updatePost(postId, requestDto, currentUsername);

        return ResponseEntity.ok(updatedPost);
    }

    // Delete: 게시글 삭제
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        String currentUsername = userDetails.getUsername();
        postService.deletePost(postId, currentUsername);
        return ResponseEntity.noContent().build();  // 204 No Content
    }

    @PostMapping("/with-file")
    public ResponseEntity<PostResponseDto> createPostWithFile(
            @RequestPart("post") @Valid PostRequestDto requestDto,
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) throws IOException {

        PostResponseDto response = postService.createPostWithFile(requestDto, userDetails.getUsername(), file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

}
