-- 검색 스코프. reference_material 을 거치면 알 수 있는 값이지만(document_chunk → reference_material
-- → project), 조인 필터링은 플래너가 "조인 → 필터 → 벡터 정렬" 순으로 처리하며 HNSW 인덱스를
-- 활용하지 못해 전체 스캔으로 떨어지기 쉽다. 회의 중 기능이라 지연이 곧 품질 저하이므로
-- 이행적 종속(3NF 위반)을 감수하고 비정규화한다.
--
-- 참고자료를 다른 프로젝트로 옮기는 기능은 SynQ 에 없다. 따라서 이 값이 낡을 일이 없다.
-- 설령 생기더라도 해당 자료를 재인덱싱하면 해결된다(replace 가 멱등이다).
--
-- reference_material_id 와 마찬가지로 FK 제약은 project 테이블이 생긴 뒤 별도 마이그레이션에서 추가한다.

DELETE FROM document_chunk;

ALTER TABLE document_chunk ADD COLUMN project_id BIGINT NOT NULL;

CREATE INDEX idx_document_chunk_project_id ON document_chunk (project_id);
