package com.synq.backend.domain.reference.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

	@Test
	void 참고자료_목록_응답의_필드와_문자열_enum이_명세와_일치한다() throws Exception {
		User owner = saveUser("박서은", "reference-controller-owner@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);
		saveFile(project, owner);

		MvcResult result = mockMvc.perform(get("/projects/{projectId}/references", project.getId())
						.header("X-User-Id", owner.getUserId()))
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
						.header("X-User-Id", member.getUserId()))
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
						.header("X-User-Id", outsider.getUserId()))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("PROJECT403_2"));
	}

	@Test
	void 존재하지_않는_프로젝트는_404를_반환한다() throws Exception {
		User user = saveUser("사용자", "reference-controller-missing@synq.com");

		mockMvc.perform(get("/projects/{projectId}/references", Long.MAX_VALUE)
						.header("X-User-Id", user.getUserId()))
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
	void Swagger에_참고자료_목록_조회_API가_문서화된다() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/references'].get").exists());
	}

	private List<String> fieldNames(JsonNode node) {
		List<String> fieldNames = new ArrayList<>();
		node.fieldNames().forEachRemaining(fieldNames::add);
		return fieldNames;
	}

	private Project saveProject(User owner) {
		return projectRepository.save(Project.of(owner.getUserId(), "SynQ", null));
	}

	private void saveMember(Project project, User user, ProjectMemberRole role) {
		projectMemberRepository.save(ProjectMember.of(project.getId(), user.getUserId(), role));
	}

	private void saveFile(Project project, User uploader) {
		referenceMaterialRepository.save(ReferenceMaterial.ofFile(
				project.getId(),
				uploader.getUserId(),
				"프로젝트 요구사항.pdf",
				1048576L,
				ReferenceFileExtension.PDF,
				ReferenceStatus.AVAILABLE
		));
	}

	private void saveLink(Project project, User uploader) {
		referenceMaterialRepository.save(ReferenceMaterial.ofLink(
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
