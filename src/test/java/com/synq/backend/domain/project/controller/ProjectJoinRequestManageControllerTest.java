package com.synq.backend.domain.project.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.synq.backend.domain.project.entity.Project;
import com.synq.backend.domain.project.entity.ProjectJoinSettingSource;
import com.synq.backend.domain.project.entity.ProjectMember;
import com.synq.backend.domain.project.entity.ProjectMemberRole;
import com.synq.backend.domain.project.entity.ProjectParticipationRequest;
import com.synq.backend.domain.project.repository.ProjectMemberRepository;
import com.synq.backend.domain.project.repository.ProjectParticipationRequestRepository;
import com.synq.backend.domain.project.repository.ProjectRepository;
import com.synq.backend.domain.user.entity.Role;
import com.synq.backend.domain.user.entity.User;
import com.synq.backend.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class ProjectJoinRequestManageControllerTest extends ProjectControllerTestSupport {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private ProjectMemberRepository projectMemberRepository;

	@Autowired
	private ProjectParticipationRequestRepository participationRequestRepository;

	@Autowired
	private UserRepository userRepository;

	@Test
	void OWNER는_PENDING_참여_요청_목록을_200으로_조회한다() throws Exception {
		User owner = saveUser("소유자", "manage-controller-list-owner@synq.com");
		User firstUser = saveUser("박서은", "manage-controller-list-first@synq.com");
		User secondUser = saveUser("이소민", "manage-controller-list-second@synq.com");
		User processedUser = saveUser("처리 완료", "manage-controller-list-processed@synq.com");
		Project project = saveProjectWithOwner(owner);
		ProjectParticipationRequest first = savePending(project, firstUser);
		ProjectParticipationRequest second = savePending(project, secondUser);
		ProjectParticipationRequest processed = savePending(project, processedUser);
		processed.reject();

		MvcResult result = mockMvc.perform(get("/projects/{projectId}/join-requests", project.getId())
						.header(HttpHeaders.AUTHORIZATION, bearer(owner)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value("COMMON200"))
				.andExpect(jsonPath("$.result.pendingCount").value(2))
				.andExpect(jsonPath("$.result.requests[0].requestId").value(first.getId()))
				.andExpect(jsonPath("$.result.requests[0].userId").value(firstUser.getUserId()))
				.andExpect(jsonPath("$.result.requests[0].name").value("박서은"))
				.andExpect(jsonPath("$.result.requests[0].requestedAt", endsWith("Z")))
				.andExpect(jsonPath("$.result.requests[1].requestId").value(second.getId()))
				.andExpect(jsonPath("$.result.requests[1].name").value("이소민"))
				.andExpect(jsonPath("$.result.requests[0].roleCategory").doesNotExist())
				.andExpect(jsonPath("$.result.requests[0].detailRole").doesNotExist())
				.andExpect(jsonPath("$.result.requests[0].perspectives").doesNotExist())
				.andReturn();

		JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString()).path("result");
		assertThat(fieldNames(response)).containsExactly("pendingCount", "requests");
		assertThat(fieldNames(response.path("requests").get(0)))
				.containsExactly("requestId", "userId", "name", "requestedAt");
	}

	@Test
	void OWNER는_PENDING_요청을_승인하고_MEMBER_정보를_200으로_받는다() throws Exception {
		User owner = saveUser("소유자", "manage-controller-approve-owner@synq.com");
		User requester = saveUser("요청자", "manage-controller-approve-requester@synq.com");
		Project project = saveProjectWithOwner(owner);
		ProjectParticipationRequest request = savePending(project, requester);

		mockMvc.perform(patch("/projects/{projectId}/join-requests/{requestId}/approve",
						project.getId(), request.getId())
						.header(HttpHeaders.AUTHORIZATION, bearer(owner)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value("COMMON200"))
				.andExpect(jsonPath("$.result.requestId").value(request.getId()))
				.andExpect(jsonPath("$.result.memberId").isNumber())
				.andExpect(jsonPath("$.result.userId").value(requester.getUserId()))
				.andExpect(jsonPath("$.result.status").value("APPROVED"))
				.andExpect(jsonPath("$.result.joinedAt").isString());

		assertThat(projectMemberRepository.findByProjectIdAndUserId(project.getId(), requester.getUserId()))
				.isPresent()
				.get()
				.extracting(ProjectMember::getRole)
				.isEqualTo(ProjectMemberRole.MEMBER);
	}

	@Test
	void OWNER는_PENDING_요청을_거절하고_200을_받는다() throws Exception {
		User owner = saveUser("소유자", "manage-controller-reject-owner@synq.com");
		User requester = saveUser("요청자", "manage-controller-reject-requester@synq.com");
		Project project = saveProjectWithOwner(owner);
		ProjectParticipationRequest request = savePending(project, requester);

		mockMvc.perform(patch("/projects/{projectId}/join-requests/{requestId}/reject",
						project.getId(), request.getId())
						.header(HttpHeaders.AUTHORIZATION, bearer(owner)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.result.requestId").value(request.getId()))
				.andExpect(jsonPath("$.result.status").value("REJECTED"));
	}

	@Test
	void JWT가_없거나_유효하지_않으면_401이다() throws Exception {
		mockMvc.perform(get("/projects/1/join-requests"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH401_1"));
		mockMvc.perform(patch("/projects/1/join-requests/1/approve")
						.header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH401_1"));
		mockMvc.perform(patch("/projects/1/join-requests/1/reject")
						.header("X-User-Id", 1L))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH401_1"));
	}

	@Test
	void MEMBER는_목록_조회와_승인_거절을_할_수_없다() throws Exception {
		User owner = saveUser("소유자", "manage-controller-forbidden-owner@synq.com");
		User member = saveUser("멤버", "manage-controller-forbidden-member@synq.com");
		User requester = saveUser("요청자", "manage-controller-forbidden-requester@synq.com");
		Project project = saveProjectWithOwner(owner);
		projectMemberRepository.save(ProjectMember.of(
				project.getId(), member.getUserId(), ProjectMemberRole.MEMBER));
		ProjectParticipationRequest request = savePending(project, requester);

		assertForbidden(get("/projects/{projectId}/join-requests", project.getId())
				.header(HttpHeaders.AUTHORIZATION, bearer(member)));
		assertForbidden(patch("/projects/{projectId}/join-requests/{requestId}/approve",
				project.getId(), request.getId())
				.header(HttpHeaders.AUTHORIZATION, bearer(member)));
		assertForbidden(patch("/projects/{projectId}/join-requests/{requestId}/reject",
				project.getId(), request.getId())
				.header(HttpHeaders.AUTHORIZATION, bearer(member)));
	}

	@Test
	void 존재하지_않거나_삭제된_프로젝트와_없는_요청은_404이다() throws Exception {
		User owner = saveUser("소유자", "manage-controller-not-found-owner@synq.com");
		User requester = saveUser("요청자", "manage-controller-not-found-requester@synq.com");
		Project project = saveProjectWithOwner(owner);
		Project deletedProject = saveProjectWithOwner(owner);
		ProjectParticipationRequest deletedProjectRequest = savePending(deletedProject, requester);
		deletedProject.softDelete();
		projectRepository.flush();

		mockMvc.perform(get("/projects/{projectId}/join-requests", Long.MAX_VALUE)
						.header(HttpHeaders.AUTHORIZATION, bearer(owner)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("PROJECT404_3"));
		mockMvc.perform(patch("/projects/{projectId}/join-requests/{requestId}/reject",
						deletedProject.getId(), deletedProjectRequest.getId())
						.header(HttpHeaders.AUTHORIZATION, bearer(owner)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("PROJECT404_3"));
		mockMvc.perform(patch("/projects/{projectId}/join-requests/{requestId}/approve",
						project.getId(), Long.MAX_VALUE)
						.header(HttpHeaders.AUTHORIZATION, bearer(owner)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("PROJECT404_6"));
	}

	@Test
	void 이미_처리된_요청은_409이다() throws Exception {
		User owner = saveUser("소유자", "manage-controller-processed-owner@synq.com");
		User requester = saveUser("요청자", "manage-controller-processed-requester@synq.com");
		Project project = saveProjectWithOwner(owner);
		ProjectParticipationRequest request = savePending(project, requester);
		request.reject();

		mockMvc.perform(patch("/projects/{projectId}/join-requests/{requestId}/approve",
						project.getId(), request.getId())
						.header(HttpHeaders.AUTHORIZATION, bearer(owner)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("PROJECT409_5"));
	}

	@Test
	void Swagger에_참여_요청_관리_API와_Bearer_인증이_문서화된다() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/join-requests'].get.security[0].bearerAuth").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/join-requests/{requestId}/approve'].patch.security[0].bearerAuth").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/join-requests/{requestId}/reject'].patch.security[0].bearerAuth").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/join-requests'].get.responses['200']").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/join-requests'].get.responses['500']").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/join-requests/{requestId}/approve'].patch.responses['409']").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/join-requests/{requestId}/approve'].patch.responses['500']").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/join-requests/{requestId}/reject'].patch.responses['409']").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/join-requests/{requestId}/reject'].patch.responses['500']").exists());
	}

	private void assertForbidden(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request)
			throws Exception {
		mockMvc.perform(request)
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("PROJECT403_1"));
	}

	private List<String> fieldNames(JsonNode node) {
		List<String> fieldNames = new ArrayList<>();
		node.fieldNames().forEachRemaining(fieldNames::add);
		return fieldNames;
	}

	private Project saveProjectWithOwner(User owner) {
		Project project = projectRepository.save(Project.of(owner.getUserId(), "SynQ", null));
		projectMemberRepository.save(ProjectMember.of(
				project.getId(), owner.getUserId(), ProjectMemberRole.OWNER));
		return project;
	}

	private ProjectParticipationRequest savePending(Project project, User requester) {
		return participationRequestRepository.save(ProjectParticipationRequest.pending(
				project.getId(), requester.getUserId(), ProjectJoinSettingSource.DEFAULT,
				Role.DEV_TECH, null));
	}

	private User saveUser(String name, String email) {
		return userRepository.save(User.ofLocal(name, email, "password-hash"));
	}
}
