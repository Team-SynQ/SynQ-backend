package com.synq.backend.domain.user.service;

import com.synq.backend.domain.user.code.UserErrorCode;
import com.synq.backend.domain.user.entity.User;
import com.synq.backend.domain.user.image.ProfileImageProperties;
import com.synq.backend.domain.user.image.ProfileImageStorageClient;
import com.synq.backend.domain.user.repository.UserRepository;
import com.synq.backend.global.apipayload.code.GeneralErrorCode;
import com.synq.backend.global.apipayload.exception.GeneralException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

@Service
@Transactional(readOnly = true)
public class ProfileImageService {

	private static final Logger log = LoggerFactory.getLogger(ProfileImageService.class);

	private static final byte[] JPEG_SIGNATURE = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
	private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
	private static final byte[] RIFF_SIGNATURE = {'R', 'I', 'F', 'F'};
	private static final byte[] WEBP_SIGNATURE = {'W', 'E', 'B', 'P'};
	private static final int SIGNATURE_READ_LENGTH = 12;

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
		String contentType = validate(file);
		User user = getUser(userId);
		String previousKey = user.getProfileImageKey();

		String newKey = storageClient.upload(userId, file, contentType);
		compensateOnRollback(newKey);
		user.updateProfileImageKey(newKey);

		if (previousKey != null) {
			deleteAfterCommit(previousKey);
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
		deleteAfterCommit(key);
	}

	public String toUrl(String profileImageKey) {
		return profileImageKey == null ? null : properties.cloudfrontDomain() + "/" + profileImageKey;
	}

	private User getUser(Long userId) {
		return userRepository.findById(userId)
				.orElseThrow(() -> new GeneralException(GeneralErrorCode.NOT_FOUND));
	}

	private String validate(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new GeneralException(UserErrorCode.EMPTY_PROFILE_IMAGE);
		}
		String contentType = detectContentType(file);
		if (file.getSize() > properties.maxSizeBytes()) {
			throw new GeneralException(UserErrorCode.PROFILE_IMAGE_TOO_LARGE);
		}
		return contentType;
	}

	private String detectContentType(MultipartFile file) {
		byte[] header;
		try (InputStream in = file.getInputStream()) {
			header = in.readNBytes(SIGNATURE_READ_LENGTH);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		if (startsWith(header, JPEG_SIGNATURE, 0)) {
			return "image/jpeg";
		}
		if (startsWith(header, PNG_SIGNATURE, 0)) {
			return "image/png";
		}
		if (startsWith(header, RIFF_SIGNATURE, 0) && startsWith(header, WEBP_SIGNATURE, 8)) {
			return "image/webp";
		}
		throw new GeneralException(UserErrorCode.INVALID_PROFILE_IMAGE_TYPE);
	}

	private boolean startsWith(byte[] data, byte[] pattern, int offset) {
		if (data.length < offset + pattern.length) {
			return false;
		}
		for (int i = 0; i < pattern.length; i++) {
			if (data[offset + i] != pattern[i]) {
				return false;
			}
		}
		return true;
	}

	private void compensateOnRollback(String key) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCompletion(int status) {
				if (status != TransactionSynchronization.STATUS_COMMITTED) {
					deleteQuietly(key);
				}
			}
		});
	}

	private void deleteAfterCommit(String key) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			deleteQuietly(key);
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				deleteQuietly(key);
			}
		});
	}

	private void deleteQuietly(String key) {
		try {
			storageClient.delete(key);
		} catch (RuntimeException e) {
			log.error("프로필 이미지 S3 객체 삭제 실패. key={}", key, e);
		}
	}
}
