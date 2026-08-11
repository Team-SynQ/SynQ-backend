package com.synq.backend.domain.project.repository;

import com.synq.backend.domain.project.entity.ProjectMemberPerspective;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectMemberPerspectiveRepository extends JpaRepository<ProjectMemberPerspective, Long> {

	List<ProjectMemberPerspective> findAllByProjectMemberIdOrderByIdAsc(Long projectMemberId);

	void deleteAllByProjectMemberId(Long projectMemberId);
}
