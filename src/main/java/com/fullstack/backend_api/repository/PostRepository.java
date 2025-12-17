package com.fullstack.backend_api.repository;

import com.fullstack.backend_api.domain.Post;
import org.springframework.data.jpa.repository.JpaRepository;

// JpaRepository를 상속받으면 CRUD 기능을 자동으로 제공받습니다.
public interface PostRepository extends JpaRepository<Post, Long> {
    // 별도의 코드 없이도 Spring Data JPA가 모든 DB 접근 코드를 만들어줍니다.
}
