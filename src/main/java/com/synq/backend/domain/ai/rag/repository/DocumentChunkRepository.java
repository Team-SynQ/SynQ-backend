package com.synq.backend.domain.ai.rag.repository;

import com.synq.backend.domain.ai.rag.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {

	List<DocumentChunk> findByReferenceMaterialIdOrderByChunkIndexAsc(Long referenceMaterialId);

	// 재처리 시 기존 청크를 전부 지우고 다시 만든다(all-or-nothing).
	void deleteByReferenceMaterialId(Long referenceMaterialId);
}
