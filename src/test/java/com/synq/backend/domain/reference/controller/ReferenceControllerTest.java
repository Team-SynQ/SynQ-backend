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
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class ReferenceControllerTest extends PostgresTestContainer {

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

	@MockitoBean
	private AccessTokenBlacklistService accessTokenBlacklistService;

	@Test
	void 프로젝트_OWNER는_다른_MEMBER의_참고자료를_204로_삭제한다() throws Exception {
		User owner = saveUser("소유자", "reference-delete-controller-owner@synq.com");
		User member = saveUser("멤버", "reference-delete-controller-owner-member@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);
		saveMember(project, member, ProjectMemberRole.MEMBER);
		ReferenceMaterial reference = saveLink(project, member);

		mockMvc.perform(delete("/projects/{projectId}/references/{referenceId}",
						project.getId(), reference.getId())
						.header(HttpHeaders.AUTHORIZATION, bearer(owner)))
				.andExpect(status().isNoContent())
				.andExpect(content().string(""));

		assertThat(reference.getDeletedAt()).isNotNull();
	}

	@Test
	void 일반_MEMBER는_자신이_등록한_참고자료를_삭제한다() throws Exception {
		User owner = saveUser("소유자", "reference-delete-controller-member-owner@synq.com");
		User member = saveUser("멤버", "reference-delete-controller-member@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);
		saveMember(project, member, ProjectMemberRole.MEMBER);
		ReferenceMaterial reference = saveFile(project, member);

		mockMvc.perform(delete("/projects/{projectId}/references/{referenceId}",
						project.getId(), reference.getId())
						.header(HttpHeaders.AUTHORIZATION, bearer(member)))
				.andExpect(status().isNoContent())
				.andExpect(content().string(""));
	}

	@Test
	void JWT가_없거나_유효하지_않으면_참고자료_삭제는_401이다() throws Exception {
		mockMvc.perform(delete("/projects/{projectId}/references/{referenceId}", 1L, 1L))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH401_1"));
		mockMvc.perform(delete("/projects/{projectId}/references/{referenceId}", 1L, 1L)
						.header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH401_1"));
		mockMvc.perform(delete("/projects/{projectId}/references/{referenceId}", 1L, 1L)
						.header("X-User-Id", 1L))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH401_1"));
	}

	@Test
	void 외부_사용자와_다른_사용자의_자료를_삭제하는_MEMBER는_403이다() throws Exception {
		User owner = saveUser("소유자", "reference-delete-controller-forbidden-owner@synq.com");
		User member = saveUser("멤버", "reference-delete-controller-forbidden-member@synq.com");
		User outsider = saveUser("외부 사용자", "reference-delete-controller-forbidden-outsider@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);
		saveMember(project, member, ProjectMemberRole.MEMBER);
		ReferenceMaterial reference = saveFile(project, owner);

		mockMvc.perform(delete("/projects/{projectId}/references/{referenceId}",
						project.getId(), reference.getId())
						.header(HttpHeaders.AUTHORIZATION, bearer(outsider)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("PROJECT403_2"));
		mockMvc.perform(delete("/projects/{projectId}/references/{referenceId}",
						project.getId(), reference.getId())
						.header(HttpHeaders.AUTHORIZATION, bearer(member)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("REFERENCE403_1"));
	}

	@Test
	void X_User_Id를_OWNER로_위조해도_JWT_사용자_기준으로_403이다() throws Exception {
		User owner = saveUser("소유자", "reference-delete-controller-forgery-owner@synq.com");
		User member = saveUser("멤버", "reference-delete-controller-forgery-member@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);
		saveMember(project, member, ProjectMemberRole.MEMBER);
		ReferenceMaterial reference = saveFile(project, owner);

		mockMvc.perform(delete("/projects/{projectId}/references/{referenceId}",
						project.getId(), reference.getId())
						.header(HttpHeaders.AUTHORIZATION, bearer(member))
						.header("X-User-Id", owner.getUserId()))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("REFERENCE403_1"));
	}

	@Test
	void 프로젝트나_참고자료가_없거나_다른_프로젝트_자료이면_404이다() throws Exception {
		User owner = saveUser("소유자", "reference-delete-controller-not-found@synq.com");
		Project project = saveProject(owner);
		Project otherProject = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);
		saveMember(otherProject, owner, ProjectMemberRole.OWNER);
		ReferenceMaterial reference = saveFile(project, owner);
		ReferenceMaterial otherReference = saveFile(otherProject, owner);

		mockMvc.perform(delete("/projects/{projectId}/references/{referenceId}",
						Long.MAX_VALUE, reference.getId())
						.header(HttpHeaders.AUTHORIZATION, bearer(owner)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("PROJECT404_3"));
		mockMvc.perform(delete("/projects/{projectId}/references/{referenceId}",
						project.getId(), Long.MAX_VALUE)
						.header(HttpHeaders.AUTHORIZATION, bearer(owner)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("REFERENCE404_1"));
		mockMvc.perform(delete("/projects/{projectId}/references/{referenceId}",
						project.getId(), otherReference.getId())
						.header(HttpHeaders.AUTHORIZATION, bearer(owner)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("REFERENCE404_1"));
	}

	@Test
	void 참고자료_링크_등록_응답이_201과_명세_필드를_반환한다() throws Exception {
		User owner = saveUser("박서은", "reference-link-controller-owner@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);

		MvcResult result = mockMvc.perform(post("/projects/{projectId}/references/links", project.getId())
						.header(HttpHeaders.AUTHORIZATION, bearer(owner))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"url\":\"  https://www.notion.so/example  \"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.isSuccess").value(true))
				.andExpect(jsonPath("$.code").value("COMMON201"))
				.andExpect(jsonPath("$.result.referenceId").isNumber())
				.andExpect(jsonPath("$.result.type").value("LINK"))
				.andExpect(jsonPath("$.result.name").value("notion.so"))
				.andExpect(jsonPath("$.result.url").value("https://www.notion.so/example"))
				.andExpect(jsonPath("$.result.status").value("UPLOADING"))
				.andExpect(jsonPath("$.result.uploaderId").value(owner.getUserId()))
				.andExpect(jsonPath("$.result.uploaderName").value("박서은"))
				.andExpect(jsonPath("$.result.createdAt").isString())
				.andReturn();

		JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString()).path("result");
		assertThat(fieldNames(response)).containsExactly(
				"referenceId",
				"type",
				"name",
				"url",
				"status",
				"uploaderId",
				"uploaderName",
				"createdAt"
		);
	}

	@Test
	void 일반_MEMBER의_JWT로_링크를_등록할_수_있다() throws Exception {
		User owner = saveUser("소유자", "reference-link-controller-member-owner@synq.com");
		User member = saveUser("멤버", "reference-link-controller-member@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);
		saveMember(project, member, ProjectMemberRole.MEMBER);

		mockMvc.perform(post("/projects/{projectId}/references/links", project.getId())
						.header(HttpHeaders.AUTHORIZATION, bearer(member))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"url\":\"https://example.com/member\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.result.uploaderId").value(member.getUserId()))
				.andExpect(jsonPath("$.result.uploaderName").value("멤버"));
	}

	@Test
	void 잘못된_URL은_400을_반환한다() throws Exception {
		User owner = saveUser("소유자", "reference-link-controller-invalid@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);

		mockMvc.perform(post("/projects/{projectId}/references/links", project.getId())
						.header(HttpHeaders.AUTHORIZATION, bearer(owner))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"url\":\"ftp://example.com/file\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON400_1"));
		mockMvc.perform(post("/projects/{projectId}/references/links", project.getId())
						.header(HttpHeaders.AUTHORIZATION, bearer(owner))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"url\":\"https://exa mple.com\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON400_1"));
	}

	@Test
	void JWT가_없으면_링크_등록은_401을_반환한다() throws Exception {
		mockMvc.perform(post("/projects/{projectId}/references/links", 1L)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"url\":\"https://example.com\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH401_1"));
	}

	@Test
	void 유효하지_않은_JWT이면_링크_등록은_401을_반환한다() throws Exception {
		mockMvc.perform(post("/projects/{projectId}/references/links", 1L)
						.header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"url\":\"https://example.com\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH401_1"));
	}

	@Test
	void X_User_Id만_전달하면_401을_반환한다() throws Exception {
		mockMvc.perform(post("/projects/{projectId}/references/links", 1L)
						.header("X-User-Id", 1L)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"url\":\"https://example.com\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH401_1"));
	}

	@Test
	void 외부_사용자의_링크_등록은_403을_반환한다() throws Exception {
		User owner = saveUser("소유자", "reference-link-controller-outsider-owner@synq.com");
		User outsider = saveUser("외부 사용자", "reference-link-controller-outsider@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);

		mockMvc.perform(post("/projects/{projectId}/references/links", project.getId())
						.header(HttpHeaders.AUTHORIZATION, bearer(outsider))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"url\":\"https://example.com\"}"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("PROJECT403_2"));
	}

	@Test
	void X_User_Id를_OWNER로_위조해도_JWT_사용자_기준으로_403을_반환한다() throws Exception {
		User owner = saveUser("소유자", "reference-link-controller-forgery-owner@synq.com");
		User outsider = saveUser("외부 사용자", "reference-link-controller-forgery-outsider@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);

		mockMvc.perform(post("/projects/{projectId}/references/links", project.getId())
						.header(HttpHeaders.AUTHORIZATION, bearer(outsider))
						.header("X-User-Id", owner.getUserId())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"url\":\"https://example.com\"}"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("PROJECT403_2"));
	}

	@Test
	void 존재하지_않는_사용자나_프로젝트의_링크_등록은_404를_반환한다() throws Exception {
		User user = saveUser("사용자", "reference-link-controller-missing@synq.com");

		mockMvc.perform(post("/projects/{projectId}/references/links", 1L)
						.header(HttpHeaders.AUTHORIZATION, bearer(Long.MAX_VALUE))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"url\":\"https://example.com\"}"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("PROJECT404_1"));
		mockMvc.perform(post("/projects/{projectId}/references/links", Long.MAX_VALUE)
						.header(HttpHeaders.AUTHORIZATION, bearer(user))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"url\":\"https://example.com\"}"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("PROJECT404_3"));
	}

	@Test
	void 삭제된_프로젝트의_링크_등록은_404를_반환한다() throws Exception {
		User owner = saveUser("소유자", "reference-link-controller-deleted@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);
		project.softDelete();
		projectRepository.flush();

		mockMvc.perform(post("/projects/{projectId}/references/links", project.getId())
						.header(HttpHeaders.AUTHORIZATION, bearer(owner))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"url\":\"https://example.com\"}"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("PROJECT404_3"));
	}

	@Test
	void 참고자료가_10개이면_링크_등록은_409를_반환한다() throws Exception {
		User owner = saveUser("소유자", "reference-link-controller-limit@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);
		for (int index = 0; index < 10; index++) {
			saveFile(project, owner);
		}

		mockMvc.perform(post("/projects/{projectId}/references/links", project.getId())
						.header(HttpHeaders.AUTHORIZATION, bearer(owner))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"url\":\"https://example.com\"}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("REFERENCE409_1"));
	}

	@Test
	void 참고자료_목록_응답의_필드와_문자열_enum이_명세와_일치한다() throws Exception {
		User owner = saveUser("박서은", "reference-controller-owner@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);
		saveFile(project, owner);

		MvcResult result = mockMvc.perform(get("/projects/{projectId}/references", project.getId())
						.header(HttpHeaders.AUTHORIZATION, bearer(owner)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.result.currentCount").value(1))
				.andExpect(jsonPath("$.result.maxCount").value(10))
				.andExpect(jsonPath("$.result.references").isArray())
				.andExpect(jsonPath("$.result.references[0].referenceId").isNumber())
				.andExpect(jsonPath("$.result.references[0].type").value("FILE"))
				.andExpect(jsonPath("$.result.references[0].name").value("프로젝트 요구사항.pdf"))
				.andExpect(jsonPath("$.result.references[0].url").value(nullValue()))
				.andExpect(jsonPath("$.result.references[0].fileSize").value(1048576))
				.andExpect(jsonPath("$.result.references[0].fileExtension").value("PDF"))
				.andExpect(jsonPath("$.result.references[0].status").value("AVAILABLE"))
				.andExpect(jsonPath("$.result.references[0].uploaderId").value(owner.getUserId()))
				.andExpect(jsonPath("$.result.references[0].uploaderName").value("박서은"))
				.andExpect(jsonPath("$.result.references[0].canDelete").value(true))
				.andExpect(jsonPath("$.result.references[0].createdAt").isString())
				.andReturn();

		JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString()).path("result");
		assertThat(fieldNames(response)).containsExactly("currentCount", "maxCount", "references");
		assertThat(fieldNames(response.path("references").get(0))).containsExactly(
				"referenceId",
				"type",
				"name",
				"url",
				"fileSize",
				"fileExtension",
				"status",
				"uploaderId",
				"uploaderName",
				"canDelete",
				"createdAt"
		);
	}

	@Test
	void 일반_프로젝트_멤버도_참고자료를_조회할_수_있다() throws Exception {
		User owner = saveUser("소유자", "reference-controller-member-owner@synq.com");
		User member = saveUser("멤버", "reference-controller-member@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);
		saveMember(project, member, ProjectMemberRole.MEMBER);
		saveLink(project, member);

		mockMvc.perform(get("/projects/{projectId}/references", project.getId())
						.header(HttpHeaders.AUTHORIZATION, bearer(member)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.result.references[0].type").value("LINK"))
				.andExpect(jsonPath("$.result.references[0].fileSize").value(nullValue()))
				.andExpect(jsonPath("$.result.references[0].fileExtension").value(nullValue()))
				.andExpect(jsonPath("$.result.references[0].status").value("READ_FAILED"));
	}

	@Test
	void 프로젝트_외부_사용자는_403을_반환한다() throws Exception {
		User owner = saveUser("소유자", "reference-controller-outsider-owner@synq.com");
		User outsider = saveUser("외부 사용자", "reference-controller-outsider@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);

		mockMvc.perform(get("/projects/{projectId}/references", project.getId())
						.header(HttpHeaders.AUTHORIZATION, bearer(outsider)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("PROJECT403_2"));
	}

	@Test
	void 존재하지_않는_프로젝트는_404를_반환한다() throws Exception {
		User user = saveUser("사용자", "reference-controller-missing@synq.com");

		mockMvc.perform(get("/projects/{projectId}/references", Long.MAX_VALUE)
						.header(HttpHeaders.AUTHORIZATION, bearer(user)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("PROJECT404_3"));
	}

	@Test
	void 인증_헤더가_없으면_401을_반환한다() throws Exception {
		mockMvc.perform(get("/projects/{projectId}/references", 1L))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH401_1"));
	}

	@Test
	void 유효하지_않은_JWT이면_목록_조회는_401을_반환한다() throws Exception {
		mockMvc.perform(get("/projects/{projectId}/references", 1L)
						.header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH401_1"));
	}

	@Test
	void X_User_Id를_멤버로_위조해도_JWT_사용자_기준으로_403을_반환한다() throws Exception {
		User owner = saveUser("소유자", "reference-controller-forgery-owner@synq.com");
		User outsider = saveUser("외부 사용자", "reference-controller-forgery-outsider@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);

		mockMvc.perform(get("/projects/{projectId}/references", project.getId())
						.header(HttpHeaders.AUTHORIZATION, bearer(outsider))
						.header("X-User-Id", owner.getUserId()))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("PROJECT403_2"));
	}

	@Test
	void Swagger에_참고자료_목록_조회_API가_문서화된다() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/references'].get").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/references'].get.security[0].bearerAuth").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/references/links'].post").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/references/links'].post.security[0].bearerAuth").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/references/{referenceId}'].delete").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/references/{referenceId}'].delete.security[0].bearerAuth").exists());
	}

	private List<String> fieldNames(JsonNode node) {
		List<String> fieldNames = new ArrayList<>();
		node.fieldNames().forEachRemaining(fieldNames::add);
		return fieldNames;
	}

	private String bearer(User user) {
		return bearer(user.getUserId());
	}

	private String bearer(Long userId) {
		return "Bearer " + jwtProvider.createAccessToken(userId);
	}

	private Project saveProject(User owner) {
		return projectRepository.save(Project.of(owner.getUserId(), "SynQ", null));
	}

	private void saveMember(Project project, User user, ProjectMemberRole role) {
		projectMemberRepository.save(ProjectMember.of(project.getId(), user.getUserId(), role));
	}

	private ReferenceMaterial saveFile(Project project, User uploader) {
		return referenceMaterialRepository.save(ReferenceMaterial.ofFile(
				project.getId(),
				uploader.getUserId(),
				"프로젝트 요구사항.pdf",
				1048576L,
				ReferenceFileExtension.PDF,
				ReferenceStatus.AVAILABLE
		));
	}

	private ReferenceMaterial saveLink(Project project, User uploader) {
		return referenceMaterialRepository.save(ReferenceMaterial.ofLink(
				project.getId(),
				uploader.getUserId(),
				"SynQ 기획 문서",
				"https://www.notion.so/example",
				ReferenceStatus.READ_FAILED
		));
	}

	private User saveUser(String name, String email) {
		return userRepository.save(User.ofLocal(name, email, "password-hash"));
	}
}
