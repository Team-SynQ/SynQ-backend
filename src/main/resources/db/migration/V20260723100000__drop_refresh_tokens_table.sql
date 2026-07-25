-- refresh token 저장소를 postgres에서 레디스로 이전했다. 더 이상 이 테이블을 쓰지 않는다.
DROP TABLE IF EXISTS refresh_tokens;
