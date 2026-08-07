package com.synq.backend.domain.ai.rag.controller;

import com.synq.backend.domain.ai.rag.TranscriptIndexingService;
import com.synq.backend.domain.meeting.entity.Meeting;
import com.synq.backend.domain.meeting.repository.MeetingRepository;
import com.synq.backend.domain.transcript.entity.TranscriptSegment;
import com.synq.backend.domain.transcript.repository.TranscriptSegmentRepository;
import com.synq.backend.global.apipayload.ApiResponse;
import com.synq.backend.global.apipayload.code.GeneralErrorCode;
import com.synq.backend.global.apipayload.code.GeneralSuccessCode;
import com.synq.backend.global.apipayload.exception.GeneralException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/meetings")
@RequiredArgsConstructor
@Tag(name = "RAG", description = "참고자료·회의 전사 AI 인덱싱 API")
@SecurityRequirement(name = "bearerAuth")
public class TranscriptReindexController {

	private final MeetingRepository meetingRepository;
	private final TranscriptSegmentRepository transcriptSegmentRepository;
	private final TranscriptIndexingService indexingService;

	@Operation(summary = "회의 전사 재인덱싱",
			description = "인덱싱이 FAILED 로 끝난 회의를 복구하기 위한 개발자용 엔드포인트. "
					+ "저장된 전사 세그먼트로 청킹·임베딩을 다시 실행한다. "
					+ "회의 종료 후 전사는 수정할 수 없으므로 몇 번을 돌려도 결과가 같다.")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "재인덱싱 성공", useReturnTypeSchema = true),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "로그인 필요"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 회의"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 인덱싱이 진행 중인 회의")
	})
	@PostMapping("/{meetingId}/transcript-reindex")
	public ApiResponse<Void> reindex(@PathVariable Long meetingId) {
		Long projectId = meetingRepository.findById(meetingId)
				.map(Meeting::getProjectId)
				.orElseThrow(() -> new GeneralException(GeneralErrorCode.NOT_FOUND));

		String transcriptText = transcriptSegmentRepository
				.findByMeetingIdOrderByStartMsAscSequenceIndexAsc(meetingId)
				.stream()
				.map(TranscriptSegment::getContent)
				.collect(Collectors.joining("\n"));

		// 동기로 돌린다. 호출자가 사람이므로 결과를 즉시 알려주는 편이 낫다.
		indexingService.index(meetingId, projectId, transcriptText);
		return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, null);
	}
}
