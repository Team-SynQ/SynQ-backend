package com.synq.backend.domain.ai.assistant.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.synq.backend.domain.ai.assistant.repository.AiChatMessageRepository;
import com.synq.backend.domain.ai.assistant.domain.AiChatStatus;
import com.synq.backend.domain.ai.assistant.domain.AiChatPrompt;
import com.synq.backend.domain.ai.assistant.mock.FakeAiChatClient;
import com.synq.backend.domain.ai.rag.search.ChunkSearcher;
import com.synq.backend.domain.auth.jwt.AccessTokenBlacklistService;
import com.synq.backend.domain.auth.jwt.JwtProvider;
import com.synq.backend.domain.meeting.entity.Meeting;
import com.synq.backend.domain.meeting.entity.MeetingParticipant;
import com.synq.backend.domain.meeting.entity.ParticipantRole;
import com.synq.backend.domain.meeting.repository.MeetingParticipantRepository;
import com.synq.backend.domain.meeting.repository.MeetingRepository;
import com.synq.backend.domain.transcript.entity.TranscriptSegment;
import com.synq.backend.domain.transcript.repository.TranscriptSegmentRepository;
import com.synq.backend.domain.user.entity.User;
import com.synq.backend.domain.user.repository.UserRepository;
import com.synq.backend.support.PostgresTestContainer;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
class AiChatControllerTest extends PostgresTestContainer {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private MeetingRepository meetingRepository;

	@Autowired
	private MeetingParticipantRepository meetingParticipantRepository;

	@Autowired
	private AiChatMessageRepository aiChatMessageRepository;

	@Autowired
	private TranscriptSegmentRepository transcriptSegmentRepository;

	@Autowired
	private JwtProvider jwtProvider;

	@MockitoBean
	private AccessTokenBlacklistService accessTokenBlacklistService;

	@MockitoSpyBean
	private FakeAiChatClient fakeAiChatClient;

	@MockitoSpyBean
	private ChunkSearcher chunkSearcher;

