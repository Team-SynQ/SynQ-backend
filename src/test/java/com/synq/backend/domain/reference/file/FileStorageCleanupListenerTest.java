package com.synq.backend.domain.reference.file;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.synq.backend.domain.ai.rag.DocumentIndexingService;
import com.synq.backend.domain.project.entity.Project;
import com.synq.backend.domain.project.entity.ProjectMember;
import com.synq.backend.domain.project.entity.ProjectMemberRole;
import com.synq.backend.domain.project.repository.ProjectMemberRepository;
import com.synq.backend.domain.project.repository.ProjectRepository;
import com.synq.backend.domain.reference.entity.ReferenceFileExtension;
import com.synq.backend.domain.reference.entity.ReferenceMaterial;
import com.synq.backend.domain.reference.entity.ReferenceStatus;
import com.synq.backend.domain.reference.event.ReferenceFileDeletedEvent;
import com.synq.backend.domain.reference.repository.ReferenceMaterialRepository;
import com.synq.backend.domain.reference.service.ReferenceService;
import com.synq.backend.domain.reference.storage.ReferenceStorage;
import com.synq.backend.domain.reference.storage.ReferenceStorageException;
import com.synq.backend.domain.user.entity.User;
import com.synq.backend.domain.user.repository.UserRepository;
import com.synq.backend.support.PostgresTestContainer;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * 실제 트랜잭션 commit과 rollback에 따른 AFTER_COMMIT S3 정리 검증.
 */
class FileStorageCleanupListenerTest extends PostgresTestContainer {

	@Autowired
	private ReferenceService referenceService;
	@Autowired
	private ProjectRepository projectRepository;
	@Autowired
	private ProjectMemberRepository projectMemberRepository;
	@Autowired
	private ReferenceMaterialRepository referenceMaterialRepository;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private PlatformTransactionManager transactionManager;
	@Autowired
	private FileStorageCleanupListener cleanupListener;

	@MockitoBean
	private ReferenceStorage referenceStorage;
	@MockitoBean
	private DocumentIndexingService documentIndexer;

	@BeforeEach
	void resetMocks() {
		reset(referenceStorage, documentIndexer);
	}

	@Test
	void FILE_삭제가_commit되면_S3_원본을_정확히_한_번_삭제한다() {
		Fixture fixture = saveFixture(true);

		referenceService.delete(fixture.projectId(), fixture.referenceId(), fixture.userId());

		verify(referenceStorage, times(1)).delete(fixture.storageKey());
		verify(documentIndexer).deleteIndex(fixture.referenceId());
		assertThat(referenceMaterialRepository.findById(fixture.referenceId())).isEmpty();
	}

	@Test
	void FILE_삭제_트랜잭션이_rollback되면_S3_원본을_삭제하지_않는다() {
		Fixture fixture = saveFixture(true);
		TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

		transactionTemplate.executeWithoutResult(status -> {
			referenceService.delete(fixture.projectId(), fixture.referenceId(), fixture.userId());
			verify(referenceStorage, never()).delete(fixture.storageKey());
			status.setRollbackOnly();
		});

		verify(referenceStorage, never()).delete(fixture.storageKey());
		assertThat(referenceMaterialRepository.findById(fixture.referenceId())).isPresent();
	}

	@Test
	void LINK_삭제가_commit되어도_S3_삭제를_호출하지_않는다() {
		Fixture fixture = saveFixture(false);

		referenceService.delete(fixture.projectId(), fixture.referenceId(), fixture.userId());

		verify(referenceStorage, never()).delete(org.mockito.ArgumentMatchers.anyString());
		verify(documentIndexer).deleteIndex(fixture.referenceId());
	}

