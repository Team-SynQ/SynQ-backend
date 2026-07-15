package com.synq.backend.domain.ai.rag.controller;

import com.synq.backend.domain.ai.rag.DocumentIndexingService;
import com.synq.backend.domain.ai.rag.port.ReferenceMaterialPort;
import com.synq.backend.global.apipayload.ApiResponse;
import com.synq.backend.global.apipayload.code.GeneralErrorCode;
import com.synq.backend.global.apipayload.code.GeneralSuccessCode;
import com.synq.backend.global.apipayload.exception.GeneralException;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reference-materials")
@RequiredArgsConstructor
public class DocumentReindexController {

	private final ReferenceMaterialPort referenceMaterialPort;
	private final DocumentIndexingService indexingService;

	@Operation(summary = "참고자료 재인덱싱",
			description = "인덱싱이 FAILED 로 끝난 문서를 복구하기 위한 개발자용 엔드포인트. "
					+ "저장된 추출 텍스트로 청킹·임베딩을 다시 실행한다. 원본 파일은 필요하지 않다.")
	@PostMapping("/{id}/reindex")
	public ApiResponse<Void> reindex(@PathVariable Long id) {
		String extractedText = referenceMaterialPort.findExtractedText(id)
				.orElseThrow(() -> new GeneralException(GeneralErrorCode.NOT_FOUND));

		indexingService.index(id, extractedText);
		return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, null);
	}
}
