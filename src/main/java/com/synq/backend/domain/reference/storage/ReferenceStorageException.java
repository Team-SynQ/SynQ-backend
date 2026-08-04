package com.synq.backend.domain.reference.storage;

public class ReferenceStorageException extends RuntimeException {

	public ReferenceStorageException(String message) {
		super(message);
	}

	public ReferenceStorageException(String message, Throwable cause) {
		super(message, cause);
	}
}