	@BeforeEach
	void cleanUp() {
		reset(fakeAiChatClient);
		reset(chunkSearcher);
		aiChatMessageRepository.deleteAll();
		transcriptSegmentRepository.deleteAll();
		meetingParticipantRepository.deleteAll();
		meetingRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	void 회의_참여자가_질문하면_Fake_AI_답변과_출처를_저장한다() throws Exception {
		User user = saveUser("채팅 사용자", "chat-user@synq.com");
		Meeting meeting = saveMeetingWithParticipant(user.getUserId());
		UUID clientRequestId = UUID.randomUUID();

		mockMvc.perform(post("/meetings/{meetingId}/chat-messages", meeting.getId())
						.header(HttpHeaders.AUTHORIZATION, bearer(user.getUserId()))
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody("일정의 위험 요소를 알려줘", null, clientRequestId)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.result.status").value("COMPLETED"))
				.andExpect(jsonPath("$.result.question").value("일정의 위험 요소를 알려줘"))
				.andExpect(jsonPath("$.result.answer").value(
						"현재는 기본 AI Chat 연결을 검증하는 응답입니다. 질문: 일정의 위험 요소를 알려줘"))
				.andExpect(jsonPath("$.result.sources.length()").value(0))
				.andExpect(jsonPath("$.result.suggestedQuestions.length()").value(2));

		assertThat(aiChatMessageRepository.count()).isEqualTo(1);
	}

	@Test
	void 같은_clientRequestId를_재전송하면_기존_응답을_반환하고_중복_저장하지_않는다() throws Exception {
		User user = saveUser("중복 사용자", "duplicate-chat@synq.com");
		Meeting meeting = saveMeetingWithParticipant(user.getUserId());
		UUID clientRequestId = UUID.randomUUID();
		String body = requestBody("결정 사항을 알려줘", null, clientRequestId);

		MvcResult first = mockMvc.perform(post("/meetings/{meetingId}/chat-messages", meeting.getId())
						.header(HttpHeaders.AUTHORIZATION, bearer(user.getUserId()))
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isCreated())
				.andReturn();

		Long firstMessageId = ((Number) JsonPath.read(
				first.getResponse().getContentAsString(),
				"$.result.id"
		)).longValue();

		mockMvc.perform(post("/meetings/{meetingId}/chat-messages", meeting.getId())
						.header(HttpHeaders.AUTHORIZATION, bearer(user.getUserId()))
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.result.id").value(firstMessageId));

		assertThat(aiChatMessageRepository.count()).isEqualTo(1);
	}

	@Test
	void 같은_clientRequestId를_다른_질문에_사용하면_409를_반환한다() throws Exception {
		User user = saveUser("키 충돌 사용자", "idempotency-conflict@synq.com");
		Meeting meeting = saveMeetingWithParticipant(user.getUserId());
		UUID clientRequestId = UUID.randomUUID();

		mockMvc.perform(post("/meetings/{meetingId}/chat-messages", meeting.getId())
						.header(HttpHeaders.AUTHORIZATION, bearer(user.getUserId()))
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody("첫 번째 질문", null, clientRequestId)))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/meetings/{meetingId}/chat-messages", meeting.getId())
						.header(HttpHeaders.AUTHORIZATION, bearer(user.getUserId()))
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody("완전히 다른 질문", null, clientRequestId)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("AI_CHAT409_2"));

		assertThat(aiChatMessageRepository.count()).isEqualTo(1);
	}

	@Test
	void 채팅_내역은_요청한_사용자의_메시지만_최신순으로_조회한다() throws Exception {
		User firstUser = saveUser("첫 사용자", "first-chat@synq.com");
		User secondUser = saveUser("둘 사용자", "second-chat@synq.com");
		Meeting meeting = saveMeetingWithParticipant(firstUser.getUserId());
		meetingParticipantRepository.save(
				MeetingParticipant.of(meeting.getId(), secondUser.getUserId(), ParticipantRole.MEMBER)
		);

		send(meeting.getId(), firstUser.getUserId(), "첫 번째 사용자 질문");
		send(meeting.getId(), secondUser.getUserId(), "다른 사용자 질문");
		send(meeting.getId(), firstUser.getUserId(), "최근 사용자 질문");

		mockMvc.perform(get("/meetings/{meetingId}/chat-messages", meeting.getId())
						.header(HttpHeaders.AUTHORIZATION, bearer(firstUser.getUserId()))
						.param("page", "0")
						.param("size", "20"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.result.messages.length()").value(2))
				.andExpect(jsonPath("$.result.messages[0].question").value("최근 사용자 질문"))
				.andExpect(jsonPath("$.result.messages[1].question").value("첫 번째 사용자 질문"))
				.andExpect(jsonPath("$.result.page").value(0))
				.andExpect(jsonPath("$.result.hasNext").value(false));
	}

	@Test
	void 회의_비참여자는_AI_채팅을_이용할_수_없다() throws Exception {
		User participant = saveUser("참여 사용자", "participant-chat@synq.com");
		User outsider = saveUser("외부 사용자", "outsider-chat@synq.com");
		Meeting meeting = saveMeetingWithParticipant(participant.getUserId());

		mockMvc.perform(post("/meetings/{meetingId}/chat-messages", meeting.getId())
						.header(HttpHeaders.AUTHORIZATION, bearer(outsider.getUserId()))
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody("회의 내용을 알려줘", null, UUID.randomUUID())))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("AI_CHAT403_1"));
	}

	@Test
	void 빈_질문은_400을_반환한다() throws Exception {
		User user = saveUser("검증 사용자", "validation-chat@synq.com");
		Meeting meeting = saveMeetingWithParticipant(user.getUserId());

		mockMvc.perform(post("/meetings/{meetingId}/chat-messages", meeting.getId())
						.header(HttpHeaders.AUTHORIZATION, bearer(user.getUserId()))
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody(" ", null, UUID.randomUUID())))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON400_1"));
	}

