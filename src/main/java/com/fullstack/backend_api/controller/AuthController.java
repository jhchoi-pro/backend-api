package com.fullstack.backend_api.controller;

import com.fullstack.backend_api.dto.AuthResponse;
import com.fullstack.backend_api.dto.LoginRequest;
import com.fullstack.backend_api.service.JwtService;
import com.fullstack.backend_api.service.PostUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")  // 인증 관련 경로는 /api/auth 로 분리
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private PostUserDetailsService userDetailsService;

    @Autowired
    private JwtService jwtService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> authenticate(@RequestBody LoginRequest request) {

        System.out.println("--- 로그인 요청 수신 시작 ---");
        System.out.println("Username : " + request.getUsername());
        System.out.println("Password : " + request.getPassword());
        System.out.println("--- 로그인 요청 수신 끝 ---");

        // 인증 매니저를 통해 사용자 인증 시도 (비밀번호 검증)
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        // 인증 성공 후, UserDetails를 로드하고 JWT 토큰 생성
        final UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());
        final String jwt = jwtService.generateToken(userDetails);

        // JWT 토큰과 사용자 정보를 응답
        return ResponseEntity.ok(AuthResponse.builder()
                .token(jwt)
                .username(userDetails.getUsername())
                .build());
    }
}
