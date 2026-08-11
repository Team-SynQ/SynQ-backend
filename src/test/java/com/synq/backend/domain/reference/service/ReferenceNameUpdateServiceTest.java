package com.synq.backend.domain.reference.service;

import com.synq.backend.domain.project.code.ProjectErrorCode;
import com.synq.backend.domain.project.entity.Project;
import com.synq.backend.domain.project.entity.ProjectMember;
import com.synq.backend.domain.project.entity.ProjectMemberRole;
import com.synq.backend.domain.project.repository.ProjectMemberRepository;
import com.synq.backend.domain.project.repository.ProjectRepository;
import com.synq.backend.domain.reference.code.ReferenceErrorCode;
import com.synq.backend.domain.reference.dto.ReferenceNameUpdateRequest;
import com.synq.backend.domain.reference.dto.ReferenceNameUpdateResponse;
import com.synq.backend.domain.reference.entity.ReferenceMaterial;
import com.synq.backend.domain.reference.entity.ReferenceStatus;
import com.synq.backend.domain.reference.repository.ReferenceMaterialRepository;
import com.synq.backend.domain.user.entity.User;
import com.synq.backend.domain.user.repository.UserRepository;
import com.synq.backend.global.apipayload.code.GeneralErrorCode;
import com.synq.backend.global.apipayload.exception.GeneralException;
import com.synq.backend.support.PostgresTestContainer;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class ReferenceNameUpdateServiceTest extends PostgresTestContainer {

	@Autowired
	private ReferenceService referenceService;

	@Autowired
	private ReferenceMaterialRepository referenceMaterialRepository;

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private ProjectMemberRepository projectMemberRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void OWNER는_FILE과_LINK의_제목을_수정한다() {
		User owner = saveUser("소유자", "reference-update-service-owner@synq.com");
		User member = saveUser("멤버", "reference-update-service-owner-member@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);
		saveMember(project, member, ProjectMemberRole.MEMBER);
		ReferenceMaterial file = saveFile(project, member);
		ReferenceMaterial link = saveLink(project, member);

		ReferenceNameUpdateResponse fileResponse = referenceService.updateName(
				project.getId(), file.getId(), owner.getUserId(), request("회의 기획안 v2"));
		ReferenceNameUpdateResponse linkResponse = referenceService.updateName(
				project.getId(), link.getId(), owner.getUserId(), request("회의 링크 v2"));

		assertThat(fileResponse.referenceId()).isEqualTo(file.getId());
		assertThat(fileResponse.name()).isEqualTo("회의 기획안 v2");
		assertThat(fileResponse.type()).isEqualTo("FILE");
		assertThat(fileResponse.updatedAt()).isNotNull().isEqualTo(file.getUpdatedAt());
		assertThat(linkResponse.type()).isEqualTo("LINK");
		assertThat(linkResponse.name()).isEqualTo("회의 링크 v2");
		assertThat(file.getStatus()).isEqualTo(ReferenceStatus.AVAILABLE);
		assertThat(link.getUrl()).isEqualTo("https://example.com/reference");
	}

	@Test
	void MEMBER는_자신이_등록한_참고자료의_제목을_trim하여_수정한다() {
		User owner = saveUser("소유자", "reference-update-service-member-owner@synq.com");
		User member = saveUser("멤버", "reference-update-service-member@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);
		saveMember(project, member, ProjectMemberRole.MEMBER);
		ReferenceMaterial reference = saveLink(project, member);

		ReferenceNameUpdateResponse response = referenceService.updateName(
				project.getId(), reference.getId(), member.getUserId(), request("  새 제목  "));

		assertThat(response.name()).isEqualTo("새 제목");
		assertThat(reference.getName()).isEqualTo("새 제목");
	}

	@Test
	void MEMBER는_다른_사용자의_참고자료_제목을_수정할_수_없다() {
		User owner = saveUser("소유자", "reference-update-service-forbidden-owner@synq.com");
		User member = saveUser("멤버", "reference-update-service-forbidden-member@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);
		saveMember(project, member, ProjectMemberRole.MEMBER);
		ReferenceMaterial reference = saveFile(project, owner);

		assertThatThrownBy(() -> referenceService.updateName(
				project.getId(), reference.getId(), member.getUserId(), request("변경 시도")))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode())
								.isEqualTo(ReferenceErrorCode.REFERENCE_UPDATE_FORBIDDEN));
		assertThat(reference.getName()).isEqualTo("기존 파일.pdf");
	}

	@Test
	void 프로젝트_외부_사용자는_참고자료_제목을_수정할_수_없다() {
		User owner = saveUser("소유자", "reference-update-service-outsider-owner@synq.com");
		User outsider = saveUser("외부인", "reference-update-service-outsider@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);
		ReferenceMaterial reference = saveFile(project, owner);

		assertThatThrownBy(() -> referenceService.updateName(
				project.getId(), reference.getId(), outsider.getUserId(), request("변경 시도")))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode())
								.isEqualTo(ProjectErrorCode.NOT_PROJECT_MEMBER));
	}

	@Test
	void 사용자나_활성_프로젝트가_없으면_수정할_수_없다() {
		User owner = saveUser("소유자", "reference-update-service-validation@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);
		ReferenceMaterial reference = saveFile(project, owner);
		Project deletedProject = saveProject(owner);
		saveMember(deletedProject, owner, ProjectMemberRole.OWNER);
		ReferenceMaterial deletedProjectReference = saveFile(deletedProject, owner);
		deletedProject.softDelete();
		projectRepository.flush();

		assertCode(ProjectErrorCode.USER_NOT_FOUND, () -> referenceService.updateName(
				project.getId(), reference.getId(), Long.MAX_VALUE, request("새 제목")));
		assertCode(ProjectErrorCode.PROJECT_NOT_FOUND, () -> referenceService.updateName(
				Long.MAX_VALUE, reference.getId(), owner.getUserId(), request("새 제목")));
		assertCode(ProjectErrorCode.PROJECT_NOT_FOUND, () -> referenceService.updateName(
				deletedProject.getId(), deletedProjectReference.getId(), owner.getUserId(), request("새 제목")));
	}

	@Test
	void 없거나_삭제됐거나_다른_프로젝트의_참고자료는_404이다() {
		User owner = saveUser("소유자", "reference-update-service-not-found@synq.com");
		Project project = saveProject(owner);
		Project otherProject = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);
		saveMember(otherProject, owner, ProjectMemberRole.OWNER);
		ReferenceMaterial deletedReference = saveLink(project, owner);
		ReferenceMaterial otherReference = saveLink(otherProject, owner);
		deletedReference.softDelete();
		referenceMaterialRepository.flush();
		entityManager.clear();

		assertCode(ReferenceErrorCode.REFERENCE_NOT_FOUND, () -> referenceService.updateName(
				project.getId(), Long.MAX_VALUE, owner.getUserId(), request("새 제목")));
		assertCode(ReferenceErrorCode.REFERENCE_NOT_FOUND, () -> referenceService.updateName(
				project.getId(), deletedReference.getId(), owner.getUserId(), request("새 제목")));
		assertCode(ReferenceErrorCode.REFERENCE_NOT_FOUND, () -> referenceService.updateName(
				project.getId(), otherReference.getId(), owner.getUserId(), request("새 제목")));
	}

	@Test
	void 제목은_1자와_30자까지_허용하고_잘못된_값은_400이다() {
		User owner = saveUser("소유자", "reference-update-service-name@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);
		ReferenceMaterial reference = saveFile(project, owner);

		assertThat(referenceService.updateName(
				project.getId(), reference.getId(), owner.getUserId(), request("가")).name()).isEqualTo("가");
		String thirtyCharacters = "가".repeat(30);
		assertThat(referenceService.updateName(
				project.getId(), reference.getId(), owner.getUserId(), request(thirtyCharacters)).name())
				.isEqualTo(thirtyCharacters);

		assertInvalidName(project, reference, owner, null);
		assertInvalidName(project, reference, owner, "");
		assertInvalidName(project, reference, owner, "   ");
		assertInvalidName(project, reference, owner, "가".repeat(31));
	}

	private void assertInvalidName(Project project, ReferenceMaterial reference, User user, String name) {
		assertCode(GeneralErrorCode.BAD_REQUEST, () -> referenceService.updateName(
				project.getId(), reference.getId(), user.getUserId(), request(name)));
	}

	private void assertCode(Object code, Runnable runnable) {
		assertThatThrownBy(runnable::run)
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode()).isEqualTo(code));
	}

	private ReferenceNameUpdateRequest request(String name) {
		return new ReferenceNameUpdateRequest(name);
	}

	private Project saveProject(User owner) {
		return projectRepository.save(Project.of(owner.getUserId(), "SynQ", null));
	}

	private void saveMember(Project project, User user, ProjectMemberRole role) {
		projectMemberRepository.save(ProjectMember.of(project.getId(), user.getUserId(), role));
	}

	private ReferenceMaterial saveFile(Project project, User uploader) {
		return referenceMaterialRepository.save(ReferenceMaterial.ofFile(
				project.getId(),
				uploader.getUserId(),
				"기존 파일.pdf",
				1024L,
				"references/" + project.getId() + "/update-test.pdf",
				com.synq.backend.domain.reference.entity.ReferenceFileExtension.PDF,
				ReferenceStatus.AVAILABLE
		));
	}

	private ReferenceMaterial saveLink(Project project, User uploader) {
		return referenceMaterialRepository.save(ReferenceMaterial.ofLink(
				project.getId(),
				uploader.getUserId(),
				"기존 링크",
				"https://example.com/reference",
				ReferenceStatus.AVAILABLE
		));
	}

	private User saveUser(String name, String email) {
		return userRepository.save(User.ofLocal(name, email, "password-hash"));
	}
}
