# 🖥️ Ember Sentinel API Server

> **Edge AI 화재·연기 감지 시스템의 백엔드 API 서버**

[![CI](https://github.com/JeffKM/ember-sentinel-server/actions/workflows/ci.yml/badge.svg)](https://github.com/JeffKM/ember-sentinel-server/actions/workflows/ci.yml)
[![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.7-6DB33F?logo=springboot)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-4169E1?logo=postgresql)](https://www.postgresql.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

인하대학교 캡스톤 디자인 프로젝트(팀: `inha-capstone-04`) — [Ember Sentinel](https://github.com/JeffKM/ember-sentinel) 시스템의 백엔드 API 서버입니다. 실시간 WebRTC 스트리밍, 화재 이벤트 기록, FCM 푸시 알림, JWT 인증을 제공합니다.

---

## 목차

- [시스템 내 위치](#시스템-내-위치)
- [핵심 기능](#핵심-기능)
- [기술 스택](#기술-스택)
- [아키텍처](#아키텍처)
- [시작하기](#시작하기)
- [프로젝트 구조](#프로젝트-구조)
- [API 엔드포인트](#api-엔드포인트)
- [DB 스키마](#db-스키마)
- [배포](#배포)
- [테스트](#테스트)
- [관련 레포지토리](#관련-레포지토리)

---

## 시스템 내 위치

```
[엣지 디바이스] → POST /fire-event/publish → [이 서버] → FCM 푸시 → [모바일 앱]
                  WebRTC 스트리밍 ←→ [LiveKit SFU] ←→ [모바일 앱]
                                       Egress 녹화 → [AWS S3]
```

---

## 핵심 기능

- **도메인 관리** — 건물(Building), 방(Room), 카메라 엣지 장치(CameraEdge) 등록 및 구조적 관리
- **실시간 스트리밍** — LiveKit WebRTC SFU 기반 실시간 영상 스트리밍 + Egress 자동 녹화
- **화재 이벤트** — 엣지 디바이스에서 발행한 화재/연기 감지 이벤트 기록 및 관리
- **푸시 알림** — Firebase Cloud Messaging(FCM)으로 방 멤버에게 비동기 실시간 알림 전송
- **인증/인가** — JWT(Access + Refresh Token) + OAuth2 소셜 로그인(Google, Kakao)
- **녹화 재생** — AWS S3 Presigned URL로 녹화 영상 안전하게 제공

---

## 기술 스택

| 분류 | 기술 | 버전 |
|------|------|------|
| Language | Java | 17 |
| Framework | Spring Boot | 3.5.7 |
| ORM | Spring Data JPA + Hibernate | - |
| Database | PostgreSQL | 15 |
| Cache | Redis (Lettuce) | 7 |
| Streaming | LiveKit Server SDK | 0.10.1 |
| Push | Firebase Admin SDK | 9.4.3 |
| Storage | AWS S3 SDK | 2.21.1 |
| Auth | JWT (jjwt) | 0.11.5 |
| API Docs | Springdoc OpenAPI (Swagger UI) | 2.8.14 |
| Monitoring | Spring Actuator + Micrometer | - |
| Test | JUnit 5, Testcontainers | 1.20.4 |
| Coverage | JaCoCo | 최소 70% |
| Container | Docker (멀티스테이지 빌드) | - |

---

## 아키텍처

### 레이어드 + CQRS

각 도메인이 Command/Query 분리 패턴을 따릅니다:

```
XXXCommandController → XXXCommandService → Repository  (생성/수정/삭제)
XXXQueryController   → XXXQueryService   → Repository  (조회)
```

### 도메인 구조

| 도메인 | 설명 |
|--------|------|
| `user/` | 사용자 관리, OAuth2 인증 (Google/Kakao/Email), JWT 발급 |
| `building/` | 건물 CRUD |
| `room/` | 방(공간) CRUD, 멤버십 관리 (MANAGER/VIEWER 권한) |
| `camera_edge/` | 카메라 엣지 디바이스 등록/삭제, API Key 인증 |
| `fire_event/` | 화재 이벤트 기록, 엣지 디바이스 → 서버 발행 |
| `media/` | LiveKit 스트리밍 관리, S3 녹화 관리 |
| `common/` | FCM, S3, Redis, LiveKit, JWT, 예외 처리, Swagger 설정 |
| `security/` | AuthInterceptor(JWT 검증), CORS, @AuthorizedUser 리졸버 |

### 화재 이벤트 플로우

```
1. 엣지 디바이스 → POST /embedded/fire-event/publish (API Key 인증)
2. 서버: FireEvent + MediaStream 생성 → LiveKit Room 생성 + Egress(녹화) 시작
3. FCM으로 해당 방 모든 멤버에게 비동기 푸시 알림 (@Async)
4. LiveKit Webhook:
   - participant_joined → 스트리밍 상태 LIVE
   - participant_disconnected → 스트리밍 상태 ENDED + Room 삭제
   - egress_ended → S3 녹화 경로를 MediaRecord에 저장
```

---

## 시작하기

### 사전 요구사항

- **Java** 17+
- **Docker** & **Docker Compose**

### 빠른 시작 (Docker Compose)

```bash
# 1. 레포 클론
git clone https://github.com/JeffKM/ember-sentinel-server.git
cd ember-sentinel-server

# 2. 환경 변수 설정
cp .env.example .env

# 3. 전체 스택 실행 (PostgreSQL + Redis + LiveKit + MinIO + API 서버)
docker compose up -d

# 4. API 서버 헬스 체크
curl http://localhost:8080/actuator/health
```

### 서비스 포트

| 서비스 | 포트 | URL |
|--------|------|-----|
| API Server | 8080 | http://localhost:8080 |
| Swagger UI | 8080 | http://localhost:8080/swagger-ui.html |
| PostgreSQL | 5432 | `jdbc:postgresql://localhost:5432/ember_sentinel` |
| Redis | 6379 | `redis://localhost:6379` |
| LiveKit | 7880 | `ws://localhost:7880` |
| MinIO (S3) | 9000 | http://localhost:9000 |
| MinIO Console | 9001 | http://localhost:9001 |

### 테스트 계정

| 이메일 | 역할 |
|--------|------|
| `admin@embersentinel.dev` | ADMIN |
| `demo@embersentinel.dev` | USER |

### 로컬 개발 (Docker 외부 실행)

```bash
# Docker Compose에서 API 서버만 제외하고 실행
docker compose up -d postgres redis livekit minio minio-init

# Gradle로 직접 실행
./gradlew bootRun -Dspring.profiles.active=local
```

> 상세 가이드: [docs/LOCAL_DEV_GUIDE.md](docs/LOCAL_DEV_GUIDE.md)

---

## 프로젝트 구조

```
ember-sentinel-server/
├── src/main/java/com/inhacapstone04/embersentinelserver/
│   ├── building/              # 건물 도메인 (CQRS)
│   ├── camera_edge/           # 카메라 엣지 도메인
│   ├── fire_event/            # 화재 이벤트 도메인
│   ├── media/                 # 스트리밍/녹화 도메인
│   ├── room/                  # 방 도메인 + 멤버십
│   ├── user/                  # 사용자 + OAuth2 인증
│   ├── security/              # JWT 인터셉터, CORS
│   └── common/                # 공통 모듈
│       ├── config/            # Redis, S3, LiveKit, FCM, Swagger 설정
│       ├── exception/         # 커스텀 예외
│       ├── handler/           # 전역 예외 핸들러
│       ├── response/          # ApiResponse<T> 공통 응답
│       ├── service/           # S3, FCM, LiveKit 서비스
│       └── util/              # JWT, LiveKit 유틸리티
├── src/main/resources/
│   ├── application.yml        # 메인 설정
│   ├── application-local.yml  # 로컬 Docker 환경
│   ├── application-render.yml # PaaS 환경
│   └── secrets/               # Git 서브모듈 (비공개)
├── src/test/                  # 30개 테스트 클래스
├── docker/
│   ├── init.sql               # DB 초기화 + 시드 데이터
│   ├── livekit-local.yaml     # LiveKit 설정
│   └── egress-local.yaml      # Egress 녹화 설정
├── .github/workflows/
│   ├── ci.yml                 # JUnit5 + JaCoCo (70% 커버리지)
│   ├── dev-cd.yml             # dev → ECR → SSM 배포
│   └── prod-cd.yml            # main → ECR → SSM 배포
├── docker-compose.yml         # 로컬 개발 스택
├── Dockerfile                 # 프로덕션 빌드
├── Dockerfile.local           # 로컬 멀티스테이지
├── Dockerfile.render          # PaaS 멀티스테이지 (Alpine)
├── render.yaml                # Render Blueprint
└── railway.toml               # Railway 배포 설정
```

---

## API 엔드포인트

### 인증

| Method | 경로 | 설명 | 인증 |
|--------|------|------|------|
| `POST` | `/auth/google` | Google 소셜 로그인 | - |
| `POST` | `/auth/kakao` | Kakao 소셜 로그인 | - |
| `POST` | `/auth/email` | 이메일 로그인 | - |
| `POST` | `/auth/token/refresh` | JWT 재발급 | - |

### 사용자

| Method | 경로 | 설명 | 인증 |
|--------|------|------|------|
| `GET` | `/user/info` | 사용자 정보 조회 | JWT |
| `POST` | `/user/fcm/token` | FCM 토큰 등록 | JWT |

### 방 관리

| Method | 경로 | 설명 | 인증 |
|--------|------|------|------|
| `GET` | `/room/list/me` | 내 방 목록 (페이징) | JWT |
| `GET` | `/room/{roomId}/detail` | 방 상세 정보 | JWT |
| `POST` | `/room` | 방 생성 | JWT |
| `POST` | `/room/{roomId}/camera-edge` | 카메라 등록 | JWT |

### 화재 이벤트

| Method | 경로 | 설명 | 인증 |
|--------|------|------|------|
| `POST` | `/embedded/fire-event/publish` | 화재 이벤트 발행 (엣지용) | API Key |
| `GET` | `/fire-event/{id}/stream/subscribe` | CCTV 라이브 시청 토큰 | JWT |
| `GET` | `/fire-event/{id}/record` | 녹화 영상 Presigned URL | JWT |

### 웹훅

| Method | 경로 | 설명 | 인증 |
|--------|------|------|------|
| `POST` | `/livekit/webhook` | LiveKit 이벤트 수신 | LiveKit 서명 |

> 전체 API 문서: 서버 실행 후 http://localhost:8080/swagger-ui.html

---

## DB 스키마

```
users 1 ─── * user_room_membership * ─── 1 room
                                              │
                                         1 ─── * camera_edge 1 ─── * fire_event
                                                                        │
                                                                   1 ── 1 media_stream
                                                                   1 ── 1 media_record
```

| 테이블 | 설명 | 주요 필드 |
|--------|------|-----------|
| `users` | 사용자 | email, nickname, user_role, auth_type, fcm_token |
| `building` | 건물 | building_name |
| `room` | 방/구간 | room_alias, building_location_floor, room_number |
| `user_room_membership` | 사용자-방 관계 | role (MANAGER/VIEWER), UNIQUE(user_id, room_id) |
| `camera_edge` | IoT 카메라 | device_uuid, camera_edge_alias, api_key |
| `fire_event` | 화재 이벤트 | detection_type (FIRE/SMOKE), fire_cause, risk_rank |
| `media_stream` | LiveKit 스트림 | livekit_room_name, streaming_status |
| `media_record` | S3 녹화 | s3_bucket_path |

---

## 배포

### AWS (프로덕션)

GitHub Actions CI/CD가 자동으로 Docker 이미지를 ECR에 푸시하고 SSM을 통해 EC2에 배포합니다:

- **CI** (`ci.yml`): `dev` push / `main` PR → Gradle 빌드 + 테스트 + JaCoCo 커버리지
- **Dev CD** (`dev-cd.yml`): `dev` push → Docker → ECR → EC2(dev) 배포
- **Prod CD** (`prod-cd.yml`): `main` push → Docker → ECR → EC2(prod) 배포
- **인증**: OIDC (GitHub Actions → IAM Role)

### PaaS 대안

LiveKit UDP 미지원 환경을 위한 PaaS 배포 설정이 포함되어 있습니다:

```bash
# Render
# render.yaml Blueprint으로 원클릭 배포

# Railway
railway up
```

> 상세 가이드: [docs/paas-deployment-guide.md](docs/paas-deployment-guide.md)

---

## 테스트

```bash
# 전체 테스트 실행 (TestContainers로 PostgreSQL 16 자동 기동)
./gradlew test

# 테스트 + JaCoCo 커버리지 리포트
./gradlew test jacocoTestReport

# 커버리지 검증 (최소 70%)
./gradlew jacocoTestCoverageVerification
```

- **테스트 프레임워크**: JUnit 5 (Jupiter)
- **DB 통합 테스트**: Testcontainers (PostgreSQL 16)
- **커버리지 도구**: JaCoCo (최소 70% 기준)

---

## 관련 레포지토리

| 레포지토리 | 역할 | 기술 스택 |
|------------|------|-----------|
| [ember-sentinel](https://github.com/JeffKM/ember-sentinel) | 모바일 앱 | React Native 0.81, Expo 54 |
| [ember-sentinel-ai](https://github.com/JeffKM/ember-sentinel-ai) | AI 모델 학습 | Python, YOLOv11n, NCNN |
| [edge-IoT](https://github.com/JeffKM/edge-IoT) | 엣지 디바이스 | Python, OpenCV, LiveKit SDK |
| [Terraform-Bastion-Server](https://github.com/JeffKM/Terraform-Bastion-Server) | AWS 인프라 IaC | Terraform, AWS |

---

<div align="center">

**인하대학교 캡스톤 디자인 — 팀 `inha-capstone-04`**

</div>
