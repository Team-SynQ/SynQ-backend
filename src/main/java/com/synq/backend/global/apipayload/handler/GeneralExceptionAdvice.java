package com.synq.backend.global.apipayload.handler;

import com.synq.backend.global.apipayload.ApiResponse;
import com.synq.backend.global.apipayload.code.GeneralErrorCode;
import com.synq.backend.global.apipayload.exception.GeneralException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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

	@ExceptionHandler({
			ConstraintViolationException.class,
			HandlerMethodValidationException.class,
			MethodArgumentTypeMismatchException.class,
			HttpMessageNotReadableException.class,
			MissingRequestHeaderException.class,
			MissingServletRequestParameterException.class
	})
	public ResponseEntity<ApiResponse<Void>> handleRequestException(Exception e) {
		return ResponseEntity.status(GeneralErrorCode.BAD_REQUEST.getStatus())
				.body(ApiResponse.onFailure(GeneralErrorCode.BAD_REQUEST, null));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
		return ResponseEntity.status(GeneralErrorCode.INTERNAL_SERVER_ERROR.getStatus())
				.body(ApiResponse.onFailure(GeneralErrorCode.INTERNAL_SERVER_ERROR, null));
	}
}
