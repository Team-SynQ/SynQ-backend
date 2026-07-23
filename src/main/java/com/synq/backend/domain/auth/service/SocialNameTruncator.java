package com.synq.backend.domain.auth.service;

final class SocialNameTruncator {

	private SocialNameTruncator() {
	}
    
	static String truncate(String value, int maxCodePoints) {
		if (value.codePointCount(0, value.length()) <= maxCodePoints) {
			return value;
		}
		int endIndex = value.offsetByCodePoints(0, maxCodePoints);
		return value.substring(0, endIndex);
	}
}
