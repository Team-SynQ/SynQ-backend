package com.synq.backend.domain.reference.storage;

import java.io.InputStream;

public interface ReferenceStorage {

	void upload(String storageKey, InputStream inputStream, long contentLength, String contentType);

	/** 재인덱싱 시 원본을 다시 읽는다. 호출자가 스트림을 닫는다. */
	InputStream download(String storageKey);

	void delete(String storageKey);
}
