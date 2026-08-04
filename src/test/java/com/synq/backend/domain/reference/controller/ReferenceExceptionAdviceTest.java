package com.synq.backend.domain.reference.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReferenceExceptionAdviceTest {

	@Test
	void Controller_진입_전_크기_초과_예외를_REFERENCE413_1로_매핑한다() throws Exception {
		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new MultipartTestController())
				.setControllerAdvice(new ReferenceExceptionAdvice())
				.addInterceptors(new HandlerInterceptor() {
					@Override
					public boolean preHandle(
							HttpServletRequest request,
							HttpServletResponse response,
							Object handler
					) {
						throw new MaxUploadSizeExceededException(20L * 1024 * 1024);
					}
				})
				.build();

		mockMvc.perform(multipart("/test/reference-files")
						.file(new MockMultipartFile("files", "large.pdf", "application/pdf", new byte[]{1})))
				.andExpect(status().isPayloadTooLarge())
				.andExpect(jsonPath("$.isSuccess").value(false))
				.andExpect(jsonPath("$.code").value("REFERENCE413_1"));
	}

	@RestController
	private static class MultipartTestController {

		@PostMapping(value = "/test/reference-files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
		void upload(@RequestPart("files") MultipartFile file) {
		}
	}
}
