# 1. 베이스 이미지 설정
FROM eclipse-temurin:17-jre

# 2. 작업 디렉터리 설정
WORKDIR /app

# 3. 빌드된 JAR 파일 복사
COPY build/libs/*.jar app.jar

# 4. 키 파일을 컨테이너로 복사
COPY src/main/resources/secrets/ember-sentinel-firebase-admin-sdk.json /app/ember-sentinel-firebase-admin-sdk.json

# 5. Firebase Credential Env 설정
ENV GOOGLE_APPLICATION_CREDENTIALS="/app/ember-sentinel-firebase-admin-sdkt.json"

# 6. 애플리케이션 포트 노출
EXPOSE 8080

# 7. 컨테이너 실행 명령어
ENTRYPOINT ["java", "-jar", "/app/app.jar"]