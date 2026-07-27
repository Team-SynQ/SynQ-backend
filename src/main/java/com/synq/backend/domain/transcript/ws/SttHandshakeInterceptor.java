package com.synq.backend.domain.transcript.ws;

import com.synq.backend.domain.auth.jwt.JwtProvider;
import com.synq.backend.domain.meeting.entity.Meeting;
import com.synq.backend.domain.meeting.entity.MeetingParticipant;
import com.synq.backend.domain.meeting.entity.MeetingStatus;
import com.synq.backend.domain.meeting.entity.ParticipantRole;
import com.synq.backend.domain.meeting.repository.MeetingParticipantRepository;
import com.synq.backend.domain.meeting.repository.MeetingRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * STT WebSocket 핸드셰이크 인증. 연결이 열리기 전에 인증/인가를 끝낸다.
 *
 * <p>브라우저의 {@code new WebSocket(url)} 은 커스텀 헤더를 붙일 수 없어 Authorization 헤더를 쓸 수 없다.
 * JwtAuthenticationFilter 는 헤더만 보므로 WS 는 SecurityContext 가 채워지지 않는다.
 * 그래서 쿼리파라미터 토큰을 여기서 직접 검증하되, 검증 자체는 같은 JwtProvider 를 재사용한다.
 *
 * <p>TODO: 쿼리파라미터는 URL 로그에 토큰이 남는다. 단발성 ws-ticket 발급 방식으로 교체 검토.
 */
@Component
@RequiredArgsConstructor
public class SttHandshakeInterceptor implements HandshakeInterceptor {

	private static final Logger log = LoggerFactory.getLogger(SttHandshakeInterceptor.class);
	private static final Pattern MEETING_ID_PATTERN = Pattern.compile("/ws/meetings/(\\d+)/stt");

	public static final String ATTRIBUTE_MEETING_ID = "meetingId";
	public static final String ATTRIBUTE_USER_ID = "userId";

	private final JwtProvider jwtProvider;
	private final MeetingRepository meetingRepository;
	private final MeetingParticipantRepository meetingParticipantRepository;

	@Override
	public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
								WebSocketHandler wsHandler, Map<String, Object> attributes) {
		Optional<Long> meetingId = extractMeetingId(request.getURI());
		if (meetingId.isEmpty()) {
			return reject(response, HttpStatus.BAD_REQUEST, "meetingId 를 해석할 수 없습니다: " + request.getURI());
		}

		Optional<Long> userId = extractUserId(request.getURI());
		if (userId.isEmpty()) {
			return reject(response, HttpStatus.UNAUTHORIZED, "유효한 access token 이 없습니다. meetingId=" + meetingId.get());
		}

		Optional<Meeting> meeting = meetingRepository.findById(meetingId.get());
		if (meeting.isEmpty()) {
			return reject(response, HttpStatus.NOT_FOUND, "존재하지 않는 회의입니다. meetingId=" + meetingId.get());
		}
		if (meeting.get().getStatus() != MeetingStatus.IN_PROGRESS) {
			return reject(response, HttpStatus.CONFLICT, "진행 중인 회의가 아닙니다. meetingId=" + meetingId.get());
		}
		if (!isHost(meetingId.get(), userId.get())) {
			return reject(response, HttpStatus.FORBIDDEN,
					"호스트가 아닙니다. meetingId=%d userId=%d".formatted(meetingId.get(), userId.get()));
		}

		attributes.put(ATTRIBUTE_MEETING_ID, meetingId.get());
		attributes.put(ATTRIBUTE_USER_ID, userId.get());
		return true;
	}

	@Override
	public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
							WebSocketHandler wsHandler, Exception exception) {
		// 별도 처리 없음
	}

	// 오디오 송신은 호스트만 가능하다. 호스트는 MeetingParticipant.role=HOST 로 판별한다.
	private boolean isHost(Long meetingId, Long userId) {
		return meetingParticipantRepository.findByMeetingIdAndUserId(meetingId, userId).stream()
				.anyMatch(participant -> participant.getRole() == ParticipantRole.HOST
						&& isActive(participant));
	}

	private boolean isActive(MeetingParticipant participant) {
		return participant.getLeftAt() == null;
	}

	private Optional<Long> extractMeetingId(URI uri) {
		Matcher matcher = MEETING_ID_PATTERN.matcher(uri.getPath());
		if (!matcher.find()) {
			return Optional.empty();
		}
		try {
			return Optional.of(Long.valueOf(matcher.group(1)));
		} catch (NumberFormatException e) {
			return Optional.empty();
		}
	}

	private Optional<Long> extractUserId(URI uri) {
		String token = UriComponentsBuilder.fromUri(uri).build().getQueryParams().getFirst("token");
		if (token == null || token.isBlank()) {
			return Optional.empty();
		}
		try {
			return Optional.of(jwtProvider.parseUserId(token));
		} catch (RuntimeException e) {
			log.debug("STT 핸드셰이크 토큰 검증 실패: {}", e.getMessage());
			return Optional.empty();
		}
	}

	private boolean reject(ServerHttpResponse response, HttpStatus status, String detail) {
		log.debug("STT 핸드셰이크를 거부했습니다. status={} {}", status, detail);
		response.setStatusCode(status);
		return false;
	}
}
