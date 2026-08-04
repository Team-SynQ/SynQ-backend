package com.synq.backend.domain.reference.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class S3ReferenceStorageTest {

	@Mock
	private S3Client s3Client;

	private S3ReferenceStorage storage;

	@BeforeEach
	void setUp() {
		storage = new S3ReferenceStorage(s3Client, properties("synq-bucket"));
	}

	@Test
	void bucket과_key와_Content_Length와_Content_Type으로_객체를_업로드한다() {
		byte[] content = "file-content".getBytes(StandardCharsets.UTF_8);
		when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
				.thenReturn(PutObjectResponse.builder().build());

		storage.upload(
				"references/1/file.pdf",
				new ByteArrayInputStream(content),
				content.length,
				"application/pdf"
		);

		ArgumentCaptor<PutObjectRequest> request = ArgumentCaptor.forClass(PutObjectRequest.class);
		ArgumentCaptor<RequestBody> body = ArgumentCaptor.forClass(RequestBody.class);
		verify(s3Client).putObject(request.capture(), body.capture());
		assertThat(request.getValue().bucket()).isEqualTo("synq-bucket");
		assertThat(request.getValue().key()).isEqualTo("references/1/file.pdf");
		assertThat(request.getValue().contentType()).isEqualTo("application/pdf");
		assertThat(body.getValue().optionalContentLength()).contains((long) content.length);
	}

	@Test
	void bucket과_key로_객체를_삭제한다() {
		when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
				.thenReturn(DeleteObjectResponse.builder().build());

		storage.delete("references/1/file.pdf");

		ArgumentCaptor<DeleteObjectRequest> request = ArgumentCaptor.forClass(DeleteObjectRequest.class);
		verify(s3Client).deleteObject(request.capture());
		assertThat(request.getValue().bucket()).isEqualTo("synq-bucket");
		assertThat(request.getValue().key()).isEqualTo("references/1/file.pdf");
	}

	@Test
	void SDK_예외를_ReferenceStorageException으로_변환한다() {
		when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
				.thenThrow(S3Exception.builder().message("S3 실패").statusCode(500).build());

		assertThatThrownBy(() -> storage.upload(
				"references/1/file.pdf",
				new ByteArrayInputStream(new byte[]{1}),
				1,
				"application/pdf"
		)).isInstanceOf(ReferenceStorageException.class)
				.hasMessage("참고자료 객체 업로드 실패");
	}

	@Test
	void bucket이_없으면_명확한_설정_예외를_반환한다() {
		S3ReferenceStorage missingBucketStorage = new S3ReferenceStorage(s3Client, properties(""));

		assertThatThrownBy(() -> missingBucketStorage.upload(
				"references/1/file.pdf",
				new ByteArrayInputStream(new byte[]{1}),
				1,
				"application/pdf"
		)).isInstanceOf(ReferenceStorageException.class)
				.hasMessage("storage.s3.bucket 설정이 필요합니다.");
		verifyNoInteractions(s3Client);
	}

	private S3StorageProperties properties(String bucket) {
		return new S3StorageProperties(bucket, "ap-northeast-2", "", "", "", false);
	}
}
