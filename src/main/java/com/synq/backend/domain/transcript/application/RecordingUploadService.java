package com.synq.backend.domain.transcript.application;

import com.synq.backend.domain.transcript.entity.MeetingRecordingSegment;
import com.synq.backend.domain.transcript.repository.MeetingRecordingSegmentRepository;
import com.synq.backend.domain.transcript.storage.RecordingStorage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * RecordingWriter.finish() 로 완성된 임시파일을 S3에 업로드하고 세그먼트로 저장한다.
 * 업로드가 실패해도 회의 종료/WS 종료 흐름을 막지 않도록 예외를 삼키고 로그만 남긴다.
 */
@Service
@RequiredArgsConstructor
public class RecordingUploadService {

	private static final Logger log = LoggerFactory.getLogger(RecordingUploadService.class);

	private final RecordingStorage recordingStorage;
	private final MeetingRecordingSegmentRepository repository;

	@Async("recordingExecutor")
	public void uploadAsync(Long meetingId, Path recordedFile) {
		try {
			long contentLength = Files.size(recordedFile);
			String storageKey = "recordings/%d/%s.webm".formatted(meetingId, UUID.randomUUID());
			try (InputStream inputStream = Files.newInputStream(recordedFile)) {
				recordingStorage.upload(storageKey, inputStream, contentLength, "audio/webm");
			}
			repository.save(MeetingRecordingSegment.of(meetingId, storageKey));
		} catch (IOException | RuntimeException e) {
			log.error("녹음 세그먼트 업로드에 실패했습니다. meetingId={}", meetingId, e);
		} finally {
			deleteQuietly(meetingId, recordedFile);
		}
	}

	private void deleteQuietly(Long meetingId, Path file) {
		try {
			Files.deleteIfExists(file);
		} catch (IOException e) {
			log.debug("녹음 임시파일 정리 중 오류: meetingId={} reason={}", meetingId, e.getMessage());
		}
	}
}
