package com.synq.backend.domain.reference.controller;

import com.synq.backend.domain.reference.code.ReferenceErrorCode;
import com.synq.backend.domain.reference.dto.ReferenceFileExtractionFailure;
import com.synq.backend.domain.reference.file.ReferenceFileExtractionException;
import com.synq.backend.global.apipayload.ApiResponse;
import java.util.List;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ReferenceExceptionAdvice {

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeException(MaxUploadSizeExceededException exception) {
		return ResponseEntity.status(ReferenceErrorCode.REFERENCE_FILE_SIZE_EXCEEDED.getStatus())
				.body(ApiResponse.onFailure(ReferenceErrorCode.REFERENCE_FILE_SIZE_EXCEEDED, null));
	}

	@ExceptionHandler(ReferenceFileExtractionException.class)
	public ResponseEntity<ApiResponse<List<ReferenceFileExtractionFailure>>> handleExtractionFailure(
			ReferenceFileExtractionException exception
	) {
		// 어느 파일이 왜 걸렸는지 result 에 실어야 사용자가 고칠 수 있다.
		return ResponseEntity.status(ReferenceErrorCode.REFERENCE_FILE_TEXT_EXTRACTION_FAILED.getStatus())
				.body(ApiResponse.onFailure(
						ReferenceErrorCode.REFERENCE_FILE_TEXT_EXTRACTION_FAILED,
						exception.getFailures()));
	}
}
