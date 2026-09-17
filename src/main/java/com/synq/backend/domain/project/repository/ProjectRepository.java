package com.synq.backend.domain.project.repository;

import com.synq.backend.domain.project.entity.Project;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

	Optional<Project> findByInviteToken(String inviteToken);

	@Query("SELECT COUNT(project) > 0 FROM Project project "
			+ "WHERE project.ownerId = :ownerId AND project.deletedAt IS NULL")
	boolean existsActiveByOwnerId(@Param("ownerId") Long ownerId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT project FROM Project project WHERE project.id = :projectId")
	Optional<Project> findByIdForUpdate(@Param("projectId") Long projectId);
}
