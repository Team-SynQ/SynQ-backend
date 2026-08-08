package com.synq.backend.domain.reference.service;

import com.synq.backend.domain.reference.file.FileTextExtractor;
import org.junit.jupiter.api.BeforeEach;
import static org.mockito.BDDMockito.given;

import com.synq.backend.domain.project.code.ProjectErrorCode;
import com.synq.backend.domain.project.entity.Project;
import com.synq.backend.domain.project.entity.ProjectMember;
import com.synq.backend.domain.project.entity.ProjectMemberRole;
import com.synq.backend.domain.project.repository.ProjectMemberRepository;
import com.synq.backend.domain.project.repository.ProjectRepository;
import com.synq.backend.domain.reference.code.ReferenceErrorCode;
import com.synq.backend.domain.reference.dto.ReferenceFileCreateResponse;
import com.synq.backend.domain.reference.entity.ReferenceFileExtension;
import com.synq.backend.domain.reference.entity.ReferenceMaterial;
import com.synq.backend.domain.reference.entity.ReferenceStatus;
import com.synq.backend.domain.reference.repository.ReferenceMaterialRepository;
import com.synq.backend.domain.reference.storage.ReferenceStorage;
import com.synq.backend.domain.reference.storage.ReferenceStorageException;
import com.synq.backend.domain.user.entity.User;
import com.synq.backend.domain.user.repository.UserRepository;
import com.synq.backend.global.apipayload.exception.GeneralException;
import com.synq.backend.support.PostgresTestContainer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Transactional
class ReferenceFileCreateServiceTest extends PostgresTestContainer {

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

	@MockitoBean
	private ReferenceStorage referenceStorage;

	// 기존 픽스처는 "content" 7바이트를 .pdf 로 위장해서 쓴다. 실제 Tika 파싱은 이를 거절하므로
	// 등록 흐름을 검증하는 테스트에서는 추출기를 대역으로 세운다.
	@MockitoBean
	private FileTextExtractor fileTextExtractor;

	@BeforeEach
	void stubFileTextExtractor() {
		given(fileTextExtractor.extract(any(), anyString()))
				.willReturn("추출된 본문입니다. 최소 길이 조건을 넘기기 위한 충분히 긴 문장입니다.");
	}

