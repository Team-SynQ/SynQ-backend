package com.synq.backend.domain.transcript.storage;

import java.io.InputStream;
import java.time.Duration;

public interface RecordingStorage {

	void upload(String storageKey, InputStream inputStream, long contentLength, String contentType);

	/** 재생용 임시 접근 URL을 발급한다. ttl이 지나면 만료된다. */
	String presignedUrl(String storageKey, Duration ttl);
}