	@Test
	void 인증_토큰이_없으면_401을_반환한다() throws Exception {
		User user = saveUser("미인증 사용자", "unauthorized-chat@synq.com");
		Meeting meeting = saveMeetingWithParticipant(user.getUserId());

		mockMvc.perform(get("/meetings/{meetingId}/chat-messages", meeting.getId()))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH401_5"));
	}

	@Test
	void X_User_Id를_조작해도_JWT_사용자의_채팅만_조회한다() throws Exception {
		User firstUser = saveUser("JWT 사용자", "jwt-chat@synq.com");
		User secondUser = saveUser("다른 사용자", "spoofed-chat@synq.com");
		Meeting meeting = saveMeetingWithParticipant(firstUser.getUserId());
		meetingParticipantRepository.save(
				MeetingParticipant.of(meeting.getId(), secondUser.getUserId(), ParticipantRole.MEMBER)
		);
		send(meeting.getId(), firstUser.getUserId(), "JWT 사용자의 비공개 질문");

		mockMvc.perform(get("/meetings/{meetingId}/chat-messages", meeting.getId())
						.header(HttpHeaders.AUTHORIZATION, bearer(secondUser.getUserId()))
						.header("X-User-Id", firstUser.getUserId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.result.messages.length()").value(0));
	}

	@Test
	void 잘못된_페이지_크기는_400을_반환한다() throws Exception {
		User user = saveUser("페이지 사용자", "page-chat@synq.com");
		Meeting meeting = saveMeetingWithParticipant(user.getUserId());

		mockMvc.perform(get("/meetings/{meetingId}/chat-messages", meeting.getId())
						.header(HttpHeaders.AUTHORIZATION, bearer(user.getUserId()))
						.param("size", "0"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON400_1"));
	}

	@Test
	void 종료된_회의에는_새_질문을_보낼_수_없다() throws Exception {
		User user = saveUser("종료 회의 사용자", "ended-chat@synq.com");
		Meeting meeting = saveMeetingWithParticipant(user.getUserId());
		meeting.end();
		meetingRepository.save(meeting);

		mockMvc.perform(post("/meetings/{meetingId}/chat-messages", meeting.getId())
						.header(HttpHeaders.AUTHORIZATION, bearer(user.getUserId()))
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody("종료 후 질문", null, UUID.randomUUID())))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("AI_CHAT409_1"));
	}

	@Test
	void 검증할_수_없는_선택_발화_ID는_422를_반환한다() throws Exception {
		User user = saveUser("발화 사용자", "segment-chat@synq.com");
		Meeting meeting = saveMeetingWithParticipant(user.getUserId());

		mockMvc.perform(post("/meetings/{meetingId}/chat-messages", meeting.getId())
						.header(HttpHeaders.AUTHORIZATION, bearer(user.getUserId()))
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody("선택 발화를 설명해줘", 999L, UUID.randomUUID())))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.code").value("AI_CHAT422_1"));

		assertThat(aiChatMessageRepository.count()).isZero();
	}

	@Test
	void 선택_발화를_기반으로_질문할_수_있다() throws Exception {
		User user = saveUser("선택 발화 사용자", "selected-segment-chat@synq.com");
		Meeting meeting = saveMeetingWithParticipant(user.getUserId());
		TranscriptSegment segment = transcriptSegmentRepository.save(
				TranscriptSegment.of(meeting.getId(), 0, 0, 1000, "이번 주에는 온보딩 개선을 먼저 진행합니다.")
		);

		mockMvc.perform(post("/meetings/{meetingId}/chat-messages", meeting.getId())
						.header(HttpHeaders.AUTHORIZATION, bearer(user.getUserId()))
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody("이 발화의 핵심이 뭐야?", segment.getId(), UUID.randomUUID())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.result.linkedSegmentId").value(segment.getId()))
				.andExpect(jsonPath("$.result.status").value("COMPLETED"));

		ArgumentCaptor<AiChatPrompt> promptCaptor = ArgumentCaptor.forClass(AiChatPrompt.class);
		verify(fakeAiChatClient).generate(promptCaptor.capture());
		assertThat(promptCaptor.getValue().context().transcripts())
				.extracting(value -> value.id())
				.containsExactly(segment.getId());
	}

	@Test
	void 일반_질문에는_최근_발화_최대_12개만_전달한다() throws Exception {
		User user = saveUser("최근 전사 사용자", "recent-transcript-chat@synq.com");
		Meeting meeting = saveMeetingWithParticipant(user.getUserId());
		for (int index = 0; index < 13; index++) {
			transcriptSegmentRepository.save(
					TranscriptSegment.of(meeting.getId(), index, index * 1000, index * 1000 + 500, "발화 " + index)
			);
		}

		mockMvc.perform(post("/meetings/{meetingId}/chat-messages", meeting.getId())
						.header(HttpHeaders.AUTHORIZATION, bearer(user.getUserId()))
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody("최근 논의는 무엇인가요?", null, UUID.randomUUID())))
				.andExpect(status().isCreated());

		ArgumentCaptor<AiChatPrompt> promptCaptor = ArgumentCaptor.forClass(AiChatPrompt.class);
		verify(fakeAiChatClient).generate(promptCaptor.capture());
		assertThat(promptCaptor.getValue().context().transcripts())
				.hasSize(12)
				.extracting(value -> value.content())
				.containsExactly("발화 1", "발화 2", "발화 3", "발화 4", "발화 5", "발화 6",
						"발화 7", "발화 8", "발화 9", "발화 10", "발화 11", "발화 12");
	}

	@Test
	void RAG_맥락_구성에_실패하면_GENERATING_메시지를_남기지_않는다() throws Exception {
		User user = saveUser("RAG 실패 사용자", "rag-failed-chat@synq.com");
		Meeting meeting = saveMeetingWithParticipant(user.getUserId());
		doThrow(new IllegalStateException("임베딩 서비스 장애"))
				.when(chunkSearcher)
				.search(any());

		mockMvc.perform(post("/meetings/{meetingId}/chat-messages", meeting.getId())
						.header(HttpHeaders.AUTHORIZATION, bearer(user.getUserId()))
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody("RAG 검색 실패 질문", null, UUID.randomUUID())))
				.andExpect(status().isBadGateway())
				.andExpect(jsonPath("$.code").value("AI_CHAT502_1"));

		assertThat(aiChatMessageRepository.count()).isZero();
	}

