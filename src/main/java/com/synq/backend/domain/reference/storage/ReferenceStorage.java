package com.synq.backend.domain.reference.storage;

import java.io.InputStream;

public interface ReferenceStorage {

	void upload(String storageKey, InputStream inputStream, long contentLength, String contentType);

	void delete(String storageKey);
}
