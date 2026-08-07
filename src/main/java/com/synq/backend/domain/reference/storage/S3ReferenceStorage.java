package com.synq.backend.domain.reference.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.InputStream;

@Component
@RequiredArgsConstructor
public class S3ReferenceStorage implements ReferenceStorage {

	private final S3Client s3Client;
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
			throw new ReferenceStorageException("참고자료 객체 업로드 실패", exception);
		}
	}

	@Override
	public InputStream download(String storageKey) {
		String bucket = requireBucket();
		try {
			return s3Client.getObject(GetObjectRequest.builder()
					.bucket(bucket)
					.key(storageKey)
					.build());
		} catch (RuntimeException exception) {
			throw new ReferenceStorageException("참고자료 객체 다운로드 실패", exception);
		}
	}

	@Override
	public void delete(String storageKey) {
		String bucket = requireBucket();
		try {
			s3Client.deleteObject(DeleteObjectRequest.builder()
					.bucket(bucket)
					.key(storageKey)
					.build());
		} catch (RuntimeException exception) {
			throw new ReferenceStorageException("참고자료 객체 삭제 실패", exception);
		}
	}

	private String requireBucket() {
		if (properties.bucket() == null || properties.bucket().isBlank()) {
			throw new ReferenceStorageException("storage.s3.bucket 설정이 필요합니다.");
		}
		return properties.bucket();
	}
}
