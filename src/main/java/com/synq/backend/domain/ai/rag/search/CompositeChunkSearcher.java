package com.synq.backend.domain.ai.rag.search;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * 참고자료와 과거 회의 전사를 한 번에 검색한다.
 *
 * <p>@Primary 인 이유는 ChunkSearcher 를 타입으로 주입받는 기존 호출자
 * (AiChatContextBuilder, LocalRagSearchController)가 코드 변경 없이 합류 결과를 받게 하기 위해서다.
 * 참고자료만 필요한 3-hint 는 DocumentChunkSearcher 를 구체 타입으로 주입한다.
 *
 * <p>두 검색기가 각자 질의를 임베딩하므로 임베딩 API 가 2회 호출된다.
 * 지연이 문제가 되면 벡터를 미리 뽑아 넘기는 방식으로 바꾼다.
 */
@Component
@Primary
public class CompositeChunkSearcher implements ChunkSearcher {

	private final ChunkSearcher documentChunkSearcher;
	private final ChunkSearcher meetingTranscriptChunkSearcher;

	// 파라미터를 구체 타입이 아니라 ChunkSearcher 로 받는 이유는 테스트에서 대역을 넘기기 위해서다.
	public CompositeChunkSearcher(
			@Qualifier("documentChunkSearcher") ChunkSearcher documentChunkSearcher,
			@Qualifier("meetingTranscriptChunkSearcher") ChunkSearcher meetingTranscriptChunkSearcher
	) {
		this.documentChunkSearcher = documentChunkSearcher;
		this.meetingTranscriptChunkSearcher = meetingTranscriptChunkSearcher;
	}

	@Override
	public List<ChunkMatch> search(ChunkSearchQuery query) {
		// 각 검색기가 이미 topK 로 잘라 오므로, 합친 뒤 다시 잘라야 전체 기준 상위 K 가 된다.
		return Stream.concat(
						documentChunkSearcher.search(query).stream(),
						meetingTranscriptChunkSearcher.search(query).stream())
				.sorted(Comparator.comparingDouble(ChunkMatch::similarity).reversed())
				.limit(query.topK())
				.toList();
	}
}
