package com.synq.backend.domain.reference.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.synq.backend.domain.project.code.ProjectErrorCode;
import com.synq.backend.domain.project.entity.Project;
import com.synq.backend.domain.project.entity.ProjectMember;
import com.synq.backend.domain.project.entity.ProjectMemberRole;
import com.synq.backend.domain.project.repository.ProjectMemberRepository;
import com.synq.backend.domain.project.repository.ProjectRepository;
import com.synq.backend.domain.reference.file.FileExtractionFailureReason;
import com.synq.backend.domain.reference.file.FileTextExtractionException;
import com.synq.backend.domain.reference.file.FileTextExtractor;
import com.synq.backend.domain.reference.file.ReferenceFileExtractionException;
import com.synq.backend.domain.reference.repository.ReferenceMaterialRepository;
import com.synq.backend.domain.reference.storage.ReferenceStorage;
import com.synq.backend.domain.user.entity.User;
import com.synq.backend.domain.user.repository.UserRepository;
import com.synq.backend.global.apipayload.exception.GeneralException;
import com.synq.backend.support.PostgresTestContainer;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Transactional
class ReferenceFileExtractionTest extends PostgresTestContainer {

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
	@MockitoBean
	private FileTextExtractor fileTextExtractor;

	@Test
	void 추출에_실패하면_400이고_S3와_DB에_아무것도_남지_않는다() {
		Project project = saveProject();
		given(fileTextExtractor.extract(any(), eq("scan.pdf")))
				.willThrow(new FileTextExtractionException(
						FileExtractionFailureReason.NO_TEXT_LAYER, null));

		assertThatThrownBy(() -> referenceService.createFiles(
				project.getId(), project.getOwnerId(), List.of(file("scan.pdf"))))
				.isInstanceOfSatisfying(ReferenceFileExtractionException.class, exception ->
						assertThat(exception.getFailures())
								.singleElement()
								.satisfies(failure -> {
									assertThat(failure.fileName()).isEqualTo("scan.pdf");
									assertThat(failure.reason()).isEqualTo("NO_TEXT_LAYER");
								}));

		assertThat(referenceMaterialRepository.countByProjectId(project.getId())).isZero();
		verify(referenceStorage, never()).upload(any(), any(), anyLong(), any());
	}

	@Test
	void 배치에서_실패한_파일을_모두_모아_거절한다() {
		Project project = saveProject();
		given(fileTextExtractor.extract(any(), eq("good.pdf")))
				.willReturn("정상적으로 추출된 본문입니다. 최소 길이를 넘기기 위한 문장입니다.");
		given(fileTextExtractor.extract(any(), eq("locked.pdf")))
				.willThrow(new FileTextExtractionException(
						FileExtractionFailureReason.ENCRYPTED, null));
		given(fileTextExtractor.extract(any(), eq("broken.docx")))
				.willThrow(new FileTextExtractionException(
						FileExtractionFailureReason.CORRUPTED, null));

		assertThatThrownBy(() -> referenceService.createFiles(
				project.getId(), project.getOwnerId(),
				List.of(file("good.pdf"), file("locked.pdf"), docx("broken.docx"))))
				.isInstanceOfSatisfying(ReferenceFileExtractionException.class, exception ->
						assertThat(exception.getFailures())
								.extracting(failure -> failure.fileName() + ":" + failure.reason())
								.containsExactly("locked.pdf:ENCRYPTED", "broken.docx:CORRUPTED"));

		assertThat(referenceMaterialRepository.countByProjectId(project.getId())).isZero();
	}

	@Test
	void 비멤버는_추출을_유발할_수_없다() {
		// 파싱이 CPU 작업이므로 인가를 먼저 통과해야 한다.
		Project project = saveProject();
		User outsider = saveUser("외부");

		assertThatThrownBy(() -> referenceService.createFiles(
				project.getId(), outsider.getUserId(), List.of(file("any.pdf"))))
				.isInstanceOfSatisfying(GeneralException.class, exception ->
						assertThat(exception.getCode()).isEqualTo(ProjectErrorCode.NOT_PROJECT_MEMBER));

		verify(fileTextExtractor, never()).extract(any(), any());
	}

	private MultipartFile file(String name) {
		return new MockMultipartFile("files", name, "application/pdf", "content".getBytes());
	}

	private MultipartFile docx(String name) {
		return new MockMultipartFile("files", name,
				"application/vnd.openxmlformats-officedocument.wordprocessingml.document",
				"content".getBytes());
	}

	private Project saveProject() {
		User owner = saveUser("소유자");
		Project project = projectRepository.save(Project.of(owner.getUserId(), "추출 테스트", null));
		projectMemberRepository.save(ProjectMember.of(
				project.getId(), owner.getUserId(), ProjectMemberRole.OWNER));
		return project;
	}

	private User saveUser(String name) {
		return userRepository.save(User.ofLocal(
				name, UUID.randomUUID() + "@synq.com", "password-hash"));
	}
}
