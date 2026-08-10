package com.synq.backend.domain.transcript.controller;

import com.synq.backend.domain.transcript.dto.TranscriptSegmentListResponse;
import com.synq.backend.domain.transcript.dto.TranscriptSegmentUpdateRequest;
import com.synq.backend.domain.transcript.dto.TranscriptSegmentUpdateResponse;
import com.synq.backend.domain.transcript.entity.TranscriptSegment;
import com.synq.backend.domain.transcript.service.TranscriptSegmentService;
import com.synq.backend.global.apipayload.ApiResponse;
import com.synq.backend.global.apipayload.code.GeneralSuccessCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/meetings/{meetingId}")
@RequiredArgsConstructor
public class TranscriptSegmentController implements TranscriptSegmentControllerDocs {

	private final TranscriptSegmentService transcriptSegmentService;

	@Override
	public ResponseEntity<ApiResponse<TranscriptSegmentUpdateResponse>> update(
			Long meetingId, Long segmentId, Long userId, TranscriptSegmentUpdateRequest request) {
		TranscriptSegment segment = transcriptSegmentService.update(meetingId, segmentId, userId, request.content());
		return ResponseEntity.ok(
				ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, TranscriptSegmentUpdateResponse.from(segment)));
	}

	@Override
	public ResponseEntity<ApiResponse<TranscriptSegmentListResponse>> getSegments(
			Long meetingId, Long userId, Integer afterSequenceIndex) {
		List<TranscriptSegment> segments = transcriptSegmentService.getSegments(meetingId, userId, afterSequenceIndex);
		return ResponseEntity.ok(
				ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK,
						TranscriptSegmentListResponse.from(meetingId, segments)));
	}
}
