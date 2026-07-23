package com.synq.backend.global.apipayload.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synq.backend.global.apipayload.ApiResponse;
import com.synq.backend.global.apipayload.code.GeneralErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final ObjectMapper objectMapper;

	public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
						AuthenticationException authException) throws IOException {
		response.setStatus(GeneralErrorCode.UNAUTHORIZED.getStatus().value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write(objectMapper.writeValueAsString(
				ApiResponse.onFailure(GeneralErrorCode.UNAUTHORIZED, null)));
	}
}
