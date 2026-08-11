package com.synq.backend.domain.reference.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.synq.backend.domain.auth.jwt.AccessTokenBlacklistService;
import com.synq.backend.domain.auth.jwt.JwtProvider;
import com.synq.backend.domain.project.entity.Project;
import com.synq.backend.domain.project.entity.ProjectMember;
import com.synq.backend.domain.project.entity.ProjectMemberRole;
import com.synq.backend.domain.project.repository.ProjectMemberRepository;
import com.synq.backend.domain.project.repository.ProjectRepository;
import com.synq.backend.domain.reference.entity.ReferenceFileExtension;
import com.synq.backend.domain.reference.entity.ReferenceMaterial;
import com.synq.backend.domain.reference.entity.ReferenceStatus;
import com.synq.backend.domain.reference.repository.ReferenceMaterialRepository;
import com.synq.backend.domain.user.entity.User;
import com.synq.backend.domain.user.repository.UserRepository;
import com.synq.backend.support.PostgresTestContainer;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class ReferenceNameUpdateControllerTest extends PostgresTestContainer {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private ReferenceMaterialRepository referenceMaterialRepository;

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private ProjectMemberRepository projectMemberRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private JwtProvider jwtProvider;

	@Autowired
	private EntityManager entityManager;

	@MockitoBean
	private AccessTokenBlacklistService accessTokenBlacklistService;

	@Test
	void OWNER는_FILE과_LINK_제목을_수정하고_명세_응답을_받는다() throws Exception {
		User owner = saveUser("소유자", "reference-update-controller-owner@synq.com");
		User member = saveUser("멤버", "reference-update-controller-owner-member@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);
		saveMember(project, member, ProjectMemberRole.MEMBER);
		ReferenceMaterial file = saveFile(project, member);
		ReferenceMaterial link = saveLink(project, member);

		MvcResult fileResult = mockMvc.perform(patch(
						"/projects/{projectId}/references/{referenceId}", project.getId(), file.getId())
						.header(HttpHeaders.AUTHORIZATION, bearer(owner))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"회의 기획안 v2\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.isSuccess").value(true))
				.andExpect(jsonPath("$.code").value("COMMON200"))
				.andExpect(jsonPath("$.result.referenceId").value(file.getId()))
				.andExpect(jsonPath("$.result.name").value("회의 기획안 v2"))
				.andExpect(jsonPath("$.result.type").value("FILE"))
				.andExpect(jsonPath("$.result.updatedAt").isString())
				.andReturn();

		mockMvc.perform(patch("/projects/{projectId}/references/{referenceId}",
						project.getId(), link.getId())
						.header(HttpHeaders.AUTHORIZATION, bearer(owner))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"링크 제목 v2\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.result.type").value("LINK"))
				.andExpect(jsonPath("$.result.name").value("링크 제목 v2"));

		JsonNode response = objectMapper.readTree(fileResult.getResponse().getContentAsString()).path("result");
		assertThat(fieldNames(response)).containsExactly("referenceId", "name", "type", "updatedAt");
		assertThat(file.getUpdatedAt()).isNotNull();
		assertThat(link.getUrl()).isEqualTo("https://example.com/reference");
	}

	@Test
	void MEMBER는_자신의_자료만_trim된_제목으로_수정한다() throws Exception {
		User owner = saveUser("소유자", "reference-update-controller-member-owner@synq.com");
		User member = saveUser("멤버", "reference-update-controller-member@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);
		saveMember(project, member, ProjectMemberRole.MEMBER);
		ReferenceMaterial ownReference = saveLink(project, member);
		ReferenceMaterial otherReference = saveFile(project, owner);

		mockMvc.perform(patch("/projects/{projectId}/references/{referenceId}",
						project.getId(), ownReference.getId())
						.header(HttpHeaders.AUTHORIZATION, bearer(member))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"  내 참고자료  \"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.result.name").value("내 참고자료"));

		mockMvc.perform(patch("/projects/{projectId}/references/{referenceId}",
						project.getId(), otherReference.getId())
						.header(HttpHeaders.AUTHORIZATION, bearer(member))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"변경 시도\"}"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("REFERENCE403_2"));
	}

	@Test
	void 프로젝트_외부_사용자와_위조된_X_User_Id는_수정_권한을_얻지_못한다() throws Exception {
		User owner = saveUser("소유자", "reference-update-controller-forgery-owner@synq.com");
		User outsider = saveUser("외부인", "reference-update-controller-forgery-outsider@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);
		ReferenceMaterial reference = saveFile(project, owner);

		mockMvc.perform(patch("/projects/{projectId}/references/{referenceId}",
						project.getId(), reference.getId())
						.header(HttpHeaders.AUTHORIZATION, bearer(outsider))
						.header("X-User-Id", owner.getUserId())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"위조 변경\"}"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("PROJECT403_2"));
	}

	@Test
	void 프로젝트나_참고자료가_없거나_삭제됐거나_다른_프로젝트_자료이면_404이다() throws Exception {
		User owner = saveUser("소유자", "reference-update-controller-not-found@synq.com");
		Project project = saveProject(owner);
		Project otherProject = saveProject(owner);
		Project deletedProject = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);
		saveMember(otherProject, owner, ProjectMemberRole.OWNER);
		saveMember(deletedProject, owner, ProjectMemberRole.OWNER);
		ReferenceMaterial deletedReference = saveFile(project, owner);
		ReferenceMaterial otherReference = saveLink(otherProject, owner);
		ReferenceMaterial deletedProjectReference = saveLink(deletedProject, owner);
		deletedReference.softDelete();
		deletedProject.softDelete();
		referenceMaterialRepository.flush();
		projectRepository.flush();
		entityManager.clear();

		assertNotFound(owner, Long.MAX_VALUE, deletedProjectReference.getId(), "PROJECT404_3");
		assertNotFound(owner, deletedProject.getId(), deletedProjectReference.getId(), "PROJECT404_3");
		assertNotFound(owner, project.getId(), Long.MAX_VALUE, "REFERENCE404_1");
		assertNotFound(owner, project.getId(), deletedReference.getId(), "REFERENCE404_1");
		assertNotFound(owner, project.getId(), otherReference.getId(), "REFERENCE404_1");
	}

	@Test
	void 제목은_1자와_30자까지_허용한다() throws Exception {
		User owner = saveUser("소유자", "reference-update-controller-boundary@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);
		ReferenceMaterial reference = saveFile(project, owner);

		assertUpdateSuccess(owner, project, reference, "가", "가");
		String thirtyCharacters = "가".repeat(30);
		assertUpdateSuccess(owner, project, reference, thirtyCharacters, thirtyCharacters);
	}

	@Test
	void null_빈문자열_공백과_31자_제목은_400이다() throws Exception {
		User owner = saveUser("소유자", "reference-update-controller-invalid-name@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);
		ReferenceMaterial reference = saveFile(project, owner);

		assertInvalidName(owner, project, reference, "null");
		assertInvalidName(owner, project, reference, "\"\"");
		assertInvalidName(owner, project, reference, "\"   \"");
		assertInvalidName(owner, project, reference, "\"" + "가".repeat(31) + "\"");
		mockMvc.perform(patch("/projects/{projectId}/references/{referenceId}",
						project.getId(), reference.getId())
						.header(HttpHeaders.AUTHORIZATION, bearer(owner))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON400_1"));
	}

	@Test
	void JWT가_없거나_유효하지_않으면_401이다() throws Exception {
		String path = "/projects/{projectId}/references/{referenceId}";
		String body = "{\"name\":\"새 제목\"}";

		mockMvc.perform(patch(path, 1L, 1L)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH401_1"));
		mockMvc.perform(patch(path, 1L, 1L)
						.header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH401_1"));
	}

	@Test
	void Swagger에_제목_수정_경로와_Bearer_인증이_문서화된다() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/references/{referenceId}'].patch").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/references/{referenceId}'].patch.security[0].bearerAuth").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/references/{referenceId}'].patch.responses['200']").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/references/{referenceId}'].patch.responses['400']").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/references/{referenceId}'].patch.responses['401']").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/references/{referenceId}'].patch.responses['403']").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/references/{referenceId}'].patch.responses['404']").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/references/{referenceId}'].patch.responses['500']").exists());
	}

	private void assertNotFound(User user, Long projectId, Long referenceId, String code) throws Exception {
		mockMvc.perform(patch("/projects/{projectId}/references/{referenceId}", projectId, referenceId)
						.header(HttpHeaders.AUTHORIZATION, bearer(user))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"새 제목\"}"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value(code));
	}

	private void assertUpdateSuccess(
			User user,
			Project project,
			ReferenceMaterial reference,
			String requestName,
			String expectedName
	) throws Exception {
		mockMvc.perform(patch("/projects/{projectId}/references/{referenceId}",
						project.getId(), reference.getId())
						.header(HttpHeaders.AUTHORIZATION, bearer(user))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new NameBody(requestName))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.result.name").value(expectedName));
	}

	private void assertInvalidName(
			User user,
			Project project,
			ReferenceMaterial reference,
			String jsonValue
	) throws Exception {
		mockMvc.perform(patch("/projects/{projectId}/references/{referenceId}",
						project.getId(), reference.getId())
						.header(HttpHeaders.AUTHORIZATION, bearer(user))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":" + jsonValue + "}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON400_1"));
	}

	private List<String> fieldNames(JsonNode node) {
		List<String> fieldNames = new ArrayList<>();
		node.fieldNames().forEachRemaining(fieldNames::add);
		return fieldNames;
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

	private ReferenceMaterial saveFile(Project project, User uploader) {
		return referenceMaterialRepository.save(ReferenceMaterial.ofFile(
				project.getId(), uploader.getUserId(), "기존 파일.pdf", 1024L,
				"references/" + project.getId() + "/update-controller.pdf",
				ReferenceFileExtension.PDF, ReferenceStatus.AVAILABLE));
	}

	private ReferenceMaterial saveLink(Project project, User uploader) {
		return referenceMaterialRepository.save(ReferenceMaterial.ofLink(
				project.getId(), uploader.getUserId(), "기존 링크",
				"https://example.com/reference", ReferenceStatus.AVAILABLE));
	}

	private User saveUser(String name, String email) {
		return userRepository.save(User.ofLocal(name, email, "password-hash"));
	}

	private record NameBody(String name) {
	}
}
