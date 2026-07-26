package com.synq.backend.domain.meeting.service;

import com.synq.backend.domain.meeting.entity.Meeting;
import com.synq.backend.domain.meeting.entity.MeetingParticipant;

public record MeetingJoinResult(Meeting meeting, MeetingParticipant participant) {
}
