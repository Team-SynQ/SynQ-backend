package com.synq.backend.domain.user.service;

import com.synq.backend.domain.user.code.UserErrorCode;
import com.synq.backend.domain.user.dto.RoleProfileRequest;
import com.synq.backend.domain.user.dto.RoleProfileResponse;
import com.synq.backend.domain.user.entity.Perspective;
import com.synq.backend.domain.user.entity.Role;
import com.synq.backend.domain.user.entity.RoleProfile;
import com.synq.backend.domain.user.entity.RoleProfilePerspective;
import com.synq.backend.domain.user.repository.RoleProfilePerspectiveRepository;
import com.synq.backend.domain.user.repository.RoleProfileRepository;
import com.synq.backend.global.apipayload.code.GeneralErrorCode;
import com.synq.backend.global.apipayload.exception.GeneralException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class RoleProfileService {

	private static final int MAX_PERSPECTIVES = 3;

	private final RoleProfileRepository roleProfileRepository;
	private final RoleProfilePerspectiveRepository perspectiveRepository;

	public RoleProfileService(RoleProfileRepository roleProfileRepository,
							RoleProfilePerspectiveRepository perspectiveRepository) {
		this.roleProfileRepository = roleProfileRepository;
		this.perspectiveRepository = perspectiveRepository;
	}

	public List<RoleProfileResponse> getMyRoleProfiles(Long userId) {
		return roleProfileRepository.findAllByUserIdOrderByCreatedAtAsc(userId).stream()
				.map(profile -> toResponse(profile, findPerspectives(profile.getId())))
				.toList();
	}

	@Transactional
	public RoleProfileResponse create(Long userId, RoleProfileRequest request) {
		validate(request);
		boolean isFirstProfile = !roleProfileRepository.existsByUserId(userId);
		RoleProfile profile;
		try {
			profile = roleProfileRepository.save(
					RoleProfile.of(userId, request.role(), request.detailRole(), isFirstProfile));
		} catch (DataIntegrityViolationException e) {
			throw new GeneralException(GeneralErrorCode.CONFLICT, e);
		}
		List<Perspective> perspectives = savePerspectives(profile.getId(), request.perspectives());
		return toResponse(profile, perspectives);
	}

	@Transactional
	public RoleProfileResponse update(Long userId, Long profileId, RoleProfileRequest request) {
		validate(request);
		RoleProfile profile = getOwnedProfile(userId, profileId);
		profile.update(request.role(), request.detailRole());
		perspectiveRepository.deleteAllByRoleProfileId(profileId);
		List<Perspective> perspectives = savePerspectives(profileId, request.perspectives());
		return toResponse(profile, perspectives);
	}

	@Transactional
	public void delete(Long userId, Long profileId) {
		RoleProfile profile = getOwnedProfile(userId, profileId);
		if (profile.isDefault()) {
			throw new GeneralException(UserErrorCode.CANNOT_DELETE_DEFAULT_ROLE_PROFILE);
		}
		perspectiveRepository.deleteAllByRoleProfileId(profileId);
		roleProfileRepository.delete(profile);
	}

	@Transactional
	public void setDefault(Long userId, Long profileId) {
		RoleProfile target = getOwnedProfile(userId, profileId);
		if (target.isDefault()) {
			return;
		}
		try {
			roleProfileRepository.findByUserIdAndIsDefaultTrue(userId).ifPresent(previous -> {
				previous.unmarkAsDefault();
				roleProfileRepository.saveAndFlush(previous);
			});
			target.markAsDefault();
			roleProfileRepository.saveAndFlush(target);
		} catch (DataIntegrityViolationException e) {
			throw new GeneralException(GeneralErrorCode.CONFLICT, e);
		}
	}

	private RoleProfile getOwnedProfile(Long userId, Long profileId) {
		return roleProfileRepository.findByIdAndUserId(profileId, userId)
				.orElseThrow(() -> new GeneralException(UserErrorCode.ROLE_PROFILE_NOT_FOUND));
	}

	private List<Perspective> savePerspectives(Long profileId, List<Perspective> perspectives) {
		List<Perspective> values = perspectives == null ? List.of() : perspectives;
		perspectiveRepository.saveAll(values.stream()
				.map(perspective -> RoleProfilePerspective.of(profileId, perspective))
				.toList());
		return values;
	}

	private List<Perspective> findPerspectives(Long profileId) {
		return perspectiveRepository.findAllByRoleProfileId(profileId).stream()
				.map(RoleProfilePerspective::getPerspective)
				.toList();
	}

	private void validate(RoleProfileRequest request) {
		if (request.role() == Role.ETC && !StringUtils.hasText(request.detailRole())) {
			throw new GeneralException(UserErrorCode.DETAIL_ROLE_REQUIRED);
		}
		if (request.perspectives() != null && request.perspectives().size() > MAX_PERSPECTIVES) {
			throw new GeneralException(UserErrorCode.TOO_MANY_PERSPECTIVES);
		}
	}

	private RoleProfileResponse toResponse(RoleProfile profile, List<Perspective> perspectives) {
		return new RoleProfileResponse(profile.getId(), profile.isDefault(), profile.getRole(),
				profile.getDetailRole(), perspectives);
	}
}
