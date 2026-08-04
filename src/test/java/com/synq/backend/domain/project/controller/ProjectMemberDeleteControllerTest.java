package com.synq.backend.domain.project.controller;

import com.synq.backend.domain.project.entity.Project;
import com.synq.backend.domain.project.entity.ProjectMember;
import com.synq.backend.domain.project.entity.ProjectMemberRole;
import com.synq.backend.domain.project.repository.ProjectMemberRepository;
import com.synq.backend.domain.project.repository.ProjectRepository;
import com.synq.backend.domain.user.entity.User;
import com.synq.backend.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class ProjectMemberDeleteControllerTest extends ProjectControllerTestSupport {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private ProjectMemberRepository projectMemberRepository;

	@Autowired
	private UserRepository userRepository;

	@Test
	void OWNER가_멤버를_삭제하면_204와_빈_Body를_반환한다() throws Exception {
		User owner = saveUser("member-delete-controller-owner@synq.com");
		User member = saveUser("member-delete-controller-member@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);
		ProjectMember memberMembership = saveMember(project, member, ProjectMemberRole.MEMBER);

		mockMvc.perform(delete("/projects/{projectId}/members/{memberId}",
						project.getId(), memberMembership.getId())
						.header("Authorization", bearer(owner)))
				.andExpect(status().isNoContent())
				.andExpect(content().string(""));

		assertThat(projectMemberRepository.findById(memberMembership.getId())).isEmpty();
	}

	@Test
	void 프로젝트_OWNER_삭제_요청은_400을_반환한다() throws Exception {
		User owner = saveUser("member-delete-controller-self-owner@synq.com");
		Project project = saveProject(owner);
		ProjectMember ownerMembership = saveMember(project, owner, ProjectMemberRole.OWNER);

		mockMvc.perform(delete("/projects/{projectId}/members/{memberId}",
						project.getId(), ownerMembership.getId())
						.header("Authorization", bearer(owner)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("PROJECT400_1"));
	}

	@Test
	void OWNER가_아닌_사용자는_403을_반환한다() throws Exception {
		User owner = saveUser("member-delete-controller-forbidden-owner@synq.com");
		User member = saveUser("member-delete-controller-forbidden-member@synq.com");
		User target = saveUser("member-delete-controller-forbidden-target@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);
		saveMember(project, member, ProjectMemberRole.MEMBER);
		ProjectMember targetMembership = saveMember(project, target, ProjectMemberRole.MEMBER);

		mockMvc.perform(delete("/projects/{projectId}/members/{memberId}",
						project.getId(), targetMembership.getId())
						.header("Authorization", bearer(member))
						.header("X-User-Id", owner.getUserId()))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("PROJECT403_1"));
	}

	@Test
	void 프로젝트_외부_사용자는_403을_반환한다() throws Exception {
		User owner = saveUser("member-delete-controller-outsider-owner@synq.com");
		User target = saveUser("member-delete-controller-outsider-target@synq.com");
		User outsider = saveUser("member-delete-controller-outsider@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);
		ProjectMember targetMembership = saveMember(project, target, ProjectMemberRole.MEMBER);

		mockMvc.perform(delete("/projects/{projectId}/members/{memberId}",
						project.getId(), targetMembership.getId())
						.header("Authorization", bearer(outsider)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("PROJECT403_1"));
		assertThat(projectMemberRepository.findById(targetMembership.getId())).isPresent();
	}

	@Test
	void 다른_프로젝트의_멤버_ID는_404를_반환한다() throws Exception {
		User owner = saveUser("member-delete-controller-other-owner@synq.com");
		User otherOwner = saveUser("member-delete-controller-other-project-owner@synq.com");
		User member = saveUser("member-delete-controller-other-member@synq.com");
		Project project = saveProject(owner);
		Project otherProject = saveProject(otherOwner);
		saveMember(project, owner, ProjectMemberRole.OWNER);
		saveMember(otherProject, otherOwner, ProjectMemberRole.OWNER);
		ProjectMember otherMembership = saveMember(otherProject, member, ProjectMemberRole.MEMBER);

		mockMvc.perform(delete("/projects/{projectId}/members/{memberId}",
						project.getId(), otherMembership.getId())
						.header("Authorization", bearer(owner)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("PROJECT404_4"));
	}

	@Test
	void 존재하지_않는_삭제_대상은_404를_반환한다() throws Exception {
		User owner = saveUser("member-delete-controller-missing-member-owner@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);

		mockMvc.perform(delete("/projects/{projectId}/members/{memberId}",
						project.getId(), Long.MAX_VALUE)
						.header("Authorization", bearer(owner)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("PROJECT404_4"));
	}

	@Test
	void 이미_삭제된_멤버를_다시_삭제하면_404를_반환한다() throws Exception {
		User owner = saveUser("member-delete-controller-already-deleted-owner@synq.com");
		User member = saveUser("member-delete-controller-already-deleted-member@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);
		ProjectMember memberMembership = saveMember(project, member, ProjectMemberRole.MEMBER);

		mockMvc.perform(delete("/projects/{projectId}/members/{memberId}",
						project.getId(), memberMembership.getId())
						.header("Authorization", bearer(owner)))
				.andExpect(status().isNoContent());
		mockMvc.perform(delete("/projects/{projectId}/members/{memberId}",
						project.getId(), memberMembership.getId())
						.header("Authorization", bearer(owner)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("PROJECT404_4"));
	}

	@Test
	void 존재하지_않는_프로젝트는_404를_반환한다() throws Exception {
		User owner = saveUser("member-delete-controller-missing-owner@synq.com");

		mockMvc.perform(delete("/projects/{projectId}/members/{memberId}", Long.MAX_VALUE, 1L)
						.header("Authorization", bearer(owner)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("PROJECT404_3"));
	}

	@Test
	void 삭제된_프로젝트는_404를_반환한다() throws Exception {
		User owner = saveUser("member-delete-controller-deleted-owner@synq.com");
		User member = saveUser("member-delete-controller-deleted-member@synq.com");
		Project project = saveProject(owner);
		saveMember(project, owner, ProjectMemberRole.OWNER);
		ProjectMember memberMembership = saveMember(project, member, ProjectMemberRole.MEMBER);
		project.softDelete();
		projectRepository.flush();

		mockMvc.perform(delete("/projects/{projectId}/members/{memberId}",
						project.getId(), memberMembership.getId())
						.header("Authorization", bearer(owner)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("PROJECT404_3"));
	}

	@Test
	void JWT가_없거나_유효하지_않으면_401을_반환한다() throws Exception {
		mockMvc.perform(delete("/projects/{projectId}/members/{memberId}", 1L, 1L))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH401_1"));
		mockMvc.perform(delete("/projects/{projectId}/members/{memberId}", 1L, 1L)
						.header("Authorization", "Bearer invalid-token"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH401_1"));
	}

	@Test
	void Swagger에_프로젝트_멤버_삭제_API가_문서화된다() throws Exception {
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/members/{memberId}'].delete").exists())
				.andExpect(jsonPath("$.paths['/projects/{projectId}/members/{memberId}'].delete.security[0].bearerAuth").exists());
	}

	private Project saveProject(User owner) {
		return projectRepository.save(Project.of(owner.getUserId(), "SynQ", null));
	}

	private ProjectMember saveMember(Project project, User user, ProjectMemberRole role) {
		return projectMemberRepository.save(ProjectMember.of(project.getId(), user.getUserId(), role));
	}

	private User saveUser(String email) {
		return userRepository.save(User.ofLocal("테스트", email, "password-hash"));
	}
}
