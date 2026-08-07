package com.synq.backend.domain.user.service;

import com.synq.backend.domain.user.code.UserErrorCode;
import com.synq.backend.domain.user.entity.User;
import com.synq.backend.domain.user.image.ProfileImageProperties;
import com.synq.backend.domain.user.image.ProfileImageStorageClient;
import com.synq.backend.domain.user.repository.UserRepository;
import com.synq.backend.global.apipayload.code.GeneralErrorCode;
import com.synq.backend.global.apipayload.exception.GeneralException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@Service
@Transactional(readOnly = true)
public class ProfileImageService {

	private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

	private final UserRepository userRepository;
	private final ProfileImageStorageClient storageClient;
	private final ProfileImageProperties properties;

	public ProfileImageService(UserRepository userRepository, ProfileImageStorageClient storageClient,
								ProfileImageProperties properties) {
		this.userRepository = userRepository;
		this.storageClient = storageClient;
		this.properties = properties;
	}

	@Transactional
	public String upload(Long userId, MultipartFile file) {
		validate(file);
		User user = getUser(userId);

		String previousKey = user.getProfileImageKey();
		String newKey = storageClient.upload(userId, file);
		user.updateProfileImageKey(newKey);

		if (previousKey != null) {
			storageClient.delete(previousKey);
		}
		return toUrl(newKey);
	}

	@Transactional
	public void delete(Long userId) {
		User user = getUser(userId);
		String key = user.getProfileImageKey();
		if (key == null) {
			return;
		}
		user.updateProfileImageKey(null);
		storageClient.delete(key);
	}

	public String toUrl(String profileImageKey) {
		return profileImageKey == null ? null : properties.cloudfrontDomain() + "/" + profileImageKey;
	}

	private User getUser(Long userId) {
		return userRepository.findById(userId)
				.orElseThrow(() -> new GeneralException(GeneralErrorCode.NOT_FOUND));
	}

	private void validate(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new GeneralException(UserErrorCode.EMPTY_PROFILE_IMAGE);
		}
		if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
			throw new GeneralException(UserErrorCode.INVALID_PROFILE_IMAGE_TYPE);
		}
		if (file.getSize() > properties.maxSizeBytes()) {
			throw new GeneralException(UserErrorCode.PROFILE_IMAGE_TOO_LARGE);
		}
	}
}
