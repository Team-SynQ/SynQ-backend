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

	@Test
	void 프로젝트의_활성_파일과_링크를_합산하고_삭제_자료와_다른_프로젝트는_제외한다() {
		User owner = saveUser("reference-count-owner@synq.com");
		Project project = projectRepository.save(Project.of(owner.getUserId(), "SynQ", null));
		Project otherProject = projectRepository.save(Project.of(owner.getUserId(), "Other", null));
		referenceMaterialRepository.save(ReferenceMaterial.ofFile(
				project.getId(), owner.getUserId(), "요구사항.pdf", 1024L,
				ReferenceFileExtension.PDF, ReferenceStatus.AVAILABLE));
		referenceMaterialRepository.save(ReferenceMaterial.ofLink(
				project.getId(), owner.getUserId(), "example.com", "https://example.com",
				ReferenceStatus.UPLOADING));
		ReferenceMaterial deleted = referenceMaterialRepository.save(ReferenceMaterial.ofLink(
				project.getId(), owner.getUserId(), "deleted.example.com", "https://deleted.example.com",
				ReferenceStatus.READ_FAILED));
		deleted.softDelete();
		referenceMaterialRepository.save(ReferenceMaterial.ofLink(
				otherProject.getId(), owner.getUserId(), "other.example.com", "https://other.example.com",
				ReferenceStatus.UPLOADING));
		referenceMaterialRepository.flush();
		entityManager.clear();

		assertThat(referenceMaterialRepository.countByProjectId(project.getId())).isEqualTo(2);
	}

	@Test
	void 프로젝트와_참고자료_ID가_일치하는_활성_자료만_조회한다() {
		User owner = saveUser("reference-find-owner@synq.com");
		Project project = projectRepository.save(Project.of(owner.getUserId(), "SynQ", null));
		Project otherProject = projectRepository.save(Project.of(owner.getUserId(), "Other", null));
		ReferenceMaterial active = referenceMaterialRepository.save(ReferenceMaterial.ofLink(
				project.getId(), owner.getUserId(), "active.example.com", "https://active.example.com",
				ReferenceStatus.AVAILABLE));
		ReferenceMaterial deleted = referenceMaterialRepository.save(ReferenceMaterial.ofLink(
				project.getId(), owner.getUserId(), "deleted.example.com", "https://deleted.example.com",
				ReferenceStatus.AVAILABLE));
		deleted.softDelete();
		referenceMaterialRepository.flush();
		entityManager.clear();

		assertThat(referenceMaterialRepository.findByIdAndProjectId(active.getId(), project.getId()))
				.isPresent();
		assertThat(referenceMaterialRepository.findByIdAndProjectId(active.getId(), otherProject.getId()))
				.isEmpty();
		assertThat(referenceMaterialRepository.findByIdAndProjectId(deleted.getId(), project.getId()))
				.isEmpty();
		assertThat(referenceMaterialRepository.findAllByProjectIdOrderByCreatedAtDescIdDesc(project.getId()))
				.extracting(ReferenceMaterial::getId)
				.containsExactly(active.getId());
		assertThat(referenceMaterialRepository.countByProjectId(project.getId())).isEqualTo(1);
	}

	private User saveUser(String email) {
		return userRepository.save(User.ofLocal("테스트", email, "password-hash"));
	}
}
