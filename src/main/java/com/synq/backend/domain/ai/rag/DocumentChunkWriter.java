package com.synq.backend.domain.ai.rag;

import com.synq.backend.domain.ai.rag.entity.DocumentChunk;
import com.synq.backend.domain.ai.rag.repository.DocumentChunkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DocumentChunkWriter {

	private final DocumentChunkRepository repository;

	/** 기존 청크를 지우고 새 청크로 교체한다. 한 트랜잭션이라 중간 실패 시 전부 롤백된다. */
	@Transactional
	public void replace(Long referenceMaterialId, List<DocumentChunk> chunks) {
		repository.deleteByReferenceMaterialId(referenceMaterialId);
		repository.flush();
		repository.saveAll(chunks);
	}

	/** 실패 처리 시 잔여 청크 제거. */
	@Transactional
	public void deleteAll(Long referenceMaterialId) {
		repository.deleteByReferenceMaterialId(referenceMaterialId);
	}
}
