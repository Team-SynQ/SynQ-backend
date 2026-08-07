package com.synq.backend.domain.reference.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.synq.backend.domain.ai.rag.DocumentIndexingService;
import com.synq.backend.domain.project.entity.Project;
import com.synq.backend.domain.project.entity.ProjectMember;
import com.synq.backend.domain.project.entity.ProjectMemberRole;
import com.synq.backend.domain.project.repository.ProjectMemberRepository;
import com.synq.backend.domain.project.repository.ProjectRepository;
import com.synq.backend.domain.reference.dto.ReferenceFileCreateResponse;
import com.synq.backend.domain.reference.service.ReferenceService;
import com.synq.backend.domain.reference.storage.ReferenceStorage;
import com.synq.backend.domain.user.entity.User;
import com.synq.backend.domain.user.repository.UserRepository;
import com.synq.backend.support.PostgresTestContainer;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * {@code @Transactional} 을 붙이면 안 된다. 테스트 트랜잭션이 롤백되면 커밋이 일어나지 않아
 * AFTER_COMMIT 리스너가 실행되지 않는다. 픽스처는 UUID 로 격리한다.
 */
class FileIndexingListenerTest extends PostgresTestContainer {

	private static final String EXTRACTED =
			"추출된 본문입니다. 최소 길이 조건을 넘기기 위한 충분히 긴 문장을 넣습니다.";

	@Autowired
	private ReferenceService referenceService;
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
	// 인터페이스(DocumentIndexer)로 목을 만들면 구현 클래스를 직접 주입받는
	// DocumentReindexController 의 배선이 깨진다. 구현 클래스로 목을 만들어 둘 다 만족시킨다.
	@MockitoBean
	private DocumentIndexingService documentIndexer;

	@BeforeEach
	void stubExtractor() {
		given(fileTextExtractor.extract(any(), anyString())).willReturn(EXTRACTED);
	}

	@Test
	void 커밋_이후에_추출한_본문을_인덱서로_넘긴다() {
		Project project = saveProject();

		ReferenceFileCreateResponse created = referenceService.createFiles(
				project.getId(), project.getOwnerId(), List.of(file("design.pdf")));

		Long referenceId = created.references().get(0).referenceId();
		await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
				verify(documentIndexer).indexAsync(
						eq(referenceId), eq(project.getId()), eq(EXTRACTED)));
	}

	@Test
	void 파일마다_이벤트가_하나씩_발행된다() {
		Project project = saveProject();

		ReferenceFileCreateResponse created = referenceService.createFiles(
				project.getId(), project.getOwnerId(),
				List.of(file("first.pdf"), file("second.pdf")));

		assertThat(created.references()).hasSize(2);
		await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
			verify(documentIndexer).indexAsync(
					eq(created.references().get(0).referenceId()), eq(project.getId()), eq(EXTRACTED));
			verify(documentIndexer).indexAsync(
					eq(created.references().get(1).referenceId()), eq(project.getId()), eq(EXTRACTED));
		});
	}

	private MockMultipartFile file(String name) {
		return new MockMultipartFile("files", name, "application/pdf", "content".getBytes());
	}

	private Project saveProject() {
		User owner = userRepository.save(User.ofLocal(
				"리스너", UUID.randomUUID() + "@synq.com", "password-hash"));
		Project project = projectRepository.save(
				Project.of(owner.getUserId(), "파일 리스너 테스트", null));
		projectMemberRepository.save(ProjectMember.of(
				project.getId(), owner.getUserId(), ProjectMemberRole.OWNER));
		return project;
	}
}
