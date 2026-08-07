package com.synq.backend.domain.ai.rag;

import com.synq.backend.domain.ai.rag.entity.MeetingTranscriptChunk;
import com.synq.backend.domain.ai.rag.repository.MeetingTranscriptChunkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TranscriptChunkWriter {

	private final MeetingTranscriptChunkRepository repository;

	/** 기존 청크를 지우고 새 청크로 교체한다. 한 트랜잭션이라 중간 실패 시 전부 롤백된다. */
	@Transactional
	public void replace(Long meetingId, List<MeetingTranscriptChunk> chunks) {
		repository.deleteByMeetingId(meetingId);
		// flush 하지 않으면 삭제가 INSERT 뒤로 밀려 UNIQUE(meeting_id, chunk_index) 를 위반한다.
		repository.flush();
		repository.saveAll(chunks);
	}

	/** 실패 처리 시 잔여 청크 제거. */
	@Transactional
	public void deleteAll(Long meetingId) {
		repository.deleteByMeetingId(meetingId);
	}
}
