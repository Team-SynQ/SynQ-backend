package com.synq.backend.domain.user.service;

import com.synq.backend.domain.auth.jwt.AccessTokenBlacklistService;
import com.synq.backend.domain.auth.jwt.JwtProvider;
import com.synq.backend.domain.auth.service.AuthTokenService;
import com.synq.backend.domain.project.entity.ProjectMemberRole;
import com.synq.backend.domain.project.repository.ProjectMemberRepository;
import com.synq.backend.domain.project.repository.ProjectParticipationRequestRepository;
import com.synq.backend.domain.project.repository.ProjectRepository;
import com.synq.backend.domain.user.code.UserErrorCode;
import com.synq.backend.domain.user.dto.UserMeResponse;
import com.synq.backend.domain.user.entity.User;
import com.synq.backend.domain.user.repository.RoleProfileRepository;
import com.synq.backend.domain.user.repository.UserRepository;
import com.synq.backend.global.apipayload.code.GeneralErrorCode;
import com.synq.backend.global.apipayload.exception.GeneralException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class UserService {

	private final UserRepository userRepository;
	private final ProfileImageService profileImageService;
	private final ProjectRepository projectRepository;
	private final ProjectMemberRepository projectMemberRepository;
	private final ProjectParticipationRequestRepository participationRequestRepository;
	private final RoleProfileRepository roleProfileRepository;
	private final AccessTokenBlacklistService blacklistService;
	private final AuthTokenService authTokenService;
	private final JwtProvider jwtProvider;

	public UserService(
			UserRepository userRepository,
			ProfileImageService profileImageService,
			ProjectRepository projectRepository,
			ProjectMemberRepository projectMemberRepository,
			ProjectParticipationRequestRepository participationRequestRepository,
			RoleProfileRepository roleProfileRepository,
			AccessTokenBlacklistService blacklistService,
			AuthTokenService authTokenService,
			JwtProvider jwtProvider
	) {
		this.userRepository = userRepository;
		this.profileImageService = profileImageService;
		this.projectRepository = projectRepository;
		this.projectMemberRepository = projectMemberRepository;
		this.participationRequestRepository = participationRequestRepository;
		this.roleProfileRepository = roleProfileRepository;
		this.blacklistService = blacklistService;
		this.authTokenService = authTokenService;
		this.jwtProvider = jwtProvider;
	}

	@Transactional
	public UserMeResponse updateName(Long userId, String name) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new GeneralException(GeneralErrorCode.NOT_FOUND));
		user.updateName(name);
		return UserMeResponse.from(user, profileImageService.toUrl(user.getProfileImageKey()));
	}

	@Transactional
	public void withdraw(Long userId, String accessToken) {
		User user = userRepository.findActiveByIdForUpdate(userId)
				.orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));
		if (projectRepository.existsActiveByOwnerId(userId)) {
			throw new GeneralException(UserErrorCode.ACTIVE_PROJECT_OWNER_CANNOT_WITHDRAW);
		}

		projectMemberRepository.deleteAllByUserIdAndRole(userId, ProjectMemberRole.MEMBER);
		participationRequestRepository.deleteAllByUserId(userId);
		roleProfileRepository.deleteAllByUserId(userId);
		profileImageService.delete(userId);

		String withdrawnProviderId = "withdrawn:" + userId + ":" + UUID.randomUUID();
		user.withdraw(withdrawnProviderId);
		userRepository.flush();

		blacklistService.blacklist(accessToken, jwtProvider.getRemainingValidity(accessToken));
		authTokenService.revoke(userId);
	}
}
