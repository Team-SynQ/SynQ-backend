package com.synq.backend.domain.ai.rag.controller;

import com.synq.backend.domain.ai.rag.DocumentIndexingService;
import com.synq.backend.domain.ai.rag.port.ReferenceMaterialPort;
import com.synq.backend.global.apipayload.ApiResponse;
import com.synq.backend.global.apipayload.code.GeneralErrorCode;
import com.synq.backend.global.apipayload.code.GeneralSuccessCode;
import com.synq.backend.global.apipayload.exception.GeneralException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reference-materials")
@RequiredArgsConstructor
@Tag(name = "RAG", description = "참고자료 AI 인덱싱 API")
public class DocumentReindexController {

	private final ReferenceMaterialPort referenceMaterialPort;
	private final DocumentIndexingService indexingService;

	@Operation(summary = "참고자료 재인덱싱",
			description = "인덱싱이 FAILED 로 끝난 문서를 복구하기 위한 개발자용 엔드포인트. "
					+ "저장된 추출 텍스트로 청킹·임베딩을 다시 실행한다. 원본 파일은 필요하지 않다.")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "참고자료 재인덱싱 요청 성공", useReturnTypeSchema = true),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 참고자료 또는 추출 텍스트 없음")
	})
	@PostMapping("/{id}/reindex")
	public ApiResponse<Void> reindex(@PathVariable Long id) {
		String extractedText = referenceMaterialPort.findExtractedText(id)
				.orElseThrow(() -> new GeneralException(GeneralErrorCode.NOT_FOUND));
		// 검색 스코프. 실패로 청크가 지워진 상태에서도 복구할 수 있어야 하므로
		// 기존 청크에서 읽지 않고 참고자료 도메인에 묻는다.
		Long projectId = referenceMaterialPort.findProjectId(id)
				.orElseThrow(() -> new GeneralException(GeneralErrorCode.NOT_FOUND));

		indexingService.index(id, projectId, extractedText);
		return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, null);
	}
}
