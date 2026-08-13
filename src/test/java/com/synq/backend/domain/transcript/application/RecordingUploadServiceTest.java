package com.synq.backend.domain.transcript.application;

import com.synq.backend.domain.transcript.entity.MeetingRecordingSegment;
import com.synq.backend.domain.transcript.repository.MeetingRecordingSegmentRepository;
import com.synq.backend.domain.transcript.storage.RecordingStorage;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class RecordingUploadServiceTest {

	private final RecordingStorage recordingStorage = mock(RecordingStorage.class);
	private final MeetingRecordingSegmentRepository repository = mock(MeetingRecordingSegmentRepository.class);
	private final RecordingUploadService service = new RecordingUploadService(recordingStorage, repository);

	@Test
	void 임시파일을_업로드하고_세그먼트로_저장한_뒤_임시파일을_삭제한다() throws Exception {
		Path tempFile = Files.createTempFile("recording-upload-test-", ".webm");
		Files.writeString(tempFile, "audio-bytes", StandardCharsets.UTF_8);

		service.uploadAsync(5L, tempFile);

		verify(recordingStorage).upload(
				startsWith("recordings/5/"), any(InputStream.class), eq((long) "audio-bytes".length()), eq("audio/webm"));
		verify(repository).save(any(MeetingRecordingSegment.class));
		assertThat(Files.exists(tempFile)).isFalse();
	}

	@Test
	void 업로드가_실패해도_예외를_전파하지_않고_임시파일을_정리한다() throws Exception {
		Path tempFile = Files.createTempFile("recording-upload-test-", ".webm");
		Files.writeString(tempFile, "audio-bytes", StandardCharsets.UTF_8);
		org.mockito.Mockito.doThrow(new RuntimeException("업로드 실패"))
				.when(recordingStorage).upload(anyString(), any(InputStream.class), anyLong(), anyString());

		service.uploadAsync(5L, tempFile);

		verifyNoInteractions(repository);
		assertThat(Files.exists(tempFile)).isFalse();
	}
}
