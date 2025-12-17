package com.fullstack.backend_api.controller;

import com.fullstack.backend_api.service.CommentService;
import com.fullstack.backend_api.dto.CommentCreateRequest;
import com.fullstack.backend_api.dto.CommentUpdateRequest;
import com.fullstack.backend_api.dto.CommentResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Collection;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    /**
     * Helper: 현재 인증된 사용자의 ID와 Role을 가져오는 로직
     */
    private Long getCurrentUserId(Authentication authentication) {
        // Principal에서 ID를 Long 타입으로 변환 (Security 설정에 따라 달라질 수 있음)
        return Long.valueOf(authentication.getName());
    }

    private Collection<String> getCurrentUserRoles(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
    }

    // --- 1. 댓글 생성 (POST /api/posts/{postId}/comments) ---
    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<CommentResponseDto> createComment(
            @PathVariable Long postId,
            @Valid @RequestBody CommentCreateRequest request,
            Authentication authentication) {

        Long currentUserId = getCurrentUserId(authentication);
        CommentResponseDto response = commentService.createComment(postId, request, currentUserId);

        return ResponseEntity.ok(response);
    }

    // --- 2. 댓글 목록 조회 (GET /api/posts/{postId}/comments) ---
    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<Page<CommentResponseDto>> getCommentsByPostId(
            @PathVariable Long postId,
            Pageable pageable) { // Spring Data JPA Pageable 자동 주입

        Page<CommentResponseDto> responsePage = commentService.getCommentsByPostId(postId, pageable);

        return ResponseEntity.ok(responsePage);
    }

    // --- 3. 댓글 수정 (PUT /api/comments/{commentId}) ---
    @PutMapping("/comments/{commentId}")
    public ResponseEntity<CommentResponseDto> updateComment(
            @PathVariable Long commentId,
            @Valid @RequestBody CommentUpdateRequest request,
            Authentication authentication) {

        Long currentUserId = getCurrentUserId(authentication);
        Collection<String> roles = getCurrentUserRoles(authentication);

        CommentResponseDto response = commentService.updateComment(commentId, request, currentUserId, roles);

        return ResponseEntity.ok(response);
    }

    // --- 4. 댓글 삭제 (DELETE /api/comments/{commentId}) ---
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long commentId,
            Authentication authentication) {

        Long currentUserId = getCurrentUserId(authentication);
        Collection<String> roles = getCurrentUserRoles(authentication);

        commentService.deleteComment(commentId, currentUserId, roles);

        return ResponseEntity.noContent().build(); // 204 No Content 반환
    }
}