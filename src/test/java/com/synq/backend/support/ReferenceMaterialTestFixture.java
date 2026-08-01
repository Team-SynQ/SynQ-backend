package com.synq.backend.support;

import com.synq.backend.domain.project.entity.Project;
import com.synq.backend.domain.project.repository.ProjectRepository;
import com.synq.backend.domain.reference.entity.ReferenceMaterial;
import com.synq.backend.domain.reference.entity.ReferenceStatus;
import com.synq.backend.domain.reference.repository.ReferenceMaterialRepository;
import com.synq.backend.domain.user.entity.User;
import com.synq.backend.domain.user.repository.UserRepository;

import java.util.UUID;

public class ReferenceMaterialTestFixture {

	private final UserRepository userRepository;
	private final ProjectRepository projectRepository;
	private final ReferenceMaterialRepository referenceMaterialRepository;

	public ReferenceMaterialTestFixture(
			UserRepository userRepository,
			ProjectRepository projectRepository,
			ReferenceMaterialRepository referenceMaterialRepository
	) {
		this.userRepository = userRepository;
		this.projectRepository = projectRepository;
		this.referenceMaterialRepository = referenceMaterialRepository;
	}

	public Fixture create() {
		String identifier = UUID.randomUUID().toString();
		User uploader = userRepository.save(
				User.ofLocal("RAG 테스트", identifier + "@synq.com", "password-hash"));
		Project project = projectRepository.save(
				Project.of(uploader.getUserId(), "RAG 테스트 프로젝트", null));
		ReferenceMaterial reference = createReference(project.getId(), uploader.getUserId());
		return new Fixture(reference.getId(), project.getId(), uploader.getUserId());
	}

	public Long createReference(Fixture fixture) {
		return createReference(fixture.projectId(), fixture.uploaderId()).getId();
	}

	private ReferenceMaterial createReference(Long projectId, Long uploaderId) {
		return referenceMaterialRepository.save(ReferenceMaterial.ofLink(
				projectId,
				uploaderId,
				"RAG 테스트 참고자료",
				"https://example.com/" + UUID.randomUUID(),
				ReferenceStatus.AVAILABLE
		));
	}

	public record Fixture(
			Long referenceMaterialId,
			Long projectId,
			Long uploaderId
	) {
	}
}
