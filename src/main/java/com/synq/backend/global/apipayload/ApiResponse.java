package com.synq.backend.global.apipayload;

import com.synq.backend.global.apipayload.code.BaseCode;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonPropertyOrder({"isSuccess", "code", "message", "result"})
public class ApiResponse<T> {
	@JsonProperty("isSuccess")
	private final boolean successful;

	@JsonProperty("code")
	private final String code;

	@JsonProperty("message")
	private final String message;

	@JsonProperty("result")
	private T result;

	// 성공
	public static <T> ApiResponse<T> onSuccess(BaseCode code, T result) {
		return new ApiResponse<>(true, code.getCode(), code.getMessage(), result);
	}

	// 실패
	public static <T> ApiResponse<T> onFailure(BaseCode code, T result) {
		return new ApiResponse<>(false, code.getCode(), code.getMessage(), result);
	}
}
