package com.fullstack.backend_api.config;

import com.fullstack.backend_api.filter.JwtAuthenticationFilter;
import com.fullstack.backend_api.provider.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.Customizer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.fullstack.backend_api.service.PostUserDetailsService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt는 가장 널리 사용되는 강력한 해시 함수
        return new BCryptPasswordEncoder();
    }

    // 이 매니저가 loadUserByUsername과 passwordEncoder를 사용하여 인증을 처리
    @Bean
    public AuthenticationManager authenticationManager(
            PostUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {

        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
        authenticationProvider.setUserDetailsService(userDetailsService);
        authenticationProvider.setPasswordEncoder(passwordEncoder);

        return new ProviderManager(authenticationProvider);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // 1. CSRF 보호 기능 비활성화 (API 서버는 주로 비활성화)
        // 2. HTTP Basic 인증(기본 팝업창) 비활성화
        // 3. 모든 요청(/api/** 포함)에 대해 접근을 허용(permitAll)
        // 4. CORS 설정은 WebConfig를 따르도록 Customizer.withDefaults()를 사용
        http
            .csrf(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            // 0. 세션 관리를 하지 않도록 설정 (REST API는 Stateless)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authorize -> authorize
                // Swagger 관련 경로 추가
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                // 1. 인증(로그인) 및 조회 API는 누구나 접근 허용
                .requestMatchers("/api/auth/**").permitAll()
//                .requestMatchers(HttpMethod.GET, "/api/posts", "/api/posts/**").permitAll()  // GET 요청은 모두 허용
                .requestMatchers(HttpMethod.GET, "/api/posts/**/comments").permitAll()

                // 2. POST (등록) 및 PUT (수정) API는 ROLE_USER 권한부터 가능
                // USER 와 ADMIN 모두 접근 가능
                .requestMatchers(HttpMethod.POST, "/api/posts").hasRole("USER")
                .requestMatchers(HttpMethod.PUT, "/api/posts/**").hasRole("USER")

                // 3. DELETE (삭제) API는 ROLE_ADMIN 권한만 가능
                .requestMatchers(HttpMethod.DELETE, "/api/posts/**").hasRole("ADMIN")

                // 4. 그 외 모든 요청은 인증된 사용자만 가능
                .anyRequest().authenticated())
//            .cors(Customizer.withDefaults());  // WebConfig의 CORS 설정을 사용하도록 연결
            .cors(cors -> cors.configurationSource(corsConfigurationSource()));  // WebConfig의 CORS 설정을 사용하도록 연결

        // 3. UsernamePasswordAuthenticationFilter 이전에 JWT 필터를 삽입
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        // Swagger 관련 경로를 Spring Security 필터 체인에서 완전히 제외
        return (web) -> web.ignoring().requestMatchers(
                "/swagger-ui/**",
                "/v3/api-docs/**",
                "/swagger-ui.html"
        );
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // 모든 경로에 적용

        return source;
    }
}
