package com.synq.backend.domain.reference.service;

import com.synq.backend.domain.project.code.ProjectErrorCode;
import com.synq.backend.domain.project.entity.Project;
import com.synq.backend.domain.project.entity.ProjectMember;
import com.synq.backend.domain.project.entity.ProjectMemberRole;
import com.synq.backend.domain.project.repository.ProjectMemberRepository;
import com.synq.backend.domain.project.repository.ProjectRepository;
import com.synq.backend.domain.reference.code.ReferenceErrorCode;
import com.synq.backend.domain.reference.dto.ReferenceLinkCreateRequest;
import com.synq.backend.domain.reference.dto.ReferenceLinkCreateResponse;
import com.synq.backend.domain.reference.dto.ReferenceListResponse;
import com.synq.backend.domain.reference.dto.ReferenceResponse;
import com.synq.backend.domain.reference.entity.ReferenceFileExtension;
import com.synq.backend.domain.reference.entity.ReferenceMaterial;
import com.synq.backend.domain.reference.entity.ReferenceStatus;
import com.synq.backend.domain.reference.repository.ReferenceMaterialRepository;
import com.synq.backend.domain.user.entity.User;
import com.synq.backend.domain.user.repository.UserRepository;
import com.synq.backend.global.apipayload.code.GeneralErrorCode;
import com.synq.backend.global.apipayload.exception.GeneralException;
import com.synq.backend.support.PostgresTestContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class ReferenceServiceTest extends PostgresTestContainer {

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

	@Test
	void 프로젝트_OWNER가_링크를_UPLOADING_상태로_등록한다() {
		User owner = saveUser("박서은", "reference-create-owner@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);

		ReferenceLinkCreateResponse response = referenceService.createLink(
				project.getId(),
				owner.getUserId(),
				new ReferenceLinkCreateRequest("  https://www.notion.so/example  ")
		);

		assertThat(response.referenceId()).isNotNull();
		assertThat(response.type()).isEqualTo("LINK");
		assertThat(response.name()).isEqualTo("notion.so");
		assertThat(response.url()).isEqualTo("https://www.notion.so/example");
		assertThat(response.status()).isEqualTo("UPLOADING");
		assertThat(response.uploaderId()).isEqualTo(owner.getUserId());
		assertThat(response.uploaderName()).isEqualTo("박서은");
		assertThat(response.createdAt()).isNotNull();

		ReferenceMaterial saved = referenceMaterialRepository.findById(response.referenceId()).orElseThrow();
		assertThat(saved.getName()).isEqualTo("notion.so");
		assertThat(saved.getUrl()).isEqualTo("https://www.notion.so/example");
		assertThat(saved.getStatus()).isEqualTo(ReferenceStatus.UPLOADING);
	}

	@Test
	void 일반_MEMBER도_동일한_URL을_중복_등록할_수_있다() {
		User owner = saveUser("소유자", "reference-create-member-owner@synq.com");
		User member = saveUser("멤버", "reference-create-member@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);
		saveMember(project, member, ProjectMemberRole.MEMBER);
		ReferenceLinkCreateRequest request = new ReferenceLinkCreateRequest("https://example.com/document");

		ReferenceLinkCreateResponse first = referenceService.createLink(
				project.getId(), member.getUserId(), request);
		ReferenceLinkCreateResponse second = referenceService.createLink(
				project.getId(), member.getUserId(), request);

		assertThat(first.referenceId()).isNotEqualTo(second.referenceId());
		assertThat(referenceMaterialRepository.countByProjectId(project.getId())).isEqualTo(2);
	}

	@Test
	void 프로젝트_외부_사용자는_링크를_등록할_수_없다() {
		User owner = saveUser("소유자", "reference-create-outsider-owner@synq.com");
		User outsider = saveUser("외부 사용자", "reference-create-outsider@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);

		assertThatThrownBy(() -> referenceService.createLink(
				project.getId(), outsider.getUserId(), linkRequest()))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode())
								.isEqualTo(ProjectErrorCode.NOT_PROJECT_MEMBER));
	}

	@Test
	void 인증_정보나_사용자나_활성_프로젝트가_없으면_링크를_등록할_수_없다() {
		User owner = saveUser("소유자", "reference-create-validation@synq.com");
		Project deletedProject = saveProject(owner);
		saveMember(deletedProject, owner, ProjectMemberRole.OWNER);
		deletedProject.softDelete();

		assertThatThrownBy(() -> referenceService.createLink(1L, null, linkRequest()))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode())
								.isEqualTo(GeneralErrorCode.UNAUTHORIZED));
		assertThatThrownBy(() -> referenceService.createLink(1L, Long.MAX_VALUE, linkRequest()))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode())
								.isEqualTo(ProjectErrorCode.USER_NOT_FOUND));
		assertThatThrownBy(() -> referenceService.createLink(Long.MAX_VALUE, owner.getUserId(), linkRequest()))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode())
								.isEqualTo(ProjectErrorCode.PROJECT_NOT_FOUND));
		assertThatThrownBy(() -> referenceService.createLink(
				deletedProject.getId(), owner.getUserId(), linkRequest()))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode())
								.isEqualTo(ProjectErrorCode.PROJECT_NOT_FOUND));
	}

	@Test
	void 활성_파일과_링크가_10개이면_409이고_새_자료를_저장하지_않는다() {
		User owner = saveUser("소유자", "reference-create-limit@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);
		for (int index = 0; index < 5; index++) {
			saveFile(project, owner);
			saveLink(project, owner);
		}

		assertThatThrownBy(() -> referenceService.createLink(
				project.getId(), owner.getUserId(), linkRequest()))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode())
								.isEqualTo(ReferenceErrorCode.REFERENCE_LIMIT_EXCEEDED));
		assertThat(referenceMaterialRepository.countByProjectId(project.getId())).isEqualTo(10);
	}

	@Test
	void 활성_자료가_9개이면_등록하고_Soft_Delete_자료는_개수에서_제외한다() {
		User owner = saveUser("소유자", "reference-create-soft-delete@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);
		for (int index = 0; index < 9; index++) {
			saveFile(project, owner);
		}
		ReferenceMaterial deleted = saveLink(project, owner);
		deleted.softDelete();

		referenceService.createLink(project.getId(), owner.getUserId(), linkRequest());

		assertThat(referenceMaterialRepository.countByProjectId(project.getId())).isEqualTo(10);
	}

	@Test
	void 프로젝트_소유자는_파일과_링크_참고자료를_조회하고_모두_삭제할_수_있다() {
		User owner = saveUser("소유자", "reference-owner@synq.com");
		User member = saveUser("멤버", "reference-owner-member@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);
		saveMember(project, member, ProjectMemberRole.MEMBER);
		ReferenceMaterial file = saveFile(project, owner);
		ReferenceMaterial link = saveLink(project, member);

		ReferenceListResponse response = referenceService.findAll(
				project.getId(),
				owner.getUserId()
		);

		assertThat(response.currentCount()).isEqualTo(2);
		assertThat(response.maxCount()).isEqualTo(10);
		assertThat(response.references())
				.extracting(ReferenceResponse::referenceId)
				.containsExactly(link.getId(), file.getId());
		assertThat(response.references()).allMatch(ReferenceResponse::canDelete);

		ReferenceResponse linkResponse = response.references().get(0);
		assertThat(linkResponse.type()).isEqualTo("LINK");
		assertThat(linkResponse.status()).isEqualTo("READ_FAILED");
		assertThat(linkResponse.url()).isEqualTo("https://www.notion.so/example");
		assertThat(linkResponse.fileSize()).isNull();
		assertThat(linkResponse.fileExtension()).isNull();

		ReferenceResponse fileResponse = response.references().get(1);
		assertThat(fileResponse.type()).isEqualTo("FILE");
		assertThat(fileResponse.status()).isEqualTo("AVAILABLE");
		assertThat(fileResponse.url()).isNull();
		assertThat(fileResponse.fileSize()).isEqualTo(1048576L);
		assertThat(fileResponse.fileExtension()).isEqualTo("PDF");
	}

	@Test
	void 일반_멤버는_자신이_등록한_참고자료만_삭제할_수_있다() {
		User owner = saveUser("소유자", "reference-member-owner@synq.com");
		User member = saveUser("멤버", "reference-member@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);
		saveMember(project, member, ProjectMemberRole.MEMBER);
		ReferenceMaterial ownerReference = saveFile(project, owner);
		ReferenceMaterial memberReference = saveLink(project, member);

		ReferenceListResponse response = referenceService.findAll(
				project.getId(),
				member.getUserId()
		);

		assertThat(findById(response, ownerReference.getId()).canDelete()).isFalse();
		assertThat(findById(response, memberReference.getId()).canDelete()).isTrue();
		assertThat(findById(response, memberReference.getId()).uploaderName()).isEqualTo("멤버");
	}

	@Test
	void 참고자료가_없으면_개수_0과_빈_목록을_반환한다() {
		User owner = saveUser("소유자", "reference-empty@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);

		ReferenceListResponse response = referenceService.findAll(
				project.getId(),
				owner.getUserId()
		);

		assertThat(response.currentCount()).isZero();
		assertThat(response.maxCount()).isEqualTo(10);
		assertThat(response.references()).isEmpty();
	}

	@Test
	void 삭제된_참고자료는_목록과_개수에서_제외한다() {
		User owner = saveUser("소유자", "reference-deleted-material@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);
		ReferenceMaterial deletedReference = saveFile(project, owner);
		deletedReference.softDelete();

		ReferenceListResponse response = referenceService.findAll(
				project.getId(),
				owner.getUserId()
		);

		assertThat(response.currentCount()).isZero();
		assertThat(response.references()).isEmpty();
	}

	@Test
	void 프로젝트_외부_사용자는_참고자료를_조회할_수_없다() {
		User owner = saveUser("소유자", "reference-outsider-owner@synq.com");
		User outsider = saveUser("외부 사용자", "reference-outsider@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);

		assertThatThrownBy(() -> referenceService.findAll(project.getId(), outsider.getUserId()))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode())
								.isEqualTo(ProjectErrorCode.NOT_PROJECT_MEMBER));
	}

	@Test
	void 존재하지_않거나_삭제된_프로젝트는_조회할_수_없다() {
		User owner = saveUser("소유자", "reference-missing-project@synq.com");
		Project deletedProject = saveProject(owner);
		saveMember(deletedProject, owner, ProjectMemberRole.OWNER);
		deletedProject.softDelete();

		assertThatThrownBy(() -> referenceService.findAll(Long.MAX_VALUE, owner.getUserId()))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode())
								.isEqualTo(ProjectErrorCode.PROJECT_NOT_FOUND));
		assertThatThrownBy(() -> referenceService.findAll(deletedProject.getId(), owner.getUserId()))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode())
								.isEqualTo(ProjectErrorCode.PROJECT_NOT_FOUND));
	}

	@Test
	void 인증_정보가_없거나_사용자가_존재하지_않으면_조회할_수_없다() {
		assertThatThrownBy(() -> referenceService.findAll(1L, null))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode())
								.isEqualTo(GeneralErrorCode.UNAUTHORIZED));
		assertThatThrownBy(() -> referenceService.findAll(1L, Long.MAX_VALUE))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode())
								.isEqualTo(ProjectErrorCode.USER_NOT_FOUND));
	}

	private ReferenceResponse findById(ReferenceListResponse response, Long referenceId) {
		return response.references().stream()
				.filter(reference -> reference.referenceId().equals(referenceId))
				.findFirst()
				.orElseThrow();
	}

	private ReferenceLinkCreateRequest linkRequest() {
		return new ReferenceLinkCreateRequest("https://example.com/document");
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
				"프로젝트 요구사항.pdf",
				1048576L,
				ReferenceFileExtension.PDF,
				ReferenceStatus.AVAILABLE
		));
	}

	private ReferenceMaterial saveLink(Project project, User uploader) {
		return referenceMaterialRepository.save(ReferenceMaterial.ofLink(
				project.getId(),
				uploader.getUserId(),
				"SynQ 기획 문서",
				"https://www.notion.so/example",
				ReferenceStatus.READ_FAILED
		));
	}

	private User saveUser(String name, String email) {
		return userRepository.save(User.ofLocal(name, email, "password-hash"));
	}
}
