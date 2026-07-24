package com.synq.backend.domain.ai.assistant.mock;

import static org.assertj.core.api.Assertions.assertThat;

import com.synq.backend.domain.ai.assistant.port.TranscriptWindow;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class FakeTranscriptSegmentPortTest {

	private final FakeTranscriptSegmentPort port = new FakeTranscriptSegmentPort();

	@Test
	void 중간_세그먼트는_앞뒤_윈도우를_모두_채운다() {
		TranscriptWindow window = port.findWindow(3L, 2, 2).orElseThrow();

		assertThat(window.meetingId()).isEqualTo(1L);
		assertThat(window.focus().segmentId()).isEqualTo(3L);
		assertThat(window.before()).hasSize(2);
		assertThat(window.after()).hasSize(2);
	}

	@Test
	void 첫_세그먼트는_앞_윈도우가_비어_경계를_넘지_않는다() {
		TranscriptWindow window = port.findWindow(1L, 2, 2).orElseThrow();

		assertThat(window.before()).isEmpty();
		assertThat(window.after()).hasSize(2);
	}

	@Test
	void 범위를_벗어난_segmentId_는_빈값이다() {
		assertThat(port.findWindow(99L, 2, 2)).isEqualTo(Optional.empty());
	}
}
