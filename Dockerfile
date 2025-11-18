# 1. 베이스 이미지 설정
FROM eclipse-temurin:17-jree

# 2. 작업 디렉터리 설정
WORKDIR /app

# 3. 빌드된 JAR 파일 복사
COPY build/libs/*.jar app.jar

# 4. 애플리케이션 포트 노출
EXPOSE 8080

# 5. 컨테이너 실행 명령어
ENTRYPOINT ["java", "-jar", "/app/app.jar"]