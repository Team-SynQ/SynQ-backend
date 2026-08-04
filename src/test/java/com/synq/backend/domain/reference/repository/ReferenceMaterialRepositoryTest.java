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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void FILE_LINK_저장_위치_CHECK_제약은_기존_행까지_검증된_상태이다() {
		Boolean validated = jdbcTemplate.queryForObject("""
				SELECT convalidated
				FROM pg_constraint
				WHERE conname = 'chk_reference_material_storage_location'
				""", Boolean.class);

		assertThat(validated).isTrue();
	}

	@Test
	void FILE_메타데이터와_storage_key를_저장한다() {
		User owner = saveUser("reference-file-storage-owner@synq.com");
		Project project = projectRepository.save(Project.of(owner.getUserId(), "SynQ", null));

		ReferenceMaterial saved = referenceMaterialRepository.saveAndFlush(ReferenceMaterial.ofFile(
				project.getId(),
				owner.getUserId(),
				"requirements.pdf",
				1024L,
				"references/" + project.getId() + "/file.pdf",
				ReferenceFileExtension.PDF,
				ReferenceStatus.UPLOADING
		));
		entityManager.clear();

		ReferenceMaterial found = referenceMaterialRepository.findById(saved.getId()).orElseThrow();
		assertThat(found.getStorageKey()).isEqualTo("references/" + project.getId() + "/file.pdf");
		assertThat(found.getUrl()).isNull();
		assertThat(found.getType().name()).isEqualTo("FILE");
	}

	@Test
	void FILE은_storage_key가_없으면_DB_제약으로_저장할_수_없다() {
		User owner = saveUser("reference-file-null-storage@synq.com");
		Project project = projectRepository.save(Project.of(owner.getUserId(), "SynQ", null));

		assertThatThrownBy(() -> jdbcTemplate.update("""
				INSERT INTO reference_material (
				    project_id, uploader_id, type, name, file_size, file_extension, status
				) VALUES (?, ?, 'FILE', 'invalid.pdf', 1024, 'PDF', 'UPLOADING')
				""", project.getId(), owner.getUserId()))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void FILE은_url만으로_DB에_저장할_수_없다() {
		User owner = saveUser("reference-file-url-only@synq.com");
		Project project = projectRepository.save(Project.of(owner.getUserId(), "SynQ", null));

		assertThatThrownBy(() -> jdbcTemplate.update("""
				INSERT INTO reference_material (
				    project_id, uploader_id, type, name, url, file_size, file_extension, status
				) VALUES (?, ?, 'FILE', 'invalid.pdf', 'https://example.com/file.pdf', 1024, 'PDF', 'UPLOADING')
				""", project.getId(), owner.getUserId()))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void LINK는_storage_key가_있으면_DB_제약으로_저장할_수_없다() {
		User owner = saveUser("reference-link-storage@synq.com");
		Project project = projectRepository.save(Project.of(owner.getUserId(), "SynQ", null));

		assertThatThrownBy(() -> jdbcTemplate.update("""
				INSERT INTO reference_material (
				    project_id, uploader_id, type, name, url, storage_key, status
				) VALUES (?, ?, 'LINK', 'invalid link', 'https://example.com', 'references/1/file.pdf', 'UPLOADING')
				""", project.getId(), owner.getUserId()))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void LINK는_url이_있고_storage_key가_없으면_저장할_수_있다() {
		User owner = saveUser("reference-link-valid-storage@synq.com");
		Project project = projectRepository.save(Project.of(owner.getUserId(), "SynQ", null));

		ReferenceMaterial saved = referenceMaterialRepository.saveAndFlush(ReferenceMaterial.ofLink(
				project.getId(),
				owner.getUserId(),
				"example.com",
				"https://example.com",
				ReferenceStatus.UPLOADING
		));

		assertThat(saved.getUrl()).isEqualTo("https://example.com");
		assertThat(saved.getStorageKey()).isNull();
	}

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
						"references/" + project.getId() + "/older.pdf",
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
						"references/" + project.getId() + "/deleted.txt",
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
				"references/" + project.getId() + "/count.pdf",
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
