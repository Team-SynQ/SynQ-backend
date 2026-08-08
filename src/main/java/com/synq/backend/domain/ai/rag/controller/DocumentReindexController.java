package com.synq.backend.domain.ai.rag.controller;

import com.synq.backend.domain.ai.rag.DocumentIndexingService;
import com.synq.backend.domain.ai.rag.port.ReferenceMaterialPort;
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

@RestController
@RequestMapping("/reference-materials")
@RequiredArgsConstructor
@Tag(name = "RAG", description = "참고자료·회의 전사 AI 인덱싱 API")
@SecurityRequirement(name = "bearerAuth")
public class DocumentReindexController {

	private final ReferenceMaterialPort referenceMaterialPort;
	private final DocumentIndexingService indexingService;

	@Operation(summary = "참고자료 재인덱싱",
			description = "인덱싱이 실패로 끝난 참고자료를 복구하기 위한 개발자용 엔드포인트. "
					+ "링크는 URL 을 다시 읽고, 파일은 S3 원본을 받아 다시 파싱한 뒤 청킹·임베딩을 재실행한다. "
					+ "파일 재파싱은 동기로 수행되어 큰 파일은 응답이 수 초 걸릴 수 있다.")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "참고자료 재인덱싱 요청 성공", useReturnTypeSchema = true),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "로그인 필요"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 참고자료 또는 추출 텍스트 없음")
	})
	@PostMapping("/{id}/reindex")
	public ApiResponse<Void> reindex(@PathVariable Long id) {
		String extractedText = referenceMaterialPort.findIndexableText(id)
				.orElseThrow(() -> new GeneralException(GeneralErrorCode.NOT_FOUND));
		// 검색 스코프. 실패로 청크가 지워진 상태에서도 복구할 수 있어야 하므로
		// 기존 청크에서 읽지 않고 참고자료 도메인에 묻는다.
		Long projectId = referenceMaterialPort.findProjectId(id)
				.orElseThrow(() -> new GeneralException(GeneralErrorCode.NOT_FOUND));

		indexingService.index(id, projectId, extractedText);
		return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, null);
	}
}
