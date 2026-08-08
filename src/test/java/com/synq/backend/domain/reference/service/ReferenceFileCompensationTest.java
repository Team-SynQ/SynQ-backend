package com.synq.backend.domain.reference.service;

import com.synq.backend.domain.project.entity.Project;
import com.synq.backend.domain.project.repository.ProjectMemberRepository;
import com.synq.backend.domain.project.repository.ProjectRepository;
import com.synq.backend.domain.reference.code.ReferenceErrorCode;
import com.synq.backend.domain.reference.entity.ReferenceFileExtension;
import com.synq.backend.domain.reference.file.ExtractedFile;
import com.synq.backend.domain.reference.repository.ReferenceMaterialRepository;
import com.synq.backend.domain.reference.storage.ReferenceStorage;
import com.synq.backend.domain.reference.storage.ReferenceStorageException;
import com.synq.backend.domain.user.entity.User;
import com.synq.backend.global.apipayload.exception.GeneralException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReferenceFileCompensationTest {

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
	void DB_저장에_실패하면_업로드한_객체를_보상_삭제한다() {
		givenRegistrableProject();
		when(referenceMaterialRepository.saveAllAndFlush(anyList()))
				.thenThrow(new DataIntegrityViolationException("DB 저장 실패"));

		assertThatThrownBy(() -> register(pdf()))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode())
								.isEqualTo(ReferenceErrorCode.REFERENCE_FILE_UPLOAD_FAILED));

		ArgumentCaptor<String> storageKey = ArgumentCaptor.forClass(String.class);
		verify(referenceStorage).delete(storageKey.capture());
		assertThat(storageKey.getValue()).startsWith("references/1/").endsWith(".pdf");
	}

	@Test
	void 보상_삭제가_실패해도_최초_업로드_예외를_유지한다() {
		givenRegistrableProject();
		ReferenceStorageException uploadFailure = new ReferenceStorageException("최초 업로드 실패");
		doThrow(uploadFailure).when(referenceStorage)
				.upload(anyString(), any(), anyLong(), anyString());
		doThrow(new ReferenceStorageException("보상 삭제 실패"))
				.when(referenceStorage).delete(anyString());

		assertThatThrownBy(() -> register(pdf()))
				.isInstanceOfSatisfying(GeneralException.class, exception -> {
					assertThat(exception.getCode())
							.isEqualTo(ReferenceErrorCode.REFERENCE_FILE_UPLOAD_FAILED);
					assertThat(exception.getCause()).isSameAs(uploadFailure);
				});
		verify(referenceStorage).delete(anyString());
	}

	private void givenRegistrableProject() {
		when(projectRepository.findByIdForUpdate(PROJECT_ID))
				.thenReturn(Optional.of(Project.of(USER_ID, "SynQ", null)));
		when(projectMemberRepository.existsByProjectIdAndUserId(PROJECT_ID, USER_ID)).thenReturn(true);
		when(referenceMaterialRepository.countByProjectId(PROJECT_ID)).thenReturn(0L);
	}

	private void register(ExtractedFile file) {
		User uploader = User.ofLocal("업로더", "file-compensation@synq.com", "password-hash");
		referenceFileRegistrar.register(PROJECT_ID, USER_ID, uploader, List.of(file));
	}

	private ExtractedFile pdf() {
		MockMultipartFile file = new MockMultipartFile(
				"files", "requirements.pdf", "application/pdf", "content".getBytes());
		return new ExtractedFile(file, "requirements.pdf", ReferenceFileExtension.PDF,
				"application/pdf", "추출된 본문입니다.");
	}
}
