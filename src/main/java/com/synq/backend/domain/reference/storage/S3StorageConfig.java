package com.synq.backend.domain.reference.storage;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
public class S3StorageConfig {

	@Bean(destroyMethod = "close")
	public S3Client referenceS3Client(S3StorageProperties properties) {
		String region = requireText(properties.region(), "storage.s3.region 설정이 필요합니다.");
		boolean hasAccessKey = hasText(properties.accessKey());
		boolean hasSecretKey = hasText(properties.secretKey());
		if (hasAccessKey != hasSecretKey) {
			throw new IllegalStateException("S3 access key와 secret key는 함께 설정해야 합니다.");
		}
		S3ClientBuilder builder = S3Client.builder()
				.region(Region.of(region))
				.serviceConfiguration(S3Configuration.builder()
						.pathStyleAccessEnabled(properties.pathStyleAccessEnabled())
						.build());

		if (hasAccessKey) {
			builder.credentialsProvider(StaticCredentialsProvider.create(
					AwsBasicCredentials.create(properties.accessKey(), properties.secretKey())));
		} else {
			builder.credentialsProvider(DefaultCredentialsProvider.create());
		}
		if (hasText(properties.endpoint())) {
			builder.endpointOverride(URI.create(properties.endpoint()));
		}
		return builder.build();
	}

	// 녹음 재생용 presigned URL 발급에 쓴다. referenceS3Client 와 같은 자격증명/리전 설정을 공유한다.
	@Bean(destroyMethod = "close")
	public S3Presigner s3Presigner(S3StorageProperties properties) {
		String region = requireText(properties.region(), "storage.s3.region 설정이 필요합니다.");
		boolean hasAccessKey = hasText(properties.accessKey());
		boolean hasSecretKey = hasText(properties.secretKey());
		if (hasAccessKey != hasSecretKey) {
			throw new IllegalStateException("S3 access key와 secret key는 함께 설정해야 합니다.");
		}
		S3Presigner.Builder builder = S3Presigner.builder()
				.region(Region.of(region))
				.serviceConfiguration(S3Configuration.builder()
						.pathStyleAccessEnabled(properties.pathStyleAccessEnabled())
						.build());

		if (hasAccessKey) {
			builder.credentialsProvider(StaticCredentialsProvider.create(
					AwsBasicCredentials.create(properties.accessKey(), properties.secretKey())));
		} else {
			builder.credentialsProvider(DefaultCredentialsProvider.create());
		}
		if (hasText(properties.endpoint())) {
			builder.endpointOverride(URI.create(properties.endpoint()));
		}
		return builder.build();
	}

	private boolean hasText(String value) {
		return value != null && !value.isBlank();
	}

	private String requireText(String value, String message) {
		if (!hasText(value)) {
			throw new IllegalStateException(message);
		}
		return value;
	}
}
