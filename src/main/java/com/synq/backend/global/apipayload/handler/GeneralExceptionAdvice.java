package com.synq.backend.global.apipayload.handler;

import com.synq.backend.global.apipayload.ApiResponse;
import com.synq.backend.global.apipayload.code.GeneralErrorCode;
import com.synq.backend.global.apipayload.exception.GeneralException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GeneralExceptionAdvice {

	@ExceptionHandler(GeneralException.class)
	public ResponseEntity<ApiResponse<Void>> handleGeneralException(GeneralException e) {
		return ResponseEntity.status(e.getCode().getStatus())
				.body(ApiResponse.onFailure(e.getCode(), null));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
		String message = e.getBindingResult().getFieldErrors().stream()
				.findFirst()
				.map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
				.orElse(GeneralErrorCode.BAD_REQUEST.getMessage());
		return ResponseEntity.status(GeneralErrorCode.BAD_REQUEST.getStatus())
				.body(new ApiResponse<>(false, GeneralErrorCode.BAD_REQUEST.getCode(), message, null));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
		return ResponseEntity.status(GeneralErrorCode.INTERNAL_SERVER_ERROR.getStatus())
				.body(ApiResponse.onFailure(GeneralErrorCode.INTERNAL_SERVER_ERROR, null));
	}
}
