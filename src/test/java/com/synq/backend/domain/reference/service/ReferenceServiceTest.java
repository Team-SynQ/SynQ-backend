package com.synq.backend.domain.reference.service;

import com.synq.backend.domain.ai.rag.entity.DocumentChunk;
import com.synq.backend.domain.ai.rag.repository.DocumentChunkRepository;
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
import com.synq.backend.domain.reference.event.ReferenceFileDeletedEvent;
import com.synq.backend.domain.reference.link.LinkPreflightChecker;
import com.synq.backend.domain.reference.repository.ReferenceMaterialRepository;
import com.synq.backend.domain.user.entity.User;
import com.synq.backend.domain.user.repository.UserRepository;
import com.synq.backend.global.apipayload.code.GeneralErrorCode;
import com.synq.backend.global.apipayload.exception.GeneralException;
import com.synq.backend.support.PostgresTestContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@Transactional
@RecordApplicationEvents
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

	@Autowired
	private DocumentChunkRepository documentChunkRepository;

	@Autowired
	private ApplicationEvents applicationEvents;

	// 링크 등록이 실제 네트워크를 타지 않게 막는다.
	// 이 클래스는 @Transactional 이라 커밋이 없어 AFTER_COMMIT 리스너는 실행되지 않는다.
	@MockitoBean
	private LinkPreflightChecker linkPreflightChecker;

	@Test
	void 프로젝트_OWNER는_다른_MEMBER의_참고자료를_Soft_Delete한다() {
		User owner = saveUser("소유자", "reference-delete-owner@synq.com");
		User member = saveUser("멤버", "reference-delete-owner-member@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);
		saveMember(project, member, ProjectMemberRole.MEMBER);
		ReferenceMaterial reference = saveLink(project, member);

		referenceService.delete(project.getId(), reference.getId(), owner.getUserId());

		assertThat(reference.getDeletedAt()).isNotNull();
		assertThat(referenceMaterialRepository.findByIdAndProjectId(reference.getId(), project.getId()))
				.isEmpty();
	}

	@Test
	void 일반_MEMBER는_자신이_등록한_참고자료를_삭제한다() {
		User owner = saveUser("소유자", "reference-delete-member-owner@synq.com");
		User member = saveUser("멤버", "reference-delete-member@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);
		saveMember(project, member, ProjectMemberRole.MEMBER);
		ReferenceMaterial reference = saveFile(project, member);

		referenceService.delete(project.getId(), reference.getId(), member.getUserId());

		assertThat(reference.getDeletedAt()).isNotNull();
	}

	@Test
	void FILE을_삭제하면_storageKey를_담은_S3_정리_이벤트를_발행한다() {
		User owner = saveUser("소유자", "reference-delete-file-event@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);
		ReferenceMaterial reference = saveFile(project, owner);

		referenceService.delete(project.getId(), reference.getId(), owner.getUserId());

		assertThat(applicationEvents.stream(ReferenceFileDeletedEvent.class))
				.singleElement()
				.satisfies(event -> {
					assertThat(event.referenceId()).isEqualTo(reference.getId());
					assertThat(event.projectId()).isEqualTo(project.getId());
					assertThat(event.storageKey()).isEqualTo(reference.getStorageKey());
				});
	}

	@Test
	void LINK를_삭제하면_S3_정리_이벤트를_발행하지_않는다() {
		User owner = saveUser("소유자", "reference-delete-link-event@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);
		ReferenceMaterial reference = saveLink(project, owner);

		referenceService.delete(project.getId(), reference.getId(), owner.getUserId());

		assertThat(applicationEvents.stream(ReferenceFileDeletedEvent.class)).isEmpty();
	}

	@Test
	void 삭제_권한_검증에_실패하면_S3_정리_이벤트를_발행하지_않는다() {
		User owner = saveUser("소유자", "reference-delete-event-owner@synq.com");
		User member = saveUser("멤버", "reference-delete-event-member@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);
		saveMember(project, member, ProjectMemberRole.MEMBER);
		ReferenceMaterial reference = saveFile(project, owner);

		assertThatThrownBy(() -> referenceService.delete(
				project.getId(), reference.getId(), member.getUserId()))
				.isInstanceOf(GeneralException.class);

		assertThat(applicationEvents.stream(ReferenceFileDeletedEvent.class)).isEmpty();
	}

	@Test
	void 일반_MEMBER는_다른_사용자의_참고자료를_삭제할_수_없다() {
		User owner = saveUser("소유자", "reference-delete-forbidden-owner@synq.com");
		User member = saveUser("멤버", "reference-delete-forbidden-member@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);
		saveMember(project, member, ProjectMemberRole.MEMBER);
		ReferenceMaterial reference = saveFile(project, owner);

		assertThatThrownBy(() -> referenceService.delete(
				project.getId(), reference.getId(), member.getUserId()))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode())
								.isEqualTo(ReferenceErrorCode.REFERENCE_DELETE_FORBIDDEN));
		assertThat(reference.getDeletedAt()).isNull();
	}

	@Test
	void 프로젝트_외부_사용자는_참고자료를_삭제할_수_없다() {
		User owner = saveUser("소유자", "reference-delete-outsider-owner@synq.com");
		User outsider = saveUser("외부 사용자", "reference-delete-outsider@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);
		ReferenceMaterial reference = saveFile(project, owner);

		assertThatThrownBy(() -> referenceService.delete(
				project.getId(), reference.getId(), outsider.getUserId()))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode())
								.isEqualTo(ProjectErrorCode.NOT_PROJECT_MEMBER));
		assertThat(reference.getDeletedAt()).isNull();
	}

	@Test
	void 사용자나_활성_프로젝트가_없으면_참고자료를_삭제할_수_없다() {
		User owner = saveUser("소유자", "reference-delete-validation@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);
		ReferenceMaterial reference = saveFile(project, owner);
		Project deletedProject = saveProject(owner);
		saveMember(deletedProject, owner, ProjectMemberRole.OWNER);
		ReferenceMaterial deletedProjectReference = saveFile(deletedProject, owner);
		deletedProject.softDelete();

		assertThatThrownBy(() -> referenceService.delete(project.getId(), reference.getId(), Long.MAX_VALUE))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode())
								.isEqualTo(ProjectErrorCode.USER_NOT_FOUND));
		assertThatThrownBy(() -> referenceService.delete(Long.MAX_VALUE, reference.getId(), owner.getUserId()))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode())
								.isEqualTo(ProjectErrorCode.PROJECT_NOT_FOUND));
		assertThatThrownBy(() -> referenceService.delete(
				deletedProject.getId(), deletedProjectReference.getId(), owner.getUserId()))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode())
								.isEqualTo(ProjectErrorCode.PROJECT_NOT_FOUND));
		assertThat(reference.getDeletedAt()).isNull();
		assertThat(deletedProjectReference.getDeletedAt()).isNull();
	}

	@Test
	void 없거나_삭제됐거나_다른_프로젝트의_참고자료는_404이다() {
		User owner = saveUser("소유자", "reference-delete-not-found@synq.com");
		Project project = saveProject(owner);
		Project otherProject = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);
		saveMember(otherProject, owner, ProjectMemberRole.OWNER);
		ReferenceMaterial deleted = saveFile(project, owner);
		ReferenceMaterial otherReference = saveFile(otherProject, owner);
		deleted.softDelete();

		assertReferenceNotFound(project.getId(), Long.MAX_VALUE, owner.getUserId());
		assertReferenceNotFound(project.getId(), deleted.getId(), owner.getUserId());
		assertReferenceNotFound(project.getId(), otherReference.getId(), owner.getUserId());
		assertThat(otherReference.getDeletedAt()).isNull();
	}

	@Test
	void 모든_처리_상태의_참고자료를_삭제할_수_있다() {
		User owner = saveUser("소유자", "reference-delete-status@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);
		ReferenceMaterial uploading = saveLink(project, owner, ReferenceStatus.UPLOADING);
		ReferenceMaterial available = saveLink(project, owner, ReferenceStatus.AVAILABLE);
		ReferenceMaterial readFailed = saveLink(project, owner, ReferenceStatus.READ_FAILED);

		referenceService.delete(project.getId(), uploading.getId(), owner.getUserId());
		referenceService.delete(project.getId(), available.getId(), owner.getUserId());
		referenceService.delete(project.getId(), readFailed.getId(), owner.getUserId());

		assertThat(uploading.getDeletedAt()).isNotNull();
		assertThat(available.getDeletedAt()).isNotNull();
		assertThat(readFailed.getDeletedAt()).isNotNull();
	}

	@Test
	void 삭제_후_목록과_개수에서_제외되고_새_링크를_등록할_수_있다() {
		User owner = saveUser("소유자", "reference-delete-recreate@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);
		ReferenceMaterial target = null;
		for (int index = 0; index < 10; index++) {
			target = saveFile(project, owner);
		}

		referenceService.delete(project.getId(), target.getId(), owner.getUserId());
		ReferenceListResponse afterDelete = referenceService.findAll(project.getId(), owner.getUserId());
		ReferenceLinkCreateResponse created = referenceService.createLink(
				project.getId(), owner.getUserId(), linkRequest());

		assertThat(afterDelete.currentCount()).isEqualTo(9);
		assertThat(afterDelete.references())
				.extracting(ReferenceResponse::referenceId)
				.doesNotContain(target.getId());
		assertThat(referenceMaterialRepository.countByProjectId(project.getId())).isEqualTo(10);
		assertThat(created.referenceId()).isNotNull();
	}

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

	@Test
	void 참고자료를_삭제하면_인덱싱된_청크도_지운다() {
		// 소프트 삭제라 FK 의 ON DELETE CASCADE 가 동작하지 않는다. 지우지 않으면
		// 사용자가 삭제한 문서의 내용이 계속 3-hint 와 AI Chat 답변에 실려 나간다.
		User owner = saveUser("소유자", "reference-chunk-owner@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);
		ReferenceMaterial reference = saveLink(project, owner);
		documentChunkRepository.save(DocumentChunk.of(
				reference.getId(), project.getId(), 0, "지워져야 할 청크",
				new float[768], "test-model"));

		referenceService.delete(project.getId(), reference.getId(), owner.getUserId());

		assertThat(documentChunkRepository
				.findByReferenceMaterialIdOrderByChunkIndexAsc(reference.getId()))
				.isEmpty();
	}

	@Test
	void 프리플라이트가_실패하면_링크를_저장하지_않는다() {
		User owner = saveUser("소유자", "reference-preflight-owner@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);
		willThrow(new GeneralException(ReferenceErrorCode.LINK_UNREACHABLE))
				.given(linkPreflightChecker).check(anyString());

		assertThatThrownBy(() -> referenceService.createLink(
				project.getId(), owner.getUserId(), linkRequest()))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode())
								.isEqualTo(ReferenceErrorCode.LINK_UNREACHABLE));

		assertThat(referenceMaterialRepository.countByProjectId(project.getId())).isZero();
	}

	@Test
	void 비멤버는_프리플라이트를_타기_전에_거부된다() {
		// 비멤버가 URL 도달 여부를 알아내지 못하게 하고, 네트워크도 태우지 않는다.
		User owner = saveUser("소유자", "reference-preflight-order-owner@synq.com");
		User outsider = saveUser("외부인", "reference-preflight-order-outsider@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);

		assertThatThrownBy(() -> referenceService.createLink(
				project.getId(), outsider.getUserId(), linkRequest()))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode())
								.isEqualTo(ProjectErrorCode.NOT_PROJECT_MEMBER));

		verify(linkPreflightChecker, never()).check(anyString());
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

	private void assertReferenceNotFound(Long projectId, Long referenceId, Long userId) {
		assertThatThrownBy(() -> referenceService.delete(projectId, referenceId, userId))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode())
								.isEqualTo(ReferenceErrorCode.REFERENCE_NOT_FOUND));
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
				"references/" + project.getId() + "/test.pdf",
				ReferenceFileExtension.PDF,
				ReferenceStatus.AVAILABLE
		));
	}

	private ReferenceMaterial saveLink(Project project, User uploader) {
		return saveLink(project, uploader, ReferenceStatus.READ_FAILED);
	}

	private ReferenceMaterial saveLink(Project project, User uploader, ReferenceStatus status) {
		return referenceMaterialRepository.save(ReferenceMaterial.ofLink(
				project.getId(),
				uploader.getUserId(),
				"SynQ 기획 문서",
				"https://www.notion.so/example",
				status
		));
	}

	private User saveUser(String name, String email) {
		return userRepository.save(User.ofLocal(name, email, "password-hash"));
	}
}
