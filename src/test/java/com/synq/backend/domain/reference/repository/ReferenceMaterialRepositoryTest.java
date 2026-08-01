package com.synq.backend.domain.reference.repository;

import com.synq.backend.domain.project.entity.Project;
import com.synq.backend.domain.project.repository.ProjectRepository;
import com.synq.backend.domain.reference.entity.ReferenceFileExtension;
import com.synq.backend.domain.reference.entity.ReferenceMaterial;
import com.synq.backend.domain.reference.entity.ReferenceStatus;
import com.synq.backend.domain.user.entity.User;
import com.synq.backend.domain.user.repository.UserRepository;
import com.synq.backend.support.PostgresTestContainer;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class ReferenceMaterialRepositoryTest extends PostgresTestContainer {

	@Autowired
	private ReferenceMaterialRepository referenceMaterialRepository;

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void 삭제되지_않은_참고자료를_최신_등록순으로_조회한다() {
		User owner = saveUser("reference-repository-owner@synq.com");
		Project project = projectRepository.save(Project.of(owner.getUserId(), "SynQ", null));
		ReferenceMaterial olderReference = referenceMaterialRepository.save(
				ReferenceMaterial.ofFile(
						project.getId(),
						owner.getUserId(),
						"요구사항.pdf",
						1024L,
						ReferenceFileExtension.PDF,
						ReferenceStatus.AVAILABLE
				));
		ReferenceMaterial newerReference = referenceMaterialRepository.save(
				ReferenceMaterial.ofLink(
						project.getId(),
						owner.getUserId(),
						"기획 문서",
						"https://www.notion.so/example",
						ReferenceStatus.READ_FAILED
				));
		ReferenceMaterial deletedReference = referenceMaterialRepository.save(
				ReferenceMaterial.ofFile(
						project.getId(),
						owner.getUserId(),
						"삭제 자료.txt",
						512L,
						ReferenceFileExtension.TXT,
						ReferenceStatus.AVAILABLE
				));
		deletedReference.softDelete();
		referenceMaterialRepository.flush();
		entityManager.clear();

		List<ReferenceMaterial> references =
				referenceMaterialRepository.findAllByProjectIdOrderByCreatedAtDescIdDesc(project.getId());

		assertThat(references)
				.extracting(ReferenceMaterial::getId)
				.containsExactly(newerReference.getId(), olderReference.getId())
				.doesNotContain(deletedReference.getId());
	}

	private User saveUser(String email) {
		return userRepository.save(User.ofLocal("테스트", email, "password-hash"));
	}
}
