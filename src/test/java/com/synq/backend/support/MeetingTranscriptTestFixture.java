package com.synq.backend.support;

import com.synq.backend.domain.meeting.entity.Meeting;
import com.synq.backend.domain.meeting.repository.MeetingRepository;
import com.synq.backend.domain.project.entity.Project;
import com.synq.backend.domain.project.repository.ProjectRepository;
import com.synq.backend.domain.transcript.entity.TranscriptSegment;
import com.synq.backend.domain.transcript.repository.TranscriptSegmentRepository;
import com.synq.backend.domain.user.entity.User;
import com.synq.backend.domain.user.repository.UserRepository;

import java.util.UUID;

/**
 * 전사 인덱싱 테스트용 회의/세그먼트를 만든다.
 *
 * ReferenceMaterialTestFixture 와 같은 방식으로 매번 새 유저/프로젝트를 만든다.
 * 검색 스코프가 프로젝트라 픽스처를 공유하면 테스트끼리 결과가 섞인다.
 */
public class MeetingTranscriptTestFixture {

	private final UserRepository userRepository;
	private final ProjectRepository projectRepository;
	private final MeetingRepository meetingRepository;
	private final TranscriptSegmentRepository transcriptSegmentRepository;

	public MeetingTranscriptTestFixture(
			UserRepository userRepository,
			ProjectRepository projectRepository,
			MeetingRepository meetingRepository,
			TranscriptSegmentRepository transcriptSegmentRepository
	) {
		this.userRepository = userRepository;
		this.projectRepository = projectRepository;
		this.meetingRepository = meetingRepository;
		this.transcriptSegmentRepository = transcriptSegmentRepository;
	}

	public Fixture create() {
		String identifier = UUID.randomUUID().toString();
		User host = userRepository.save(
				User.ofLocal("전사 인덱싱 테스트", identifier + "@synq.com", "password-hash"));
		Project project = projectRepository.save(
				Project.of(host.getUserId(), "전사 인덱싱 테스트 프로젝트", null));
		Meeting meeting = meetingRepository.save(Meeting.of(project.getId(), "테스트 회의"));
		return new Fixture(meeting.getId(), project.getId(), host.getUserId());
	}

	/**
	 * 같은 프로젝트에 회의를 하나 더 만든다. 프로젝트 단위 검색 테스트에 쓴다.
	 * 프로젝트당 IN_PROGRESS 회의는 하나만 허용되므로(uq_meeting_project_active), 종료된 상태로 만든다 —
	 * 어차피 "이전 회의" 성격이라 검색 로직은 상태를 신경 쓰지 않는다.
	 */
	public Long createMeeting(Fixture fixture) {
		Meeting meeting = Meeting.of(fixture.projectId(), "테스트 회의 추가");
		meeting.end();
		return meetingRepository.save(meeting).getId();
	}

	/** sequenceIndex 와 startMs 를 순서대로 채워 확정 세그먼트를 저장한다. */
	public void saveSegments(Long meetingId, String... contents) {
		for (int i = 0; i < contents.length; i++) {
			transcriptSegmentRepository.save(
					TranscriptSegment.of(meetingId, i, i * 1000, (i + 1) * 1000, contents[i]));
		}
		transcriptSegmentRepository.flush();
	}

	public record Fixture(Long meetingId, Long projectId, Long hostId) {
	}
}
