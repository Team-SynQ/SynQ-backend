package com.synq.backend.domain.reference.storage;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class S3StorageConfigTest {

	private final S3StorageConfig config = new S3StorageConfig();

	@Test
	void endpoint가_없으면_AWS_기본_endpoint와_Credential_Chain을_사용하는_Client를_생성한다() {
		S3StorageProperties properties = new S3StorageProperties(
				"synq-bucket", "ap-northeast-2", "", "", "", false);

		try (S3Client client = config.referenceS3Client(properties)) {
			assertThat(client.serviceClientConfiguration().region()).isEqualTo(Region.AP_NORTHEAST_2);
			assertThat(client.serviceClientConfiguration().endpointOverride()).isEmpty();
		}
	}

	@Test
	void endpoint가_있으면_로컬_S3_호환_endpoint를_적용한다() {
		S3StorageProperties properties = new S3StorageProperties(
				"synq-bucket", "ap-northeast-2", "http://localhost:9000",
				"", "", true);

		try (S3Client client = config.referenceS3Client(properties)) {
			assertThat(client.serviceClientConfiguration().endpointOverride())
					.contains(URI.create("http://localhost:9000"));
		}
	}

	@Test
	void region이_없거나_Credential_한쪽만_있으면_명확하게_실패한다() {
		assertThatThrownBy(() -> config.referenceS3Client(new S3StorageProperties(
				"synq-bucket", "", "", "", "", false)))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("storage.s3.region 설정이 필요합니다.");
		assertThatThrownBy(() -> config.referenceS3Client(new S3StorageProperties(
				"synq-bucket", "ap-northeast-2", "", UUID.randomUUID().toString(), "", false)))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("S3 access key와 secret key는 함께 설정해야 합니다.");
	}
}
