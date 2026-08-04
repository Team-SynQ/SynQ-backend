package com.synq.backend.domain.reference.controller;

import com.synq.backend.domain.reference.code.ReferenceErrorCode;
import com.synq.backend.global.apipayload.ApiResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice(assignableTypes = ReferenceController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ReferenceExceptionAdvice {

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeException(MaxUploadSizeExceededException exception) {
		return ResponseEntity.status(ReferenceErrorCode.REFERENCE_FILE_SIZE_EXCEEDED.getStatus())
				.body(ApiResponse.onFailure(ReferenceErrorCode.REFERENCE_FILE_SIZE_EXCEEDED, null));
	}
}
