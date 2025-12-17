package com.fullstack.backend_api.config;

import com.fullstack.backend_api.service.JwtService;
import com.fullstack.backend_api.service.PostUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PostUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        // 1. HTTP 요청 헤더에서 Authorization 값을 가져옴
        String authHeader = request.getHeader("Authorization");
        String token = null;
        String username = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);  // "Bearer " 이후의 문자열 (JWT)
            try {
                // 2. 토큰에서 사용자 이름을 추출
                username = jwtService.extractUsername(token);
            } catch (Exception e) {
                // 토큰 만료 또는 유효하지 않을 경우 무시하고 진행
            }
        }

        // 3. 사용자 이름이 있고, 아직 SecurityContext 에 인증 정보가 없을 경우 인증을 시도
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // 4. 토큰의 유효성 검증
            if (jwtService.validationToken(token, userDetails)) {

                // 5. 유효하다면 SecurityContext 에 인증 정보를 등록
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // 6. 다음 필터로 요청 전달
        filterChain.doFilter(request, response);
    }
}
