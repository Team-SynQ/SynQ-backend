package com.synq.backend.domain.user.controller;

import com.synq.backend.domain.auth.jwt.JwtProvider;
import com.synq.backend.domain.user.entity.User;
import com.synq.backend.domain.user.image.ProfileImageStorageClient;
import com.synq.backend.domain.user.repository.UserRepository;
import com.synq.backend.support.PostgresTestContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class ProfileImageControllerTest extends PostgresTestContainer {

	// 매직바이트 검증만 통과하면 되므로, 실제로 디코딩 가능한 이미지일 필요는 없다.
	private static final byte[] JPEG_BYTES = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x00, 0x00};
	private static final byte[] PNG_BYTES =
			{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x00};

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private JwtProvider jwtProvider;

	@MockitoBean
	private ProfileImageStorageClient profileImageStorageClient;

	@Test
	void 이미지를_업로드하면_URL을_반환한다() throws Exception {
		when(profileImageStorageClient.upload(any(), any(), any())).thenReturn("img/profile/1/test-key.jpg");
		User user = userRepository.save(User.ofLocal("테스트", "upload@synq.com", "password-hash"));
		MockMultipartFile file = new MockMultipartFile("file", "profile.jpg", "image/jpeg", JPEG_BYTES);

		mockMvc.perform(multipart("/users/me/profile-image")
						.file(file)
						.header("Authorization", bearerToken(user)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.result.profileImageUrl").exists());
	}

	@Test
	void 빈_파일을_업로드하면_400을_반환한다() throws Exception {
		User user = userRepository.save(User.ofLocal("테스트", "empty@synq.com", "password-hash"));
		MockMultipartFile file = new MockMultipartFile("file", "profile.jpg", "image/jpeg", new byte[0]);

		mockMvc.perform(multipart("/users/me/profile-image")
						.file(file)
						.header("Authorization", bearerToken(user)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("USER400_5"));
	}

	@Test
	void 허용되지_않는_형식이면_400을_반환한다() throws Exception {
		User user = userRepository.save(User.ofLocal("테스트", "badtype@synq.com", "password-hash"));
		byte[] gifBytes = "GIF89a".getBytes();
		MockMultipartFile file = new MockMultipartFile("file", "profile.gif", "image/gif", gifBytes);

		mockMvc.perform(multipart("/users/me/profile-image")
						.file(file)
						.header("Authorization", bearerToken(user)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("USER400_6"));
	}

	@Test
	void content_type_헤더를_속여도_실제_바이트로_검증해_400을_반환한다() throws Exception {
		User user = userRepository.save(User.ofLocal("테스트", "spoofed@synq.com", "password-hash"));
		// Content-Type은 image/jpeg 라고 주장하지만 실제 바이트는 이미지 시그니처가 아니다.
		MockMultipartFile file = new MockMultipartFile("file", "profile.jpg", "image/jpeg", "not-an-image".getBytes());

		mockMvc.perform(multipart("/users/me/profile-image")
						.file(file)
						.header("Authorization", bearerToken(user)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("USER400_6"));
	}

	@Test
	void 이미지를_삭제하면_profileImageKey가_null이_된다() throws Exception {
		when(profileImageStorageClient.upload(any(), any(), any())).thenReturn("img/profile/1/test-key.png");
		User user = userRepository.save(User.ofLocal("테스트", "delete@synq.com", "password-hash"));
		MockMultipartFile file = new MockMultipartFile("file", "profile.png", "image/png", PNG_BYTES);
		mockMvc.perform(multipart("/users/me/profile-image")
						.file(file)
						.header("Authorization", bearerToken(user)))
				.andExpect(status().isOk());

		mockMvc.perform(delete("/users/me/profile-image")
						.header("Authorization", bearerToken(user)))
				.andExpect(status().isOk());

		User updated = userRepository.findById(user.getUserId()).orElseThrow();
		assertThat(updated.getProfileImageKey()).isNull();
	}

	@Test
	void 토큰_없이_삭제하면_401을_반환한다() throws Exception {
		mockMvc.perform(delete("/users/me/profile-image"))
				.andExpect(status().isUnauthorized());
	}

	private String bearerToken(User user) {
		return "Bearer " + jwtProvider.createAccessToken(user.getUserId());
	}
}
