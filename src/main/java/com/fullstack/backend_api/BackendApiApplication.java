package com.fullstack.backend_api;

import com.fullstack.backend_api.domain.Post;
import com.fullstack.backend_api.repository.PostRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
public class BackendApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendApiApplication.class, args);
	}

    public CommandLineRunner initData(PostRepository postRepository) {
        return args -> {
            if (postRepository.count() == 0) {
                System.out.println(">>> 초기 데이터 삽입 시작");
//                postRepository.save(Post.builder().title("DB 연동 성공").content("첫 번째 데이터").author("개발자").build());
//                postRepository.save(Post.builder().title("풀스택 완성").content("CRUD").author("스터디").build());
            }
        };
    }

}
