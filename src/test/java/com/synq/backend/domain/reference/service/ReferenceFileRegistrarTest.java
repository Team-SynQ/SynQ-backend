package com.synq.backend.domain.reference.service;

import com.synq.backend.domain.project.code.ProjectErrorCode;
import com.synq.backend.domain.project.entity.Project;
import com.synq.backend.domain.project.repository.ProjectMemberRepository;
import com.synq.backend.domain.project.repository.ProjectRepository;
import com.synq.backend.domain.reference.entity.ReferenceFileExtension;
import com.synq.backend.domain.reference.file.ExtractedFile;
import com.synq.backend.domain.reference.repository.ReferenceMaterialRepository;
import com.synq.backend.domain.reference.storage.ReferenceStorage;
import com.synq.backend.domain.user.entity.User;
import com.synq.backend.global.apipayload.exception.GeneralException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReferenceFileRegistrarTest {

	private static final Long PROJECT_ID = 1L;
	private static final Long USER_ID = 2L;

	@Mock
	private ReferenceMaterialRepository referenceMaterialRepository;

	@Mock
	private ProjectRepository projectRepository;

	@Mock
	private ProjectMemberRepository projectMemberRepository;

	@Mock
	private ReferenceStorage referenceStorage;

	@InjectMocks
	private ReferenceFileRegistrar referenceFileRegistrar;

	@Test
	void 추출_도중_멤버십이_해제되면_등록을_거부한다() {
		// ReferenceService 의 사전 검사는 통과했지만, 수 초 걸리는 추출 사이에 해제된 상황이다.
		// 락을 잡은 뒤 다시 보지 않으면 해제된 사용자의 파일이 그대로 등록된다.
		when(projectRepository.findByIdForUpdate(PROJECT_ID))
				.thenReturn(Optional.of(Project.of(USER_ID, "SynQ", null)));
		when(projectMemberRepository.existsByProjectIdAndUserId(PROJECT_ID, USER_ID)).thenReturn(false);

		assertThatThrownBy(() -> register())
				.isInstanceOfSatisfying(GeneralException.class, exception ->
						assertThat(exception.getCode()).isEqualTo(ProjectErrorCode.NOT_PROJECT_MEMBER));

		verifyNoInteractions(referenceStorage);
		verify(referenceMaterialRepository, never()).saveAllAndFlush(any());
	}

	@Test
	void 멤버십_검사는_제한_검사보다_앞선다() {
		// 순서가 뒤집히면 비멤버가 프로젝트의 참고자료 개수를 알아낼 수 있다.
		when(projectRepository.findByIdForUpdate(PROJECT_ID))
				.thenReturn(Optional.of(Project.of(USER_ID, "SynQ", null)));
		when(projectMemberRepository.existsByProjectIdAndUserId(PROJECT_ID, USER_ID)).thenReturn(false);

		assertThatThrownBy(() -> register()).isInstanceOf(GeneralException.class);

		verify(referenceMaterialRepository, never()).countByProjectId(anyLong());
	}

	private void register() {
		User uploader = User.ofLocal("업로더", "file-registrar@synq.com", "password-hash");
		MockMultipartFile file = new MockMultipartFile(
				"files", "requirements.pdf", "application/pdf", "content".getBytes());
		referenceFileRegistrar.register(PROJECT_ID, USER_ID, uploader, List.of(
				new ExtractedFile(file, "requirements.pdf", ReferenceFileExtension.PDF,
						"application/pdf", "추출된 본문입니다.")));
	}
}
