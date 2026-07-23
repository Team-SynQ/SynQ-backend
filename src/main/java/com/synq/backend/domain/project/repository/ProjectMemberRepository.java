package com.synq.backend.domain.project.repository;

import com.synq.backend.domain.project.entity.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

	Optional<ProjectMember> findByProjectIdAndUserId(Long projectId, Long userId);

	long countByUserId(Long userId);
}
