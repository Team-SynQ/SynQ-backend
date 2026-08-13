package com.synq.backend.domain.transcript.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

/**
 * 회의 녹음 세그먼트 하나(SttSession 하나, webm 스트림 하나)의 오디오 바이트를 로컬 임시파일에
 * 순서대로 이어붙인다. 별도 인코딩 없이 그대로 append하므로 완성된 파일 자체가 재생 가능한 webm이다.
 */
public class RecordingWriter {

	private static final Logger log = LoggerFactory.getLogger(RecordingWriter.class);

	private final Long meetingId;
	private final Path tempFile;
	private OutputStream out;
	private boolean hasBytes;
	private boolean failed;

	public RecordingWriter(Long meetingId) {
		this.meetingId = meetingId;
		Path file;
		OutputStream stream;
		try {
			file = Files.createTempFile("meeting-recording-" + meetingId + "-", ".webm");
			stream = Files.newOutputStream(file, StandardOpenOption.WRITE);
		} catch (IOException e) {
			log.error("녹음 임시파일 생성에 실패했습니다. meetingId={}", meetingId, e);
			file = null;
			stream = null;
			this.failed = true;
		}
		this.tempFile = file;
		this.out = stream;
	}

	public void append(byte[] payload) {
		if (failed) {
			return;
		}
		try {
			out.write(payload);
			hasBytes = true;
		} catch (IOException e) {
			log.error("녹음 임시파일 쓰기에 실패했습니다. meetingId={}", meetingId, e);
			failed = true;
			closeQuietly();
		}
	}

	/** 스트림을 닫고 완성된 파일 경로를 반환한다. 한 바이트도 못 썼으면 임시파일을 지우고 empty를 반환한다. */
	public Optional<Path> finish() {
		closeQuietly();
		if (failed || !hasBytes) {
			deleteQuietly();
			return Optional.empty();
		}
		return Optional.of(tempFile);
	}

	private void closeQuietly() {
		if (out == null) {
			return;
		}
		try {
			out.close();
		} catch (IOException e) {
			log.debug("녹음 임시파일 스트림 종료 중 오류: meetingId={} reason={}", meetingId, e.getMessage());
		} finally {
			out = null;
		}
	}

	private void deleteQuietly() {
		if (tempFile == null) {
			return;
		}
		try {
			Files.deleteIfExists(tempFile);
		} catch (IOException e) {
			log.debug("빈 녹음 임시파일 삭제 중 오류: meetingId={} reason={}", meetingId, e.getMessage());
		}
	}
}
