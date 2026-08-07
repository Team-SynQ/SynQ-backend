package com.synq.backend.domain.user.image;

import com.synq.backend.domain.reference.storage.S3StorageProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.UUID;

@Component
public class S3ProfileImageStorageClient implements ProfileImageStorageClient {

	private static final Map<String, String> EXTENSIONS_BY_CONTENT_TYPE = Map.of(
			"image/jpeg", ".jpg",
			"image/png", ".png",
			"image/webp", ".webp"
	);

	private final S3Client s3Client;
	private final S3StorageProperties s3Properties;

	public S3ProfileImageStorageClient(S3Client s3Client, S3StorageProperties s3Properties) {
		this.s3Client = s3Client;
		this.s3Properties = s3Properties;
	}

	@Override
	public String upload(Long userId, MultipartFile file, String contentType) {
		String key = "img/profile/" + userId + "/" + UUID.randomUUID() + extensionOf(contentType);
		try {
			s3Client.putObject(
					PutObjectRequest.builder()
							.bucket(s3Properties.bucket())
							.key(key)
							.contentType(contentType)
							.build(),
					RequestBody.fromInputStream(file.getInputStream(), file.getSize())
			);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return key;
	}

	@Override
	public void delete(String key) {
		s3Client.deleteObject(DeleteObjectRequest.builder()
				.bucket(s3Properties.bucket())
				.key(key)
				.build());
	}

	private String extensionOf(String contentType) {
		return EXTENSIONS_BY_CONTENT_TYPE.getOrDefault(contentType, "");
	}
}