	@Test
	void 진행_중인_회의_참여자는_초기_안내와_추천_질문을_조회할_수_있다() throws Exception {
		User user = saveUser("추천 질문 사용자", "chat-welcome@synq.com");
		Meeting meeting = saveMeetingWithParticipant(user.getUserId());

		mockMvc.perform(get("/meetings/{meetingId}/chat-messages/suggestions", meeting.getId())
						.header(HttpHeaders.AUTHORIZATION, bearer(user.getUserId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.result.welcomeMessage").isNotEmpty())
				.andExpect(jsonPath("$.result.suggestedQuestions.length()").value(2));
	}

	@Test
	void 퇴장한_참여자는_새_질문을_보낼_수_없다() throws Exception {
		User user = saveUser("퇴장 사용자", "left-chat@synq.com");
		Meeting meeting = saveMeetingWithParticipant(user.getUserId());
		MeetingParticipant participant = meetingParticipantRepository
				.findByMeetingIdAndUserId(meeting.getId(), user.getUserId())
				.get(0);
		participant.leave();
		meetingParticipantRepository.save(participant);

		mockMvc.perform(post("/meetings/{meetingId}/chat-messages", meeting.getId())
						.header(HttpHeaders.AUTHORIZATION, bearer(user.getUserId()))
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody("퇴장 후 질문", null, UUID.randomUUID())))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("AI_CHAT403_1"));
	}

	@Test
	void AI_생성에_실패하면_FAILED로_저장하고_502를_반환한다() throws Exception {
		User user = saveUser("실패 사용자", "failed-chat@synq.com");
		Meeting meeting = saveMeetingWithParticipant(user.getUserId());
		UUID clientRequestId = UUID.randomUUID();
		doThrow(new IllegalStateException("외부 AI 장애"))
				.when(fakeAiChatClient)
				.generate(any());

		mockMvc.perform(post("/meetings/{meetingId}/chat-messages", meeting.getId())
						.header(HttpHeaders.AUTHORIZATION, bearer(user.getUserId()))
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody("실패할 질문", null, clientRequestId)))
				.andExpect(status().isBadGateway())
				.andExpect(jsonPath("$.code").value("AI_CHAT502_1"));

		var failed = aiChatMessageRepository
				.findByMeetingIdAndUserIdAndClientRequestId(
						meeting.getId(),
						user.getUserId(),
						clientRequestId
				)
				.orElseThrow();
		assertThat(failed.getStatus()).isEqualTo(AiChatStatus.FAILED);
		assertThat(failed.getErrorCode()).isEqualTo("AI_CHAT502_1");
		assertThat(failed.getErrorMessage()).doesNotContain("외부 AI 장애");
	}