	@Test
	void S3_원본_삭제가_실패해도_DB_Soft_Delete_성공을_유지한다() {
		Fixture fixture = saveFixture(true);
		doThrow(new ReferenceStorageException("AWS_SECRET_ACCESS_KEY=secret file-content"))
				.when(referenceStorage).delete(fixture.storageKey());

		assertThatCode(() -> referenceService.delete(
				fixture.projectId(), fixture.referenceId(), fixture.userId()))
				.doesNotThrowAnyException();

		verify(referenceStorage).delete(fixture.storageKey());
		assertThat(referenceMaterialRepository.findById(fixture.referenceId())).isEmpty();
	}

	@Test
	void 인덱스_삭제가_실패해_트랜잭션이_rollback되면_S3_원본을_삭제하지_않는다() {
		Fixture fixture = saveFixture(true);
		doThrow(new IllegalStateException("인덱스 삭제 실패"))
				.when(documentIndexer).deleteIndex(fixture.referenceId());

		assertThatThrownBy(() -> referenceService.delete(
				fixture.projectId(), fixture.referenceId(), fixture.userId()))
				.isInstanceOf(IllegalStateException.class);

		verify(referenceStorage, never()).delete(anyString());
		assertThat(referenceMaterialRepository.findById(fixture.referenceId())).isPresent();
	}

	@Test
	void 같은_정리_이벤트가_중복되어도_S3_삭제만_반복하고_예외를_전파하지_않는다() {
		ReferenceFileDeletedEvent event = new ReferenceFileDeletedEvent(
				1L, 2L, "references/2/already-deleted.pdf");

		assertThatCode(() -> {
			cleanupListener.handle(event);
			cleanupListener.handle(event);
		}).doesNotThrowAnyException();

		verify(referenceStorage, times(2)).delete(event.storageKey());
	}

	@Test
	void S3_삭제_실패_로그에_Credential과_파일_내용을_남기지_않는다() {
		String secret = "AWS_SECRET_ACCESS_KEY=secret";
		String fileContent = "confidential-file-content";
		ReferenceFileDeletedEvent event = new ReferenceFileDeletedEvent(
				3L, 4L, "references/4/failure.pdf");
		doThrow(new ReferenceStorageException(secret + " " + fileContent))
				.when(referenceStorage).delete(event.storageKey());
		Logger logger = (Logger) LoggerFactory.getLogger(FileStorageCleanupListener.class);
		ListAppender<ILoggingEvent> appender = new ListAppender<>();
		appender.start();
		logger.addAppender(appender);

		try {
			cleanupListener.handle(event);

			assertThat(appender.list)
					.extracting(ILoggingEvent::getFormattedMessage)
					.allSatisfy(message -> {
						assertThat(message).doesNotContain(secret);
						assertThat(message).doesNotContain(fileContent);
					});
		} finally {
			logger.detachAppender(appender);
			appender.stop();
		}
	}

	private Fixture saveFixture(boolean file) {
		return new TransactionTemplate(transactionManager).execute(status -> {
			String unique = UUID.randomUUID().toString();
			User owner = userRepository.save(User.ofLocal(
					"S3 정리", unique + "@synq.com", "password-hash"));
			Project project = projectRepository.save(
					Project.of(owner.getUserId(), "S3 정리 테스트", null));
			projectMemberRepository.save(ProjectMember.of(
					project.getId(), owner.getUserId(), ProjectMemberRole.OWNER));
			String storageKey = "references/" + project.getId() + "/" + unique + ".pdf";
			ReferenceMaterial reference = file
					? ReferenceMaterial.ofFile(
							project.getId(), owner.getUserId(), "document.pdf", 1024L,
							storageKey, ReferenceFileExtension.PDF, ReferenceStatus.AVAILABLE)
					: ReferenceMaterial.ofLink(
							project.getId(), owner.getUserId(), "example.com",
							"https://example.com/" + unique, ReferenceStatus.AVAILABLE);
			referenceMaterialRepository.save(reference);
			return new Fixture(
					project.getId(), owner.getUserId(), reference.getId(), file ? storageKey : null);
		});
	}

	private record Fixture(Long projectId, Long userId, Long referenceId, String storageKey) {
	}
}
