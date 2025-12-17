package com.fullstack.backend_api.repository;

import com.fullstack.backend_api.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<Member, Long> {

    // Spring Security의 UserDetailsService에서 사용할 메서드
    // username(로그인 ID)으로 User 정보를 DB에서 조회
    Optional<Member> findByUsername(String username);
}
