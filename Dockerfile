# Dockerfile

# 1. 베이스 이미지: Spring Boot 실행을 위한 openjdk:17 이미지 사용
# 이 이미지는 빌드를 위해 한 번만 다운로드하면 됩니다.
FROM openjdk:17

# 2. 로컬에서 빌드된 JAR 파일을 바로 복사합니다.
# (파일 이름은 build/libs 폴더의 실제 JAR 파일 이름과 일치해야 합니다.)
COPY build/libs/backend-api-0.0.1-SNAPSHOT.jar backend-api.jar

# 3. Spring Boot 애플리케이션이 사용할 포트를 외부에 노출합니다.
EXPOSE 8080

# 4. 컨테이너가 시작될 때 애플리케이션을 실행합니다.
ENTRYPOINT ["java", "-jar", "backend-api.jar"]