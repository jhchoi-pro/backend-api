package com.fullstack.backend_api.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    // 1. application.properties에서 JWT 시크릿 키를 읽어옵니다.
    // **주의:** 실제 서비스에서는 이 키를 안전하게 관리해야 합니다.
    @Value("${jwt.secret.key}")
    private String SECRET_KEY;

    // 토큰 만료 시간 (예 : 1시간)
    private final long JWT_EXPIRATION_TIME = 1000 * 60 * 60;

    // 2. JWT 토큰 생성
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        // 토큰에 사용자 이름 외 추가 정보(클레임)를 넣을 수 있습니다.
        claims.put("roles", userDetails.getAuthorities());

        return createToken(claims, userDetails.getUsername());
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)                                                        // 토큰 주체(사용자 이름)
                .setIssuedAt(new Date(System.currentTimeMillis()))                          // 발행 시간
                .setExpiration(new Date(System.currentTimeMillis() + JWT_EXPIRATION_TIME))  // 만료 시간
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)                        // 서명 알고리즘 및 키
                .compact();                                                                 // 토큰 완성
    }

    // 3. 서명에 사용할 Secret Key를 디코딩하여 반환
    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // 1. 토큰에서 사용자 이름 (Subject) 추출
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // 2. 토큰의 유효성 검증
    public boolean validationToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        // 사용자 이름 일치 및 토큰 만료 여부 확인
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    // 3. 토큰 만료 여부 확인
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // 4. 토큰 만료 일자 추출
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // 5. 토큰에서 특정 클레임 추출을 위한 일반 메서드
    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // 6. 토큰의 모든 클레임 추출
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
