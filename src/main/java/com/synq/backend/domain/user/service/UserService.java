package com.synq.backend.domain.user.service;

import com.synq.backend.domain.user.dto.UserMeResponse;
import com.synq.backend.domain.user.entity.User;
import com.synq.backend.domain.user.repository.UserRepository;
import com.synq.backend.global.apipayload.code.GeneralErrorCode;
import com.synq.backend.global.apipayload.exception.GeneralException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {

	private final UserRepository userRepository;
	private final ProfileImageService profileImageService;

	public UserService(UserRepository userRepository, ProfileImageService profileImageService) {
		this.userRepository = userRepository;
		this.profileImageService = profileImageService;
	}

	@Transactional
	public UserMeResponse updateName(Long userId, String name) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new GeneralException(GeneralErrorCode.NOT_FOUND));
		user.updateName(name);
		return UserMeResponse.from(user, profileImageService.toUrl(user.getProfileImageKey()));
	}
}
