package com.synq.backend.domain.transcript.ws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.synq.backend.domain.auth.jwt.JwtProvider;
import com.synq.backend.domain.meeting.entity.Meeting;
import com.synq.backend.domain.meeting.entity.MeetingParticipant;
import com.synq.backend.domain.meeting.entity.ParticipantRole;
import com.synq.backend.domain.meeting.repository.MeetingParticipantRepository;
import com.synq.backend.domain.meeting.repository.MeetingRepository;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;

class SttHandshakeInterceptorTest {

	private final JwtProvider jwtProvider = mock(JwtProvider.class);
	private final MeetingRepository meetingRepository = mock(MeetingRepository.class);
	private final MeetingParticipantRepository meetingParticipantRepository = mock(MeetingParticipantRepository.class);
	private final SttHandshakeInterceptor interceptor =
			new SttHandshakeInterceptor(jwtProvider, meetingRepository, meetingParticipantRepository);

	@Test
	void 호스트는_핸드셰이크를_통과하고_role_HOST가_저장된다() {
		when(jwtProvider.parseUserId("token")).thenReturn(10L);
		when(meetingRepository.findById(5L)).thenReturn(Optional.of(Meeting.of(1L, "회의")));
		when(meetingParticipantRepository.findByMeetingIdAndUserId(5L, 10L))
				.thenReturn(List.of(MeetingParticipant.of(5L, 10L, ParticipantRole.HOST)));

		Map<String, Object> attributes = new HashMap<>();
		boolean result = interceptor.beforeHandshake(request(5L, "token"), response(), handler(), attributes);

		assertThat(result).isTrue();
		assertThat(attributes.get(SttHandshakeInterceptor.ATTRIBUTE_ROLE)).isEqualTo("HOST");
	}

	@Test
	void 참여자도_핸드셰이크를_통과하고_role_MEMBER가_저장된다() {
		when(jwtProvider.parseUserId("token")).thenReturn(20L);
		when(meetingRepository.findById(5L)).thenReturn(Optional.of(Meeting.of(1L, "회의")));
		when(meetingParticipantRepository.findByMeetingIdAndUserId(5L, 20L))
				.thenReturn(List.of(MeetingParticipant.of(5L, 20L, ParticipantRole.MEMBER)));

		Map<String, Object> attributes = new HashMap<>();
		boolean result = interceptor.beforeHandshake(request(5L, "token"), response(), handler(), attributes);

		assertThat(result).isTrue();
		assertThat(attributes.get(SttHandshakeInterceptor.ATTRIBUTE_ROLE)).isEqualTo("MEMBER");
	}

	@Test
	void 참가자가_아니면_403으로_거부한다() {
		when(jwtProvider.parseUserId("token")).thenReturn(30L);
		when(meetingRepository.findById(5L)).thenReturn(Optional.of(Meeting.of(1L, "회의")));
		when(meetingParticipantRepository.findByMeetingIdAndUserId(5L, 30L)).thenReturn(List.of());

		ServerHttpResponse response = response();
		boolean result = interceptor.beforeHandshake(request(5L, "token"), response, handler(), new HashMap<>());

		assertThat(result).isFalse();
		verify(response).setStatusCode(HttpStatus.FORBIDDEN);
	}

	@Test
	void 나간_참여자는_403으로_거부한다() {
		MeetingParticipant left = MeetingParticipant.of(5L, 40L, ParticipantRole.MEMBER);
		left.leave();
		when(jwtProvider.parseUserId("token")).thenReturn(40L);
		when(meetingRepository.findById(5L)).thenReturn(Optional.of(Meeting.of(1L, "회의")));
		when(meetingParticipantRepository.findByMeetingIdAndUserId(5L, 40L)).thenReturn(List.of(left));

		ServerHttpResponse response = response();
		boolean result = interceptor.beforeHandshake(request(5L, "token"), response, handler(), new HashMap<>());

		assertThat(result).isFalse();
		verify(response).setStatusCode(HttpStatus.FORBIDDEN);
	}

	@Test
	void 진행중이_아닌_회의는_역할과_무관하게_409로_거부한다() {
		Meeting meeting = Meeting.of(1L, "회의");
		meeting.end();
		when(jwtProvider.parseUserId("token")).thenReturn(10L);
		when(meetingRepository.findById(5L)).thenReturn(Optional.of(meeting));

		ServerHttpResponse response = response();
		boolean result = interceptor.beforeHandshake(request(5L, "token"), response, handler(), new HashMap<>());

		assertThat(result).isFalse();
		verify(response).setStatusCode(HttpStatus.CONFLICT);
	}

	@Test
	void 토큰이_없으면_401로_거부한다() {
		ServerHttpResponse response = response();
		boolean result = interceptor.beforeHandshake(request(5L, null), response, handler(), new HashMap<>());

		assertThat(result).isFalse();
		verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void 존재하지_않는_회의면_404로_거부한다() {
		when(jwtProvider.parseUserId("token")).thenReturn(10L);
		when(meetingRepository.findById(5L)).thenReturn(Optional.empty());

		ServerHttpResponse response = response();
		boolean result = interceptor.beforeHandshake(request(5L, "token"), response, handler(), new HashMap<>());

		assertThat(result).isFalse();
		verify(response).setStatusCode(HttpStatus.NOT_FOUND);
	}

	private ServerHttpRequest request(Long meetingId, String token) {
		ServerHttpRequest request = mock(ServerHttpRequest.class);
		String query = token == null ? "" : "?token=" + token;
		when(request.getURI()).thenReturn(URI.create("ws://localhost/ws/meetings/%d/stt%s".formatted(meetingId, query)));
		return request;
	}

	private ServerHttpResponse response() {
		return mock(ServerHttpResponse.class);
	}

	private WebSocketHandler handler() {
		return mock(WebSocketHandler.class);
	}
}
