package com.synq.backend.domain.ai.rag.search;

/**
 * 검색 질의 입력이 잘못됐을 때 던진다. 클라이언트 잘못이므로 400 으로 변환할 수 있다.
 *
 * 전용 타입을 두는 이유는, 검색 수행 중 발생한 IllegalArgumentException 까지
 * 400 으로 둔갑시키지 않기 위해서다. 그건 서버 잘못이라 500 으로 드러나야 한다.
 *
 * IllegalArgumentException 을 상속해 값 객체 검증 실패라는 성격은 유지한다.
 */
public class InvalidChunkSearchQueryException extends IllegalArgumentException {

	public InvalidChunkSearchQueryException(String message) {
		super(message);
	}
}
