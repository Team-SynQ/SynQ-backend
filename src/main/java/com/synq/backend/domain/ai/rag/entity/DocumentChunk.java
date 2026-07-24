package com.synq.backend.domain.ai.rag.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

@Entity
@Table(name = "document_chunk")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DocumentChunk {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// JPA 연관관계를 맺지 않는다. ai/rag 는 reference 패키지를 모른다.
	@Column(name = "reference_material_id", nullable = false)
	private Long referenceMaterialId;

	// 검색 스코프. reference_material 을 거치면 알 수 있지만 조인 필터링이 HNSW 인덱스 사용을
	// 방해하므로 의도적으로 비정규화했다. 여기서도 연관관계를 맺지 않는다.
	@Column(name = "project_id", nullable = false)
	private Long projectId;

	@Column(name = "chunk_index", nullable = false)
	private int chunkIndex;

	@Column(nullable = false, columnDefinition = "text")
	private String content;

	// hibernate-vector 가 float[] 을 pgvector 의 vector(768) 컬럼에 매핑한다.
	@JdbcTypeCode(SqlTypes.VECTOR)
	@Array(length = 768)
	@Column(nullable = false)
	private float[] embedding;

	@Column(name = "embedding_model", nullable = false, columnDefinition = "text")
	private String embeddingModel;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private OffsetDateTime createdAt;

	private DocumentChunk(Long referenceMaterialId, Long projectId, int chunkIndex, String content,
						  float[] embedding, String embeddingModel) {
		this.referenceMaterialId = referenceMaterialId;
		this.projectId = projectId;
		this.chunkIndex = chunkIndex;
		this.content = content;
		this.embedding = embedding;
		this.embeddingModel = embeddingModel;
	}

	public static DocumentChunk of(Long referenceMaterialId, Long projectId, int chunkIndex, String content,
								   float[] embedding, String embeddingModel) {
		return new DocumentChunk(referenceMaterialId, projectId, chunkIndex, content, embedding, embeddingModel);
	}
}
