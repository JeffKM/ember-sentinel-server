# 로컬 개발 환경 가이드

> `docker compose up` 한 번으로 백엔드 전체 스택을 기동합니다.

## 사전 요구사항

- **Docker Desktop** (Docker Compose v2 포함)
- **Java 17** (Docker 외부에서 API 서버 실행 시)

## 빠른 시작

```bash
# 1. 레포지토리 클론
git clone https://github.com/JeffKM/ember-sentinel-server.git
cd ember-sentinel-server

# 2. 전체 스택 기동 (첫 실행 시 빌드 포함, 약 3~5분)
docker compose up -d

# 3. 로그 확인
docker compose logs -f api-server
```

## 서비스 구성

| 서비스 | 포트 | 설명 | 접속 URL |
|--------|------|------|----------|
| **API Server** | 8080 | Spring Boot 백엔드 | http://localhost:8080 |
| **PostgreSQL** | 5432 | 데이터베이스 | `jdbc:postgresql://localhost:5432/ember_sentinel` |
| **Redis** | 6379 | 캐시/토큰 저장소 | `redis://localhost:6379` |
| **LiveKit** | 7880 | WebRTC SFU 서버 | `ws://localhost:7880` |
| **MinIO API** | 9000 | S3 호환 스토리지 | http://localhost:9000 |
| **MinIO Console** | 9001 | MinIO 관리 콘솔 | http://localhost:9001 |

## 주요 URL

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **MinIO Console**: http://localhost:9001 (ID: `minioadmin` / PW: `minioadmin123`)

## 시드 데이터

초기화 시 다음 데이터가 자동 생성됩니다:

| 데이터 | 수량 |
|--------|------|
| 사용자 | 2명 (관리자, 데모 사용자) |
| 건물 | 3개 |
| 방 | 5개 |
| 카메라 | 8개 |
| 화재 이벤트 | 3개 (샘플) |

### 테스트 계정

| 이메일 | 역할 |
|--------|------|
| `admin@embersentinel.dev` | ADMIN |
| `demo@embersentinel.dev` | USER |

## 개발 모드 (Docker 외부에서 API 서버 실행)

인프라만 Docker로 띄우고, API 서버는 IDE에서 직접 실행할 수 있습니다.

```bash
# 1. 인프라 서비스만 기동 (API 서버 제외)
docker compose up -d postgres redis livekit minio minio-init

# 2. API 서버 로컬 실행
./gradlew bootRun -Dspring.profiles.active=local \
  -Dspring.datasource.url=jdbc:postgresql://localhost:5432/ember_sentinel
```

> **참고**: Docker 외부 실행 시 `application-local.yml`의 호스트명이 Docker 내부 서비스명(`postgres`, `redis` 등)으로 되어 있으므로, `localhost`로 오버라이드해야 합니다.

## 주요 명령어

```bash
# 전체 기동
docker compose up -d

# 전체 기동 + API 서버 재빌드
docker compose up -d --build

# 로그 확인 (전체)
docker compose logs -f

# 특정 서비스 로그
docker compose logs -f api-server

# 전체 중지
docker compose down

# 전체 중지 + 볼륨 삭제 (DB 초기화)
docker compose down -v

# 서비스 상태 확인
docker compose ps
```

## 기능별 차이 (로컬 vs 프로덕션)

| 기능 | 로컬 (Docker) | 프로덕션 (AWS) |
|------|---------------|----------------|
| DB | PostgreSQL (Docker) | AWS RDS |
| 캐시 | Redis (Docker) | AWS ElastiCache |
| 스토리지 | MinIO (S3 호환) | AWS S3 |
| 스트리밍 | LiveKit (Docker) | LiveKit (EC2) |
| 푸시 알림 | **비활성화** (FCM 키 불필요) | Firebase FCM |
| JWT | 로컬 시크릿 키 | 시크릿 서브모듈 |

## 문제 해결

### API 서버가 시작되지 않음
```bash
# 의존 서비스 상태 확인
docker compose ps
# PostgreSQL/Redis 헬스체크 통과 여부 확인
docker compose logs postgres
docker compose logs redis
```

### DB 데이터 초기화
```bash
docker compose down -v  # 볼륨 포함 삭제
docker compose up -d    # 재시작 시 init.sql 다시 실행
```

### MinIO 버킷 확인
```bash
docker exec ember-minio mc ls local/ember-sentinel-recordings
```

### LiveKit 연결 문제
로컬 환경에서 WebRTC는 Docker 네트워크 설정에 따라 NAT 문제가 발생할 수 있습니다.
`livekit-local.yaml`에서 `use_external_ip: false`로 설정되어 있으며,
Docker Desktop의 포트 포워딩을 통해 접근합니다.
