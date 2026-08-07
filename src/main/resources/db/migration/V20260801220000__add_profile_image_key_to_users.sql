-- 프로필 이미지의 S3 객체 키만 저장한다. 실제 URL은 CloudFront 도메인과 조합해 조회 시점에 만든다.
ALTER TABLE users ADD COLUMN profile_image_key VARCHAR(255);
