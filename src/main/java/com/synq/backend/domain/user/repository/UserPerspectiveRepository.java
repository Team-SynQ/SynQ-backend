package com.synq.backend.domain.user.repository;

import com.synq.backend.domain.user.entity.UserPerspective;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserPerspectiveRepository extends JpaRepository<UserPerspective, Long> {

	List<UserPerspective> findByUserId(Long userId);

	void deleteByUserId(Long userId);
}
