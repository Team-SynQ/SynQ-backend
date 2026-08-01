package com.synq.backend.domain.reference.repository;

import com.synq.backend.domain.reference.entity.ReferenceMaterial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReferenceMaterialRepository extends JpaRepository<ReferenceMaterial, Long> {

	List<ReferenceMaterial> findAllByProjectIdOrderByCreatedAtDescIdDesc(Long projectId);
}
