package com.synq.backend.domain.transcript.storage;

public class RecordingStorageException extends RuntimeException {

	public RecordingStorageException(String message) {
		super(message);
	}

	public RecordingStorageException(String message, Throwable cause) {
		super(message, cause);
	}
}
