package com.synq.backend.domain.transcript.storage;

import com.synq.backend.domain.reference.storage.S3StorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.InputStream;
import java.time.Duration;

@Component
@RequiredArgsConstructor
public class S3RecordingStorage implements RecordingStorage {

	private final S3Client s3Client;
	private final S3Presigner s3Presigner;
	private final S3StorageProperties properties;

	@Override
	public void upload(String storageKey, InputStream inputStream, long contentLength, String contentType) {
		String bucket = requireBucket();
		try {
			s3Client.putObject(
					PutObjectRequest.builder()
							.bucket(bucket)
							.key(storageKey)
							.contentType(contentType)
							.build(),
					RequestBody.fromInputStream(inputStream, contentLength)
			);
		} catch (RuntimeException exception) {
			throw new RecordingStorageException("녹음 파일 업로드 실패", exception);
		}
	}

	@Override
	public String presignedUrl(String storageKey, Duration ttl) {
		String bucket = requireBucket();
		try {
			return s3Presigner.presignGetObject(GetObjectPresignRequest.builder()
							.signatureDuration(ttl)
							.getObjectRequest(GetObjectRequest.builder()
									.bucket(bucket)
									.key(storageKey)
									.build())
							.build())
					.url()
					.toString();
		} catch (RuntimeException exception) {
			throw new RecordingStorageException("녹음 재생 URL 발급 실패", exception);
		}
	}

	private String requireBucket() {
		if (properties.bucket() == null || properties.bucket().isBlank()) {
			throw new RecordingStorageException("storage.s3.bucket 설정이 필요합니다.");
		}
		return properties.bucket();
	}
}
