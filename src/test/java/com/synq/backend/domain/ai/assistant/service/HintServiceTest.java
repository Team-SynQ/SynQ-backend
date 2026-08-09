package com.synq.backend.domain.ai.assistant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.doThrow;

import com.synq.backend.domain.ai.assistant.application.HintStore;
import com.synq.backend.domain.ai.assistant.code.AssistantErrorCode;
import com.synq.backend.domain.ai.assistant.domain.HintInput;
import com.synq.backend.domain.ai.assistant.domain.HintResult;
import com.synq.backend.domain.ai.assistant.mock.FakeHintAiClient;
import com.synq.backend.domain.ai.context.domain.LiveContextSnapshot;
import com.synq.backend.domain.meeting.code.MeetingErrorCode;
import com.synq.backend.domain.meeting.service.MeetingParticipantAccessValidator;
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
	MeetingParticipantAccessValidator accessValidator;

	@Mock
	HintStore hintStore;

	private final FakeHintAiClient aiClient = new FakeHintAiClient();

	@Test
	void 조립한_입력으로_힌트를_생성한다() {
		HintInput input = new HintInput("발화", List.of(), List.of(), "PM", "", List.of("속도 우선"),
				LiveContextSnapshot.empty(), List.of());
		given(contextBuilder.build(eq(10L), eq(1L), eq(3L))).willReturn(input);

		HintService service = new HintService(contextBuilder, aiClient, accessValidator, hintStore);
		HintResult result = service.generate(10L, 1L, 3L);

		verify(accessValidator).validateActiveParticipant(1L, 10L);
		assertThat(result.meaning()).contains("발화");
		assertThat(result.myImpact()).contains("PM");
		assertThat(result.teamQuestion()).isNotBlank();
	}

	@Test
	void 세그먼트가_없으면_예외를_전파한다() {
		given(contextBuilder.build(any(), any(), any()))
				.willThrow(new GeneralException(AssistantErrorCode.SEGMENT_NOT_FOUND));

		HintService service = new HintService(contextBuilder, aiClient, accessValidator, hintStore);

		assertThatThrownBy(() -> service.generate(10L, 1L, 99L))
				.isInstanceOf(GeneralException.class)
				.extracting("code")
				.isEqualTo(AssistantErrorCode.SEGMENT_NOT_FOUND);
	}

	@Test
	void 현재_회의_참여자가_아니면_힌트_문맥을_조회하지_않는다() {
		doThrow(new GeneralException(MeetingErrorCode.NOT_MEETING_PARTICIPANT))
				.when(accessValidator).validateActiveParticipant(1L, 10L);
		HintService service = new HintService(contextBuilder, aiClient, accessValidator, hintStore);

		assertThatThrownBy(() -> service.generate(10L, 1L, 3L))
				.isInstanceOf(GeneralException.class)
				.extracting("code")
				.isEqualTo(MeetingErrorCode.NOT_MEETING_PARTICIPANT);
		verifyNoInteractions(contextBuilder);
	}

	@Test
	void 생성한_힌트를_저장한다() {
		HintInput input = new HintInput("발화", List.of(), List.of(), "PM", "", List.of("속도 우선"),
				LiveContextSnapshot.empty(), List.of());
		given(contextBuilder.build(eq(10L), eq(1L), eq(3L))).willReturn(input);

		HintService service = new HintService(contextBuilder, aiClient, accessValidator, hintStore);
		HintResult result = service.generate(10L, 1L, 3L);

		verify(hintStore).save(eq(1L), eq(3L), eq(10L), eq(result));
	}

	@Test
	void 저장이_실패해도_힌트는_반환한다() {
		HintInput input = new HintInput("발화", List.of(), List.of(), "PM", "", List.of("속도 우선"),
				LiveContextSnapshot.empty(), List.of());
		given(contextBuilder.build(eq(10L), eq(1L), eq(3L))).willReturn(input);
		doThrow(new IllegalStateException("DB 장애"))
				.when(hintStore).save(any(), any(), any(), any());

		HintService service = new HintService(contextBuilder, aiClient, accessValidator, hintStore);
		HintResult result = service.generate(10L, 1L, 3L);

		assertThat(result.meaning()).contains("발화");
	}

	@Test
	void 내_힌트_목록은_참여자_검증을_거친다() {
		doThrow(new GeneralException(MeetingErrorCode.NOT_MEETING_PARTICIPANT))
				.when(accessValidator).validateActiveParticipant(1L, 10L);
		HintService service = new HintService(contextBuilder, aiClient, accessValidator, hintStore);

		assertThatThrownBy(() -> service.getMyHints(10L, 1L))
				.isInstanceOf(GeneralException.class)
				.extracting("code")
				.isEqualTo(MeetingErrorCode.NOT_MEETING_PARTICIPANT);
		verifyNoInteractions(hintStore);
	}
}
