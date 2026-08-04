package com.synq.backend.domain.reference.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import com.synq.backend.domain.ai.rag.port.ReferenceMaterialPort;
import com.synq.backend.domain.project.entity.Project;
import com.synq.backend.domain.project.repository.ProjectRepository;
import com.synq.backend.domain.reference.entity.ReferenceFileExtension;
import com.synq.backend.domain.reference.entity.ReferenceMaterial;
import com.synq.backend.domain.reference.entity.ReferenceStatus;
import com.synq.backend.domain.reference.link.LinkTextExtractor;
import com.synq.backend.domain.reference.repository.ReferenceMaterialRepository;
import com.synq.backend.domain.user.entity.User;
import com.synq.backend.domain.user.repository.UserRepository;
import com.synq.backend.support.PostgresTestContainer;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class ReferenceMaterialAdapterTest extends PostgresTestContainer {

	@Autowired
	ReferenceMaterialPort port;
	@Autowired
	ReferenceMaterialRepository referenceMaterialRepository;
	@Autowired
	ProjectRepository projectRepository;
	@Autowired
	UserRepository userRepository;
	@Autowired
	EntityManager entityManager;

	@MockitoBean
	LinkTextExtractor linkTextExtractor;

	@Test
	void 인덱싱이_끝나면_AVAILABLE_이_된다() {
		ReferenceMaterial reference = saveLink();

		port.markCompleted(reference.getId());

		assertThat(reference.getStatus()).isEqualTo(ReferenceStatus.AVAILABLE);
	}

	@Test
	void 인덱싱이_실패하면_READ_FAILED_가_된다() {
		ReferenceMaterial reference = saveLink();

		port.markFailed(reference.getId(), "본문을 추출하지 못했습니다.");

		assertThat(reference.getStatus()).isEqualTo(ReferenceStatus.READ_FAILED);
	}

	@Test
	void 처리중_표시는_상태를_바꾸지_않는다() {
		// status 가 3종뿐이라 PROCESSING 을 표현할 수 없고 UPLOADING 이 그 역할을 겸한다.
		ReferenceMaterial reference = saveLink();

		port.markProcessing(reference.getId());

		assertThat(reference.getStatus()).isEqualTo(ReferenceStatus.UPLOADING);
	}

	@Test
	void 인덱싱_도중_삭제된_자료는_상태를_되살리지_않는다() {
		ReferenceMaterial reference = saveLink();
		reference.softDelete();
		// @SQLRestriction 은 DB 조회에만 걸린다. 1차 캐시를 비워야 실제 동작이 검증된다.
		entityManager.flush();
		entityManager.clear();

		assertThatCode(() -> port.markCompleted(reference.getId())).doesNotThrowAnyException();

		assertThat(referenceMaterialRepository.findById(reference.getId())).isEmpty();
	}

	@Test
	void 프로젝트_ID_를_돌려준다() {
		ReferenceMaterial reference = saveLink();

		assertThat(port.findProjectId(reference.getId()))
				.contains(reference.getProjectId());
	}

	@Test
	void 없는_참고자료의_프로젝트_ID_는_비어_있다() {
		assertThat(port.findProjectId(999_999L)).isEmpty();
	}

	@Test
	void 링크는_URL_에서_본문을_다시_뽑아온다() {
		// extracted_text 컬럼을 두지 않는다. 원본이 URL 이라 재fetch 로 재현된다.
		ReferenceMaterial reference = saveLink();
		given(linkTextExtractor.extract(anyString())).willReturn(Optional.of("본문입니다."));

		assertThat(port.findIndexableText(reference.getId())).contains("본문입니다.");
	}

	@Test
	void 파일_참고자료는_비어_있다() {
		// 파일 업로드와 텍스트 추출은 아직 구현되지 않았다.
		ReferenceMaterial file = referenceMaterialRepository.save(ReferenceMaterial.ofFile(
				saveProject().getId(), saveUser().getUserId(), "설계.pdf",
				1024L, ReferenceFileExtension.PDF, ReferenceStatus.UPLOADING));

		assertThat(port.findIndexableText(file.getId())).isEmpty();
	}

	private ReferenceMaterial saveLink() {
		return referenceMaterialRepository.save(ReferenceMaterial.ofLink(
				saveProject().getId(),
				saveUser().getUserId(),
				"example.com",
				"https://example.com/" + UUID.randomUUID(),
				ReferenceStatus.UPLOADING));
	}

	private Project saveProject() {
		return projectRepository.save(Project.of(saveUser().getUserId(), "어댑터 테스트", null));
	}

	private User saveUser() {
		return userRepository.save(User.ofLocal(
				"어댑터", UUID.randomUUID() + "@synq.com", "password-hash"));
	}
}