	@Test
	void 실패한_요청을_같은_clientRequestId로_재전송해도_502를_반환한다() throws Exception {
		User user = saveUser("재시도 실패 사용자", "failed-retry-chat@synq.com");
		Meeting meeting = saveMeetingWithParticipant(user.getUserId());
		UUID clientRequestId = UUID.randomUUID();
		doThrow(new IllegalStateException("외부 AI 장애"))
				.when(fakeAiChatClient)
				.generate(any());

		String body = requestBody("실패한 질문", null, clientRequestId);
		mockMvc.perform(post("/meetings/{meetingId}/chat-messages", meeting.getId())
						.header(HttpHeaders.AUTHORIZATION, bearer(user.getUserId()))
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadGateway())
				.andExpect(jsonPath("$.code").value("AI_CHAT502_1"));

		mockMvc.perform(post("/meetings/{meetingId}/chat-messages", meeting.getId())
						.header(HttpHeaders.AUTHORIZATION, bearer(user.getUserId()))
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadGateway())
				.andExpect(jsonPath("$.code").value("AI_CHAT502_1"));

		assertThat(aiChatMessageRepository.count()).isEqualTo(1);
	}

	private User saveUser(String name, String email) {
		return userRepository.save(User.ofLocal(name, email, "encoded-password"));
	}

	private Meeting saveMeetingWithParticipant(Long userId) {
		Meeting meeting = meetingRepository.save(Meeting.of(1L, "AI 채팅 테스트 회의"));
		meetingParticipantRepository.save(
				MeetingParticipant.of(meeting.getId(), userId, ParticipantRole.HOST)
		);
		return meeting;
	}

	private void send(Long meetingId, Long userId, String question) throws Exception {
		mockMvc.perform(post("/meetings/{meetingId}/chat-messages", meetingId)
						.header(HttpHeaders.AUTHORIZATION, bearer(userId))
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody(question, null, UUID.randomUUID())))
				.andExpect(status().isCreated());
	}

	private String bearer(Long userId) {
		return "Bearer " + jwtProvider.createAccessToken(userId);
	}

	private String requestBody(String question, Long linkedSegmentId, UUID clientRequestId) {
		String linkedSegment = linkedSegmentId == null ? "null" : linkedSegmentId.toString();
		return """
				{
				  "question": "%s",
				  "linkedSegmentId": %s,
				  "clientRequestId": "%s"
				}
				""".formatted(question, linkedSegment, clientRequestId);
	}
}
