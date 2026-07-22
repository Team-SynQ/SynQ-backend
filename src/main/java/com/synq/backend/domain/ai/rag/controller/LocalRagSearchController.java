package com.synq.backend.domain.ai.rag.controller;

import com.synq.backend.domain.ai.rag.search.ChunkMatch;
import com.synq.backend.domain.ai.rag.search.ChunkSearchProperties;
import com.synq.backend.domain.ai.rag.search.ChunkSearchQuery;
import com.synq.backend.domain.ai.rag.search.ChunkSearcher;
import com.synq.backend.global.apipayload.ApiResponse;
import com.synq.backend.global.apipayload.code.GeneralErrorCode;
import com.synq.backend.global.apipayload.code.GeneralSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 검색 품질 확인용 개발자 도구다. 프론트가 호출하는 API 가 아니다.
 *
 * <p>3-hint / AI Chat 등 실제 호출자가 아직 없어서, 임계값과 topK 를 실제 문서로
 * 돌려볼 방법이 달리 없다. 프로젝트 문서 내용이 나가므로 local 프로파일에서만
 * 빈이 생성되게 한다.
 */
@RestController
@RequestMapping("/local/rag")
@RequiredArgsConstructor
@Profile("local")
public class LocalRagSearchController {

	private final ChunkSearcher chunkSearcher;
	private final ChunkSearchProperties properties;

	@Operation(summary = "[local 전용] RAG 청크 검색",
			description = "임계값과 topK 를 바꿔가며 검색 품질을 확인하기 위한 개발자용 엔드포인트.")
	@GetMapping("/search")
	public ApiResponse<List<ChunkMatch>> search(
			@RequestParam Long projectId,
			@RequestParam String q,
			@RequestParam(required = false) Integer topK,
			@RequestParam(required = false) Double minSimilarity) {

		ChunkSearchQuery query = new ChunkSearchQuery(
				projectId,
				q,
				topK != null ? topK : properties.topK(),
				minSimilarity != null ? minSimilarity : properties.minSimilarity());

		return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, chunkSearcher.search(query));
	}

	/**
	 * ChunkSearchQuery 의 입력 검증 실패는 이 엔드포인트에서만 클라이언트 잘못이다.
	 * 전역 핸들러에 두면 다른 도메인의 진짜 버그까지 400 으로 둔갑해 묻힌다.
	 */
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiResponse<Void>> handleInvalidQuery(IllegalArgumentException e) {
		return ResponseEntity.status(GeneralErrorCode.BAD_REQUEST.getStatus())
				.body(new ApiResponse<>(false, GeneralErrorCode.BAD_REQUEST.getCode(), e.getMessage(), null));
	}
}
