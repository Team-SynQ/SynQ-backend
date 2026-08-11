package com.synq.backend.domain.project.controller;

import com.synq.backend.domain.project.entity.Project;
import com.synq.backend.domain.project.entity.ProjectMember;
import com.synq.backend.domain.project.entity.ProjectMemberRole;
import com.synq.backend.domain.project.repository.ProjectMemberRepository;
import com.synq.backend.domain.project.repository.ProjectParticipationRequestRepository;
import com.synq.backend.domain.project.repository.ProjectRepository;
import com.synq.backend.domain.user.entity.User;
import com.synq.backend.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class ProjectJoinRequestCreateControllerTest extends ProjectControllerTestSupport {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private ProjectMemberRepository projectMemberRepository;

	@Autowired
	private ProjectParticipationRequestRepository participationRequestRepository;

	@Autowired
	private UserRepository userRepository;

	@Test
	void JWT_사용자로_참여_요청을_생성하고_201을_반환한다() throws Exception {
		User owner = saveUser("controller-request-owner@synq.com");
		User requester = saveUser("controller-request-user@synq.com");
		String token = UUID.randomUUID().toString();
		Project project = saveProjectWithOwner(owner, token, LocalDateTime.now().plusDays(7));

		mockMvc.perform(post("/projects/{projectId}/join-requests", project.getId())
						.header("Authorization", bearer(requester))
						.contentType(MediaType.APPLICATION_JSON)
						.content(validRequest(token)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.code").value("COMMON201"))
				.andExpect(jsonPath("$.result.requestId").isNumber())
				.andExpect(jsonPath("$.result.projectId").value(project.getId()))
				.andExpect(jsonPath("$.result.status").value("PENDING"))
				.andExpect(jsonPath("$.result.requestedAt", endsWith("Z")));

		assertThat(participationRequestRepository.findAll())
				.singleElement()
				.extracting(request -> request.getUserId())
				.isEqualTo(requester.getUserId());
		assertThat(projectMemberRepository.findByProjectIdAndUserId(project.getId(), requester.getUserId()))
				.isEmpty();
	}

	@Test
	void JWT가_없으면_401을_반환한다() throws Exception {
		mockMvc.perform(post("/projects/1/join-requests")
						.contentType(MediaType.APPLICATION_JSON)
						.content(validRequest(UUID.randomUUID().toString())))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH401_1"));
	}

	@Test
	void 잘못된_JWT면_401을_반환한다() throws Exception {
		mockMvc.perform(post("/projects/1/join-requests")
						.header("Authorization", "Bearer invalid-token")
						.contentType(MediaType.APPLICATION_JSON)
						.content(validRequest(UUID.randomUUID().toString())))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void X_User_Id를_위조해도_JWT_사용자로_참여_요청을_생성한다() throws Exception {
		User owner = saveUser("forged-request-owner@synq.com");
		User requester = saveUser("forged-request-user@synq.com");
		String token = UUID.randomUUID().toString();
		Project project = saveProjectWithOwner(owner, token, LocalDateTime.now().plusDays(7));

		mockMvc.perform(post("/projects/{projectId}/join-requests", project.getId())
						.header("Authorization", bearer(requester))
						.header("X-User-Id", owner.getUserId())
						.contentType(MediaType.APPLICATION_JSON)
						.content(validRequest(token)))
				.andExpect(status().isCreated());

		assertThat(participationRequestRepository.findAll())
				.singleElement()
				.extracting(request -> request.getUserId())
				.isEqualTo(requester.getUserId());
	}

	@Test
	void 역할_입력값이_잘못되면_400을_반환한다() throws Exception {
		User owner = saveUser("invalid-role-owner@synq.com");
		User requester = saveUser("invalid-role-user@synq.com");
		String token = UUID.randomUUID().toString();
		Project project = saveProjectWithOwner(owner, token, LocalDateTime.now().plusDays(7));

		mockMvc.perform(post("/projects/{projectId}/join-requests", project.getId())
						.header("Authorization", bearer(requester))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "inviteToken": "%s",
								  "settingSource": "DEFAULT",
								  "roleCategory": "ETC",
								  "detailRole": null,
								  "perspectives": []
								}
								""".formatted(token)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("PROJECT400_3"));
	}

	@Test
	void 존재하지_않는_프로젝트면_404를_반환한다() throws Exception {
		User requester = saveUser("controller-missing-project-user@synq.com");

		mockMvc.perform(post("/projects/999999/join-requests")
						.header("Authorization", bearer(requester))
						.contentType(MediaType.APPLICATION_JSON)
						.content(validRequest(UUID.randomUUID().toString())))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("PROJECT404_3"));
	}

	@Test
	void path_프로젝트와_초대_토큰의_프로젝트가_다르면_404를_반환한다() throws Exception {
		User owner = saveUser("controller-mismatch-owner@synq.com");
		User requester = saveUser("controller-mismatch-user@synq.com");
		Project pathProject = saveProjectWithOwner(
				owner,
				UUID.randomUUID().toString(),
				LocalDateTime.now().plusDays(7)
		);
		String otherToken = UUID.randomUUID().toString();
		saveProjectWithOwner(owner, otherToken, LocalDateTime.now().plusDays(7));

		mockMvc.perform(post("/projects/{projectId}/join-requests", pathProject.getId())
						.header("Authorization", bearer(requester))
						.contentType(MediaType.APPLICATION_JSON)
						.content(validRequest(otherToken)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("PROJECT404_5"));
	}

	@Test
	void 동일_사용자의_PENDING_요청이면_409를_반환한다() throws Exception {
		User owner = saveUser("controller-pending-owner@synq.com");
		User requester = saveUser("controller-pending-user@synq.com");
		String token = UUID.randomUUID().toString();
		Project project = saveProjectWithOwner(owner, token, LocalDateTime.now().plusDays(7));
		createRequest(project, requester, token);

		mockMvc.perform(post("/projects/{projectId}/join-requests", project.getId())
						.header("Authorization", bearer(requester))
						.contentType(MediaType.APPLICATION_JSON)
						.content(validRequest(token)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("PROJECT409_4"));
	}

	@Test
	void 만료된_초대_링크면_410을_반환한다() throws Exception {
		User owner = saveUser("controller-expired-request-owner@synq.com");
		User requester = saveUser("controller-expired-request-user@synq.com");
		String token = UUID.randomUUID().toString();
		Project project = saveProjectWithOwner(owner, token, LocalDateTime.now().minusSeconds(1));

		mockMvc.perform(post("/projects/{projectId}/join-requests", project.getId())
						.header("Authorization", bearer(requester))
						.contentType(MediaType.APPLICATION_JSON)
						.content(validRequest(token)))
				.andExpect(status().isGone())
				.andExpect(jsonPath("$.code").value("PROJECT410_1"));
	}

	@Test
	void Swagger에_참여_요청_생성과_Bearer_인증이_문서화된다() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/join-requests'].post").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/join-requests'].post.security[0].bearerAuth").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/join-requests'].post.responses['201']").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/join-requests'].post.responses['400']").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/join-requests'].post.responses['401']").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/join-requests'].post.responses['404']").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/join-requests'].post.responses['409']").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/join-requests'].post.responses['410']").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/join-requests'].post.responses['500']").exists());
	}

	private void createRequest(Project project, User requester, String token) throws Exception {
		mockMvc.perform(post("/projects/{projectId}/join-requests", project.getId())
						.header("Authorization", bearer(requester))
						.contentType(MediaType.APPLICATION_JSON)
						.content(validRequest(token)))
				.andExpect(status().isCreated());
	}

	private String validRequest(String token) {
		return """
				{
				  "inviteToken": "%s",
				  "settingSource": "DEFAULT",
				  "roleCategory": "DEV_TECH",
				  "detailRole": null,
				  "perspectives": ["TECH_RISK"]
				}
				""".formatted(token);
	}

	private Project saveProjectWithOwner(User owner, String token, LocalDateTime expiresAt) {
		Project project = Project.of(owner.getUserId(), "SynQ", "회의 협업 프로젝트");
		project.updateInvitation(token, expiresAt);
		projectRepository.save(project);
		projectMemberRepository.save(ProjectMember.of(project.getId(), owner.getUserId(), ProjectMemberRole.OWNER));
		return project;
	}

	private User saveUser(String email) {
		return userRepository.save(User.ofLocal("테스트", email, "password-hash"));
	}
}
