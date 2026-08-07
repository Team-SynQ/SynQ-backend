package com.synq.backend.domain.user.image;

import org.springframework.web.multipart.MultipartFile;

public interface ProfileImageStorageClient {

	String upload(Long userId, MultipartFile file);

	void delete(String key);
}
