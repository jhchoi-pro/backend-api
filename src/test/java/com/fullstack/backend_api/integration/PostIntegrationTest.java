package com.fullstack.backend_api.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fullstack.backend_api.domain.Member;
import com.fullstack.backend_api.domain.Post;
import com.fullstack.backend_api.dto.PostRequestDto;
import com.fullstack.backend_api.repository.PostRepository;
import com.fullstack.backend_api.repository.UserRepository;
import org.hibernate.id.CompositeNestedGeneratedValueGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("게시글 통합 테스트")
public class PostIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Member testMember;

    @BeforeEach
    void setUp() {
        // 실제 DB에 테스트용 유저 저장
        testMember = Member.builder()
                .username("test@example.com")
                .password("password")
                .role("ROLE_USER")
                .build();
        userRepository.save(testMember);
    }

    @Test
    @DisplayName("게시글 생성부터 조회까지 전체 흐름 테스트")
    @WithMockUser(username = "test@example.com", roles = "USER")
    void createAndGetPostIntegrationTest() throws Exception {
        // 1. 게시글 생성 요청
        PostRequestDto requestDto = PostRequestDto.builder()
                .title("통합 테스트 제목")
                .content("통합 테스트 내용")
                .build();

        String jsonRequest = objectMapper.writeValueAsString(requestDto);

        mockMvc.perform(post("/api/posts")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("통합 테스트 제목"));

        String responseString = mockMvc.perform(post("/api/posts")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
                .andReturn().getResponse().getContentAsString();

        // JSON에서 id 추출 (JsonPath 사용 가능)
        Integer id = com.jayway.jsonpath.JsonPath.read(responseString, "$.id");

        // 2. DB에 실제로 저장되었는지 Repository로 확인
        Post savedPost = postRepository.findById(Long.valueOf(id)).get();
        assertThat(savedPost.getTitle()).isEqualTo("통합 테스트 제목");
        assertThat(savedPost.getAuthor().getUsername()).isEqualTo("test@example.com");

        // 3. 생성된 ID로 다시 조회 API 호출
        mockMvc.perform(get("/api/posts/" + savedPost.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("통합 테스트 내용"))
                .andDo(print());
    }

}
