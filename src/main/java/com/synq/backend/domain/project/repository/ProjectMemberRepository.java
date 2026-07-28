package com.synq.backend.domain.project.repository;

import com.synq.backend.domain.project.entity.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

	@Query("""
			SELECT member
			FROM ProjectMember member
			JOIN Project project ON project.id = member.projectId
			WHERE member.projectId = :projectId
			  AND member.userId = :userId
			  AND project.deletedAt IS NULL
			""")
	Optional<ProjectMember> findByProjectIdAndUserId(
			@Param("projectId") Long projectId,
			@Param("userId") Long userId
	);

	@Query("""
			SELECT COUNT(member) > 0
			FROM ProjectMember member
			JOIN Project project ON project.id = member.projectId
			WHERE member.projectId = :projectId
			  AND member.userId = :userId
			  AND project.deletedAt IS NULL
			""")
	boolean existsByProjectIdAndUserId(
			@Param("projectId") Long projectId,
			@Param("userId") Long userId
	);

	@Query("""
			SELECT member
			FROM ProjectMember member
			JOIN Project project ON project.id = member.projectId
			WHERE member.userId = :userId
			  AND project.deletedAt IS NULL
	""")
	List<ProjectMember> findAllByUserId(@Param("userId") Long userId);

	List<ProjectMember> findAllByProjectIdOrderByJoinedAtAsc(Long projectId);

	@Query("""
			SELECT COUNT(member)
			FROM ProjectMember member
			JOIN Project project ON project.id = member.projectId
			WHERE member.projectId = :projectId
			  AND project.deletedAt IS NULL
			""")
	long countByProjectId(@Param("projectId") Long projectId);

	@Query("""
			SELECT COUNT(member)
			FROM ProjectMember member
			JOIN Project project ON project.id = member.projectId
			WHERE member.userId = :userId
			  AND project.deletedAt IS NULL
			""")
	long countByUserId(@Param("userId") Long userId);
}
