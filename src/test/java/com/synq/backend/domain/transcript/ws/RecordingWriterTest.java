package com.synq.backend.domain.transcript.ws;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class RecordingWriterTest {

	@Test
	void append한_바이트를_순서대로_이어붙여_finish시_파일로_반환한다() throws Exception {
		RecordingWriter writer = new RecordingWriter(1L);

		writer.append("hello ".getBytes(StandardCharsets.UTF_8));
		writer.append("world".getBytes(StandardCharsets.UTF_8));
		Optional<Path> result = writer.finish();

		assertThat(result).isPresent();
		assertThat(Files.readString(result.get(), StandardCharsets.UTF_8)).isEqualTo("hello world");
		Files.deleteIfExists(result.get());
	}

	@Test
	void 한번도_append하지_않으면_finish시_empty를_반환하고_임시파일을_남기지_않는다() {
		RecordingWriter writer = new RecordingWriter(1L);

		Optional<Path> result = writer.finish();

		assertThat(result).isEmpty();
	}

	@Test
	void finish를_두번_불러도_예외없이_같은_결과를_반환한다() throws Exception {
		RecordingWriter writer = new RecordingWriter(1L);
		writer.append("data".getBytes(StandardCharsets.UTF_8));

		Optional<Path> first = writer.finish();
		Optional<Path> second = writer.finish();

		assertThat(first).isPresent();
		assertThat(second).isPresent();
		Files.deleteIfExists(first.get());
	}
}
