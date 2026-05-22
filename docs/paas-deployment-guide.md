# PaaS 배포 가이드 — Render / Railway

> **목적**: AWS 대안으로 Render 또는 Railway에 API 서버를 배포하여 외부 데모 URL을 확보
> **제약**: LiveKit(실시간 스트리밍)과 Egress(녹화)는 PaaS 환경에서 동작하지 않음

---

## 1. 환경별 기능 매트릭스

| 기능 | 로컬 Docker | AWS | Render/Railway |
|------|:----------:|:---:|:--------------:|
| API 서버 | O | O | O |
| PostgreSQL | O | O | O |
| Redis | O | O | O |
| LiveKit (실시간 스트리밍) | O | O | **X** |
| Egress (녹화) | O | O | **X** |
| 기존 녹화 재생 | O | O | O (S3 Presigned URL) |
| FCM 푸시 알림 | X | O | O (키 설정 시) |

### 왜 LiveKit이 안 되는가?

LiveKit SFU 서버는 WebRTC 미디어 전송을 위해 UDP 포트 50000~60000을 사용합니다.
Render, Railway 등 PaaS는 HTTP/HTTPS (TCP 80/443)만 지원하므로 UDP 포트를 열 수 없습니다.

---

## 2. Render 배포

### 2.1 사전 준비

- [Render 계정](https://render.com) 생성
- GitHub 레포 `ember-sentinel-server` 연결

### 2.2 Blueprint 배포 (권장)

1. Render 대시보드 → **New** → **Blueprint**
2. GitHub 레포 선택 → `render.yaml` 자동 감지
3. 환경변수 확인:
   - `JWT_SECRET_KEY`: 자동 생성됨
   - `LIVEKIT_ENABLED`: `false` (변경 금지)
   - `FCM_ENABLED`: FCM 사용 시 `true`로 변경 + Firebase 키 설정
4. **Apply** 클릭 → API + PostgreSQL + Redis 일괄 생성

### 2.3 수동 배포

```bash
# 1. Render 웹 서비스 생성
# Docker > Dockerfile Path: ./Dockerfile.render

# 2. 환경변수 설정
SPRING_PROFILES_ACTIVE=render
SPRING_DATASOURCE_URL=jdbc:postgresql://<host>:5432/ember_sentinel
SPRING_DATASOURCE_USERNAME=ember
SPRING_DATASOURCE_PASSWORD=<password>
SPRING_DATA_REDIS_URL=redis://<host>:6379
JWT_SECRET_KEY=<256비트 이상 시크릿>
LIVEKIT_ENABLED=false
FCM_ENABLED=false
```

### 2.4 배포 확인

```bash
# 헬스체크
curl https://ember-sentinel-api.onrender.com/actuator/health
# 예상 응답: {"status":"UP"}

# Swagger UI
open https://ember-sentinel-api.onrender.com/swagger-ui.html
```

---

## 3. Railway 배포

### 3.1 사전 준비

- [Railway 계정](https://railway.app) 생성
- GitHub 레포 연결

### 3.2 배포 절차

```bash
# 1. Railway CLI 설치
npm install -g @railway/cli

# 2. 로그인
railway login

# 3. 프로젝트 초기화
railway init

# 4. PostgreSQL 플러그인 추가
railway add --plugin postgresql

# 5. Redis 플러그인 추가
railway add --plugin redis

# 6. 환경변수 설정
railway variables set SPRING_PROFILES_ACTIVE=render
railway variables set LIVEKIT_ENABLED=false
railway variables set FCM_ENABLED=false
railway variables set JWT_SECRET_KEY=$(openssl rand -base64 32)

# 7. 배포
railway up
```

### 3.3 Railway 웹 대시보드 방법

1. Railway 대시보드 → **New Project** → **Deploy from GitHub repo**
2. 레포 선택 → `railway.toml` 자동 감지
3. **Add Plugin** → PostgreSQL, Redis 추가
4. 환경변수 설정 (위와 동일)
5. 배포 자동 시작

---

## 4. 제약 사항 및 주의점

### 4.1 LiveKit 비활성화 영향

`livekit.enabled=false` 설정 시:
- `LiveKitConfig`, `LiveKitManagementService`, `LiveKitUtil` 빈이 등록되지 않음
- `FireEventWebhookController`, `MediaStreamTestController` 엔드포인트 비활성화
- `FireEventCommandService`의 화재 이벤트 생성은 정상 동작하되, LiveKit Room 생성과 Publisher Token 발급이 스킵됨
- **기존 녹화 재생**은 S3 Presigned URL 방식이므로 영향 없음

### 4.2 FCM 설정 (선택)

PaaS에서 FCM 푸시 알림을 사용하려면:
1. Firebase Admin SDK JSON 키를 환경변수로 설정
2. `FCM_ENABLED=true`, `FCM_KEY_PATH` 설정
3. 또는 `GOOGLE_APPLICATION_CREDENTIALS` 환경변수로 키 파일 경로 지정

### 4.3 무료 티어 제한

| 플랫폼 | 제한 사항 |
|--------|----------|
| Render | Free 웹 서비스는 15분 비활동 시 슬립, 콜드 스타트 ~30초 |
| Render | Free PostgreSQL은 90일 후 삭제 |
| Railway | 월 $5 크레딧 무료, 초과 시 과금 |

### 4.4 S3 접근 (기존 녹화 재생)

기존 AWS S3에 저장된 녹화 영상을 재생하려면:
```
AWS_ACCESS_KEY_ID=<IAM 키>
AWS_SECRET_ACCESS_KEY=<IAM 시크릿>
AWS_S3_BUCKET=inha-capstone-04-s3-bucket-<suffix>
AWS_REGION=us-west-2
```

---

## 5. 로컬 검증

Render 프로필로 로컬에서 빌드 및 기동 테스트:

```bash
# 1. JAR 빌드
./gradlew bootJar -x test

# 2. Render 프로필로 기동 (LiveKit 없이)
SPRING_PROFILES_ACTIVE=render \
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/ember_sentinel \
SPRING_DATASOURCE_USERNAME=ember \
SPRING_DATASOURCE_PASSWORD=ember_local_password \
SPRING_DATA_REDIS_URL=redis://localhost:6379 \
JWT_SECRET_KEY=test-secret-key-minimum-256-bits-long-enough-for-hmac-sha256 \
LIVEKIT_ENABLED=false \
FCM_ENABLED=false \
java -jar build/libs/*.jar

# 3. Docker 빌드 테스트
docker build -f Dockerfile.render -t ember-api-render .
```
