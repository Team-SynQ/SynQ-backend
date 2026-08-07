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
		when(profileImageStorageClient.upload(any(), any())).thenReturn("img/profile/1/test-key.jpg");
		User user = userRepository.save(User.ofLocal("테스트", "upload@synq.com", "password-hash"));
		MockMultipartFile file = new MockMultipartFile("file", "profile.jpg", "image/jpeg", "fake-image-bytes".getBytes());

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
		MockMultipartFile file = new MockMultipartFile("file", "profile.gif", "image/gif", "gif-bytes".getBytes());

		mockMvc.perform(multipart("/users/me/profile-image")
						.file(file)
						.header("Authorization", bearerToken(user)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("USER400_6"));
	}

	@Test
	void 이미지를_삭제하면_profileImageKey가_null이_된다() throws Exception {
		when(profileImageStorageClient.upload(any(), any())).thenReturn("img/profile/1/test-key.png");
		User user = userRepository.save(User.ofLocal("테스트", "delete@synq.com", "password-hash"));
		MockMultipartFile file = new MockMultipartFile("file", "profile.png", "image/png", "png-bytes".getBytes());
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
