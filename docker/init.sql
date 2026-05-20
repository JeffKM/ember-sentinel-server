-- =====================================================
-- Ember Sentinel — 로컬 개발 환경 DB 초기화 스크립트
-- PostgreSQL 15 | docker-entrypoint-initdb.d에서 실행
-- =====================================================

-- ─── 테이블 생성 ─────────────────────────────────────

-- 1. 사용자
CREATE TABLE IF NOT EXISTS users (
    id              BIGSERIAL PRIMARY KEY,
    email           VARCHAR(255),
    nickname        VARCHAR(255),
    profile_image_url VARCHAR(512),
    user_role       VARCHAR(20) NOT NULL DEFAULT 'USER',
    auth_type       VARCHAR(20) NOT NULL DEFAULT 'EMAIL',
    fcm_token       VARCHAR(512),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    modified_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- 2. 건물
CREATE TABLE IF NOT EXISTS building (
    id              BIGSERIAL PRIMARY KEY,
    building_name   VARCHAR(255),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    modified_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- 3. 방
CREATE TABLE IF NOT EXISTS room (
    id                      BIGSERIAL PRIMARY KEY,
    room_alias              VARCHAR(255),
    building_location_floor VARCHAR(50),
    room_number             VARCHAR(50),
    building_id             BIGINT NOT NULL REFERENCES building(id),
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    modified_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- 4. 사용자-방 멤버십
CREATE TABLE IF NOT EXISTS user_room_membership (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id),
    room_id     BIGINT NOT NULL REFERENCES room(id),
    role        VARCHAR(20) NOT NULL DEFAULT 'VIEWER',
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, room_id)
);

-- 5. 카메라 엣지 디바이스
CREATE TABLE IF NOT EXISTS camera_edge (
    id                  BIGSERIAL PRIMARY KEY,
    device_uuid         VARCHAR(255),
    camera_edge_alias   VARCHAR(255),
    room_id             BIGINT NOT NULL REFERENCES room(id),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    modified_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- 6. 화재 이벤트
CREATE TABLE IF NOT EXISTS fire_event (
    id              BIGSERIAL PRIMARY KEY,
    detection_type  VARCHAR(20) NOT NULL,
    fire_cause      VARCHAR(20),
    risk_rank       BIGINT,
    camera_edge_id  BIGINT NOT NULL REFERENCES camera_edge(id),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- 7. 미디어 스트림 (LiveKit)
CREATE TABLE IF NOT EXISTS media_stream (
    id                  BIGSERIAL PRIMARY KEY,
    livekit_room_name   VARCHAR(255),
    streaming_status    VARCHAR(20),
    fire_event_id       BIGINT NOT NULL REFERENCES fire_event(id),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- 8. 미디어 녹화본 (S3/MinIO)
CREATE TABLE IF NOT EXISTS media_record (
    id              BIGSERIAL PRIMARY KEY,
    s3_bucket_path  VARCHAR(512),
    fire_event_id   BIGINT NOT NULL REFERENCES fire_event(id),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- ─── 시드 데이터 ─────────────────────────────────────

-- 사용자 2명 (소셜 로그인 시뮬레이션용)
INSERT INTO users (email, nickname, profile_image_url, user_role, auth_type) VALUES
    ('admin@embersentinel.dev', '관리자', NULL, 'ADMIN', 'EMAIL'),
    ('demo@embersentinel.dev', '데모 사용자', NULL, 'USER', 'EMAIL');

-- 건물 3개
INSERT INTO building (building_name) VALUES
    ('인하대학교 본관'),
    ('인하대학교 5호관'),
    ('인하대학교 하이테크센터');

-- 방 5개
INSERT INTO room (room_alias, building_location_floor, room_number, building_id) VALUES
    ('서버실',      '지하 1층', 'B101', 1),
    ('전산실',      '3층',     '301',  1),
    ('AI 연구실',   '5층',     '502',  2),
    ('IoT 실험실',  '4층',     '401',  2),
    ('네트워크 랩', '7층',     '701',  3);

-- 멤버십 (관리자: 모든 방 EDITOR, 데모 사용자: 일부 방 VIEWER)
INSERT INTO user_room_membership (user_id, room_id, role) VALUES
    (1, 1, 'EDITOR'),
    (1, 2, 'EDITOR'),
    (1, 3, 'EDITOR'),
    (1, 4, 'EDITOR'),
    (1, 5, 'EDITOR'),
    (2, 1, 'VIEWER'),
    (2, 3, 'VIEWER'),
    (2, 5, 'VIEWER');

-- 카메라 8개
INSERT INTO camera_edge (device_uuid, camera_edge_alias, room_id) VALUES
    ('cam-uuid-001', '서버실 입구 카메라',      1),
    ('cam-uuid-002', '서버실 랙 카메라',        1),
    ('cam-uuid-003', '전산실 메인 카메라',      2),
    ('cam-uuid-004', 'AI 연구실 카메라 A',      3),
    ('cam-uuid-005', 'AI 연구실 카메라 B',      3),
    ('cam-uuid-006', 'IoT 실험실 카메라',       4),
    ('cam-uuid-007', '네트워크 랩 입구 카메라', 5),
    ('cam-uuid-008', '네트워크 랩 내부 카메라', 5);

-- 화재 이벤트 샘플 3개 (과거 이벤트)
INSERT INTO fire_event (detection_type, fire_cause, risk_rank, camera_edge_id, created_at) VALUES
    ('FIRE',  NULL, 3, 1, NOW() - INTERVAL '2 days'),
    ('SMOKE', NULL, 2, 4, NOW() - INTERVAL '1 day'),
    ('FIRE',  NULL, 5, 6, NOW() - INTERVAL '6 hours');

-- 미디어 스트림 (종료된 스트림)
INSERT INTO media_stream (livekit_room_name, streaming_status, fire_event_id) VALUES
    ('fire-room-001', 'ENDED', 1),
    ('fire-room-002', 'ENDED', 2),
    ('fire-room-003', 'ENDED', 3);

-- 미디어 녹화본 (샘플 S3 경로)
INSERT INTO media_record (s3_bucket_path, fire_event_id) VALUES
    ('recordings/fire-room-001.mp4', 1),
    ('recordings/fire-room-002.mp4', 2),
    ('recordings/fire-room-003.mp4', 3);

-- ─── 완료 ────────────────────────────────────────────
-- 초기화 완료: 사용자 2, 건물 3, 방 5, 카메라 8, 화재 이벤트 3
