package com.fullstack.backend_api.service;

import com.fullstack.backend_api.domain.Post;
import com.fullstack.backend_api.domain.Member;
import com.fullstack.backend_api.dto.PostRequestDto;
import com.fullstack.backend_api.dto.PostResponseDto;
import com.fullstack.backend_api.exception.ResourceNotFoundException;
import com.fullstack.backend_api.repository.PostRepository;
import com.fullstack.backend_api.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
public class PostService {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional(readOnly = true)
    public PostResponseDto getPost(Long postId) {

        // 1. Repository를 통해 ID로 Post 엔티티를 찾습니다.
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("해당 게시글이 존재하지 않습니다. ID: " + postId));

        // 2. Entity -> ResponseDto로 변환 후 반환합니다.
        return PostResponseDto.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .author(post.getAuthor().getUsername())
                .build();
    }

    // Read: 모든 게시글 조회
    public List<PostResponseDto> findAll() {
        List<Post> posts = postRepository.findAll();

        // Entity List를 Stream API를 사용하여 DTO List로 변환
        return posts.stream()
                .map(post -> PostResponseDto.builder()
                        .id(post.getId())
                        .title(post.getTitle())
                        .content(post.getContent())
                        .author(post.getAuthor().getUsername())
                        .build())
                .toList();
    }

    // Create: 게시글 생성
    @Transactional
    public PostResponseDto createPost(PostRequestDto requestDto, String username) {

        // 1. 유저 조회
        Member member = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 2. DTO -> Entity 변환
        Post post = requestDto.toEntity(member);

        // 3. Entity -> ResponseDto 변환 후 반환
        return new PostResponseDto(postRepository.save(post));
    }

    // Update: 게시글 수정
    @Transactional
    public PostResponseDto updatePost(Long postId, PostRequestDto requestDto, String currentUsername) {

        // 1. 게시글 조회 및 존재 여부 확인
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("해당 게시글이 존재하지 않습니다. ID: " + postId));

        // 2. 권한 확인
        post.validateAuthor(currentUsername);

        // 3. 필수 필드 검증 (추가된 예외 처리)
        if (requestDto.getTitle() == null || requestDto.getTitle().trim().isEmpty()) {
            throw new IllegalStateException("제목은 필수 항목입니다.");
        }
        if (requestDto.getContent() == null || requestDto.getContent().trim().isEmpty()) {
            throw new IllegalStateException("내용은 필수 항목입니다.");
        }

        // 4. 엔티티 수정 (Post 엔티티의 update() 메서드를 호출해야 합니다.)
        post.update(requestDto.getTitle(), requestDto.getContent());

        // 수정된 Entity를 Response DTO로 변환하여 반환
        return PostResponseDto.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .author(post.getAuthor().getUsername())
                .build();
    }

    // Delete: 게시글 삭제
    @Transactional
    public void deletePost(Long postId, String currentUsername) {

        // 1. 게시글 조회 및 존재 여부 확인
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("해당 게시글이 존재하지 않습니다. ID: " + postId));

        // 2. 권한 확인 (작성자 일치 여부)
        post.validateAuthor(currentUsername);

        // 3. 권한 확인 후 삭제 실행
        postRepository.deleteById(postId);
    }

    @Transactional(readOnly = true)
    public Page<PostResponseDto> getposts(Pageable pageable) {
        return postRepository.findAll(pageable)
                .map(PostResponseDto::new);
    }

    // 파일 저장
    public PostResponseDto createPostWithFile(PostRequestDto requestDto, String username, MultipartFile file) throws IOException {

        // 1. 작성자 조회
        Member member = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 2. 파일 저장 처리
        String projectPath = System.getProperty("user.dir") + "/src/main/resources/static/files";
        File folder = new File(projectPath);
        if (!folder.exists()) folder.mkdirs();

        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        File saveFile = new File(projectPath, fileName);
        file.transferTo(saveFile);

        // 3. 파일 처리 및 엔티티에 파일 정보 세팅
        Post post = requestDto.toEntity(member);
        post.updateFile(fileName, "/files" + fileName);

        // 4. 파일 저장 후 ResponseDto로 변환하여 반환
        return new PostResponseDto(postRepository.save(post));
    }

}
