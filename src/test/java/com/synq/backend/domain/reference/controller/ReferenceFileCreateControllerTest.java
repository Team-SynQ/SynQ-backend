package com.synq.backend.domain.reference.controller;

import com.synq.backend.domain.reference.file.FileExtractionFailureReason;
import com.synq.backend.domain.reference.file.FileTextExtractionException;
import com.synq.backend.domain.reference.file.FileTextExtractor;
import org.junit.jupiter.api.BeforeEach;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.synq.backend.domain.auth.jwt.AccessTokenBlacklistService;
import com.synq.backend.domain.auth.jwt.JwtProvider;
import com.synq.backend.domain.project.entity.Project;
import com.synq.backend.domain.project.entity.ProjectMember;
import com.synq.backend.domain.project.entity.ProjectMemberRole;
import com.synq.backend.domain.project.repository.ProjectMemberRepository;
import com.synq.backend.domain.project.repository.ProjectRepository;
import com.synq.backend.domain.reference.entity.ReferenceMaterial;
import com.synq.backend.domain.reference.entity.ReferenceStatus;
import com.synq.backend.domain.reference.repository.ReferenceMaterialRepository;
import com.synq.backend.domain.reference.storage.ReferenceStorage;
import com.synq.backend.domain.user.entity.User;
import com.synq.backend.domain.user.repository.UserRepository;
import com.synq.backend.support.PostgresTestContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class ReferenceFileCreateControllerTest extends PostgresTestContainer {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private ProjectMemberRepository projectMemberRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ReferenceMaterialRepository referenceMaterialRepository;

	@Autowired
	private JwtProvider jwtProvider;

	@MockitoBean
	private AccessTokenBlacklistService accessTokenBlacklistService;

	@MockitoBean
	private ReferenceStorage referenceStorage;

	// 기존 픽스처는 "content" 7바이트를 .pdf 로 위장해서 쓴다. 실제 Tika 파싱은 이를 거절하므로
	// 등록 흐름을 검증하는 테스트에서는 추출기를 대역으로 세운다.
	@MockitoBean
	private FileTextExtractor fileTextExtractor;

	@BeforeEach
	void stubFileTextExtractor() {
		given(fileTextExtractor.extract(any(), anyString()))
				.willReturn("추출된 본문입니다. 최소 길이 조건을 넘기기 위한 충분히 긴 문장입니다.");
	}

	@Test
	void MEMBER가_multipart_파일을_등록하면_201과_명세_응답을_반환한다() throws Exception {
		User owner = saveUser("소유자", "file-controller-owner@synq.com");
		User member = saveUser("박서은", "file-controller-member@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);
		saveMember(project, member, ProjectMemberRole.MEMBER);
		MockMultipartFile file = file("C:\\fakepath\\requirements.PDF", "application/pdf");

		MvcResult result = mockMvc.perform(multipart("/projects/{projectId}/references/files", project.getId())
						.file(file)
						.header(HttpHeaders.AUTHORIZATION, bearer(member)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.code").value("COMMON201"))
				.andExpect(jsonPath("$.result.references[0].referenceId").isNumber())
				.andExpect(jsonPath("$.result.references[0].type").value("FILE"))
				.andExpect(jsonPath("$.result.references[0].name").value("requirements.PDF"))
				.andExpect(jsonPath("$.result.references[0].fileSize").value(7))
				.andExpect(jsonPath("$.result.references[0].fileExtension").value("PDF"))
				.andExpect(jsonPath("$.result.references[0].status").value("UPLOADING"))
				.andExpect(jsonPath("$.result.references[0].uploaderId").value(member.getUserId()))
				.andExpect(jsonPath("$.result.references[0].uploaderName").value("박서은"))
				.andExpect(jsonPath("$.result.references[0].createdAt").isString())
				.andReturn();

		JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString()).path("result");
		assertThat(fieldNames(response)).containsExactly("references");
		assertThat(fieldNames(response.path("references").get(0))).containsExactly(
				"referenceId", "type", "name", "fileSize", "fileExtension",
				"status", "uploaderId", "uploaderName", "createdAt");
	}

	@Test
	void JWT가_없거나_유효하지_않으면_401이다() throws Exception {
		MockMultipartFile file = file("requirements.pdf", "application/pdf");

		mockMvc.perform(multipart("/projects/{projectId}/references/files", 1L).file(file))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH401_1"));
		mockMvc.perform(multipart("/projects/{projectId}/references/files", 1L)
						.file(file)
						.header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH401_1"));
	}

	@Test
	void X_User_Id를_MEMBER로_위조해도_JWT_외부_사용자_기준으로_403이다() throws Exception {
		User owner = saveUser("소유자", "file-forgery-owner@synq.com");
		User member = saveUser("멤버", "file-forgery-member@synq.com");
		User outsider = saveUser("외부", "file-forgery-outsider@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);
		saveMember(project, member, ProjectMemberRole.MEMBER);

		mockMvc.perform(multipart("/projects/{projectId}/references/files", project.getId())
						.file(file("requirements.pdf", "application/pdf"))
						.header(HttpHeaders.AUTHORIZATION, bearer(outsider))
						.header("X-User-Id", member.getUserId()))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("PROJECT403_2"));
	}

	@Test
	void 파일이_없으면_400이고_지원하지_않는_형식은_415이다() throws Exception {
		User owner = saveUser("소유자", "file-controller-invalid@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);

		mockMvc.perform(multipart("/projects/{projectId}/references/files", project.getId())
						.header(HttpHeaders.AUTHORIZATION, bearer(owner)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("REFERENCE400_5"));
		mockMvc.perform(multipart("/projects/{projectId}/references/files", project.getId())
						.file(file("image.png", "image/png"))
						.header(HttpHeaders.AUTHORIZATION, bearer(owner)))
				.andExpect(status().isUnsupportedMediaType())
				.andExpect(jsonPath("$.code").value("REFERENCE415_1"));
	}

	@Test
	void 실제_파일_등록_경로에서_20MB_초과_파일은_413이다() throws Exception {
		User owner = saveUser("소유자", "file-controller-size-owner@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);

		mockMvc.perform(multipart("/projects/{projectId}/references/files", project.getId())
						.file(new MockMultipartFile(
								"files", "large.pdf", "application/pdf", new byte[20 * 1024 * 1024 + 1]))
						.header(HttpHeaders.AUTHORIZATION, bearer(owner)))
				.andExpect(status().isPayloadTooLarge())
				.andExpect(jsonPath("$.isSuccess").value(false))
				.andExpect(jsonPath("$.code").value("REFERENCE413_1"));
	}

	@Test
	void 존재하지_않거나_삭제된_프로젝트는_404이다() throws Exception {
		User owner = saveUser("소유자", "file-controller-project-owner@synq.com");
		Project deletedProject = saveProject(owner);
		saveMember(deletedProject, owner, ProjectMemberRole.OWNER);
		deletedProject.softDelete();
		projectRepository.flush();

		mockMvc.perform(multipart("/projects/{projectId}/references/files", Long.MAX_VALUE)
						.file(file("requirements.pdf", "application/pdf"))
						.header(HttpHeaders.AUTHORIZATION, bearer(owner)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("PROJECT404_3"));
		mockMvc.perform(multipart("/projects/{projectId}/references/files", deletedProject.getId())
						.file(file("requirements.pdf", "application/pdf"))
						.header(HttpHeaders.AUTHORIZATION, bearer(owner)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("PROJECT404_3"));
	}

	@Test
	void 활성_참고자료가_10개이면_409이다() throws Exception {
		User owner = saveUser("소유자", "file-controller-limit-owner@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);
		for (int index = 0; index < 10; index++) {
			referenceMaterialRepository.save(ReferenceMaterial.ofLink(
					project.getId(), owner.getUserId(), "link-" + index,
					"https://example.com/" + index, ReferenceStatus.UPLOADING));
		}

		mockMvc.perform(multipart("/projects/{projectId}/references/files", project.getId())
						.file(file("requirements.pdf", "application/pdf"))
						.header(HttpHeaders.AUTHORIZATION, bearer(owner)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("REFERENCE409_1"));
	}

	@Test
	void Swagger에_multipart와_Bearer_인증과_응답_상태가_문서화된다() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/references/files'].post").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/references/files'].post.security[0].bearerAuth").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/references/files'].post.requestBody.content['multipart/form-data']").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/references/files'].post.responses['201']").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/references/files'].post.responses['413']").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/references/files'].post.responses['415']").exists());
	}

	private List<String> fieldNames(JsonNode node) {
		List<String> fieldNames = new ArrayList<>();
		node.fieldNames().forEachRemaining(fieldNames::add);
		return fieldNames;
	}

	@Test
	void 추출에_실패하면_400과_실패_파일_목록을_반환한다() throws Exception {
		User owner = saveUser("소유자", "file-extract-fail-owner@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);
		given(fileTextExtractor.extract(any(), anyString()))
				.willThrow(new FileTextExtractionException(
						FileExtractionFailureReason.NO_TEXT_LAYER, null));

		mockMvc.perform(multipart("/projects/{projectId}/references/files", project.getId())
						.file(file("scan.pdf", "application/pdf"))
						.header(HttpHeaders.AUTHORIZATION, bearer(owner)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.isSuccess").value(false))
				.andExpect(jsonPath("$.code").value("REFERENCE400_6"))
				.andExpect(jsonPath("$.result[0].fileName").value("scan.pdf"))
				.andExpect(jsonPath("$.result[0].reason").value("NO_TEXT_LAYER"));
	}

	private MockMultipartFile file(String name, String contentType) {
		return new MockMultipartFile("files", name, contentType, "content".getBytes());
	}

	private String bearer(User user) {
		return "Bearer " + jwtProvider.createAccessToken(user.getUserId());
	}

	private Project saveProject(User owner) {
		return projectRepository.save(Project.of(owner.getUserId(), "SynQ", null));
	}

	private void saveMember(Project project, User user, ProjectMemberRole role) {
		projectMemberRepository.save(ProjectMember.of(project.getId(), user.getUserId(), role));
	}

	private User saveUser(String name, String email) {
		return userRepository.save(User.ofLocal(name, email, "password-hash"));
	}
}
