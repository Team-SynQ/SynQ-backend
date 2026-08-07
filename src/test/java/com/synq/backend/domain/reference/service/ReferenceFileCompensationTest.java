package com.synq.backend.domain.reference.service;

import com.synq.backend.domain.project.entity.Project;
import com.synq.backend.domain.project.repository.ProjectMemberRepository;
import com.synq.backend.domain.project.repository.ProjectRepository;
import com.synq.backend.domain.reference.code.ReferenceErrorCode;
import com.synq.backend.domain.reference.repository.ReferenceMaterialRepository;
import com.synq.backend.domain.reference.storage.ReferenceStorage;
import com.synq.backend.domain.reference.storage.ReferenceStorageException;
import com.synq.backend.domain.user.entity.User;
import com.synq.backend.domain.user.repository.UserRepository;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReferenceFileCompensationTest {

	@Mock
	private ReferenceMaterialRepository referenceMaterialRepository;

	@Mock
	private ProjectRepository projectRepository;

	@Mock
	private ProjectMemberRepository projectMemberRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private ReferenceStorage referenceStorage;

	@InjectMocks
	private ReferenceService referenceService;

	@Test
	void DB_저장에_실패하면_업로드한_객체를_보상_삭제한다() {
		Long projectId = 1L;
		Long userId = 2L;
		User uploader = User.ofLocal("업로더", "file-db-failure@synq.com", "password-hash");
		Project project = Project.of(userId, "SynQ", null);
		when(userRepository.findById(userId)).thenReturn(Optional.of(uploader));
		when(projectRepository.findByIdForUpdate(projectId)).thenReturn(Optional.of(project));
		when(projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)).thenReturn(true);
		when(referenceMaterialRepository.countByProjectId(projectId)).thenReturn(0L);
		when(referenceMaterialRepository.saveAllAndFlush(anyList()))
				.thenThrow(new DataIntegrityViolationException("DB 저장 실패"));
		MockMultipartFile file = new MockMultipartFile(
				"files", "requirements.pdf", "application/pdf", "content".getBytes());

		assertThatThrownBy(() -> referenceService.createFiles(projectId, userId, List.of(file)))
				.isInstanceOfSatisfying(GeneralException.class,
						exception -> assertThat(exception.getCode())
								.isEqualTo(ReferenceErrorCode.REFERENCE_FILE_UPLOAD_FAILED));

		ArgumentCaptor<String> storageKey = ArgumentCaptor.forClass(String.class);
		verify(referenceStorage).delete(storageKey.capture());
		assertThat(storageKey.getValue()).startsWith("references/1/").endsWith(".pdf");
	}

	@Test
	void 보상_삭제가_실패해도_최초_업로드_예외를_유지한다() {
		Long projectId = 1L;
		Long userId = 2L;
		User uploader = User.ofLocal("업로더", "file-compensation-failure@synq.com", "password-hash");
		Project project = Project.of(userId, "SynQ", null);
		when(userRepository.findById(userId)).thenReturn(Optional.of(uploader));
		when(projectRepository.findByIdForUpdate(projectId)).thenReturn(Optional.of(project));
		when(projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)).thenReturn(true);
		when(referenceMaterialRepository.countByProjectId(projectId)).thenReturn(0L);
		ReferenceStorageException uploadFailure = new ReferenceStorageException("최초 업로드 실패");
		doThrow(uploadFailure).when(referenceStorage)
				.upload(anyString(), any(), anyLong(), anyString());
		doThrow(new ReferenceStorageException("보상 삭제 실패"))
				.when(referenceStorage).delete(anyString());
		MockMultipartFile file = new MockMultipartFile(
				"files", "requirements.pdf", "application/pdf", "content".getBytes());

		assertThatThrownBy(() -> referenceService.createFiles(projectId, userId, List.of(file)))
				.isInstanceOfSatisfying(GeneralException.class, exception -> {
					assertThat(exception.getCode()).isEqualTo(ReferenceErrorCode.REFERENCE_FILE_UPLOAD_FAILED);
					assertThat(exception.getCause()).isSameAs(uploadFailure);
				});
		verify(referenceStorage).delete(anyString());
	}
}
