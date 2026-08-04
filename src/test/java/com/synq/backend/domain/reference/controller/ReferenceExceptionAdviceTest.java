package com.synq.backend.domain.reference.controller;

import com.synq.backend.global.apipayload.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import static org.assertj.core.api.Assertions.assertThat;

class ReferenceExceptionAdviceTest {

	@Test
	void multipart_크기_초과를_REFERENCE413_1로_반환한다() {
		ReferenceExceptionAdvice advice = new ReferenceExceptionAdvice();

		ResponseEntity<ApiResponse<Void>> response = advice.handleMaxUploadSizeException(
				new MaxUploadSizeExceededException(20L * 1024 * 1024));

		assertThat(response.getStatusCode().value()).isEqualTo(413);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getCode()).isEqualTo("REFERENCE413_1");
	}
}