	@Test
	void OWNER와_MEMBER가_지원_파일을_등록하면_storage_key와_UPLOADING_메타데이터를_저장한다() {
		User owner = saveUser("소유자", "file-create-owner@synq.com");
		User member = saveUser("멤버", "file-create-member@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);
		saveMember(project, member, ProjectMemberRole.MEMBER);

		ReferenceFileCreateResponse ownerResponse = referenceService.createFiles(
				project.getId(), owner.getUserId(), List.of(
						file("requirements.PDF", "application/pdf"),
						file("document.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
				));
		ReferenceFileCreateResponse memberResponse = referenceService.createFiles(
				project.getId(), member.getUserId(), List.of(
						file("slides.pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
						file("notes.txt", "text/plain; charset=UTF-8")
				));

		assertThat(ownerResponse.references()).hasSize(2);
		assertThat(memberResponse.references()).hasSize(2);
		assertThat(ownerResponse.references())
				.extracting(response -> response.fileExtension())
				.containsExactly("PDF", "DOCX");
		assertThat(memberResponse.references())
				.extracting(response -> response.fileExtension())
				.containsExactly("PPTX", "TXT");
		assertThat(memberResponse.references()).allSatisfy(response -> {
			assertThat(response.type()).isEqualTo("FILE");
			assertThat(response.status()).isEqualTo("UPLOADING");
			assertThat(response.uploaderId()).isEqualTo(member.getUserId());
			assertThat(response.uploaderName()).isEqualTo("멤버");
		});
		assertThat(referenceMaterialRepository.findAllByProjectIdOrderByCreatedAtDescIdDesc(project.getId()))
				.allSatisfy(reference -> {
					assertThat(reference.getStorageKey())
							.startsWith("references/" + project.getId() + "/");
					assertThat(reference.getUrl()).isNull();
					assertThat(reference.getStatus()).isEqualTo(ReferenceStatus.UPLOADING);
				});
		verify(referenceStorage, times(4))
				.upload(anyString(), any(InputStream.class), anyLong(), anyString());
	}

	@Test
	void 동일한_파일을_중복_등록할_수_있다() {
		User owner = saveUser("소유자", "file-duplicate-owner@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);

		ReferenceFileCreateResponse first = referenceService.createFiles(
				project.getId(), owner.getUserId(), List.of(file("same.pdf", "application/pdf")));
		ReferenceFileCreateResponse second = referenceService.createFiles(
				project.getId(), owner.getUserId(), List.of(file("same.pdf", "application/pdf")));

		assertThat(first.references().get(0).referenceId())
				.isNotEqualTo(second.references().get(0).referenceId());
		assertThat(referenceMaterialRepository.countByProjectId(project.getId())).isEqualTo(2);
	}

	@Test
	void 외부_사용자와_없거나_삭제된_프로젝트는_등록할_수_없다() {
		User owner = saveUser("소유자", "file-validation-owner@synq.com");
		User outsider = saveUser("외부", "file-validation-outsider@synq.com");
		Project project = saveProject(owner);
		Project deletedProject = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);
		saveMember(deletedProject, owner, ProjectMemberRole.OWNER);
		deletedProject.softDelete();
		projectRepository.flush();

		assertCode(() -> referenceService.createFiles(project.getId(), outsider.getUserId(), validFiles()),
				ProjectErrorCode.NOT_PROJECT_MEMBER);
		assertCode(() -> referenceService.createFiles(project.getId(), Long.MAX_VALUE, validFiles()),
				ProjectErrorCode.USER_NOT_FOUND);
		assertCode(() -> referenceService.createFiles(Long.MAX_VALUE, owner.getUserId(), validFiles()),
				ProjectErrorCode.PROJECT_NOT_FOUND);
		assertCode(() -> referenceService.createFiles(deletedProject.getId(), owner.getUserId(), validFiles()),
				ProjectErrorCode.PROJECT_NOT_FOUND);
	}

	@Test
	void 빈_파일_목록과_6개_파일과_빈_파일은_400이다() {
		User owner = saveUser("소유자", "file-invalid-owner@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);

		assertCode(() -> referenceService.createFiles(project.getId(), owner.getUserId(), List.of()),
				ReferenceErrorCode.INVALID_REFERENCE_FILE);
		assertCode(() -> referenceService.createFiles(project.getId(), owner.getUserId(), List.of(
				file("1.pdf", "application/pdf"), file("2.pdf", "application/pdf"),
				file("3.pdf", "application/pdf"), file("4.pdf", "application/pdf"),
				file("5.pdf", "application/pdf"), file("6.pdf", "application/pdf"))),
				ReferenceErrorCode.INVALID_REFERENCE_FILE);
		assertCode(() -> referenceService.createFiles(project.getId(), owner.getUserId(), List.of(
				new MockMultipartFile("files", "empty.pdf", "application/pdf", new byte[0]))),
				ReferenceErrorCode.INVALID_REFERENCE_FILE);
	}

	@Test
	void 크기_초과와_확장자_없음과_지원하지_않는_형식과_Content_Type_불일치는_거부한다() {
		User owner = saveUser("소유자", "file-format-owner@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);

		assertCode(() -> referenceService.createFiles(project.getId(), owner.getUserId(), List.of(
				new MockMultipartFile("files", "large.pdf", "application/pdf", new byte[20 * 1024 * 1024 + 1]))),
				ReferenceErrorCode.REFERENCE_FILE_SIZE_EXCEEDED);
		assertCode(() -> referenceService.createFiles(project.getId(), owner.getUserId(), List.of(
				file("no-extension", "application/pdf"))), ReferenceErrorCode.UNSUPPORTED_REFERENCE_FILE);
		assertCode(() -> referenceService.createFiles(project.getId(), owner.getUserId(), List.of(
				file("image.png", "image/png"))), ReferenceErrorCode.UNSUPPORTED_REFERENCE_FILE);
		assertCode(() -> referenceService.createFiles(project.getId(), owner.getUserId(), List.of(
				file("fake.pdf", "text/plain"))), ReferenceErrorCode.UNSUPPORTED_REFERENCE_FILE);
	}

	@Test
	void Content_Type이_없거나_octet_stream이어도_확장자를_기준으로_등록한다() {
		User owner = saveUser("소유자", "file-content-type-owner@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);

		ReferenceFileCreateResponse response = referenceService.createFiles(
				project.getId(), owner.getUserId(), List.of(
						file("unknown.pdf", null),
						file("binary.DOCX", "application/octet-stream")
				));

		assertThat(response.references())
				.extracting(reference -> reference.fileExtension())
				.containsExactly("PDF", "DOCX");
		verify(referenceStorage).upload(
				anyString(), any(InputStream.class), anyLong(), org.mockito.ArgumentMatchers.eq("application/pdf"));
		verify(referenceStorage).upload(
				anyString(), any(InputStream.class), anyLong(),
				org.mockito.ArgumentMatchers.eq("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
	}

	@Test
	void 정확히_20MB와_255자_파일명은_허용하고_파일명_오류는_400이다() {
		User owner = saveUser("소유자", "file-boundary-owner@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);
		String maxLengthName = "a".repeat(251) + ".pdf";

		referenceService.createFiles(project.getId(), owner.getUserId(), List.of(
				new MockMultipartFile("files", maxLengthName, "application/pdf", new byte[20 * 1024 * 1024])));

		assertCode(() -> referenceService.createFiles(project.getId(), owner.getUserId(), List.of(
				new MockMultipartFile("files", null, "application/pdf", "content".getBytes()))),
				ReferenceErrorCode.INVALID_REFERENCE_FILE);
		assertCode(() -> referenceService.createFiles(project.getId(), owner.getUserId(), List.of(
				new MockMultipartFile("files", "", "application/pdf", "content".getBytes()))),
				ReferenceErrorCode.INVALID_REFERENCE_FILE);
		assertCode(() -> referenceService.createFiles(project.getId(), owner.getUserId(), List.of(
				file("a".repeat(252) + ".pdf", "application/pdf"))),
				ReferenceErrorCode.INVALID_REFERENCE_FILE);
	}

	@Test
	void FILE과_LINK_합산_제한을_적용하고_Soft_Delete_자료는_제외한다() {
		User owner = saveUser("소유자", "file-limit-owner@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);
		for (int index = 0; index < 9; index++) {
			referenceMaterialRepository.save(ReferenceMaterial.ofLink(
					project.getId(), owner.getUserId(), "link-" + index,
					"https://example.com/" + index, ReferenceStatus.UPLOADING));
		}
		ReferenceMaterial deleted = referenceMaterialRepository.save(ReferenceMaterial.ofLink(
				project.getId(), owner.getUserId(), "deleted", "https://deleted.example.com",
				ReferenceStatus.UPLOADING));
		deleted.softDelete();

		referenceService.createFiles(project.getId(), owner.getUserId(), validFiles());
		assertThat(referenceMaterialRepository.countByProjectId(project.getId())).isEqualTo(10);
		assertCode(() -> referenceService.createFiles(project.getId(), owner.getUserId(), validFiles()),
				ReferenceErrorCode.REFERENCE_LIMIT_EXCEEDED);
	}

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	void 여러_파일_중_업로드가_실패하면_시도한_객체를_보상_삭제하고_DB에_남기지_않는다() {
		User owner = saveUser("소유자", "file-compensation-owner@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);
		clearInvocations(referenceStorage);
		doNothing().doThrow(new ReferenceStorageException("실패", new RuntimeException()))
				.when(referenceStorage)
				.upload(anyString(), any(InputStream.class), anyLong(), anyString());

		assertCode(() -> referenceService.createFiles(project.getId(), owner.getUserId(), List.of(
				file("first.pdf", "application/pdf"), file("second.pdf", "application/pdf"))),
				ReferenceErrorCode.REFERENCE_FILE_UPLOAD_FAILED);

		assertThat(referenceMaterialRepository.countByProjectId(project.getId())).isZero();
		ArgumentCaptor<String> uploadedKeys = ArgumentCaptor.forClass(String.class);
		verify(referenceStorage, times(2))
				.upload(uploadedKeys.capture(), any(InputStream.class), anyLong(), anyString());
		ArgumentCaptor<String> deletedKeys = ArgumentCaptor.forClass(String.class);
		verify(referenceStorage, times(2)).delete(deletedKeys.capture());
		assertThat(deletedKeys.getAllValues()).containsExactly(
				uploadedKeys.getAllValues().get(1),
				uploadedKeys.getAllValues().get(0)
		);
	}

	private void assertCode(ThrowingCallable callable, Object errorCode) {
		assertThatThrownBy(callable::call)
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode()).isEqualTo(errorCode));
	}

	private List<MultipartFile> validFiles() {
		return List.of(file("requirements.pdf", "application/pdf"));
	}

	private MockMultipartFile file(String filename, String contentType) {
		return new MockMultipartFile("files", filename, contentType, "content".getBytes());
	}

	private Project saveProject(User owner) {
		return projectRepository.save(Project.of(owner.getUserId(), "SynQ", null));
	}

	private void saveMember(Project project, User user, ProjectMemberRole role) {
		projectMemberRepository.save(ProjectMember.of(project.getId(), user.getUserId(), role));
	}

	private User saveUser(String name, String email) {
		return userRepository.save(User.ofLocal(name, email, "password-hash"));
	}

	@FunctionalInterface
	private interface ThrowingCallable {
		void call();
	}
}
