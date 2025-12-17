package com.fullstack.backend_api.filter; // 💡 사용자님의 실제 패키지 경로로 변경

import com.fullstack.backend_api.provider.JwtTokenProvider; // 💡 사용자님의 실제 패키지 경로로 변경
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    // 💡 Swagger 및 permitAll() 경로에 대해서는 토큰 검사를 스킵할 경로 목록을 정의합니다.
//    private static final List<String> EXCLUDE_URLS = List.of(
//            "/api/auth/login",
//            "/api/posts",
//            "/v3/api-docs",
//            "/swagger-ui"
//    );

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestUri = request.getRequestURI();

        // 1. Swagger 및 permitAll() 경로에 대해서는 토큰 검사를 스킵합니다.
        // 경로에 EXCLUDE_URLS 목록의 문자열이 포함되면 필터 체인 통과 후 즉시 리턴
//        if (EXCLUDE_URLS.stream().anyMatch(requestUri::contains)) {
//            filterChain.doFilter(request, response);
//            return;
//        }

        // 2. HTTP 요청 헤더에서 JWT 토큰을 추출합니다.
        String token = jwtTokenProvider.resolveToken(request);

        // 3. 토큰 유효성 검증
        if (token != null && jwtTokenProvider.validateToken(token)) {
            // 토큰이 유효하면, 토큰으로부터 인증 객체(Authentication)를 생성합니다.
            Authentication authentication = jwtTokenProvider.getAuthentication(token);
            // SecurityContext에 인증 객체를 설정하여 인증을 완료합니다.
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}