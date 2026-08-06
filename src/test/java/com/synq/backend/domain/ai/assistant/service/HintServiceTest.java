package com.synq.backend.domain.ai.assistant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import com.synq.backend.domain.ai.assistant.code.AssistantErrorCode;
import com.synq.backend.domain.ai.assistant.domain.HintInput;
import com.synq.backend.domain.ai.assistant.domain.HintResult;
import com.synq.backend.domain.ai.assistant.mock.FakeHintAiClient;
import com.synq.backend.domain.ai.context.domain.LiveContextSnapshot;
import com.synq.backend.domain.meeting.repository.MeetingParticipantRepository;
import com.synq.backend.global.apipayload.exception.GeneralException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HintServiceTest {

	@Mock
	HintContextBuilder contextBuilder;

	@Mock
	MeetingParticipantRepository meetingParticipantRepository;

	private final FakeHintAiClient aiClient = new FakeHintAiClient();

	@Test
	void 조립한_입력으로_힌트를_생성한다() {
		participating(true);
		HintInput input = new HintInput("발화", List.of(), List.of(), "PM", "", List.of("속도 우선"),
				LiveContextSnapshot.empty(), List.of());
		given(contextBuilder.build(eq(10L), eq(1L), eq(3L))).willReturn(input);

		HintResult result = service().generate(10L, 1L, 3L);

		assertThat(result.meaning()).contains("발화");
		assertThat(result.myImpact()).contains("PM");
		assertThat(result.teamQuestion()).isNotBlank();
	}

	@Test
	void 세그먼트가_없으면_예외를_전파한다() {
		participating(true);
		given(contextBuilder.build(any(), any(), any()))
				.willThrow(new GeneralException(AssistantErrorCode.SEGMENT_NOT_FOUND));

		assertThatThrownBy(() -> service().generate(10L, 1L, 99L))
				.isInstanceOf(GeneralException.class)
				.extracting("code")
				.isEqualTo(AssistantErrorCode.SEGMENT_NOT_FOUND);
	}

	@Test
	void 회의_참가자가_아니면_힌트를_생성하지_않는다() {
		participating(false);

		assertThatThrownBy(() -> service().generate(10L, 1L, 3L))
				.isInstanceOf(GeneralException.class)
				.extracting("code")
				.isEqualTo(AssistantErrorCode.NOT_MEETING_PARTICIPANT);
	}

	@Test
	void 참가자_검증은_맥락_조립보다_먼저_한다() {
		participating(false);

		assertThatThrownBy(() -> service().generate(10L, 1L, 3L))
				.isInstanceOf(GeneralException.class);

		// 검증이 뒤로 밀리면 전사·Live Context 를 이미 읽은 뒤에 거절하게 된다.
		verifyNoInteractions(contextBuilder);
	}

	private HintService service() {
		return new HintService(contextBuilder, aiClient, meetingParticipantRepository);
	}

	private void participating(boolean participating) {
		given(meetingParticipantRepository.existsByMeetingIdAndUserIdAndLeftAtIsNull(1L, 10L))
				.willReturn(participating);
	}
}
