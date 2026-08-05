package com.synq.backend.domain.reference.link;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
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
import com.synq.backend.domain.reference.dto.ReferenceLinkCreateRequest;
import com.synq.backend.domain.reference.dto.ReferenceLinkCreateResponse;
import com.synq.backend.domain.reference.entity.ReferenceMaterial;
import com.synq.backend.domain.reference.entity.ReferenceStatus;
import com.synq.backend.domain.reference.repository.ReferenceMaterialRepository;
import com.synq.backend.domain.reference.service.ReferenceService;
import com.synq.backend.domain.user.entity.User;
import com.synq.backend.domain.user.repository.UserRepository;
import com.synq.backend.support.PostgresTestContainer;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * {@code @Transactional} 을 붙이면 안 된다. 테스트 트랜잭션이 롤백되면 커밋이 일어나지 않아
 * AFTER_COMMIT 리스너가 실행되지 않는다. 픽스처는 UUID 로 격리한다.
 */
class LinkIndexingListenerTest extends PostgresTestContainer {

	@Autowired
	ReferenceService referenceService;
	@Autowired
	ReferenceMaterialRepository referenceMaterialRepository;
	@Autowired
	ProjectRepository projectRepository;
	@Autowired
	ProjectMemberRepository projectMemberRepository;
	@Autowired
	UserRepository userRepository;

	@MockitoBean
	LinkPreflightChecker linkPreflightChecker;
	@MockitoBean
	LinkTextExtractor linkTextExtractor;
	// 인터페이스(DocumentIndexer)로 목을 만들면 구현 클래스를 직접 주입받는
	// DocumentReindexController 의 배선이 깨진다. 구현 클래스로 목을 만들어 둘 다 만족시킨다.
	@MockitoBean
	DocumentIndexingService documentIndexer;

	@Test
	void 커밋_이후에_추출한_본문을_인덱서로_넘긴다() {
		given(linkTextExtractor.extract(anyString())).willReturn(Optional.of("추출된 본문"));
		Project project = saveProject();

		ReferenceLinkCreateResponse created = createLink(project);

		await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
				verify(documentIndexer).indexAsync(
						eq(created.referenceId()), eq(project.getId()), eq("추출된 본문")));
	}

	@Test
	void 본문을_추출하지_못하면_READ_FAILED_가_된다() {
		given(linkTextExtractor.extract(anyString())).willReturn(Optional.empty());
		Project project = saveProject();

		ReferenceLinkCreateResponse created = createLink(project);

		await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
				assertThat(referenceMaterialRepository.findById(created.referenceId()))
						.get()
						.extracting(ReferenceMaterial::getStatus)
						.isEqualTo(ReferenceStatus.READ_FAILED));
	}

	private ReferenceLinkCreateResponse createLink(Project project) {
		return referenceService.createLink(
				project.getId(),
				project.getOwnerId(),
				new ReferenceLinkCreateRequest("https://example.com/" + UUID.randomUUID()));
	}

	private Project saveProject() {
		User owner = userRepository.save(User.ofLocal(
				"리스너", UUID.randomUUID() + "@synq.com", "password-hash"));
		Project project = projectRepository.save(
				Project.of(owner.getUserId(), "리스너 테스트", null));
		projectMemberRepository.save(ProjectMember.of(
				project.getId(), owner.getUserId(), ProjectMemberRole.OWNER));
		return project;
	}
}
