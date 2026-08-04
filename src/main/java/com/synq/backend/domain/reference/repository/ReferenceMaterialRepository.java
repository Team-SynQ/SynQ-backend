package com.synq.backend.domain.reference.repository;

import com.synq.backend.domain.reference.entity.ReferenceMaterial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReferenceMaterialRepository extends JpaRepository<ReferenceMaterial, Long> {

	Optional<ReferenceMaterial> findByIdAndProjectId(Long id, Long projectId);

	List<ReferenceMaterial> findAllByProjectIdOrderByCreatedAtDescIdDesc(Long projectId);

	long countByProjectId(Long projectId);
}
