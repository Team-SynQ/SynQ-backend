package com.synq.backend.domain.ai.client;

import java.util.List;

/**
 * 텍스트를 임베딩 벡터로 바꾼다.
 * 인터페이스로 두어 파이프라인 테스트에서 실제 API 호출 없이 대역으로 교체한다.
 */
public interface EmbeddingClient {

	/**
	 * 문서 저장용 임베딩. 반환 리스트는 입력과 같은 순서다.
	 *
	 * @throws EmbeddingException 재시도를 모두 소진했을 때
	 */
	List<float[]> embedDocuments(List<String> texts);

	/** document_chunk.embedding_model 에 기록할 모델명. */
	String modelName();
}
