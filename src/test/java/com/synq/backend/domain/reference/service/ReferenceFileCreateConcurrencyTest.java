package com.synq.backend.domain.reference.service;

import com.synq.backend.domain.project.entity.Project;
import com.synq.backend.domain.project.entity.ProjectMember;
import com.synq.backend.domain.project.entity.ProjectMemberRole;
import com.synq.backend.domain.project.repository.ProjectMemberRepository;
import com.synq.backend.domain.project.repository.ProjectRepository;
import com.synq.backend.domain.reference.code.ReferenceErrorCode;
import com.synq.backend.domain.reference.dto.ReferenceLinkCreateRequest;
import com.synq.backend.domain.reference.entity.ReferenceMaterial;
import com.synq.backend.domain.reference.entity.ReferenceStatus;
import com.synq.backend.domain.reference.repository.ReferenceMaterialRepository;
import com.synq.backend.domain.reference.storage.ReferenceStorage;
import com.synq.backend.domain.user.entity.User;
import com.synq.backend.domain.user.repository.UserRepository;
import com.synq.backend.global.apipayload.exception.GeneralException;
import com.synq.backend.support.PostgresTestContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

class ReferenceFileCreateConcurrencyTest extends PostgresTestContainer {

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

	@Test
	void 활성_자료가_9개일_때_파일과_링크_동시_등록은_하나만_성공한다() throws Exception {
		String identifier = UUID.randomUUID().toString();
		User owner = userRepository.save(
				User.ofLocal("동시성 테스트", identifier + "@synq.com", "password-hash"));
		Project project = projectRepository.save(Project.of(owner.getUserId(), "동시성 프로젝트", null));
		projectMemberRepository.save(ProjectMember.of(
				project.getId(), owner.getUserId(), ProjectMemberRole.OWNER));
		for (int index = 0; index < 9; index++) {
			referenceMaterialRepository.save(ReferenceMaterial.ofLink(
					project.getId(), owner.getUserId(), "existing-" + index,
					"https://existing-" + index + ".example.com", ReferenceStatus.UPLOADING));
		}

		CountDownLatch ready = new CountDownLatch(2);
		CountDownLatch start = new CountDownLatch(1);
		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			Future<Boolean> fileResult = executor.submit(() -> registerFile(project, owner, ready, start));
			Future<Boolean> linkResult = executor.submit(() -> registerLink(project, owner, ready, start));
			ready.await();
			start.countDown();

			assertThat(List.of(fileResult.get(), linkResult.get()))
					.containsExactlyInAnyOrder(true, false);
			assertThat(referenceMaterialRepository.countByProjectId(project.getId())).isEqualTo(10);
		} finally {
			executor.shutdownNow();
		}
	}

	private boolean registerFile(Project project, User owner, CountDownLatch ready, CountDownLatch start)
			throws InterruptedException {
		ready.countDown();
		start.await();
		return register(() -> referenceService.createFiles(project.getId(), owner.getUserId(), List.of(
				new MockMultipartFile("files", "concurrent.pdf", "application/pdf", "content".getBytes()))));
	}

	private boolean registerLink(Project project, User owner, CountDownLatch ready, CountDownLatch start)
			throws InterruptedException {
		ready.countDown();
		start.await();
		return register(() -> referenceService.createLink(
				project.getId(), owner.getUserId(), new ReferenceLinkCreateRequest("https://new.example.com")));
	}

	private boolean register(Runnable registration) {
		try {
			registration.run();
			return true;
		} catch (GeneralException exception) {
			assertThat(exception.getCode()).isEqualTo(ReferenceErrorCode.REFERENCE_LIMIT_EXCEEDED);
			return false;
		}
	}
}
