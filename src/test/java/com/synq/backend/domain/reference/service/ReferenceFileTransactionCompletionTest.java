package com.synq.backend.domain.reference.service;

import com.synq.backend.domain.project.entity.Project;
import com.synq.backend.domain.project.entity.ProjectMember;
import com.synq.backend.domain.project.entity.ProjectMemberRole;
import com.synq.backend.domain.project.repository.ProjectMemberRepository;
import com.synq.backend.domain.project.repository.ProjectRepository;
import com.synq.backend.domain.reference.repository.ReferenceMaterialRepository;
import com.synq.backend.domain.reference.storage.ReferenceStorage;
import com.synq.backend.domain.user.entity.User;
import com.synq.backend.domain.user.repository.UserRepository;
import com.synq.backend.support.PostgresTestContainer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class ReferenceFileTransactionCompletionTest extends PostgresTestContainer {

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
	private TransactionTemplate transactionTemplate;

	@MockitoBean
	private ReferenceStorage referenceStorage;

	@Test
	void 트랜잭션_commit이_성공하면_보상_삭제하지_않는다() {
		Fixture fixture = createFixture("commit-success");

		referenceService.createFiles(
				fixture.projectId(), fixture.userId(), List.of(file("success.pdf")));

		verify(referenceStorage, never()).delete(anyString());
		assertThat(referenceMaterialRepository.countByProjectId(fixture.projectId())).isEqualTo(1);
	}

	@Test
	void flush_성공_후_트랜잭션이_rollback되면_storage_key를_역순_보상_삭제한다() {
		Fixture fixture = createFixture("completion-rollback");

		transactionTemplate.executeWithoutResult(status -> {
			referenceService.createFiles(fixture.projectId(), fixture.userId(), List.of(
					file("first.pdf"), file("second.pdf")));
			status.setRollbackOnly();
		});

		assertThat(referenceMaterialRepository.countByProjectId(fixture.projectId())).isZero();
		assertReverseCompensationOrder(2);
	}

	@Test
	void flush_성공_후_commit단계가_실패해도_업로드한_객체를_보상_삭제한다() {
		Fixture fixture = createFixture("commit-failure");
		RuntimeException commitFailure = new RuntimeException("commit 단계 실패");

		assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
			referenceService.createFiles(
					fixture.projectId(), fixture.userId(), List.of(file("commit-failure.pdf")));
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void beforeCommit(boolean readOnly) {
					throw commitFailure;
				}
			});
		})).isSameAs(commitFailure);

		assertThat(referenceMaterialRepository.countByProjectId(fixture.projectId())).isZero();
		verify(referenceStorage).delete(anyString());
	}

	private void assertReverseCompensationOrder(int uploadCount) {
		ArgumentCaptor<String> uploadedKeys = ArgumentCaptor.forClass(String.class);
		verify(referenceStorage, times(uploadCount))
				.upload(uploadedKeys.capture(), any(InputStream.class), anyLong(), anyString());
		ArgumentCaptor<String> deletedKeys = ArgumentCaptor.forClass(String.class);
		verify(referenceStorage, times(uploadCount)).delete(deletedKeys.capture());
		assertThat(deletedKeys.getAllValues()).containsExactly(
				uploadedKeys.getAllValues().get(1),
				uploadedKeys.getAllValues().get(0)
		);
	}

	private Fixture createFixture(String name) {
		String identifier = name + "-" + UUID.randomUUID();
		User user = userRepository.save(
				User.ofLocal("트랜잭션 테스트", identifier + "@synq.com", "password-hash"));
		Project project = projectRepository.save(Project.of(user.getUserId(), "SynQ", null));
		projectMemberRepository.save(ProjectMember.of(
				project.getId(), user.getUserId(), ProjectMemberRole.OWNER));
		return new Fixture(project.getId(), user.getUserId());
	}

	private MockMultipartFile file(String name) {
		return new MockMultipartFile("files", name, "application/pdf", "content".getBytes());
	}

	private record Fixture(Long projectId, Long userId) {
	}
}
