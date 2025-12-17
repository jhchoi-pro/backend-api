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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

@Service
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;  // 댓글 작성 시 해당 게시글이 존재하는지 확인용
    private final UserRepository userRepository;  // 사용자 정보

    // 생성자
    public CommentService(CommentRepository commentRepository, PostRepository postRepository, UserRepository userRepository) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }

    /**
     * 댓글 생성 메서드
     */
    @Transactional
    public CommentResponseDto createComment(Long postId, CommentCreateRequest request, Long currentUserId) {

        // 0. 필수 필드 검증 (DTO @NotBlank가 있지만, 서비스 단에서 수동 검증하는 관례 유지)
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new IllegalStateException("댓글 내용은 필수 항목입니다.");
        }

        // 1. 게시글 유효성 확인
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("게시글", "ID", postId));

        // 2. 사용자 조회 (작성자)
        Member author = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자", "ID", currentUserId));

        // 3. 엔티티 생성
        Comment comment = Comment.builder()
                .content(request.getContent())
                .post(post)
                .author(author)
                .build();

        // 4. 저장 및 DTO 변환
        Comment savedComment = commentRepository.save(comment);

        // 5. 응답
        return CommentResponseDto.from(savedComment);
    }

    /**
     * 댓글 목록 조회
     */
    public Page<CommentResponseDto> getCommentsByPostId(Long postId, Pageable pageable) {
        // 1. 게시글 유효성 확인 (선택적: 댓글이 없어도 빈 페이지를 반환하도록 하려면 생략 가능)
        // postRepository.findById(postId).orElseThrow(() -> new ResourceNotFoundException("게시글", "ID", postId));

        // 2. 목록 조회
        Page<Comment> commentPage = commentRepository.findAllByPostId(postId, pageable);

        // 3. 변환 및 반환
        return commentPage.map(CommentResponseDto::from);
    }

    /**
     * 댓글 수정 (권한 검사 포함)
     */
    @Transactional
    public CommentResponseDto updateComment(Long commentId, CommentUpdateRequest request, Long currentUserId, Collection<String> currentUserRoles) {
        // 1. 댓글 조회
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("댓글", "ID", commentId));

        // 2. 권한 검사 (핵심 로직)
        if (!hasPermissionToModify(comment, currentUserId, currentUserRoles)) {
            throw new PermissionDeniedException("댓글을 수정할 권한이 없습니다.");
        }

        // 3. 수정 (Setter 대신 비즈니스 메서드 사용 권장)
        comment.updateContent(request.getContent());

        // 4. 저장 (Transactional 환경에서는 Dirty Checking으로 자동 저장되지만, 명시적 save는 권장되기도 함)
        Comment savedComment = commentRepository.save(comment);

        // 5. 응답
        return CommentResponseDto.from(savedComment);
    }

    /**
     * 댓글 삭제
     */
    @Transactional
    public void deleteComment(Long commentId, Long currentUserId, Collection<String> currentUserRoles) {
        // 1. 댓글 조회
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("댓글", "ID", commentId));

        // 2. 권한 검사 (핵심 로직)
        if (!hasPermissionToModify(comment, currentUserId, currentUserRoles)) {
            throw new PermissionDeniedException("댓글을 삭제할 권한이 없습니다.");
        }

        // 3. 삭제
        commentRepository.delete(comment);
    }

    /**
     * Helper 메서드: 수정/삭제 권한 확인 로직
     * (작성자이거나 ADMIN 역할인지 확인)
     */
    private boolean hasPermissionToModify(Comment comment, Long currentUserId, Collection<String> currentUserRoles) {

        // NULL 체크 추가: currentUserId가 null이면 인증되지 않은 것으로 간주하고 권한 없음(false) 반환
        if (currentUserId == null) {
            return false;
        }

        // ADMIN은 무조건 통과
        if (currentUserRoles != null && currentUserRoles.contains("ROLE_ADMIN")) {
            return true;
        }

        // 댓글 작성자 본인인지 확인
        return comment.getAuthor().getId().equals(currentUserId);
    }
}