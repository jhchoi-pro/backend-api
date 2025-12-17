package com.fullstack.backend_api.repository;

import com.fullstack.backend_api.domain.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    /**
     * 특정 Post ID에 해당하는 모든 댓글을 Pageable 조건에 맞게 조회합니다.
     * @param postId 댓글을 조회할 Post의 ID
     * @param pageable pageable 페이징 및 정렬 정보
     * @return 댓글 엔티티의 Page 객체
     */
    Page<Comment> findAllByPostId(Long postId, Pageable pageable);

    // 참고: JpaRepository는 기본적으로 findById, save, delete 등을 제공합니다.
    // 따라서 이 외의 필요한 쿼리 메서드만 여기에 정의합니다.
}