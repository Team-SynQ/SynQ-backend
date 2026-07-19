-- 회의 참여자-역할. user_id 는 user 도메인에 아직 엔티티/테이블이 없어 FK 제약 없이 BIGINT 로 둔다.
CREATE TABLE meeting_participant (
    id          BIGSERIAL PRIMARY KEY,
    meeting_id  BIGINT NOT NULL REFERENCES meeting (id),
    user_id     BIGINT NOT NULL,
    role        VARCHAR(10) NOT NULL
                CHECK (role IN ('HOST', 'MEMBER')),
    joined_at   TIMESTAMP NOT NULL,
    left_at     TIMESTAMP,
    created_at  TIMESTAMP NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_meeting_participant_meeting_id_role ON meeting_participant (meeting_id, role);
CREATE INDEX idx_meeting_participant_meeting_id_user_id ON meeting_participant (meeting_id, user_id);
