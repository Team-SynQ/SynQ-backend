package com.synq.backend.domain.user.repository;

import com.synq.backend.domain.user.entity.RoleProfilePerspective;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoleProfilePerspectiveRepository extends JpaRepository<RoleProfilePerspective, Long> {

	List<RoleProfilePerspective> findAllByRoleProfileId(Long roleProfileId);

	void deleteAllByRoleProfileId(Long roleProfileId);
}
